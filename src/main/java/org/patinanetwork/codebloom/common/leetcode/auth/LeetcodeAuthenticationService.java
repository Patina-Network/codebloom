package org.patinanetwork.codebloom.common.leetcode.auth;

public interface LeetcodeAuthenticationService {

    AuthenticationCredentials getAuthenticationCredentials();

    void refresh();
}
