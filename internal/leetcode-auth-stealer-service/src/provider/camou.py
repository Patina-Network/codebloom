"""Ephemeral LeetCode email login using a standalone InvisiblePlaywright browser."""

import asyncio
import json
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from dataclasses import dataclass
from typing import Any
from urllib.parse import urlsplit

import structlog
from camoufox.async_api import AsyncCamoufox
from playwright.async_api import Browser, BrowserContext, Page, Response, Route
from playwright.async_api import Error as BrowserError
from playwright.async_api import TimeoutError as BrowserTimeout
from pydantic import BaseModel, Field, ValidationError

from config import Config
from provider.dto import (
    AuthCookies,
    AuthenticationTimeout,
    AuthenticationUnavailable,
    BadCredentials,
    Credentials,
    ManualVerificationRequired,
)

logger: structlog.stdlib.BoundLogger = structlog.get_logger()


def _log_decision(event: str, **kwargs) -> None:
    """Helper to log decision points with structured context."""
    logger.info(event, **kwargs)


LOGIN_URL = "https://leetcode.com/accounts/login/"
HOME_URL = "https://leetcode.com/"
GRAPHQL_URL = "https://leetcode.com/graphql/"
TOKEN_SELECTOR = (
    'input[name="cf-turnstile-response"], input[name="cf_challenge_response"]'
)
WIDGET_SELECTOR = '.cf-turnstile, iframe[src^="https://challenges.cloudflare.com/"]'
SPINNING_INDICATOR_SELECTOR = '.spinner, .loading, [class*="spin"], [class*="load"]'


@asynccontextmanager
async def _browser_session(c: Config) -> AsyncIterator[Browser]:
    _log_decision(
        "browser_session_start",
        environment=c.environment,
        disable_virtual=c.disable_virtual,
        auth_timeout_seconds=c.auth_timeout_seconds,
    )
    headless = (
        False
        if c.environment == "local"
        else (False if c.disable_virtual == True else "virtual")
    )
    _log_decision(
        "browser_session_headless_config",
        environment=c.environment,
        disable_virtual=c.disable_virtual,
        headless_mode=headless,
    )

    async with AsyncCamoufox(
        headless=headless,
        humanize=True,
        geoip=True,
        disable_coop=True,
        i_know_what_im_doing=True,
    ) as browser:
        _log_decision("browser_session_created", headless=headless)
        assert isinstance(browser, Browser)
        yield browser
        _log_decision("browser_session_closed", headless=headless)


class _LoginForm(BaseModel):
    errors: dict[str, Any] | list[Any] = Field(default_factory=list)


class _LoginResponse(BaseModel):
    requires_two_step_verification: bool = Field(
        default=False, alias="requiresTwoStepVerification", strict=True
    )
    form: _LoginForm | None = None


def _check_rejection(body: str) -> None:
    _log_decision(
        "check_rejection_start",
        body_length=len(body),
        body_preview=body[:200] if len(body) > 200 else body,
    )
    try:
        response = _LoginResponse.model_validate_json(body)
        _log_decision("check_rejection_json_parsed", response=response)
    except ValidationError:
        logger.exception(f"validation failed for body {body}")
        _log_decision(
            "check_rejection_validation_error", body=body, exception="ValidationError"
        )
        raise AuthenticationUnavailable()

    if response.requires_two_step_verification:
        _log_decision(
            "check_rejection_2fa_required",
            requires_two_step_verification=True,
            body=body,
        )
        raise ManualVerificationRequired()

    if response.form and response.form.errors:
        error_text = json.dumps(response.form.errors).lower()
        _log_decision(
            "check_rejection_form_errors",
            errors=response.form.errors,
            error_text=error_text,
        )
        if any(word in error_text for word in ("captcha", "turnstile", "challenge")):
            _log_decision(
                "check_rejection_challenge_error",
                error_text=error_text,
                exception="AuthenticationUnavailable",
            )
            raise AuthenticationUnavailable()
        _log_decision(
            "check_rejection_bad_credentials",
            error_text=error_text,
            exception="BadCredentials",
        )
        raise BadCredentials()
    _log_decision("check_rejection_passed")


def _auth_cookies(cookies) -> AuthCookies | None:
    _log_decision("auth_cookies_start", cookies_count=len(cookies))
    values = {}
    for cookie in cookies:
        domain = cookie.get("domain")
        if domain not in ("leetcode.com", ".leetcode.com"):
            _log_decision(
                "auth_cookies_skipping_domain",
                domain=domain,
                reason="not_leetcode_domain",
            )
            continue
        name = cookie.get("name")
        if name in ("LEETCODE_SESSION", "csrftoken"):
            values[name] = cookie.get("value")
            _log_decision(
                "auth_cookies_collected",
                cookie_name=name,
                has_value=bool(cookie.get("value")),
            )
    if values.get("LEETCODE_SESSION") and values.get("csrftoken"):
        _log_decision("auth_cookies_success", cookies_count=len(values))
        return AuthCookies(values["LEETCODE_SESSION"], values["csrftoken"])
    _log_decision(
        "auth_cookies_missing",
        has_session="LEETCODE_SESSION" in values,
        has_csrftoken="csrftoken" in values,
    )
    return None


