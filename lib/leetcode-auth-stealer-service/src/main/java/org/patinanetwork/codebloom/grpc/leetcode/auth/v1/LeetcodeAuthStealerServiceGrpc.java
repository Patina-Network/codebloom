package org.patinanetwork.codebloom.grpc.leetcode.auth.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class LeetcodeAuthStealerServiceGrpc {

  private LeetcodeAuthStealerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "codebloom.leetcode.auth.v1.LeetcodeAuthStealerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest,
      org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> getAuthenticateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Authenticate",
      requestType = org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest.class,
      responseType = org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest,
      org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> getAuthenticateMethod() {
    io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest, org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> getAuthenticateMethod;
    if ((getAuthenticateMethod = LeetcodeAuthStealerServiceGrpc.getAuthenticateMethod) == null) {
      synchronized (LeetcodeAuthStealerServiceGrpc.class) {
        if ((getAuthenticateMethod = LeetcodeAuthStealerServiceGrpc.getAuthenticateMethod) == null) {
          LeetcodeAuthStealerServiceGrpc.getAuthenticateMethod = getAuthenticateMethod =
              io.grpc.MethodDescriptor.<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest, org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Authenticate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LeetcodeAuthStealerServiceMethodDescriptorSupplier("Authenticate"))
              .build();
        }
      }
    }
    return getAuthenticateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LeetcodeAuthStealerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceStub>() {
        @java.lang.Override
        public LeetcodeAuthStealerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthStealerServiceStub(channel, callOptions);
        }
      };
    return LeetcodeAuthStealerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static LeetcodeAuthStealerServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceBlockingV2Stub>() {
        @java.lang.Override
        public LeetcodeAuthStealerServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthStealerServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return LeetcodeAuthStealerServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LeetcodeAuthStealerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceBlockingStub>() {
        @java.lang.Override
        public LeetcodeAuthStealerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthStealerServiceBlockingStub(channel, callOptions);
        }
      };
    return LeetcodeAuthStealerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LeetcodeAuthStealerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthStealerServiceFutureStub>() {
        @java.lang.Override
        public LeetcodeAuthStealerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthStealerServiceFutureStub(channel, callOptions);
        }
      };
    return LeetcodeAuthStealerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void authenticate(org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAuthenticateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LeetcodeAuthStealerService.
   */
  public static abstract class LeetcodeAuthStealerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LeetcodeAuthStealerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LeetcodeAuthStealerService.
   */
  public static final class LeetcodeAuthStealerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LeetcodeAuthStealerServiceStub> {
    private LeetcodeAuthStealerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthStealerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthStealerServiceStub(channel, callOptions);
    }

    /**
     */
    public void authenticate(org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAuthenticateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LeetcodeAuthStealerService.
   */
  public static final class LeetcodeAuthStealerServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<LeetcodeAuthStealerServiceBlockingV2Stub> {
    private LeetcodeAuthStealerServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthStealerServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthStealerServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse authenticate(org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getAuthenticateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service LeetcodeAuthStealerService.
   */
  public static final class LeetcodeAuthStealerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LeetcodeAuthStealerServiceBlockingStub> {
    private LeetcodeAuthStealerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthStealerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthStealerServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse authenticate(org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAuthenticateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LeetcodeAuthStealerService.
   */
  public static final class LeetcodeAuthStealerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LeetcodeAuthStealerServiceFutureStub> {
    private LeetcodeAuthStealerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthStealerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthStealerServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse> authenticate(
        org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAuthenticateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_AUTHENTICATE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_AUTHENTICATE:
          serviceImpl.authenticate((org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest) request,
              (io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getAuthenticateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateRequest,
              org.patinanetwork.codebloom.grpc.leetcode.auth.v1.AuthenticateResponse>(
                service, METHODID_AUTHENTICATE)))
        .build();
  }

  private static abstract class LeetcodeAuthStealerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LeetcodeAuthStealerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.patinanetwork.codebloom.grpc.leetcode.auth.v1.LeetcodeAuthStealerServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LeetcodeAuthStealerService");
    }
  }

  private static final class LeetcodeAuthStealerServiceFileDescriptorSupplier
      extends LeetcodeAuthStealerServiceBaseDescriptorSupplier {
    LeetcodeAuthStealerServiceFileDescriptorSupplier() {}
  }

  private static final class LeetcodeAuthStealerServiceMethodDescriptorSupplier
      extends LeetcodeAuthStealerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LeetcodeAuthStealerServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LeetcodeAuthStealerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LeetcodeAuthStealerServiceFileDescriptorSupplier())
              .addMethod(getAuthenticateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
