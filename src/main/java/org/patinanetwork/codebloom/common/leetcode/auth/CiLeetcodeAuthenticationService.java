package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("ci")
public class CiLeetcodeAuthenticationService implements LeetcodeAuthenticationService {

    @Override
    public AuthenticationCredentials getAuthenticationCredentials() {
        return new AuthenticationCredentials(Optional.empty(), Optional.empty());
    }

    @Override
    public void refresh() {}
}
