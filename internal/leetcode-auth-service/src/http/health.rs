use axum::{extract::State, http::StatusCode};

use crate::http::state::AppState;

pub async fn ready(State(state): State<AppState>) -> Result<&'static str, StatusCode> {
    state.health_check_repository.check().await.map_err(|err| {
        tracing::warn!(%err, "redis health check failed");
        StatusCode::SERVICE_UNAVAILABLE
    })?;

    Ok("ok")
}

pub async fn live() -> &'static str {
    "ok"
}
