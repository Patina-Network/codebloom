"""Prometheus collectors, typed metric updates, and exposition."""

from typing import Literal

from prometheus_client import CONTENT_TYPE_LATEST, Counter, generate_latest

type AuthFailureReason = Literal[
    "invalid_argument",
    "bad_credentials",
    "manual_verification_required",
    "authentication_unavailable",
    "timeout",
    "cancelled",
    "internal_error",
    "incomplete_authentication",
]

_AUTH_CALLS = Counter(
    "leetcode_auth_calls_total",
    "Completed authentication calls by outcome and failure reason.",
    ("outcome", "reason"),
)


def record_auth_success() -> None:
    _AUTH_CALLS.labels(outcome="success", reason="").inc()


def record_auth_failure(reason: AuthFailureReason) -> None:
    _AUTH_CALLS.labels(outcome="failure", reason=reason).inc()


def render_metrics() -> tuple[bytes, str]:
    return generate_latest(), CONTENT_TYPE_LATEST
