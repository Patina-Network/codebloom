use crate::http::state::AppState;
use axum::{
    extract::State,
    http::{StatusCode, header},
    response::IntoResponse,
};

pub async fn metrics(State(state): State<AppState>) -> Result<impl IntoResponse, StatusCode> {
    // add more repo / non-auto metric collections here
    state.sys_collector.collect();

    let body = state.prometheus_handle.render();

    Ok((
        [(
            header::CONTENT_TYPE,
            "text/plain; version=0.0.4; charset=utf-8",
        )],
        body,
    ))
}
