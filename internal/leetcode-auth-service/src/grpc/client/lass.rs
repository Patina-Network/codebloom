use std::time::Duration;

use anyhow::Context;
use tonic::{
    Status,
    transport::{Channel, Endpoint},
};

use crate::{
    config::Config,
    domain::auth::AuthToken,
    proto::lass::{
        AuthenticateRequest, leetcode_auth_stealer_service_client::LeetcodeAuthStealerServiceClient,
    },
};

/// Logs into leetcode.com and returns fresh credentials.
/// Leetcode Auth Stealer Service
pub trait LASS: Clone + Send + Sync + 'static {
    fn authenticate(&self) -> impl Future<Output = Result<AuthToken, Status>> + Send;
}

#[derive(Clone)]
pub struct GrpcLASS {
    client: LeetcodeAuthStealerServiceClient<Channel>,
    login: String,
    password: String,
}

impl GrpcLASS {
    /// Connects lazily so this service starts, and keeps serving stored credentials, while LASS is
    /// down.
    pub fn new(config: &Config) -> anyhow::Result<Self> {
        let channel = Endpoint::from_shared(config.grpc_lass_url.clone())
            .context("LASS_URL must be a valid URI")?
            .connect_timeout(Duration::from_secs(5))
            .timeout(config.grpc_lass_timeout)
            .connect_lazy();

        Ok(Self {
            client: LeetcodeAuthStealerServiceClient::new(channel),
            login: config.leetcode_auth_username.clone(),
            password: config.leetcode_auth_password.clone(),
        })
    }
}

impl LASS for GrpcLASS {
    async fn authenticate(&self) -> Result<AuthToken, Status> {
        let response = self
            .client
            .clone()
            .authenticate(AuthenticateRequest {
                login: self.login.clone(),
                password: self.password.clone(),
            })
            .await?
            .into_inner();

        AuthToken::new(response.leetcode_session, response.csrf_token)
            .ok_or_else(|| Status::internal("LASS returned incomplete credentials"))
    }
}

#[cfg(test)]
pub mod fake {
    use std::sync::{
        Arc,
        atomic::{AtomicUsize, Ordering},
    };

    use tokio::sync::Semaphore;

    use super::*;

    pub enum Outcome {
        Succeed(AuthToken),
        Fail,
    }

    /// Counts calls. A gated fake blocks every call until [`FakeAuthenticator::release`].
    #[derive(Clone)]
    pub struct FakeAuthenticator {
        outcome: Arc<Outcome>,
        calls: Arc<AtomicUsize>,
        gate: Arc<Semaphore>,
    }

    impl FakeAuthenticator {
        pub fn new(outcome: Outcome) -> Self {
            Self::with_permits(outcome, Semaphore::MAX_PERMITS)
        }

        pub fn gated(outcome: Outcome) -> Self {
            Self::with_permits(outcome, 0)
        }

        fn with_permits(outcome: Outcome, permits: usize) -> Self {
            Self {
                outcome: Arc::new(outcome),
                calls: Arc::new(AtomicUsize::new(0)),
                gate: Arc::new(Semaphore::new(permits)),
            }
        }

        pub fn release(&self) {
            self.gate.add_permits(1);
        }

        pub fn calls(&self) -> usize {
            self.calls.load(Ordering::SeqCst)
        }

        pub async fn wait_for_calls(&self, calls: usize) {
            while self.calls() < calls {
                tokio::task::yield_now().await;
            }
        }
    }

    impl LASS for FakeAuthenticator {
        async fn authenticate(&self) -> Result<AuthToken, Status> {
            self.calls.fetch_add(1, Ordering::SeqCst);
            self.gate.acquire().await.unwrap().forget();

            match &*self.outcome {
                Outcome::Succeed(auth) => Ok(auth.clone()),
                Outcome::Fail => Err(Status::unavailable("LASS unavailable")),
            }
        }
    }
}
