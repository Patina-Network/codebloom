package org.patinanetwork.codebloom.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcClientInterceptor;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class GrpcChannelFactory {

    private final GrpcRequestIdInterceptor requestIdInterceptor = new GrpcRequestIdInterceptor();
    private final ObservationGrpcClientInterceptor observationInterceptor;

    public GrpcChannelFactory(ObservationRegistry observationRegistry) {
        this.observationInterceptor = new ObservationGrpcClientInterceptor(observationRegistry);
    }

    public ManagedChannel create(String target, boolean tls, Duration timeout) {
        var builder = ManagedChannelBuilder.forTarget(target)
                .intercept(new GrpcClientTimeoutInterceptor(timeout), requestIdInterceptor, observationInterceptor);

        return (tls ? builder.useTransportSecurity() : builder.usePlaintext()).build();
    }
}
