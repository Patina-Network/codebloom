# `internal/`

This directory houses applications & services that are more related to the developers of Codebloom in comparison to the client-facing servers & UIs to codebloom.

## Structure

_Last updated: 11/03/2026_

This `internal/` directory currently consists of

- `standup-bot`: a Discord bot written in [Rust](https://rust-lang.org/) to send daily standup messages to our private Discord server.
  - [View detailed documentation](./standup-bot/README.md)
  - [View detailed documentation](./scripts/README.md)
- `leetcode-auth-service`: a Rust gRPC service that stores the leetcode.com credentials in Redis and refreshes them through `leetcode-auth-stealer-service`.
  - [View detailed documentation](./leetcode-auth-service/README.md)
- `leetcode-auth-stealer-service`: a Python gRPC authentication service that replaces the old auth stealer service with a direct email/password one.
  - [View detailed documentation](./leetcode-auth-stealer-service/README.md)
