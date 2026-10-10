package org.patinanetwork.codebloom.playwright;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.email.EmailClient;
import org.patinanetwork.codebloom.common.email.Message;
import org.patinanetwork.codebloom.common.email.client.github.GithubOAuthEmailClient;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.patinanetwork.codebloom.scheduled.auth.CodeExtractor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlaywrightClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; U; Android 4.4.1; SAMSUNG SM-J210G Build/KTU84P) AppleWebKit/536.31 (KHTML, like Gecko) Chrome/48.0.2090.359 Mobile Safari/601.9";

    private final ServerUrlUtils serverUrlUtils;
    private final EmailClient emailClient;
    private final PlaywrightProvider playwrightProvider;

    public PlaywrightClient(
            final ServerUrlUtils serverUrlUtils,
            final GithubOAuthEmailClient githubOAuthEmailClient,
            final PlaywrightProvider playwrightProvider) {
        this.serverUrlUtils = serverUrlUtils;
        this.emailClient = githubOAuthEmailClient;
        this.playwrightProvider = playwrightProvider;
    }

    private <T> T withPage(final Function<Page, T> consumer) {
        return playwrightProvider.withPage(USER_AGENT, consumer);
    }

    public byte[] getCodebloomLeaderboardScreenshot(final int page, final Tag tag) {
        log.info("Loading page {} for screenshot...", page);
        return withPage(p -> {
            p.navigate("%s/leaderboard?%s=true&page=%s"
                    .formatted(serverUrlUtils.getUrl(), tag.name().toLowerCase(), page));
            p.waitForTimeout(5_000);
            byte[] screenshot = p.screenshot(
                    new Page.ScreenshotOptions().setType(ScreenshotType.PNG).setFullPage(true));
            return screenshot;
        });
    }

    public Optional<Auth> getLeetcodeCookie(String githubUsername, String githubPassword) {
        return withPage((p) -> {
            p.navigate("https://leetcode.com/accounts/github/login/?next=%2F");

            log.info("Navigated to leetcode.com login");

            p.waitForLoadState(LoadState.NETWORKIDLE);

            p.fill("#login_field", githubUsername);
            p.fill("#password", githubPassword);

            log.info("Filled in credentials, clicking login...");

            p.click("input[name=\"commit\"]");

            if (p.isVisible("#device-verification-prompt") || p.isVisible("#session-otp-input-description")) {
                log.info("2FA Required");
                List<Message> messages;

                p.waitForTimeout(10000);

                try {
                    messages = emailClient.getPastMessages();
                } catch (Exception e) {
                    log.info("Failed to retrieve past messages");
                    throw new RuntimeException("Failed to retrieve past messages", e);
                }

                Message target = null;
                for (Message m : messages) {
                    // Don't break so we can get the newest available code.
                    if (m.getSubject().equals("[GitHub] Please verify your device")) {
                        log.info("Found verification email");
                        target = m;
                    }
                }

                if (target == null) {
                    throw new RuntimeException(
                            "Something went wrong when parsing the inbox. Manual intervention required");
                }

                String code = CodeExtractor.extractCode(target.getMessage());

                log.info("Found code in email, will fill now...");

                if (code == null) {
                    throw new RuntimeException("Code was not found in the email. Manual intervention required.");
                }

                p.fill("input[name='otp']", code);

                log.info("Page filled!");
            }

            if (p.isVisible("button[name=\"authorize\"]")) {
                List<ElementHandle> buttons = p.querySelectorAll("button[name=\"authorize\"]");
                if (buttons.size() > 1) {
                    // Click the second button (which is the actual authorize button)
                    buttons.get(1).click();
                }
                log.info("Authorization button clicked");
            }

            p.waitForURL("https://leetcode.com/");

            log.info("Back to leetcode.com!");

            if (p.url().equals("https://leetcode.com/")) {
                var cookieMap = p.context().cookies().stream()
                        .filter(cookie -> cookie.name.equals("LEETCODE_SESSION") || cookie.name.equals("csrftoken"))
                        .collect(Collectors.toMap(cookie -> cookie.name, cookie -> cookie.value));

                String sessionToken = cookieMap.get("LEETCODE_SESSION");
                String csrf = cookieMap.get("csrftoken");
                if (sessionToken != null) {
                    log.info("Cookie found!");
                    return Optional.of(Auth.builder()
                            .token(sessionToken)
                            .csrf(Optional.ofNullable(csrf))
                            .build());
                }
            } else {
                log.info("Should be authenticated but not authenticated.");
            }

            return Optional.empty();
        });
    }
}
