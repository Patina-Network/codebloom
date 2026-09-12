# `.github/scripts`

This directory contains helper scripts that are used in our CI/CD workflows. These scripts are written in [TypeScript](https://www.typescriptlang.org/) and [Bun Shell](https://bun.com/docs/runtime/shell).

## Structure

```
.github/scripts
├── build-image                         # helps build image and (optionally) deploy to Docker Hub
├── copy-prod-db                        # helps copy prod db to staging db
├── help-message                        # leaves one-time comment in PR indicating all possible slash commands
├── load-secrets                        # used to load environment variables (and automatically mask them in GitHub Actions) as a JS object.
├── notion                              # notion-specific logic (includes helper functions that can be shared as well as a `main()` function to directly run Notion verification checks against PR & commits)
├── patches                             # bun patches applied to certain packages to fulfill our need
├── redeploy                            # redeployment logic (db migrations and Kubernetes manifest PRs)
├── test                                # includes multiple different test flows (backend, frontend, compile checks only)
├── types.ts                            # shared types
├── utils                               # shared utils
│   ├── colors.ts                       # colors to apply to stdout
│   ├── run-backend-instance.ts         # shared function to run backend in CI asynchronously
│   ├── run-frontend-instance.ts        # shared function to run frontend in CI asynchronously
│   ├── run-local-db.ts                 # shared function to run a local pg db in CI asynchronously
│   ├── send-message                    # shared function to send a message to GitHub PR
│   ├── update-commit-status            # shared function to add/update commit status to GitHub commit
│   ├── update-pr-description.ts        # shared function to update description of GitHub PR
│   └── upload.ts                       # shared function to deploy test coverage information to our test coverage providers
└── validate-db                         # helps validate current db changes against a certain database to ensure data integrity
```

## Requirements

- `bun` - We would recommend that you install it using `brew install bun` but feel free to use whatever you want.

## Run

Run scripts from the repository root with Bun. Each script declares its required arguments in its `yargs` configuration.

For deployment, `redeploy/index.ts` requires `--sha` (or `GITHUB_SHA`) and GitHub App credentials in the process environment. Select the environment with `--environment staging|production`. Running the script locally does not automatically load environment files.
