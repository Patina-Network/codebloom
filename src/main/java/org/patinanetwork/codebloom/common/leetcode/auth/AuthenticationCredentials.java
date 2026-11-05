package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;

public record AuthenticationCredentials(Optional<String> token, Optional<String> csrf) {}
