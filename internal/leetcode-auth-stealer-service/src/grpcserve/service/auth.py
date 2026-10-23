import asyncio
from collections.abc import Callable

import grpc
import structlog

from config import Config
from generated import (
    LeetcodeAuthStealerService_pb2,
    LeetcodeAuthStealerService_pb2_grpc,
)
from prom import AuthFailureReason, record_auth_failure, record_auth_success
from provider.camou import CamouProvider
from provider.dto import (
    AuthenticationTimeout,
    AuthenticationUnavailable,
    BadCredentials,
    Credentials,
    ManualVerificationRequired,
)

logger = structlog.get_logger()


class AuthService(
    LeetcodeAuthStealerService_pb2_grpc.LeetcodeAuthStealerServiceServicer
):
    def __init__(
        self,
        c: Config,
        provider: CamouProvider,
        on_failure: Callable[[], None],
    ) -> None:
        self.provider = provider
        self.timeout_seconds = c.auth_timeout_seconds
        self.on_failure = on_failure

    def _fail(self, reason: AuthFailureReason) -> None:
        record_auth_failure(reason)

        logger.warning(
            "Authentication failed, marking service as unalive", reason=reason
        )
        self.on_failure()

    async def Authenticate(
        self,
        request: LeetcodeAuthStealerService_pb2.AuthenticateRequest,
        context: grpc.aio.ServicerContext,
    ):
        if not request.login.strip() or not request.password:
            record_auth_failure("invalid_argument")
            await context.abort(
                grpc.StatusCode.INVALID_ARGUMENT, "Login and password are required"
            )

        try:
            async with asyncio.timeout(self.timeout_seconds):
                cookies = await self.provider.authenticate(
                    Credentials(request.login, request.password)
                )
        except BadCredentials:
            self._fail("bad_credentials")
            await context.abort(
                grpc.StatusCode.UNAUTHENTICATED, "Invalid LeetCode credentials"
            )
        except ManualVerificationRequired:
            self._fail("manual_verification_required")
            await context.abort(
                grpc.StatusCode.FAILED_PRECONDITION,
                "Manual two-step verification is required",
            )
        except AuthenticationUnavailable:
            self._fail("authentication_unavailable")
            await context.abort(
                grpc.StatusCode.UNAVAILABLE, "LeetCode authentication is unavailable"
            )
        except AuthenticationTimeout, TimeoutError:
            self._fail("timeout")
            await context.abort(
                grpc.StatusCode.DEADLINE_EXCEEDED, "LeetCode authentication timed out"
            )
        except asyncio.CancelledError:
            self._fail("cancelled")
            raise
        except Exception:
            self._fail("internal_error")
            logger.exception("Authenticate failed")
            await context.abort(grpc.StatusCode.INTERNAL, "Authenticate failed")

        if not cookies.leetcode_session or not cookies.csrf_token:
            self._fail("incomplete_authentication")
            await context.abort(
                grpc.StatusCode.INTERNAL, "Provider returned incomplete authentication"
            )

        response = LeetcodeAuthStealerService_pb2.AuthenticateResponse(
            leetcode_session=cookies.leetcode_session,
            csrf_token=cookies.csrf_token,
        )

        record_auth_success()
        return response
