package org.patinanetwork.codebloom.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GrpcClientTimeoutInterceptor implements ClientInterceptor {

    private final Duration timeout;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return next.newCall(method, callOptions.withDeadlineAfter(timeout));
    }
}
