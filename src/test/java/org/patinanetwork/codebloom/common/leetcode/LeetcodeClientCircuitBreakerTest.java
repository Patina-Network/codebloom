package org.patinanetwork.codebloom.common.leetcode;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.patinanetwork.codebloom.common.leetcode.auth.AuthenticationCredentials;
import org.patinanetwork.codebloom.common.leetcode.auth.LeetcodeAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
        classes = LeetcodeClientCircuitBreakerTest.TestApp.class,
        properties = {
            "resilience4j.retry.instances.leetcodeClient.max-attempts=1",
            "resilience4j.retry.instances.leetcodeClient.wait-duration=1ms",
            "resilience4j.circuitbreaker.instances.leetcodeClient.sliding-window-size=2",
            "resilience4j.circuitbreaker.instances.leetcodeClient.minimum-number-of-calls=2",
            "resilience4j.circuitbreaker.instances.leetcodeClient.failure-rate-threshold=50",
            "resilience4j.circuitbreaker.instances.leetcodeClient.wait-duration-in-open-state=60s"
        })
class LeetcodeClientCircuitBreakerTest {

    @Autowired
    private LeetcodeClientImpl leetcodeClient;

    @MockitoBean
    private LeetcodeAuthenticationService leetcodeAuthenticationService;

    @MockitoBean
    private HttpClient httpClient;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(LeetcodeClientImpl.class)
    static class TestApp {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Object target = AopTestUtils.getTargetObject(leetcodeClient);
        ReflectionTestUtils.setField(target, "client", httpClient);

        when(leetcodeAuthenticationService.getAuthenticationCredentials())
                .thenReturn(new AuthenticationCredentials(Optional.of("test-cookie"), Optional.empty()));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("boom"));
    }

    @Test
    void findQuestionBySlugOpenCircuit() {
        assertThrows(RuntimeException.class, () -> leetcodeClient.findQuestionBySlug("two-sum"));
        assertThrows(RuntimeException.class, () -> leetcodeClient.findQuestionBySlug("two-sum"));
        assertThrows(CallNotPermittedException.class, () -> leetcodeClient.findQuestionBySlug("two-sum"));
    }
}
