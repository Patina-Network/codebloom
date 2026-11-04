use axum::extract::State;

use crate::http::state::AppState;

// we are wasting heap allocations here, but it doesn't matter
// because no machine calls `/version`. we would only call it manually
// to see what deployed version of the service is available, that's all
pub async fn version(State(state): State<AppState>) -> String {
    state.config.version.clone().unwrap_or("N/A".to_string())
}