async def _wait_for_spinners(page: Page) -> None:
    spinners = page.locator(SPINNING_INDICATOR_SELECTOR)
    while await spinners.count():
        await asyncio.sleep(0.5)


async def _has_challenge_token(page: Page) -> bool:
    _log_decision("_has_challenge_token_start")
    tokens = page.locator(TOKEN_SELECTOR)

    for index in range(await tokens.count()):
        if await tokens.nth(index).input_value():
            _log_decision("_has_challenge_token_found", index=index)
            return True

    _log_decision("_has_challenge_token_not_found")
    return False


async def _try_click_challenge(page: Page) -> bool:
    """Find a visible challenge and click on it's checkbox area"""

    def valid_box(box) -> bool:
        return box["width"] >= 180 and 45 <= box["height"] <= 150

    _log_decision("_try_click_challenge_start")
    tokens = page.locator(TOKEN_SELECTOR)

    for index in range(await tokens.count()):
        token = tokens.nth(index)

        if await token.input_value():
            _log_decision("_try_click_challenge_token_already_filled", index=index)
            continue

        ancestor = token

        for _ in range(4):
            ancestor = ancestor.locator("..")
            box = await ancestor.bounding_box()

            if box and valid_box(box) and box["width"] <= 800:
                _log_decision(
                    "_try_click_challenge_clicking_token",
                    index=index,
                    box_x=box["x"],
                    box_y=box["y"],
                    box_width=box["width"],
                    box_height=box["height"],
                )
                await page.mouse.click(
                    box["x"] + 30,
                    box["y"] + box["height"] / 2,
                )
                _log_decision("_try_click_challenge_success", index=index)
                return True

    widgets = page.locator(WIDGET_SELECTOR)

    for index in range(await widgets.count()):
        box = await widgets.nth(index).bounding_box()

        if box and valid_box(box):
            _log_decision(
                "_try_click_challenge_clicking_widget",
                index=index,
                box_x=box["x"],
                box_y=box["y"],
                box_width=box["width"],
                box_height=box["height"],
            )
            await page.mouse.click(
                box["x"] + 30,
                box["y"] + box["height"] / 2,
            )
            _log_decision("_try_click_challenge_widget_success", index=index)
            return True

    _log_decision("_try_click_challenge_failed_no_valid_target")
    return False


async def _wait_for_form(page: Page):
    login = page.locator("#id_login")
    spinners = page.locator(SPINNING_INDICATOR_SELECTOR)
    seen_spinner = False

    _log_decision(
        "_wait_for_form_start",
        login_visible_before_wait=await login.is_visible(),
        spinner_count_before_wait=await spinners.count(),
    )

    try:
        try:
            _log_decision("_wait_for_form_waiting_networkidle")
            await page.wait_for_load_state("networkidle")
            _log_decision("_wait_for_form_networkidle_reached")
        except BrowserTimeout:
            _log_decision("_wait_for_form_networkidle_timeout")
            raise

        _log_decision("_wait_for_form_sleeping_3_seconds")
        await asyncio.sleep(3)

        # Wait for spinning indicators to disappear
        spinner_count = await spinners.count()
        _log_decision("_wait_for_form_waiting_spinners", spinner_count=spinner_count)
        while await spinners.count():
            seen_spinner = True
            _log_decision("_wait_for_form_spinner_sleep", spinner_count=spinner_count)
            await asyncio.sleep(1)
            spinner_count = await spinners.count()
            if await login.is_visible():
                _log_decision("_wait_for_form_login_became_visible")
                break

        _log_decision("_wait_for_form_attempting_click_challenges")
        while True:
            spinner_count = await spinners.count()

            if spinner_count:
                seen_spinner = True

            challenge_complete = await _has_challenge_token(page)

            if (
                await login.is_visible()
                and not spinner_count
                and (seen_spinner or challenge_complete)
            ):
                _log_decision("_wait_for_form_success")
                return

            _log_decision("_wait_for_form_trying_click_challenge")
            await _try_click_challenge(page)
            _log_decision("_wait_for_form_post_click_sleep")
            await asyncio.sleep(5)

    except TimeoutError:
        logger.error("waiting for form timed out")
        _log_decision(
            "_wait_for_form_timeout",
            exception="TimeoutError",
        )
        raise AuthenticationUnavailable()


