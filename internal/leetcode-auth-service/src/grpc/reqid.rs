use tonic::{
    body::Body,
    codegen::http::{Request, Response},
    metadata::MetadataMap,
};
use tonic_middleware::{Middleware, ServiceBound};
use tracing::{Instrument, field};

pub const REQUEST_ID_KEY: &str = "x-request-id";

pub fn request_id(metadata: &MetadataMap) -> Option<String> {
    metadata
        .get(REQUEST_ID_KEY)?
        .to_str()
        .ok()
        .map(str::to_owned)
}

#[derive(Debug, Clone)]
pub struct GrpcRequestIdMiddleware;

#[tonic::async_trait]
impl<S> Middleware<S> for GrpcRequestIdMiddleware
where
    S: ServiceBound,
    S::Future: Send,
{
    async fn call(&self, req: Request<Body>, mut service: S) -> Result<Response<Body>, S::Error> {
        let span = tracing::info_span!("grpc_request", request_id = field::Empty);

        if let Some(id) = req
            .headers()
            .get(REQUEST_ID_KEY)
            .and_then(|v| v.to_str().ok())
        {
            span.record("request_id", id);
        }

        service.call(req).instrument(span).await
    }
}
