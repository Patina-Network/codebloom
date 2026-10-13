package org.patinanetwork.codebloom.common.leetcode.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("leetcode.auth.direct")
@ConditionalOnProperty(name = "leetcode.auth.strategy", havingValue = "direct")
public record DirectLeetcodeAuthenticationClientProperties(String username, String password) {}
