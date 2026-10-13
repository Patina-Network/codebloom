package org.patinanetwork.codebloom.common.leetcode.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.playwright.PlaywrightClient;

public class GitHubLeetcodeAuthenticationClientTest {

    private final PlaywrightClient playwrightClient = mock(PlaywrightClient.class);
    private final GitHubLeetcodeAuthenticationClient client = new GitHubLeetcodeAuthenticationClient(
            playwrightClient, new GitHubLeetcodeAuthenticationClientProperties("gh-user", "gh-pass"));

    @Test
    void mapsTokenAndCsrfFromPlaywrightAuth() {
        when(playwrightClient.getLeetcodeCookie("gh-user", "gh-pass"))
                .thenReturn(Optional.of(
                        Auth.builder().token("session-token").csrf(Optional.of("csrf-token")).build()));

        assertEquals(
                Optional.of(new AuthenticationCredentials("session-token", "csrf-token")), client.getLeetcodeCookie());
    }

    @Test
    void returnsEmptyWhenPlaywrightFindsNoCookie() {
        when(playwrightClient.getLeetcodeCookie("gh-user", "gh-pass")).thenReturn(Optional.empty());

        assertTrue(client.getLeetcodeCookie().isEmpty());
    }
}
