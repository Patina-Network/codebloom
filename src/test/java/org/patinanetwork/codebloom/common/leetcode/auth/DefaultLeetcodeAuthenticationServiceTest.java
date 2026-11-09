package org.patinanetwork.codebloom.common.leetcode.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc.LeetcodeAuthServiceBlockingStub;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest;

class DefaultLeetcodeAuthenticationServiceTest {

    private final LeetcodeAuthServiceBlockingStub stub = mock(LeetcodeAuthServiceBlockingStub.class);
    private final DefaultLeetcodeAuthenticationService service = new DefaultLeetcodeAuthenticationService(stub);

    @Test
    void returnsStoredCredentials() {
        when(stub.getToken(any(GetTokenRequest.class)))
                .thenReturn(GetTokenResponse.newBuilder()
                        .setToken("session")
                        .setCsrf("csrf")
                        .build());

        AuthenticationCredentials credentials = service.getAuthenticationCredentials();

        assertEquals(Optional.of("session"), credentials.token());
        assertEquals(Optional.of("csrf"), credentials.csrf());
    }

    @Test
    void returnsEmptyCredentialsWhenAuthServiceFails() {
        when(stub.getToken(any(GetTokenRequest.class))).thenThrow(Status.NOT_FOUND.asRuntimeException());

        AuthenticationCredentials credentials = service.getAuthenticationCredentials();

        assertTrue(credentials.token().isEmpty());
        assertTrue(credentials.csrf().isEmpty());
    }

    @Test
    void refreshRequestsTokenRefresh() {
        service.refresh();

        verify(stub).refreshToken(any(RefreshTokenRequest.class));
    }
}
