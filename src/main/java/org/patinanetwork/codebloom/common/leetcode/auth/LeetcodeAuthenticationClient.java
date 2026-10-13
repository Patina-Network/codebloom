package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;

public interface LeetcodeAuthenticationClient {
    Optional<AuthenticationCredentials> getLeetcodeCookie();
}
