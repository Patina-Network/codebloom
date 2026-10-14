"""Prometheus collectors, typed metric updates, and exposition."""

from collections.abc import Iterable, Mapping, Sequence
from typing import Literal, get_args

from prometheus_client import CONTENT_TYPE_LATEST, Counter, generate_latest


def _counter(
    name: str,
    documentation: str,
    labelnames: Sequence[str] = (),
    initial_labels: Iterable[Mapping[str, str]] = (),
) -> Counter:
    """
    create a counter whose label combinations are exported at 0 before first use.

    https://prometheus.io/docs/practices/the_zen/#avoid-missing-metrics
    """
    initial = list(initial_labels)
    if labelnames and not initial:
        raise ValueError(f"{name}: labeled counters must declare initial_labels")

    counter = Counter(name, documentation, labelnames)

    for labels in initial:
        counter.labels(**labels)

    return counter


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

_AUTH_CALLS = _counter(
    "leetcode_auth_calls_total",
    "Completed authentication calls by outcome and failure reason.",
    ("outcome", "reason"),
    [{"outcome": "success", "reason": ""}]
    + [
        {"outcome": "failure", "reason": reason}
        for reason in get_args(AuthFailureReason.__value__)
    ],
)


def record_auth_success() -> None:
    _AUTH_CALLS.labels(outcome="success", reason="").inc()


def record_auth_failure(reason: AuthFailureReason) -> None:
    _AUTH_CALLS.labels(outcome="failure", reason=reason).inc()


def render_metrics() -> tuple[bytes, str]:
    return generate_latest(), CONTENT_TYPE_LATEST
