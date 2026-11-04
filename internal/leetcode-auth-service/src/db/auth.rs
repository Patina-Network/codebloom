use std::time::Duration;

use chrono::{DateTime, SecondsFormat, Utc};

use redis::{RedisError, Script, aio::ConnectionManager};

use crate::domain::auth::{AuthToken, StoredAuth};

/// Hash with `token`, `csrf` and `date_stored` fields. No TTL: stale credentials are still served
/// until a refresh succeeds, and the key is never deleted.
pub const AUTH_KEY: &str = "codebloom:leetcode:auth:key";

/// Held by whichever pod is refreshing. Expires on its own so a crashed pod cannot block refreshes
/// forever.
pub const LOCK_KEY: &str = "codebloom:leetcode:auth:lock";

/// Deletes the lock only if `owner` still holds it, so a pod whose lock expired cannot release the
/// lock another pod has since taken.
const UNLOCK_IF_OWNER: &str = r"
if redis.call('GET', KEYS[1]) == ARGV[1] then
  return redis.call('DEL', KEYS[1])
end
return 0
";

pub trait AuthRepository: Clone + Send + Sync + 'static {
    fn get(&self) -> impl Future<Output = Result<StoredAuth, RedisError>> + Send;

    fn store(
        &self,
        auth: &AuthToken,
        date_stored: DateTime<Utc>,
    ) -> impl Future<Output = Result<(), RedisError>> + Send;

    /// `false` when another owner holds the lock.
    fn try_lock(
        &self,
        owner: &str,
        ttl: Duration,
    ) -> impl Future<Output = Result<bool, RedisError>> + Send;

    fn unlock(&self, owner: &str) -> impl Future<Output = Result<(), RedisError>> + Send;
}

#[derive(Clone)]
pub struct RedisAuthRepository {
    conn: ConnectionManager,
    unlock_script: Script,
}

impl RedisAuthRepository {
    pub fn new(conn: ConnectionManager) -> Self {
        Self {
            conn,
            unlock_script: Script::new(UNLOCK_IF_OWNER),
        }
    }
}

impl AuthRepository for RedisAuthRepository {
    async fn get(&self) -> Result<StoredAuth, RedisError> {
        let mut conn = self.conn.clone();
        let (token, csrf, date_stored): (Option<String>, Option<String>, Option<String>) =
            redis::cmd("HMGET")
                .arg(AUTH_KEY)
                .arg(&["token", "csrf", "date_stored"])
                .query_async(&mut conn)
                .await?;

        Ok(StoredAuth {
            auth: token
                .zip(csrf)
                .and_then(|(token, csrf)| AuthToken::new(token, csrf)),
            date_stored: date_stored.as_deref().and_then(parse_date_stored),
        })
    }

    async fn store(&self, auth: &AuthToken, date_stored: DateTime<Utc>) -> Result<(), RedisError> {
        let mut conn = self.conn.clone();

        redis::cmd("HSET")
            .arg(AUTH_KEY)
            .arg("token")
            .arg(&auth.token)
            .arg("csrf")
            .arg(&auth.csrf)
            .arg("date_stored")
            .arg(format_date_stored(date_stored))
            .query_async(&mut conn)
            .await
    }

    async fn try_lock(&self, owner: &str, ttl: Duration) -> Result<bool, RedisError> {
        let mut conn = self.conn.clone();
        let acquired: Option<String> = redis::cmd("SET")
            .arg(LOCK_KEY)
            .arg(owner)
            .arg("NX")
            .arg("PX")
            .arg(u64::try_from(ttl.as_millis()).unwrap_or(u64::MAX))
            .query_async(&mut conn)
            .await?;

        Ok(acquired.is_some())
    }

    async fn unlock(&self, owner: &str) -> Result<(), RedisError> {
        let mut conn = self.conn.clone();

        self.unlock_script
            .key(LOCK_KEY)
            .arg(owner)
            .invoke_async::<()>(&mut conn)
            .await
    }
}

fn format_date_stored(value: DateTime<Utc>) -> String {
    value.to_rfc3339_opts(SecondsFormat::Millis, true)
}

/// An unparsable value reads as missing, so the entry counts as stale and the next refresh
/// overwrites it.
fn parse_date_stored(value: &str) -> Option<DateTime<Utc>> {
    DateTime::parse_from_rfc3339(value)
        .ok()
        .map(|value| value.with_timezone(&Utc))
}

#[cfg(test)]
pub mod fake {
    use std::sync::Arc;

    use parking_lot::Mutex;

    use super::*;

    #[derive(Debug, Clone, Default)]
    pub struct FakeState {
        pub stored: StoredAuth,
        /// Lock owner; expiry is not modelled.
        pub lock: Option<String>,
    }

    #[derive(Clone)]
    pub enum FakeAuthRepository {
        Unavailable,
        Stored(Arc<Mutex<FakeState>>),
    }

    impl FakeAuthRepository {
        pub fn stored(stored: StoredAuth) -> Self {
            Self::Stored(Arc::new(Mutex::new(FakeState { stored, lock: None })))
        }

        fn state(&self) -> Result<&Mutex<FakeState>, RedisError> {
            match self {
                Self::Unavailable => Err(std::io::Error::other("redis unavailable").into()),
                Self::Stored(state) => Ok(state),
            }
        }

        pub fn snapshot(&self) -> FakeState {
            self.state()
                .expect("unavailable repository has no state")
                .lock()
                .clone()
        }

        pub fn set_lock(&self, owner: Option<&str>) {
            self.state()
                .expect("unavailable repository has no state")
                .lock()
                .lock = owner.map(str::to_string);
        }
    }

    impl AuthRepository for FakeAuthRepository {
        async fn get(&self) -> Result<StoredAuth, RedisError> {
            Ok(self.state()?.lock().stored.clone())
        }

        async fn store(
            &self,
            auth: &AuthToken,
            date_stored: DateTime<Utc>,
        ) -> Result<(), RedisError> {
            self.state()?.lock().stored = StoredAuth {
                auth: Some(auth.clone()),
                date_stored: Some(date_stored),
            };
            Ok(())
        }

        async fn try_lock(&self, owner: &str, _ttl: Duration) -> Result<bool, RedisError> {
            let mut state = self.state()?.lock();
            if state.lock.is_some() {
                return Ok(false);
            }

            state.lock = Some(owner.to_string());
            Ok(true)
        }

        async fn unlock(&self, owner: &str) -> Result<(), RedisError> {
            let mut state = self.state()?.lock();
            if state.lock.as_deref() == Some(owner) {
                state.lock = None;
            }
            Ok(())
        }
    }
}

#[cfg(test)]
mod tests {
    use chrono::SubsecRound;

    use super::*;

    #[test]
    fn date_stored_round_trips_and_rejects_garbage() {
        let now = Utc::now().trunc_subsecs(3);
        assert_eq!(parse_date_stored(&format_date_stored(now)), Some(now));

        for garbage in ["", "yesterday", "2026-13-01T00:00:00.000Z"] {
            assert_eq!(parse_date_stored(garbage), None, "{garbage:?}");
        }
    }
}
