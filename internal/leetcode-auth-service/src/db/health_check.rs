use std::time::Duration;

use anyhow::Context;
use redis::aio::ConnectionManager;

#[derive(Clone)]
pub struct HealthCheckRepository {
    conn: ConnectionManager,
}

impl HealthCheckRepository {
    pub fn new(conn: ConnectionManager) -> Self {
        Self { conn }
    }

    pub async fn check(&self) -> anyhow::Result<()> {
        let mut conn = self.conn.clone();

        tokio::time::timeout(
            Duration::from_secs(3),
            redis::cmd("PING").query_async::<()>(&mut conn),
        )
        .await
        .context("redis health check timed out")?
        .context("redis health check failed")?;

        Ok(())
    }
}
