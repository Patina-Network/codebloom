use bb8_redis::{
    bb8::RunError,
    redis::RedisError,
};
use chrono::ParseError;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum RedisClientError {
    #[error("redis pool error: {0}")]
    Pool(#[from] RunError<RedisError>),
    #[error("redis error: {0}")]
    Redis(#[from] RedisError),
    #[error("failed to parse standup datetime: {0}")]
    DateTimeParse(#[from] ParseError),
}
