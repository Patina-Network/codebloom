use std::time::Duration;

use chrono::{DateTime, TimeDelta, Utc};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AuthToken {
    pub token: String,
    pub csrf: String,
}

impl AuthToken {
    pub fn new(token: String, csrf: String) -> Option<Self> {
        (!token.is_empty() && !csrf.is_empty()).then_some(Self { token, csrf })
    }
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct StoredAuth {
    pub auth: Option<AuthToken>,
    pub date_stored: Option<DateTime<Utc>>,
}

impl StoredAuth {
    pub fn is_stale(&self, now: DateTime<Utc>, max_age: Duration) -> bool {
        if self.auth.is_none() {
            return true;
        }

        let Some(stored_at) = self.date_stored else {
            return true;
        };

        let max_age = TimeDelta::from_std(max_age).unwrap_or(TimeDelta::MAX);
        now.signed_duration_since(stored_at) >= max_age
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const MAX_AGE: Duration = Duration::from_secs(4 * 60 * 60);

    fn auth() -> Option<AuthToken> {
        AuthToken::new("token".into(), "csrf".into())
    }

    #[test]
    fn incomplete_credentials_are_rejected() {
        assert_eq!(AuthToken::new(String::new(), "csrf".into()), None);
        assert_eq!(AuthToken::new("token".into(), String::new()), None);
    }

    #[test]
    fn entries_go_stale_exactly_at_max_age() {
        let now = Utc::now();
        let stored_at = |age: TimeDelta| StoredAuth {
            auth: auth(),
            date_stored: Some(now - age),
        };

        assert!(
            !stored_at(TimeDelta::hours(4) - TimeDelta::milliseconds(1)).is_stale(now, MAX_AGE)
        );
        assert!(stored_at(TimeDelta::hours(4)).is_stale(now, MAX_AGE));
        // A clock ahead of ours must not force a refresh.
        assert!(!stored_at(TimeDelta::hours(-1)).is_stale(now, MAX_AGE));
    }

    #[test]
    fn incomplete_entries_are_stale() {
        let now = Utc::now();

        for stored in [
            StoredAuth::default(),
            StoredAuth {
                auth: None,
                date_stored: Some(now),
            },
            StoredAuth {
                auth: auth(),
                date_stored: None,
            },
        ] {
            assert!(stored.is_stale(now, MAX_AGE), "{stored:?}");
        }
    }
}
