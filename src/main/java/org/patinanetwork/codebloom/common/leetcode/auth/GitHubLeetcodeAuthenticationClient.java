package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.patinanetwork.codebloom.playwright.PlaywrightClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(GitHubLeetcodeAuthenticationClientProperties.class)
@ConditionalOnProperty(name = "leetcode.auth.strategy", havingValue = "github")
@AllArgsConstructor
public class GitHubLeetcodeAuthenticationClient implements LeetcodeAuthenticationClient {
    private final PlaywrightClient playwrightClient;
    private final GitHubLeetcodeAuthenticationClientProperties properties;

    @Override
    public Optional<AuthenticationCredentials> getLeetcodeCookie() {
        return playwrightClient
                .getLeetcodeCookie(properties.username(), properties.password())
                .map(a -> new AuthenticationCredentials(a.getToken(), a.getCsrf()));
    }
}
