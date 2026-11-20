package org.patinanetwork.codebloom.config;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc.LeetcodeAuthServiceBlockingStub;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(LeetcodeAuthServiceGrpcClientConfiguration.class)
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
    ManagedChannel lasServiceChannel(
            LeetcodeAuthServiceGrpcClientConfiguration authProps,
            GrpcRequestIdInterceptor requestIdInterceptor,
            ObservationGrpcClientInterceptor observationInterceptor) {

        var builder = ManagedChannelBuilder.forTarget(authProps.host())
                .intercept(interceptors(authProps.timeout(), requestIdInterceptor, observationInterceptor));

        return (authProps.tls() ? builder.useTransportSecurity() : builder.usePlaintext()).build();
    }

    @Bean
    LeetcodeAuthServiceBlockingStub lasServiceBlockingStub(ManagedChannel lasServiceChannel) {

        return LeetcodeAuthServiceGrpc.newBlockingStub(lasServiceChannel);
    }
}
