use tonic::{Request, Response, Status};

use crate::{
    db::auth::AuthRepository,
    grpc::{client::lass::LASS, redis_status},
    proto::leetcodeauth::{
        GetTokenRequest, GetTokenResponse, RefreshTokenRequest, RefreshTokenResponse,
        leetcode_auth_service_server,
    },
    service::refresh::{RefreshSource, TokenManager},
};

pub use leetcode_auth_service_server::LeetcodeAuthServiceServer;

pub struct LeetcodeAuthService<R: AuthRepository, A: LASS> {
    repository: R,
    token_manager: TokenManager<R, A>,
}

impl<R: AuthRepository, A: LASS> LeetcodeAuthService<R, A> {
    pub fn new(repository: R, token_manager: TokenManager<R, A>) -> Self {
        Self {
            repository,
            token_manager,
        }
    }
}

#[tonic::async_trait]
impl<R: AuthRepository, A: LASS> leetcode_auth_service_server::LeetcodeAuthService
    for LeetcodeAuthService<R, A>
{
    async fn get_token(
        &self,
        _request: Request<GetTokenRequest>,
    ) -> Result<Response<GetTokenResponse>, Status> {
        let auth = self
            .repository
            .get()
            .await
            .map_err(redis_status)?
            .auth
            .ok_or_else(|| Status::not_found("no leetcode credentials stored yet"))?;

        Ok(GetTokenResponse {
            token: auth.token,
            csrf: auth.csrf,
        }
        .into())
    }

    async fn refresh_token(
        &self,
        _request: Request<RefreshTokenRequest>,
    ) -> Result<Response<RefreshTokenResponse>, Status> {
        let started = self
            .token_manager
            .trigger(RefreshSource::Rpc)
            .await
            .map_err(redis_status)?
            .is_some();

        Ok(RefreshTokenResponse { started }.into())
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use chrono::DateTime;
    use tonic::Code;

    use super::{leetcode_auth_service_server::LeetcodeAuthService as _, *};
    use crate::{
        db::auth::fake::FakeAuthRepository,
        domain::auth::{AuthToken, StoredAuth},
        grpc::client::lass::fake::{FakeAuthenticator, Outcome},
    };

    fn service(
        repository: FakeAuthRepository,
        lass: FakeAuthenticator,
    ) -> LeetcodeAuthService<FakeAuthRepository, FakeAuthenticator> {
        let token_refresher = TokenManager::new(
            repository.clone(),
            lass,
            Duration::from_secs(60),
            Duration::from_secs(60),
            "test-pod".to_string(),
        );
        LeetcodeAuthService::new(repository, token_refresher)
    }

    fn token(value: &str) -> AuthToken {
        AuthToken::new(value.into(), format!("{value}-csrf")).unwrap()
    }

    #[tokio::test]
    async fn get_token_reports_missing_and_unavailable_store() {
        let lass = FakeAuthenticator::new(Outcome::Fail);

        let error = service(
            FakeAuthRepository::stored(StoredAuth::default()),
            lass.clone(),
        )
        .get_token(Request::new(GetTokenRequest {}))
        .await
        .unwrap_err();
        assert_eq!(error.code(), Code::NotFound);

        let error = service(FakeAuthRepository::Unavailable, lass)
            .get_token(Request::new(GetTokenRequest {}))
            .await
            .unwrap_err();
        assert_eq!(error.code(), Code::Unavailable);
        assert_eq!(error.message(), "token store unavailable");
    }

    #[tokio::test]
    async fn get_token_serves_stale_credentials_while_a_refresh_runs() {
        let repository = FakeAuthRepository::stored(StoredAuth {
            auth: Some(token("old")),
            date_stored: Some(DateTime::UNIX_EPOCH),
        });
        let lass = FakeAuthenticator::gated(Outcome::Succeed(token("new")));
        let service = service(repository, lass.clone());

        let started = service
            .refresh_token(Request::new(RefreshTokenRequest {}))
            .await
            .unwrap()
            .into_inner()
            .started;
        assert!(started);

        let ignored = service
            .refresh_token(Request::new(RefreshTokenRequest {}))
            .await
            .unwrap()
            .into_inner()
            .started;
        assert!(!ignored);

        lass.wait_for_calls(1).await;
        assert_eq!(lass.calls(), 1);

        let response = service
            .get_token(Request::new(GetTokenRequest {}))
            .await
            .unwrap()
            .into_inner();
        assert_eq!(response.token, "old");
        assert_eq!(response.csrf, "old-csrf");
        assert_eq!(lass.calls(), 1, "get_token must never start a refresh");
    }
}
