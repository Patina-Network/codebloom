# `e2e/`

This directory contains Codebloom E2E tests written in [`TypeScript`](https://www.typescriptlang.org/) with [`Playwright`](https://playwright.dev/).

The [E2E workflow](../.github/workflows/e2e.yml) runs daily at 17:18 UTC. Authorized contributors can run it on a same-repository PR by commenting `/e2e-test`.

> [!NOTE]
> GitHub scheduled workflows can be delayed during periods of high loads.

## Requirements

- `pnpm` - Follow the main developer installation instructions under [local setup](https://docs.patinanetwork.org/codebloom/local/SETUP/).

## Run

To run all e2e tests, reset your local database to the E2E seeds, then launch the tests:

```bash
# just e2e-dev loads .env and .env.shared.yaml, then launches the backend server & frontend server.
just drop && just migrate-e2e && just e2e-dev
```

E2E only uses the seeds in [`db/e2e`](../db/e2e), never the mock data in `db/repeated`. The integration tests use up their seeded users, so reset the database before every run.

To utilize Playwright codegen (click to generate test code), run the following command:

```bash
pnpm run e2e-codegen
```

You can pass in any extra args supported by Playwright from this documentation [here](https://playwright.dev/docs/running-tests#run-specific-tests).

## External tests

`src/external/` calls real external services and runs only in the `external` project (Chromium, no retries):

- `school-email.spec.ts`: sends a real school verification email over SMTP, reads it over IMAP, and clicks the magic link. The backend runs with the `e2e` Spring profile, which accepts `@gmail.com` as NYU.
- `leetcode-onboarding.spec.ts`: links the LeetCode account whose About Me contains the verify key in [`src/const.ts`](src/const.ts).
- `leetcode-submission-details.spec.ts`: snapshots the backend LeetCode client's authenticated result for the Two Sum submission ID in [`src/const.ts`](src/const.ts).

The `e2e` Spring profile exposes `/api/e2e/leetcode/submission/{submissionId}` and disables scheduled jobs and the development startup question-bank sync. The endpoint is not active with the `prod` profile.

The backend and username lookup use staging LAS over TLS. The backend can request a LAS refresh if LeetCode indicates throttling or an invalid session.

`pnpm install` generates the LAS TypeScript client from `internal/leetcode-auth-service/proto/v1/leetcode_auth.proto`. After changing the proto, run `pnpm run proto:generate` in `e2e/`; the generated files are ignored by Git. Use `await using` with `LasClient` to close its channel at the end of the scope.

`just e2e-dev` loads the root `.env`, then decrypts `.env.shared.yaml`; shared values take precedence. Put the `E2E_*` inbox settings in the shared file for local development. The tests also require VPN access. To run only the external tests:

```bash
just drop && just migrate-e2e && just e2e-dev --project=external
```

To set up a different LeetCode account, follow the steps in [`src/const.ts`](src/const.ts).

## CI

The scheduled workflow and `/e2e-test` run every E2E test in one Playwright run against a single, freshly migrated database seeded only by [`db/e2e`](../db/e2e). Runs share staging LAS and the email inbox, so they are serialized.

Configure these GitHub Actions secrets for the IMAP inbox:

- `E2E_EMAIL_IMAP_HOST` and `E2E_EMAIL_IMAP_PORT`: the IMAP server and TLS port.
- `E2E_EMAIL_USERNAME`: a Gmail address that receives the verification message.
- `E2E_EMAIL_PASSWORD`: its IMAP app password.

The existing `CI_APP_CODEBLOOM_EMAIL_HOST`, `CI_APP_CODEBLOOM_EMAIL_PORT`, `CI_APP_CODEBLOOM_EMAIL_TYPE`, `CI_APP_CODEBLOOM_EMAIL_USERNAME`, and `CI_APP_CODEBLOOM_EMAIL_PASSWORD` secrets must configure a real SMTP sender. CI also needs the existing `HEADSCALE_ADDRESS`, `HEADSCALE_USER`, and `HEADSCALE_PREAUTHKEY` secrets to reach staging LAS.
