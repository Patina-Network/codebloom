package org.patinanetwork.codebloom.config;

import io.grpc.ManagedChannel;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc;
import org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuthServiceGrpc.LeetcodeAuthServiceBlockingStub;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LeetcodeAuthServiceGrpcClientConfiguration.class)
public class GrpcClientConfiguration {

    @Bean
    ManagedChannel lasServiceChannel(
            GrpcChannelFactory grpcChannelFactory, LeetcodeAuthServiceGrpcClientConfiguration authProps) {

        return grpcChannelFactory.create(authProps.host(), authProps.tls(), authProps.timeout());
    }

    @Bean
    LeetcodeAuthServiceBlockingStub lasServiceBlockingStub(ManagedChannel lasServiceChannel) {

        return LeetcodeAuthServiceGrpc.newBlockingStub(lasServiceChannel);
    }
}
