package org.patinanetwork.codebloom.config;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.LeetcodeAuthStealerServiceGrpc;
import org.patinanetwork.codebloom.grpc.leetcode.auth.v1.LeetcodeAuthStealerServiceGrpc.LeetcodeAuthStealerServiceBlockingStub;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(LeetcodeAuthStealerGrpcClientConfiguration.class)
public class GrpcClientConfiguration {

    private final ObservationRegistry observationRegistry;

    @Bean
    GrpcRequestIdInterceptor grpcRequestIdInterceptor() {
        return new GrpcRequestIdInterceptor();
    }

    @Bean
    ObservationGrpcClientInterceptor grpcObservationInterceptor() {
        return new ObservationGrpcClientInterceptor(observationRegistry);
    }

    private ClientInterceptor[] interceptors(
            Duration timeout,
            GrpcRequestIdInterceptor requestIdInterceptor,
            ObservationGrpcClientInterceptor observationInterceptor) {

        return new ClientInterceptor[] {
            new GrpcClientTimeoutInterceptor(timeout), requestIdInterceptor, observationInterceptor
        };
    }

    @Bean
    ManagedChannel lassServiceChannel(
            LeetcodeAuthStealerGrpcClientConfiguration authStealerProps,
            GrpcRequestIdInterceptor requestIdInterceptor,
            ObservationGrpcClientInterceptor observationInterceptor) {

        var builder = ManagedChannelBuilder.forTarget(authStealerProps.host())
                .intercept(interceptors(authStealerProps.timeout(), requestIdInterceptor, observationInterceptor));

        return (authStealerProps.tls() ? builder.useTransportSecurity() : builder.usePlaintext()).build();
    }

    @Bean
    LeetcodeAuthStealerServiceBlockingStub lassServiceBlockingStub(ManagedChannel lassServiceChannel) {

        return LeetcodeAuthStealerServiceGrpc.newBlockingStub(lassServiceChannel);
    }
}
