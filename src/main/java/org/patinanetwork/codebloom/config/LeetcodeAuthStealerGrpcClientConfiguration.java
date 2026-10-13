package org.patinanetwork.codebloom.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc.leetcode-auth-stealer-service")
public record LeetcodeAuthStealerGrpcClientConfiguration(String host, boolean tls, Duration timeout) {}
