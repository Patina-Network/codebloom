package org.patinanetwork.codebloom.common.leetcode.auth;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.LeetcodeAuthStealerServiceGrpc.LeetcodeAuthStealerServiceBlockingStub;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@EnableConfigurationProperties(DirectLeetcodeAuthenticationClientProperties.class)
@ConditionalOnProperty(name = "leetcode.auth.strategy", havingValue = "direct")
@AllArgsConstructor
@Slf4j
public class DirectLeetcodeAuthenticationClient implements LeetcodeAuthenticationClient {
    private final LeetcodeAuthStealerServiceBlockingStub lassService;
    private final DirectLeetcodeAuthenticationClientProperties properties;

    @Override
    public Optional<AuthenticationCredentials> getLeetcodeCookie() {
        try {
            return Optional.of(lassService.authenticate(AuthenticateRequest.newBuilder()
                            .setLogin(properties.username())
                            .setPassword(properties.password())
                            .build()))
                    .map(r -> new AuthenticationCredentials(r.getLeetcodeSession(), r.getCsrfToken()));
        } catch (Exception e) {
            log.error("failed to get leetcode cookie from LASS service", e);
            return Optional.empty();
        }
    }
}
