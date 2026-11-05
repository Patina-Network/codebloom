use std::time::Duration;

use chrono::Utc;
use metrics::counter;
use redis::RedisError;
use tokio::{task::JoinHandle, time::MissedTickBehavior};
use tracing::{debug, error, info, warn};

use crate::{db::auth::AuthRepository, grpc::client::lass::LASS};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RefreshOutcome {
    Stored,
    LassFailed,
    StoreFailed,
}

impl RefreshOutcome {
    fn as_str(self) -> &'static str {
        match self {
            Self::Stored => "stored",
            Self::LassFailed => "lass_failed",
            Self::StoreFailed => "store_failed",
        }
    }
}

#[derive(Clone)]
pub struct TokenManager<R: AuthRepository, A: LASS> {
    repository: R,
    authenticator: A,
    max_age: Duration,
    lock_ttl: Duration,
    hostname: String,
}

impl<R: AuthRepository, A: LASS> TokenManager<R, A> {
    pub fn new(
        repository: R,
        authenticator: A,
        max_age: Duration,
        lock_ttl: Duration,
        hostname: String,
    ) -> Self {
        Self {
            repository,
            authenticator,
            max_age,
            lock_ttl,
            hostname,
        }
    }

    /// Starts a refresh in the background. `None` means another refresh holds the lock and this
    /// call was ignored. If the refresh panics, the lock is not released and expires after
    /// `lock_ttl`.
    pub async fn trigger(&self) -> Result<Option<JoinHandle<RefreshOutcome>>, RedisError> {
        let owner = lock_owner(&self.hostname);
        if !self.repository.try_lock(&owner, self.lock_ttl).await? {
            return Ok(None);
        }

        let this = self.clone();
        Ok(Some(tokio::spawn(async move {
            let outcome = this.refresh().await;

            if let Err(error) = this.repository.unlock(&owner).await {
                warn!(%error, "could not release refresh lock, it will expire on its own");
            }

            counter!("leetcode_auth_refresh_total", "result" => outcome.as_str()).increment(1);
            outcome
        })))
    }

    async fn refresh(&self) -> RefreshOutcome {
        info!("requesting new credentials from LASS");
        let auth = match self.authenticator.authenticate().await {
            Ok(auth) => auth,
            Err(status) => {
                warn!(code = ?status.code(), message = status.message(), "LASS refresh failed");
                return RefreshOutcome::LassFailed;
            }
        };

        let date_stored = Utc::now();
        match self.repository.store(&auth, date_stored).await {
            Ok(()) => {
                info!(%date_stored, "stored new credentials");
                RefreshOutcome::Stored
            }
            Err(error) => {
                error!(%error, "could not store new credentials");
                RefreshOutcome::StoreFailed
            }
        }
    }
}

// scheduled refresh methods
impl<R: AuthRepository, A: LASS> TokenManager<R, A> {
    pub async fn refresh_if_stale(&self) -> Option<JoinHandle<RefreshOutcome>> {
        let stored = match self.repository.get().await {
            Ok(stored) => stored,
            Err(error) => {
                warn!(%error, "could not read stored credentials, skipping scheduled refresh");
                return None;
            }
        };

        if !stored.is_stale(Utc::now(), self.max_age) {
            debug!("stored credentials are fresh");
            return None;
        }

        info!(date_stored = ?stored.date_stored, "stored credentials are stale");
        match self.trigger().await {
            Ok(Some(running)) => Some(running),
            Ok(None) => {
                info!("another refresh holds the lock, skipping scheduled refresh");
                None
            }
            Err(error) => {
                warn!(%error, "could not take refresh lock, skipping scheduled refresh");
                None
            }
        }
    }

    pub async fn run_schedule(self, every: Duration) {
        let mut interval = tokio::time::interval(every);
        interval.set_missed_tick_behavior(MissedTickBehavior::Delay);

        loop {
            interval.tick().await;
            self.refresh_if_stale().await;
        }
    }
}

fn lock_owner(hostname: &str) -> String {
    let now = Utc::now().timestamp_nanos_opt().unwrap_or_default();
    format!("{hostname}:{now}")
}

#[cfg(test)]
mod tests {
    use chrono::TimeDelta;

    use super::*;
    use crate::{
        db::auth::fake::FakeAuthRepository,
        domain::auth::{AuthToken, StoredAuth},
        grpc::client::lass::fake::{FakeAuthenticator, Outcome},
    };

    const MAX_AGE: Duration = Duration::from_secs(4 * 60 * 60);
    const LOCK_TTL: Duration = Duration::from_secs(60);

    fn token_refresher(
        repository: &FakeAuthRepository,
        lass: &FakeAuthenticator,
    ) -> TokenManager<FakeAuthRepository, FakeAuthenticator> {
        TokenManager::new(
            repository.clone(),
            lass.clone(),
            MAX_AGE,
            LOCK_TTL,
            "test-pod".to_string(),
        )
    }

