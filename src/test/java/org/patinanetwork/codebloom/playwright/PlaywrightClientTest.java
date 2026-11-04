package org.patinanetwork.codebloom.playwright;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.usertag.Tag;
import org.patinanetwork.codebloom.common.url.ServerUrlUtils;

public class PlaywrightClientTest {

    private ServerUrlUtils serverUrlUtils = mock(ServerUrlUtils.class);
    private PlaywrightProvider playwrightProvider = mock(PlaywrightProvider.class);

    private Page page = mock(Page.class);

    private PlaywrightClient playwrightClient;

    @BeforeEach
    public void setup() {
        playwrightClient = new PlaywrightClient(serverUrlUtils, playwrightProvider);

        when(playwrightProvider.withPage(anyString(), any())).thenAnswer(p -> {
            Function<Page, Object> fn = p.getArgument(1);
            return fn.apply(page);
        });

        byte[] screenshot = new byte[] {1, 2, 3};
        when(page.screenshot(any(ScreenshotOptions.class))).thenReturn(screenshot);
    }

    @Test
    void testGetCodebloomLeaderboardScreenshotSuccess() {
        when(serverUrlUtils.getUrl()).thenReturn("https://stg.codebloom.patinanetwork.org");

        byte[] expected = new byte[] {1, 2, 3};
        when(page.screenshot(any(ScreenshotOptions.class))).thenReturn(expected);

        byte[] screenshot = playwrightClient.getCodebloomLeaderboardScreenshot(1, Tag.Rpi);

        assertNotNull(screenshot, "Screenshot should not be null");
        assertTrue(screenshot.length > 0, "Screenshot should have content");

        verify(page).navigate("https://stg.codebloom.patinanetwork.org/leaderboard?rpi=true&page=1");
        verify(serverUrlUtils).getUrl();
    }

    @Test
    void testGetCodebloomLeaderboardScreenshotDifferentPage() {
        when(serverUrlUtils.getUrl()).thenReturn("https://stg.codebloom.patinanetwork.org");

        byte[] screenshot = playwrightClient.getCodebloomLeaderboardScreenshot(2, Tag.Baruch);

        assertNotNull(screenshot, "Screenshot should not be null");
        assertTrue(screenshot.length > 0, "Screenshot should have content");

        verify(page).navigate("https://stg.codebloom.patinanetwork.org/leaderboard?baruch=true&page=2");
        verify(serverUrlUtils).getUrl();
    }

    @Test
    void testGetCodebloomLeaderboardScreenshotDifferentTag() {
        when(serverUrlUtils.getUrl()).thenReturn("https://stg.codebloom.patinanetwork.org");

        byte[] screenshot = playwrightClient.getCodebloomLeaderboardScreenshot(1, Tag.Gwc);

        assertNotNull(screenshot, "Screenshot should not be null");
        assertTrue(screenshot.length > 0, "Screenshot should have content");

        verify(page).navigate("https://stg.codebloom.patinanetwork.org/leaderboard?gwc=true&page=1");
        verify(serverUrlUtils).getUrl();
    }
}
