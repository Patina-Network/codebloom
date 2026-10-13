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
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.LeetcodeAuthStealerServiceGrpc.LeetcodeAuthStealerServiceBlockingStub;

public class DirectLeetcodeAuthenticationClientTest {

    private final LeetcodeAuthStealerServiceBlockingStub lassService =
            mock(LeetcodeAuthStealerServiceBlockingStub.class);
    private final DirectLeetcodeAuthenticationClient client = new DirectLeetcodeAuthenticationClient(
            lassService, new DirectLeetcodeAuthenticationClientProperties("user@example.com", "hunter2"));

    @Test
    void mapsSessionAndCsrfFromLassResponse() {
        when(lassService.authenticate(any(AuthenticateRequest.class)))
                .thenReturn(AuthenticateResponse.newBuilder()
                        .setLeetcodeSession("session-token")
                        .setCsrfToken("csrf-token")
                        .build());

        Optional<AuthenticationCredentials> result = client.getLeetcodeCookie();

        assertEquals(Optional.of(new AuthenticationCredentials("session-token", "csrf-token")), result);
        verify(lassService)
                .authenticate(AuthenticateRequest.newBuilder()
                        .setLogin("user@example.com")
                        .setPassword("hunter2")
                        .build());
    }

    @Test
    void returnsEmptyWhenLassCallFails() {
        when(lassService.authenticate(any(AuthenticateRequest.class)))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertTrue(client.getLeetcodeCookie().isEmpty());
    }
}
