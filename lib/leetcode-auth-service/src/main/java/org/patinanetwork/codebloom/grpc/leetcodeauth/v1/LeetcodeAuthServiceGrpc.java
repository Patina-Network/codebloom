package org.patinanetwork.codebloom.grpc.leetcodeauth.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class LeetcodeAuthServiceGrpc {

  private LeetcodeAuthServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "leetcodeauth.LeetcodeAuthService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest,
      org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> getGetTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getToken",
      requestType = org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest.class,
      responseType = org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest,
      org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> getGetTokenMethod() {
    io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest, org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> getGetTokenMethod;
    if ((getGetTokenMethod = LeetcodeAuthServiceGrpc.getGetTokenMethod) == null) {
      synchronized (LeetcodeAuthServiceGrpc.class) {
        if ((getGetTokenMethod = LeetcodeAuthServiceGrpc.getGetTokenMethod) == null) {
          LeetcodeAuthServiceGrpc.getGetTokenMethod = getGetTokenMethod =
              io.grpc.MethodDescriptor.<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest, org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LeetcodeAuthServiceMethodDescriptorSupplier("getToken"))
              .build();
        }
      }
    }
    return getGetTokenMethod;
  }

  private static volatile io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest,
      org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> getRefreshTokenMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "refreshToken",
      requestType = org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest.class,
      responseType = org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest,
      org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> getRefreshTokenMethod() {
    io.grpc.MethodDescriptor<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest, org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> getRefreshTokenMethod;
    if ((getRefreshTokenMethod = LeetcodeAuthServiceGrpc.getRefreshTokenMethod) == null) {
      synchronized (LeetcodeAuthServiceGrpc.class) {
        if ((getRefreshTokenMethod = LeetcodeAuthServiceGrpc.getRefreshTokenMethod) == null) {
          LeetcodeAuthServiceGrpc.getRefreshTokenMethod = getRefreshTokenMethod =
              io.grpc.MethodDescriptor.<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest, org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "refreshToken"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LeetcodeAuthServiceMethodDescriptorSupplier("refreshToken"))
              .build();
        }
      }
    }
    return getRefreshTokenMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LeetcodeAuthServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceStub>() {
        @java.lang.Override
        public LeetcodeAuthServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthServiceStub(channel, callOptions);
        }
      };
    return LeetcodeAuthServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static LeetcodeAuthServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceBlockingV2Stub>() {
        @java.lang.Override
        public LeetcodeAuthServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return LeetcodeAuthServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LeetcodeAuthServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceBlockingStub>() {
        @java.lang.Override
        public LeetcodeAuthServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthServiceBlockingStub(channel, callOptions);
        }
      };
    return LeetcodeAuthServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LeetcodeAuthServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LeetcodeAuthServiceFutureStub>() {
        @java.lang.Override
        public LeetcodeAuthServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LeetcodeAuthServiceFutureStub(channel, callOptions);
        }
      };
    return LeetcodeAuthServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * may return NOT_FOUND until the first refresh succeeds.
     * </pre>
     */
    default void getToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTokenMethod(), responseObserver);
    }

    /**
     * <pre>
     * Starts a refresh in the background and returns immediately. Calls made
     * while a refresh is running are ignored. The stored credentials keep being
     * served until a refresh succeeds.
     * </pre>
     */
    default void refreshToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRefreshTokenMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LeetcodeAuthService.
   */
  public static abstract class LeetcodeAuthServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LeetcodeAuthServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LeetcodeAuthService.
   */
  public static final class LeetcodeAuthServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LeetcodeAuthServiceStub> {
    private LeetcodeAuthServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * may return NOT_FOUND until the first refresh succeeds.
     * </pre>
     */
    public void getToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetTokenMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Starts a refresh in the background and returns immediately. Calls made
     * while a refresh is running are ignored. The stored credentials keep being
     * served until a refresh succeeds.
     * </pre>
     */
    public void refreshToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest request,
        io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRefreshTokenMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LeetcodeAuthService.
   */
  public static final class LeetcodeAuthServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<LeetcodeAuthServiceBlockingV2Stub> {
    private LeetcodeAuthServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     * <pre>
     * may return NOT_FOUND until the first refresh succeeds.
     * </pre>
     */
    public org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse getToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts a refresh in the background and returns immediately. Calls made
     * while a refresh is running are ignored. The stored credentials keep being
     * served until a refresh succeeds.
     * </pre>
     */
    public org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse refreshToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getRefreshTokenMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service LeetcodeAuthService.
   */
  public static final class LeetcodeAuthServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LeetcodeAuthServiceBlockingStub> {
    private LeetcodeAuthServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * may return NOT_FOUND until the first refresh succeeds.
     * </pre>
     */
    public org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse getToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetTokenMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts a refresh in the background and returns immediately. Calls made
     * while a refresh is running are ignored. The stored credentials keep being
     * served until a refresh succeeds.
     * </pre>
     */
    public org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse refreshToken(org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRefreshTokenMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LeetcodeAuthService.
   */
  public static final class LeetcodeAuthServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LeetcodeAuthServiceFutureStub> {
    private LeetcodeAuthServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LeetcodeAuthServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LeetcodeAuthServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * may return NOT_FOUND until the first refresh succeeds.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse> getToken(
        org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetTokenMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Starts a refresh in the background and returns immediately. Calls made
     * while a refresh is running are ignored. The stored credentials keep being
     * served until a refresh succeeds.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse> refreshToken(
        org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRefreshTokenMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TOKEN = 0;
  private static final int METHODID_REFRESH_TOKEN = 1;

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
        case METHODID_GET_TOKEN:
          serviceImpl.getToken((org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest) request,
              (io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse>) responseObserver);
          break;
        case METHODID_REFRESH_TOKEN:
          serviceImpl.refreshToken((org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest) request,
              (io.grpc.stub.StreamObserver<org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse>) responseObserver);
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
          getGetTokenMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenRequest,
              org.patinanetwork.codebloom.grpc.leetcodeauth.v1.GetTokenResponse>(
                service, METHODID_GET_TOKEN)))
        .addMethod(
          getRefreshTokenMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenRequest,
              org.patinanetwork.codebloom.grpc.leetcodeauth.v1.RefreshTokenResponse>(
                service, METHODID_REFRESH_TOKEN)))
        .build();
  }

  private static abstract class LeetcodeAuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LeetcodeAuthServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.patinanetwork.codebloom.grpc.leetcodeauth.v1.LeetcodeAuth.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LeetcodeAuthService");
    }
  }

  private static final class LeetcodeAuthServiceFileDescriptorSupplier
      extends LeetcodeAuthServiceBaseDescriptorSupplier {
    LeetcodeAuthServiceFileDescriptorSupplier() {}
  }

  private static final class LeetcodeAuthServiceMethodDescriptorSupplier
      extends LeetcodeAuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LeetcodeAuthServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (LeetcodeAuthServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LeetcodeAuthServiceFileDescriptorSupplier())
              .addMethod(getGetTokenMethod())
              .addMethod(getRefreshTokenMethod())
              .build();
        }
      }
    }
    return result;
  }
}
