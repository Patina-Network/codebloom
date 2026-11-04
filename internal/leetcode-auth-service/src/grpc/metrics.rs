use std::{
    sync::Arc,
    task::{Context, Poll},
    time::Instant,
};

use bytes::Bytes;
use metrics::{counter, histogram};
use pin_project_lite::pin_project;
use tonic::{
    Status,
    body::Body,
    codegen::http::{HeaderMap, Request, Response},
};
use tonic_middleware::{Middleware, ServiceBound};

// TODO: migrate this to a separate library

#[derive(Debug, Clone)]
pub struct GrpcRequestMetricsMiddleware;

#[tonic::async_trait]
impl<S> Middleware<S> for GrpcRequestMetricsMiddleware
where
    S: ServiceBound,
    S::Future: Send,
{
    async fn call(&self, req: Request<Body>, mut service: S) -> Result<Response<Body>, S::Error> {
        let start = Instant::now();
        let grpc_method = Arc::from(req.uri().path().trim_start_matches('/'));

        let result = service.call(req).await;

        match result {
            Ok(response) => {
                if let Some(status) = status_code_from(response.headers()) {
                    record_grpc_request_metrics(grpc_method, status, start);
                    return Ok(response);
                }

                let (parts, body) = response.into_parts();
                let body = ObservedGrpcBody {
                    body,
                    recorder: GrpcMetricsRecorder {
                        grpc_method,
                        start,
                        recorded: false,
                    },
                };

                Ok(Response::from_parts(parts, Body::new(body)))
            }
            Err(err) => {
                record_grpc_request_metrics(grpc_method, "TRANSPORT_ERROR", start);
                Err(err)
            }
        }
    }
}

fn status_code_from(headers: &HeaderMap) -> Option<&'static str> {
    headers
        .get("grpc-status")
        .map(|value| match value.as_bytes() {
            b"0" => "OK",
            b"1" => "CANCELLED",
            b"2" => "UNKNOWN",
            b"3" => "INVALID_ARGUMENT",
            b"4" => "DEADLINE_EXCEEDED",
            b"5" => "NOT_FOUND",
            b"6" => "ALREADY_EXISTS",
            b"7" => "PERMISSION_DENIED",
            b"8" => "RESOURCE_EXHAUSTED",
            b"9" => "FAILED_PRECONDITION",
            b"10" => "ABORTED",
            b"11" => "OUT_OF_RANGE",
            b"12" => "UNIMPLEMENTED",
            b"13" => "INTERNAL",
            b"14" => "UNAVAILABLE",
            b"15" => "DATA_LOSS",
            b"16" => "UNAUTHENTICATED",
            _ => "UNKNOWN",
        })
}

fn record_grpc_request_metrics(grpc_method: Arc<str>, status: &'static str, start: Instant) {
    counter!(
        "grpc_requests_total",
        "method" => grpc_method.clone(),
        "status" => status,
    )
    .increment(1);

    histogram!(
        "grpc_request_duration_seconds",
        "method" => grpc_method,
        "status" => status,
    )
    .record(start.elapsed().as_secs_f64());
}

pin_project! {
    struct ObservedGrpcBody {
        #[pin]
        body: Body,
        recorder: GrpcMetricsRecorder,
    }
}

struct GrpcMetricsRecorder {
    grpc_method: Arc<str>,
    start: Instant,
    recorded: bool,
}

impl GrpcMetricsRecorder {
    fn record(&mut self, status: &'static str) {
        if self.recorded {
            return;
        }

        self.recorded = true;
        record_grpc_request_metrics(self.grpc_method.clone(), status, self.start);
    }
}

impl Drop for GrpcMetricsRecorder {
    fn drop(&mut self) {
        if !self.recorded {
            self.record("INCOMPLETE");
        }
    }
}

impl http_body::Body for ObservedGrpcBody {
    type Data = Bytes;
    type Error = Status;

    fn poll_frame(
        self: std::pin::Pin<&mut Self>,
        cx: &mut Context<'_>,
    ) -> Poll<Option<Result<http_body::Frame<Self::Data>, Self::Error>>> {
        let this = self.project();

        match this.body.poll_frame(cx) {
            Poll::Ready(Some(Ok(frame))) => {
                if let Some(trailers) = frame.trailers_ref() {
                    let status = status_code_from(trailers).unwrap_or("UNKNOWN");
                    this.recorder.record(status);
                }

                Poll::Ready(Some(Ok(frame)))
            }
            Poll::Ready(Some(Err(status))) => {
                let code = match status.code() {
                    tonic::Code::Ok => "OK",
                    tonic::Code::Cancelled => "CANCELLED",
                    tonic::Code::Unknown => "UNKNOWN",
                    tonic::Code::InvalidArgument => "INVALID_ARGUMENT",
                    tonic::Code::DeadlineExceeded => "DEADLINE_EXCEEDED",
                    tonic::Code::NotFound => "NOT_FOUND",
                    tonic::Code::AlreadyExists => "ALREADY_EXISTS",
                    tonic::Code::PermissionDenied => "PERMISSION_DENIED",
                    tonic::Code::ResourceExhausted => "RESOURCE_EXHAUSTED",
                    tonic::Code::FailedPrecondition => "FAILED_PRECONDITION",
                    tonic::Code::Aborted => "ABORTED",
                    tonic::Code::OutOfRange => "OUT_OF_RANGE",
                    tonic::Code::Unimplemented => "UNIMPLEMENTED",
                    tonic::Code::Internal => "INTERNAL",
                    tonic::Code::Unavailable => "UNAVAILABLE",
                    tonic::Code::DataLoss => "DATA_LOSS",
                    tonic::Code::Unauthenticated => "UNAUTHENTICATED",
                };

                this.recorder.record(code);

                Poll::Ready(Some(Err(status)))
            }
            Poll::Ready(None) => {
                this.recorder.record("OK");
                Poll::Ready(None)
            }
            Poll::Pending => Poll::Pending,
        }
    }

    fn size_hint(&self) -> http_body::SizeHint {
        self.body.size_hint()
    }

    fn is_end_stream(&self) -> bool {
        self.body.is_end_stream()
    }
}
