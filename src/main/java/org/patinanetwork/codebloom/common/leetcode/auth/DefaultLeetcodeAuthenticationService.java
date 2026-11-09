package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc.LeetcodeAuthServiceBlockingStub;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@Profile("!ci")
public class DefaultLeetcodeAuthenticationService implements LeetcodeAuthenticationService {

    private final LeetcodeAuthServiceBlockingStub leetcodeAuthGrpcService;

    public AuthenticationCredentials getAuthenticationCredentials() {
        try {

            var tokenResponse = leetcodeAuthGrpcService.getToken(
                    GetTokenRequest.newBuilder().build());

            return new AuthenticationCredentials(
                    Optional.ofNullable(tokenResponse.getToken()), Optional.ofNullable(tokenResponse.getCsrf()));
        } catch (Exception e) {
            log.error("LeetcodeAuthenticationService.getAuthenticationCredentials failed", e);
            return new AuthenticationCredentials(Optional.empty(), Optional.empty());
        }
    }

    @Async
    public void refresh() {
        leetcodeAuthGrpcService.refreshToken(RefreshTokenRequest.newBuilder().build());
    }
}
