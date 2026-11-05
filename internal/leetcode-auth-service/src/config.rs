use std::{env, time::Duration};

use anyhow::Context;
use tracing::info;

#[derive(Clone)]
pub struct Config {
    pub grpc_port: u16,
    pub http_port: u16,
    pub grpc_reflection: bool,
    pub version: Option<String>,
    /// Identifies this pod as the holder of the refresh lock.
    pub hostname: String,
    pub redis_url: String,
    pub grpc_lass_url: String,
    pub grpc_lass_timeout: Duration,
    /// Defaults to outlasting one LASS call, so the lock does not expire mid-refresh.
    pub refresh_lock_ttl: Duration,
    pub leetcode_auth_username: String,
    pub leetcode_auth_password: String,
    pub refresh_check_interval: Duration,
    pub token_max_age: Duration,
}

impl Config {
    pub fn log(&self) {
        info!(
            grpc_port = self.grpc_port,
            http_port = self.http_port,
            grpc_reflection = self.grpc_reflection,
            version = self.version.as_deref().unwrap_or("N/A"),
            hostname = self.hostname,
            lass_url = self.grpc_lass_url,
            lass_timeout = ?self.grpc_lass_timeout,
            refresh_lock_ttl = ?self.refresh_lock_ttl,
            refresh_check_interval = ?self.refresh_check_interval,
            token_max_age = ?self.token_max_age,
            "loaded config"
        );
    }

    pub fn from_env() -> anyhow::Result<Self> {
        Self::from_vars(|key| env::var(key).ok())
    }

    fn from_vars(var: impl Fn(&str) -> Option<String>) -> anyhow::Result<Self> {
        let lass_timeout = seconds(&var, "GRPC_LASS_TIMEOUT_SECONDS", 180);

        Ok(Self {
            grpc_reflection: var("GRPC_REFLECTION")
                .and_then(|s| s.parse().ok())
                .unwrap_or(false),
            grpc_port: var("GRPC_PORT")
                .and_then(|s| s.parse().ok())
                .unwrap_or(50052),
            http_port: var("HTTP_PORT")
                .and_then(|s| s.parse().ok())
                .unwrap_or(8082),
            version: var("VERSION").filter(|v| !v.is_empty()),
            hostname: var("HOSTNAME")
                .filter(|v| !v.is_empty())
                .unwrap_or_else(|| "unknown".to_string()),
            redis_url: required(&var, "REDIS_URL")?,
            grpc_lass_url: {
                let host = var("GRPC_LASS_HOST")
                    .filter(|v| !v.is_empty())
                    .unwrap_or_else(|| "localhost:50051".to_string());
                let tls = var("GRPC_LASS_TLS")
                    .and_then(|b| b.parse::<bool>().ok())
                    .unwrap_or(false);

                format!("{}{host}", if tls { "https://" } else { "http://" })
            },
            grpc_lass_timeout: lass_timeout,
            refresh_lock_ttl: seconds(
                &var,
                "REFRESH_LOCK_TTL_SECONDS",
                lass_timeout.as_secs() + 30,
            ),
            leetcode_auth_username: required(&var, "LEETCODE_AUTH_USERNAME")?,
            leetcode_auth_password: required(&var, "LEETCODE_AUTH_PASSWORD")?,
            refresh_check_interval: seconds(&var, "REFRESH_CHECK_INTERVAL_SECONDS", 15 * 60),
            token_max_age: seconds(&var, "TOKEN_MAX_AGE_SECONDS", 4 * 60 * 60),
        })
    }
}

fn required(var: &impl Fn(&str) -> Option<String>, name: &str) -> anyhow::Result<String> {
    var(name)
        .filter(|v| !v.is_empty())
        .with_context(|| format!("missing {name}"))
}

/// Zero is rejected because `tokio::time::interval` panics on a zero period.
fn seconds(var: &impl Fn(&str) -> Option<String>, name: &str, default: u64) -> Duration {
    Duration::from_secs(
        var(name)
            .and_then(|s| s.parse().ok())
            .filter(|&v| v > 0)
            .unwrap_or(default),
    )
}
