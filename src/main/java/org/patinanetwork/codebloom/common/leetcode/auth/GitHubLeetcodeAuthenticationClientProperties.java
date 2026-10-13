package org.patinanetwork.codebloom.common.leetcode.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("leetcode.auth.github")
@ConditionalOnProperty(name = "leetcode.auth.strategy", havingValue = "github")
public record GitHubLeetcodeAuthenticationClientProperties(String username, String password) {}
