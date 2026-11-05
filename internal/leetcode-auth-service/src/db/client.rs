use anyhow::Context;
use redis::aio::{ConnectionManager, ConnectionManagerConfig};

use crate::config::Config;

/// Connects lazily so the service starts while Redis is down and `/readyz` reports it, instead of
/// crash-looping. `ConnectionManager` reconnects on its own; clones share one connection.
pub fn connect(config: &Config) -> anyhow::Result<ConnectionManager> {
    let client = redis::Client::open(config.redis_url.as_str()).context("invalid REDIS_URL")?;

    ConnectionManager::new_lazy_with_config(client, ConnectionManagerConfig::new())
        .context("failed to create redis connection manager")
}
