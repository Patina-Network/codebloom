# LeetCode Auth Service

gRPC service that owns the leetcode.com credentials (`LEETCODE_SESSION` + `csrftoken`) used by the Codebloom backend.

- `proto/v1/leetcode_auth.proto`: `leetcodeauth.LeetcodeAuthService`
  - `getToken`: returns the stored `token` and `csrf`. `NOT_FOUND` until the first refresh succeeds, `UNAVAILABLE` if Redis is down.
  - `refreshToken`: starts a refresh in the background and returns right away. `started` is `false` when any instance is already refreshing; that call is ignored.

Credentials come from [`leetcode-auth-stealer-service`](../leetcode-auth-stealer-service/README.md) (LASS). This service is the only one that calls LASS; its proto is compiled straight from `../leetcode-auth-stealer-service/proto`.

## Storage

One Redis hash, `codebloom:leetcode:auth:key`, with the fields `token`, `csrf` and `date_stored` (RFC 3339, UTC). The hash has no TTL and is never deleted.

A refresh writes all three fields in one `HSET`, and only after LASS returns a non-empty token and csrf. The latest successful refresh always wins. A failed LASS call or Redis error leaves the stored credentials untouched, and `getToken` keeps serving them.

## Refreshing

- Only one refresh runs at a time across all instances, whether it came from `refreshToken` or the schedule. Triggers that arrive while one is running are ignored, not queued.
- The lock is `codebloom:leetcode:auth:lock`, taken with `SET NX PX`. Its value identifies the holder (hostname and the time it was taken), and it is released by a Lua script that deletes it only if that holder still owns it. It expires after `REFRESH_LOCK_TTL_SECONDS`, so a pod that crashes or panics mid-refresh blocks refreshes for at most that long.
- Every `REFRESH_CHECK_INTERVAL_SECONDS` (and once at startup) the service reads `date_stored` and refreshes if the entry is missing, incomplete, unparsable, or at least `TOKEN_MAX_AGE_SECONDS` old.

## Quickstart

Requires Rust 1.94 or newer and `protoc`.

```bash
# uses the repo root .env (copied from the root example.env)
just dev             # http on :8082, grpc on :50052
```

```bash
grpcurl -plaintext localhost:50052 leetcodeauth.LeetcodeAuthService/getToken
grpcurl -plaintext localhost:50052 leetcodeauth.LeetcodeAuthService/refreshToken
```

The Docker build context is `internal/`, because `build.rs` needs LASS's proto:

```bash
docker build -f internal/leetcode-auth-service/Dockerfile internal
```

## Configuration

- `REDIS_URL`: required.
- `LEETCODE_AUTH_USERNAME`, `LEETCODE_AUTH_PASSWORD`: required, sent to LASS.
- `GRPC_LASS_HOST`: LASS gRPC host and port, without a URI scheme; default `localhost:50051`.
- `GRPC_LASS_TLS`: `true` selects HTTPS; `false` selects HTTP (default `false`).
- `GRPC_LASS_TIMEOUT_SECONDS`: deadline for one LASS login, default `180`.
- `REFRESH_LOCK_TTL_SECONDS`: how long the refresh lock lives if it is never released, default `GRPC_LASS_TIMEOUT_SECONDS + 30`. Keep it above `GRPC_LASS_TIMEOUT_SECONDS`, or a slow refresh can lose its lock and a second pod may refresh at the same time.
- `REFRESH_CHECK_INTERVAL_SECONDS`: how often the stored entry's age is checked, default `900` (15 minutes).
- `TOKEN_MAX_AGE_SECONDS`: age at which the scheduled check refreshes, default `14400` (4 hours).
- `GRPC_PORT` / `HTTP_PORT`: default `50052` / `8082`.
- `GRPC_REFLECTION`: `true` to serve gRPC reflection, default `false`.
- `HOSTNAME`: names this pod in the refresh lock's value. Kubernetes sets it to the pod name; default `unknown`.

Zero or unparsable durations fall back to the default.

## HTTP

- `/livez`: always `ok`.
- `/readyz`: `503` when Redis does not answer `PING` within 3 seconds, so the pod is taken out of the Service until Redis is back. The pod starts and stays alive while Redis is down; the Redis connection opens on first use and reconnects on its own.
- `/metrics`: Prometheus, including `grpc_requests_total`, `grpc_request_duration_seconds` and `leetcode_auth_refresh_total{result="stored|lass_failed|store_failed"}`.
- `/version`: `VERSION` env var.

## Code layout

Same layering as `members-grpc-service`:

| File                  | Holds                                                                                              |
| --------------------- | -------------------------------------------------------------------------------------------------- |
| `src/domain/auth.rs`  | `AuthToken`, `StoredAuth` and the staleness rule. No I/O.                                          |
| `src/db/auth.rs`      | `AuthRepository` trait, `RedisAuthRepository`, and `FakeAuthRepository` for tests.                 |
| `src/grpc/client/lass.rs` | `LASS` trait, `LASSGrpcClient` (client for LASS), and `FakeAuthenticator` for tests.          |
| `src/service/refresh.rs` | `TokenManager`: the Redis-locked refresh and the schedule.                                      |
| `src/grpc/auth.rs`    | gRPC handlers, generic over `AuthRepository` and `LASS`.                                           |

Tests are unit tests only and need neither Redis nor LASS.
