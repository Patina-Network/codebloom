# Codebloom Standup Bot

Codebloom Standup Bot is an internal application written in [Rust](https://rust-lang.org/) & [Tokio](https://tokio.rs/) that houses the logic required to send the Codebloom development team's daily standup message.

### Requirements

- [`rustup`](https://rust-lang.org/tools/install/) - the Rust toolchain/package manager/build tool
- [`redis`](https://redis.io/) - Used to track last time standup message was sent

### Run

1. Create `.env`
1. Copy everything from `example.env` to `.env`
1. Fill out your `.env` file
1. Simply run `cargo run` to compile & run the binary.

`DISCORD_ROLE_ID` is required and must be an unsigned 64-bit integer. It selects
the role mentioned in standup messages and the members eligible for end-of-day
reminders. Set it in `.env` locally and in the deployed service's environment.

### Deployment

_Last updated: 02/15/2026_

- The [`./Dockerfile`](./Dockerfile) is used to build an image, which is uploaded to [hub.docker.com/r/tahminator/codebloom-standup-bot](https://hub.docker.com/r/tahminator/codebloom-standup-bot)
- The service is currently deployed to a personal VPS owned by [@tahminator](https://github.com/tahminator)
    > [!NOTE]
    > We currently deploy it this way due to the fact that the current architecture for our main service relies on DigitalOcean App Platform, and paying another $5/mo for this small service seemed overkill. In the future, we would like to find a better alternative.
- The composite workflow to deploy this service can be found at [`.github/composite/redeploy/internal/standup-bot/action.yml`](../../.github/composite/redeploy/internal/standup-bot/action.yml)
- The [Bun Shell](https://bun.com/docs/runtime/shell) scripts used to deploy this service can be found at [`.github/scripts/redeploy/internal/standup-bot/index.ts`](../../.github/scripts/redeploy/internal/standup-bot/index.ts)
