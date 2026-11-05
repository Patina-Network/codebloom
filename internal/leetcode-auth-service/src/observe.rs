use std::time::Instant;

use chrono::{DateTime, Utc};
use metrics::{
    Unit, counter, describe_counter, describe_gauge, describe_histogram, gauge, histogram,
};
use metrics_exporter_prometheus::{BuildError, Matcher, PrometheusBuilder};
use strum::IntoStaticStr;
use tonic::Status;

use crate::{
    grpc::metrics::code_name,
    service::refresh::{RefreshOutcome, RefreshSource},
};

const REFRESH_TOTAL: &str = "leetcode_auth_refresh_total";
const REFRESH_REQUESTS: &str = "leetcode_auth_refresh_requests_total";
const REFRESH_IN_PROGRESS: &str = "leetcode_auth_refresh_in_progress";
const LASS_DURATION: &str = "leetcode_auth_lass_request_duration_seconds";
const SCHEDULED_CHECKS: &str = "leetcode_auth_scheduled_checks_total";
const CREDENTIALS_STORED_AT: &str = "leetcode_auth_credentials_stored_timestamp_seconds";

/// A LASS login drives a browser and may run until `GRPC_LASS_TIMEOUT_SECONDS` (180s by default).
const LASS_BUCKETS: &[f64] = &[
    1.0, 5.0, 10.0, 20.0, 30.0, 45.0, 60.0, 90.0, 120.0, 180.0, 240.0,
];

/// Whether a refresh request took the lock.
#[derive(Debug, Clone, Copy, PartialEq, Eq, IntoStaticStr)]
#[strum(serialize_all = "snake_case")]
pub enum RefreshRequestResult {
    Started,
    /// Another refresh holds the lock, so the request was ignored.
    Locked,
    LockError,
}

/// What a scheduled staleness check found.
#[derive(Debug, Clone, Copy, PartialEq, Eq, IntoStaticStr)]
#[strum(serialize_all = "snake_case")]
pub enum ScheduledCheck {
    Fresh,
    Stale,
    Missing,
    ReadError,
}

pub fn configure(builder: PrometheusBuilder) -> Result<PrometheusBuilder, BuildError> {
    builder.set_buckets_for_metric(Matcher::Full(LASS_DURATION.to_string()), LASS_BUCKETS)
}

pub fn describe() {
    describe_counter!(
        REFRESH_TOTAL,
        "Finished refreshes by result: stored, lass_failed or store_failed."
    );
    describe_counter!(
        REFRESH_REQUESTS,
        "Refresh requests by source (rpc or schedule) and result: started, locked (another refresh holds the lock) or lock_error."
    );
    describe_gauge!(
        REFRESH_IN_PROGRESS,
        "Refreshes currently running in this pod."
    );
    describe_histogram!(
        LASS_DURATION,
        Unit::Seconds,
        "Duration of LASS Authenticate calls by gRPC status."
    );
    describe_counter!(
        SCHEDULED_CHECKS,
        "Scheduled staleness checks by result: fresh, stale, missing or read_error."
    );
    describe_gauge!(
        CREDENTIALS_STORED_AT,
        Unit::Seconds,
        "Unix time the stored credentials were written, as last seen by this pod."
    );
}

pub fn record_refresh(outcome: RefreshOutcome) {
    let result: &'static str = outcome.into();
    counter!(REFRESH_TOTAL, "result" => result).increment(1);
}

pub fn record_refresh_request(source: RefreshSource, result: RefreshRequestResult) {
    let source: &'static str = source.into();
    let result: &'static str = result.into();
    counter!(REFRESH_REQUESTS, "source" => source, "result" => result).increment(1);
}

pub fn record_scheduled_check(result: ScheduledCheck) {
    let result: &'static str = result.into();
    counter!(SCHEDULED_CHECKS, "result" => result).increment(1);
}

pub fn record_credentials_stored_at(date_stored: DateTime<Utc>) {
    gauge!(CREDENTIALS_STORED_AT).set(date_stored.timestamp_millis() as f64 / 1000.0);
}

pub async fn time_lass_call<T>(call: impl Future<Output = Result<T, Status>>) -> Result<T, Status> {
    let start = Instant::now();
    let result = call.await;
    let status = match &result {
        Ok(_) => "OK",
        Err(status) => code_name(status.code()),
    };
    histogram!(LASS_DURATION, "status" => status).record(start.elapsed().as_secs_f64());
    result
}

pub async fn track_refresh<T>(refresh: impl Future<Output = T>) -> T {
    let _in_progress = InProgress::start();
    refresh.await
}

struct InProgress;

impl InProgress {
    fn start() -> Self {
        gauge!(REFRESH_IN_PROGRESS).increment(1.0);
        Self
    }
}

impl Drop for InProgress {
    fn drop(&mut self) {
        gauge!(REFRESH_IN_PROGRESS).decrement(1.0);
    }
}
