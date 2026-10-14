package org.patinanetwork.codebloom.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
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
    ManagedChannel lassServiceChannel(final LeetcodeAuthStealerGrpcClientConfiguration authStealerProps) {
        var builder = ManagedChannelBuilder.forTarget(authStealerProps.host())
                .intercept(new ClientInterceptor() {
                    @Override
                    public <Q, R> ClientCall<Q, R> interceptCall(
                            final MethodDescriptor<Q, R> method, final CallOptions options, final Channel next) {
                        return next.newCall(method, options.withDeadlineAfter(authStealerProps.timeout()));
                    }
                })
                .intercept(new ObservationGrpcClientInterceptor(observationRegistry));
        return (authStealerProps.tls() ? builder.useTransportSecurity() : builder.usePlaintext()).build();
    }

    @Bean
    LeetcodeAuthStealerServiceBlockingStub lassServiceBlockingStub(final ManagedChannel lassServiceChannel) {
        return LeetcodeAuthStealerServiceGrpc.newBlockingStub(lassServiceChannel);
    }
}
