# `e2e/`

This directory contains Codebloom E2E tests written in [`TypeScript`](https://www.typescriptlang.org/) with [`Playwright`](https://playwright.dev/).

These tests run daily at 12:18 ET every day under [.github/scripts/test/run-e2e-tests.ts](../.github/scripts/test/run-e2e-tests.ts).

> [!NOTE]
> GitHub scheduled workflows can be delayed during periods of high loads.

## Requirements

- `pnpm` - Follow the main developer installation instructions under [local setup](https://docs.patinanetwork.org/codebloom/local/SETUP/).

## Run

To run all e2e tests, simply use the following snippet below:

```bash
# will automatically launch the backend server & frontend server.
pnpm run e2e
```

To utilize Playwright codegen (click to generate test code), run the following command:

```bash
pnpm run e2e-codegen
```

You can pass in any extra args supported by Playwright from this documentation [here](https://playwright.dev/docs/running-tests#run-specific-tests).

> [!NOTE]
> In order to have a clean `e2e` run locally, run `just drop && just migrate` to reset your local database.
