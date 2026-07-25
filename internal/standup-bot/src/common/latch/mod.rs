pub mod error;

use std::{
    sync::Arc,
    time::Duration,
};

use tokio::{
    sync::Semaphore,
    time::timeout,
};

use crate::common::latch::error::CountdownLatchError;

/// [CountdownLatch] is the Rust equivalent of
/// [Java's CountDownLatch](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CountDownLatch.html),
/// which can be used to synchronize thread operations.
///
/// # Example
///
/// ```ignore
/// let latch = CountdownLatch::new(1);
///
/// tokio::spawn(async move {
///     if let Err(e) = client.start().await {
///         println!("Client error: {e:?}");
///     }
///     latch.count_down();
/// })
///
/// latch.wait().await;
/// ```
#[derive(Clone)]
pub struct CountdownLatch {
    count: u8,
    sem: Arc<Semaphore>,
}

#[allow(dead_code)]
impl CountdownLatch {
    /// Create a new [CountdownLatch] with the specified count of `n`.
    pub fn new(n: u8) -> Self {
        CountdownLatch {
            count: n,
            sem: Arc::new(Semaphore::new(0)),
        }
    }

    /// Count down by a factor of `1`.
    pub fn count_down(&self) -> () {
        self.sem.add_permits(1);
    }

    /// Will wait until `count_down` has been called `n` times.
    pub async fn wait(&self) -> Result<(), CountdownLatchError> {
        let permit = self.sem.acquire_many(self.count as u32).await?;
        permit.forget();
        Ok(())
    }

    /// Will wait until `count_down` has been called `n` times or timeout if we have waited `ms`
    /// milliseconds.
    pub async fn wait_until(&self, ms: u32) -> Result<(), CountdownLatchError> {
        let permit = timeout(
            Duration::from_millis(ms as u64),
            self.sem.acquire_many(self.count as u32),
        )
        .await??;

        permit.forget();

        Ok(())
    }
}
