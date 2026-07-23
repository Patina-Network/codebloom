use thiserror::Error;
use tokio::{
    sync::AcquireError,
    time::error::Elapsed,
};

#[derive(Debug, Error)]
pub enum CountdownLatchError {
    #[error("failed to acquire semaphore permit: {0}")]
    Acquire(#[from] AcquireError),
    #[error("timed out waiting for latch: {0}")]
    Elapsed(#[from] Elapsed),
}