async def _complete_form_challenge(page: Page, max_clicks: int) -> None:
    """Complete form challenge with extended waiting for slow VM conditions."""
    _log_decision(
        "_complete_form_challenge_start",
        max_clicks=max_clicks,
    )
    absent_since = None
    next_click = 0
    clicks = 0

    widgets = page.locator(WIDGET_SELECTOR)

    try:
        while True:
            now = asyncio.get_running_loop().time()
            tokens = page.locator(TOKEN_SELECTOR)
            token_count = await tokens.count()
            spinners = page.locator(SPINNING_INDICATOR_SELECTOR)
            spinning_count = await spinners.count()

            _log_decision(
                "_complete_form_challenge_loop_iteration",
                token_count=token_count,
                spinning_count=spinning_count,
                clicks=clicks,
                max_clicks=max_clicks,
                absent_since=absent_since,
            )

            if token_count:
                _log_decision("_complete_form_challenge_token_present")
                absent_since = None

                if await _has_challenge_token(page):
                    _log_decision("_complete_form_challenge_token_filled")
                    await _wait_for_spinners(page)
                    _log_decision("_complete_form_challenge_success")
                    return

            elif await widgets.count():  # challenge still present
                _log_decision("_complete_form_challenge_widget_present")
                absent_since = None

            elif spinning_count > 0:  # spinning indicator present
                _log_decision(
                    "_complete_form_challenge_spinning",
                    spinning_count=spinning_count,
                )
                absent_since = None
                await asyncio.sleep(1)
                continue

            else:  # challenge absent and no spinning
                _log_decision("_complete_form_challenge_challenge_absent")
                absent_since = now if absent_since is None else absent_since
                if now - absent_since >= 2:  # absence confirmed
                    _log_decision("_complete_form_challenge_absence_confirmed")
                    return

            if clicks < max_clicks and now >= next_click:  # click retry eligible
                _log_decision("_complete_form_challenge_checking_click_eligible")
                if await _try_click_challenge(page):
                    clicks += 1
                    _log_decision(
                        "_complete_form_challenge_click_performed", clicks=clicks
                    )
                next_click = now + 1

            await asyncio.sleep(0.4)

    except TimeoutError:  # Challenge timed out
        logger.exception("complete form challenge timed out")
        _log_decision(
            "_complete_form_challenge_timeout",
            clicks=clicks,
            max_clicks=max_clicks,
            exception="TimeoutError",
        )
        raise AuthenticationUnavailable()


def _is_login_response(response: Response) -> bool:
    result = (
        urlsplit(response.url).path.rstrip("/") == "/accounts/login"
        and response.request.method == "POST"
    )
    _log_decision(
        "_is_login_response_check",
        response_url=response.url,
        path=urlsplit(response.url).path.rstrip("/"),
        method=response.request.method,
        is_login_response=result,
    )
    return result


# TODO: This works BEAUTIFULLY, but keep an eye on this if it's being too much of a hindrance
async def _block_assets(route: Route):
    if route.request.resource_type in ["image", "media", "font"]:
        _log_decision(
            "_block_assets_aborting", resource_type=route.request.resource_type
        )
        await route.abort()
    else:
        _log_decision(
            "_block_assets_continuing", resource_type=route.request.resource_type
        )
        await route.continue_()


