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
├── redeploy                            # redeployment logic (db migrations, DigitalOcean, Coolify)
├── test                                # includes multiple different test flows (backend, frontend, compile checks only)
├── types.ts                            # shared types
├── utils                               # shared utils
│   ├── colors.ts                       # colors to apply to stdout
│   ├── run-backend-instance.ts         # shared function to run backend in CI asynchronously
│   ├── run-frontend-instance.ts        # shared function to run frontend in CI asynchronously
│   ├── run-local-db.ts                 # shared function to run a local pg db in CI asynchronously
│   ├── send-message                    # shared function to send a message to GitHub PR
│   ├── update-pr-description.ts        # shared function to update description of GitHub PR
│   └── upload.ts                       # shared function to deploy test coverage information to our test coverage providers
└── validate-db                         # helps validate current db changes against a certain database to ensure data integrity
```

## Requirements

- `bun` - We would recommend that you install it using `brew install bun` but feel free to use whatever you want.

## Run

To run a script, simply use the following snippet below:

```bash
bun run .github/scripts/redeploy/index.ts

# or use shorthand since it's an index.ts file

bun run .github/scripts/redeploy

# you do not need to call `dotenvx run --` before calling Bun Shell scripts. they already have a way to
# parse env files and use them as a JS object at runtime (masked in GitHub Actions).

# if you need to explicitly pass in an environment variable, do it like so
ENVIRONMENT=staging bun run .github/scripts/redeploy
```