    fn token(value: &str) -> AuthToken {
        AuthToken::new(value.into(), format!("{value}-csrf")).unwrap()
    }

    fn stored(value: &str, age: TimeDelta) -> StoredAuth {
        StoredAuth {
            auth: Some(token(value)),
            date_stored: Some(Utc::now() - age),
        }
    }

    #[tokio::test]
    async fn triggers_during_a_refresh_are_ignored_until_it_finishes() {
        let repository = FakeAuthRepository::stored(stored("old", TimeDelta::hours(1)));
        let lass = FakeAuthenticator::gated(Outcome::Succeed(token("new")));
        let token_refresher = token_refresher(&repository, &lass);

        let running = token_refresher
            .trigger()
            .await
            .unwrap()
            .expect("first trigger starts a refresh");
        for _ in 0..5 {
            assert!(token_refresher.trigger().await.unwrap().is_none());
        }

        lass.release();
        assert_eq!(running.await.unwrap(), RefreshOutcome::Stored);
        assert_eq!(lass.calls(), 1);
        assert_eq!(repository.snapshot().stored.auth, Some(token("new")));
        assert_eq!(repository.snapshot().lock, None);

        lass.release();
        let next = token_refresher
            .trigger()
            .await
            .unwrap()
            .expect("a finished refresh allows the next one");
        assert_eq!(next.await.unwrap(), RefreshOutcome::Stored);
        assert_eq!(lass.calls(), 2);
    }

    #[tokio::test]
    async fn a_lock_held_by_another_instance_blocks_refreshes() {
        let repository = FakeAuthRepository::stored(StoredAuth::default());
        let lass = FakeAuthenticator::new(Outcome::Succeed(token("new")));
        repository.set_lock(Some("other-pod:0"));

        let token_refresher = token_refresher(&repository, &lass);
        assert!(token_refresher.trigger().await.unwrap().is_none());
        assert!(token_refresher.refresh_if_stale().await.is_none());
        assert_eq!(lass.calls(), 0);
        assert_eq!(repository.snapshot().lock.as_deref(), Some("other-pod:0"));
    }

    #[tokio::test]
    async fn failed_refreshes_keep_stored_credentials_and_release_the_lock() {
        let before = stored("old", TimeDelta::hours(5));
        let repository = FakeAuthRepository::stored(before.clone());
        let lass = FakeAuthenticator::new(Outcome::Fail);

        let running = token_refresher(&repository, &lass)
            .trigger()
            .await
            .unwrap()
            .unwrap();
        assert_eq!(running.await.unwrap(), RefreshOutcome::LassFailed);
        assert_eq!(repository.snapshot().stored, before);
        assert_eq!(repository.snapshot().lock, None);
    }

    #[tokio::test]
    async fn the_latest_refresh_overwrites_credentials_written_meanwhile() {
        let repository = FakeAuthRepository::stored(stored("old", TimeDelta::hours(5)));
        let lass = FakeAuthenticator::gated(Outcome::Succeed(token("ours")));
        let running = token_refresher(&repository, &lass)
            .trigger()
            .await
            .unwrap()
            .unwrap();
        lass.wait_for_calls(1).await;

        repository
            .store(&token("theirs"), Utc::now())
            .await
            .unwrap();

        lass.release();
        assert_eq!(running.await.unwrap(), RefreshOutcome::Stored);
        assert_eq!(repository.snapshot().stored.auth, Some(token("ours")));
    }

    #[tokio::test]
    async fn scheduled_checks_only_refresh_stale_or_missing_credentials() {
        for (initial, expect_refresh) in [
            (stored("fresh", TimeDelta::hours(1)), false),
            (stored("stale", TimeDelta::hours(5)), true),
            (StoredAuth::default(), true),
        ] {
            let repository = FakeAuthRepository::stored(initial);
            let lass = FakeAuthenticator::new(Outcome::Succeed(token("new")));

            match token_refresher(&repository, &lass).refresh_if_stale().await {
                Some(running) => assert_eq!(running.await.unwrap(), RefreshOutcome::Stored),
                None => assert!(!expect_refresh),
            }

            assert_eq!(lass.calls(), usize::from(expect_refresh));
        }
    }

    #[tokio::test]
    async fn unreachable_store_never_calls_lass() {
        let lass = FakeAuthenticator::new(Outcome::Succeed(token("new")));
        let token_refresher = token_refresher(&FakeAuthRepository::Unavailable, &lass);

        assert!(token_refresher.refresh_if_stale().await.is_none());
        assert!(token_refresher.trigger().await.is_err());
        assert_eq!(lass.calls(), 0);
    }
}
