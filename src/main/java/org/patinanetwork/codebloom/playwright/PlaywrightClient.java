package org.patinanetwork.codebloom.playwright;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.email.client.github.GithubOAuthEmailClient;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlaywrightClient {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; U; Android 4.4.1; SAMSUNG SM-J210G Build/KTU84P) AppleWebKit/536.31 (KHTML, like Gecko) Chrome/48.0.2090.359 Mobile Safari/601.9";

    private final ServerUrlUtils serverUrlUtils;
    private final PlaywrightProvider playwrightProvider;

    public PlaywrightClient(
            final ServerUrlUtils serverUrlUtils,
            final GithubOAuthEmailClient githubOAuthEmailClient,
            final PlaywrightProvider playwrightProvider) {
        this.serverUrlUtils = serverUrlUtils;
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
}
