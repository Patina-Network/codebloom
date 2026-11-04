pub mod health;
pub mod metrics;
pub mod state;
pub mod version;

use axum::{Router, routing::get};
use tower_http::catch_panic;

use crate::http::state::AppState;

pub fn router(state: AppState) -> Router {
    Router::new()
        .route("/livez", get(health::live))
        .route("/readyz", get(health::ready))
        .route("/metrics", get(metrics::metrics))
        .route("/version", get(version::version))
        .layer(catch_panic::CatchPanicLayer::new())
        .with_state(state)
}