@dataclass(repr=True)
class CamouProvider:
    c: Config

    async def authenticate(self, credentials: Credentials) -> AuthCookies:
        _log_decision(
            "authenticate_start",
            login=credentials.login,
            auth_timeout_seconds=self.c.auth_timeout_seconds,
        )
        try:
            _log_decision(
                "authenticate_timeout_context_start",
                auth_timeout_seconds=self.c.auth_timeout_seconds,
            )
            async with asyncio.timeout(self.c.auth_timeout_seconds):
                _log_decision("authenticate_browser_session_start")
                async with _browser_session(self.c) as browser:
                    _log_decision("authenticate_browser_session_obtained")
                    context = await browser.new_context()
                    _log_decision("authenticate_browser_context_created")
                    try:
                        _log_decision("authenticate_calling_login")
                        return await self._login(context, credentials)
                    finally:
                        _log_decision("authenticate_closing_browser_context")
                        await context.close()
        except TimeoutError, BrowserTimeout:
            _log_decision(
                "authenticate_timeout",
                exception="TimeoutError or BrowserTimeout",
                auth_timeout_seconds=self.c.auth_timeout_seconds,
            )
            raise AuthenticationTimeout()
        except BrowserError, OSError:
            _log_decision(
                "authenticate_browser_error",
                exception="BrowserError or OSError",
            )
            raise AuthenticationUnavailable()

        _log_decision(
            "authenticate_returning_empty_cookies", reason="unexpected_code_path"
        )
        return AuthCookies()

    async def _login(self, context: BrowserContext, credentials: Credentials):
        page: Page | None = None

        _log_decision(
            "login_start",
            login=credentials.login,
        )
        try:
            _log_decision("login_creating_page")
            page = await context.new_page()
            _log_decision(
                "login_page_configured",
            )
            # await page.route("**/*", _block_assets)
            page.on(
                "console",
                lambda msg: logger.info(
                    "browser console",
                    type=msg.type,
                    text=msg.text,
                ),
            )

            page.on(
                "requestfailed",
                lambda request: logger.warning(
                    "request failed",
                    url=request.url,
                    failure=request.failure,
                ),
            )

            page.on(
                "response",
                lambda response: (
                    logger.info(
                        "cloudflare response",
                        url=response.url,
                        status=response.status,
                    )
                    if "challenges.cloudflare.com" in response.url
                    else None
                ),
            )

            _log_decision(
                "login_navigating_to_login_url",
                url=LOGIN_URL,
                wait_until="domcontentloaded",
            )
            await page.goto(
                LOGIN_URL,
                wait_until="domcontentloaded",
            )
            _log_decision(
                "login_navigated_to_login_url",
                url=page.url,
                status="success",
            )

            _log_decision(
                "login_waiting_for_form",
            )
            await _wait_for_form(page)
            _log_decision("login_form_visible")

            _log_decision("login_filling_credentials", login=credentials.login)
            await page.locator("#id_login").fill(credentials.login)
            await page.locator("#id_password").fill(credentials.password)
            _log_decision("login_credentials_filled", login=credentials.login)

            _log_decision(
                "login_completing_form_challenge",
                max_clicks=self.c.max_challenge_try_clicks,
            )
            await _complete_form_challenge(
                page,
                self.c.max_challenge_try_clicks,
            )
            _log_decision("login_form_challenge_completed")

            _log_decision(
                "login_waiting_for_login_response",
            )
            async with page.expect_response(_is_login_response) as pending:
                _log_decision("login_clicking_signin_button")
                await page.locator("#signin_btn").click()
                _log_decision("login_signin_button_clicked")

            response = await pending.value
            _log_decision(
                "login_login_response_received",
                url=response.url,
                status=response.status,
                method=response.request.method,
            )
            try:
                _log_decision("login_reading_response_body")
                body = await response.text()
                _log_decision("login_response_body_read", body_length=len(body))
            except BrowserError:
                # Firefox can discard a successful POST body during its redirect.
                # Continue checking for a homepage redirect and auth cookies.
                _log_decision(
                    "login_response_body_read_failed",
                    reason="BrowserError (Firefox redirect behavior)",
                )
                # Continue checking for a homepage redirect and auth cookies.
                body = None

            if body is not None:
                _log_decision("login_checking_rejection")
                _check_rejection(body)
                _log_decision("login_check_rejection_passed")

            if response.status >= 400:
                _log_decision(
                    "login_error_response",
                    status=response.status,
                    exception="AuthenticationUnavailable",
                )
                logger.exception(f"response status is {response.status}")
                raise AuthenticationUnavailable()

            _log_decision(
                "login_starting_cookie_poll",
            )
            try:
                _log_decision("login_cookie_poll_loop_start")
                while True:
                    path = urlsplit(page.url).path
                    _log_decision(
                        "login_cookie_poll_iteration",
                        current_path=path,
                    )
                    if path != "/":
                        _log_decision(
                            "login_not_homepage",
                            current_path=path,
                        )
                        if any(
                            marker in path
                            for marker in ("two-factor", "two_factor", "two-step")
                        ):
                            _log_decision(
                                "login_two_factor_detected",
                                path=path,
                                exception="ManualVerificationRequired",
                            )
                            raise ManualVerificationRequired()
                    elif (
                        cookies := _auth_cookies(await context.cookies([HOME_URL]))
                    ) is not None:
                        _log_decision(
                            "login_auth_cookies_success",
                            session_cookie_present=True,
                            csrftoken_present=True,
                        )
                        return cookies
                    _log_decision("login_cookie_poll_sleep")
                    await asyncio.sleep(0.5)
            except TimeoutError:
                _log_decision(
                    "login_cookie_poll_timeout",
                    exception="TimeoutError",
                )
                logger.exception(
                    "200 resp but timed out while trying to get auth cookies"
                )
                raise AuthenticationUnavailable()
        except Exception:
            _log_decision(
                "login_unhandled_exception",
                page_url=page.url if page else None,
                exception="Exception",
            )
            if page:
                pass
                # unused
                # print(await page.content())
            raise
