use tonic::Status;

pub mod auth;
pub mod client;
pub mod metrics;

pub fn redis_status(error: redis::RedisError) -> Status {
    tracing::error!(%error, "redis operation failed");
    Status::unavailable("token store unavailable")
}
