<a id="readme-top"></a>

<h1>
  <a href="https://codebloom.patinanetwork.org">
    <img src="js/public/logo.png" alt="Logo" width="40" height="40" align="absmiddle">
  </a>
  <a href="https://codebloom.patinanetwork.org">
    codebloom.patinanetwork.org
  </a>
</h1>

[![PROD SHA](https://img.shields.io/badge/dynamic/json?label=PROD%20SHA&query=$.payload.version&style=flat&url=https://codebloom.patinanetwork.org/api)](https://codebloom.patinanetwork.org/api)
[![BACKEND COVERAGE](https://img.shields.io/sonar/coverage/codebloom_backend?server=https%3A%2F%2Fsonarcloud.io&style=flat&label=BACKEND%20COVERAGE)](https://sonarcloud.io/dashboard?id=codebloom_backend)
[![FRONTEND COVERAGE](https://img.shields.io/sonar/coverage/codebloom_frontend?server=https%3A%2F%2Fsonarcloud.io&style=flat&label=FRONTEND%20COVERAGE)](https://sonarcloud.io/dashboard?id=codebloom_frontend)
[![TOTAL BUILDS](https://img.shields.io/badge/dynamic/json?label=TOTAL%20BUILDS&query=$.total_count&url=https://api.github.com/repos/tahminator/codebloom/actions/runs&style=flat&color=blue)](https://github.com/tahminator/codebloom/actions)

<div align="center">
  <img src="screenshots/stg-landing.png" alt="Landing" width="100%" style="margin-bottom: -20px;">
  <img src="screenshots/stg-leaderboard.png" alt="Leaderboard" width="48%">
  <img src="screenshots/stg-dashboard.png" alt="Dashboard" width="48%">
</div>

Codebloom is a leaderboard system that game-ifies the aspect of solving LeetCode problems in order to help motivate engineers of all levels (students, new grads, FTEs) study for their technical interviews.

## Features

_Last updated: 02/14/2026_

Codebloom features include

- [User profiles with submission history](./docs/features/USER-PROFILE.md)
- [Automatic LeetCode syncing in the background](./docs/features/BGSYNC.md)
- [University-specific leaderboards](./docs/features/LEADERBOARD.md)
- [Problem Of The Day](./docs/features/POTD.md)
- [Automatic achievements](./docs/features/ACHIEVEMENTS.md)
- [Embeddable widgets](./docs/features/EMBED.md)
- [Dynamic question scoring](./docs/features/SCORES.md)
- [Live multiplayer duels](./docs/features/DUELS.md)
- [Club/organization integrations via Discord servers](./docs/features/DISCORD-INTEGRATIONS.md)

[Goto `docs/features` to view all feature documentation](./docs/features)

Codebloom is a public-first platform that does not require authentication to view most data, so please feel free to click around at [codebloom.patinanetwork.org](https://codebloom.patinanetwork.org)!

## Structure

_Last updated: 02/14/2026_

This repository is a monorepo that contains many different pieces of Codebloom, which include:

- Our public task board on [Notion](https://www.notion.so/) which is used to define a relationship between tasks and PRs
  - [View public taskboard](https://codebloom.notion.site/)
  - [Goto `.github/scripts/notion/index.ts` to view custom PR/commit check validation implementation](./.github/scripts/notion/index.ts)
- Our backend which is written in [Java 25](https://dev.java/) and [Spring Boot 3](https://spring.io/projects/spring-boot). The backend interfaces with our database via [pgJDBC](https://jdbc.postgresql.org/), Discord via [JDA](https://jda.wiki/), [leetcode.com](https://leetcode.com) via a custom abstraction over their [GraphQL](https://graphql.org/) API as well as [Playwright](https://playwright.dev/java/) to obtain credentials to [leetcode.com](https://leetcode.com) via [GitHub OAuth](https://github.com).
  - [Goto `src/`](./src/)
  - [View detailed documentation](./src/README.md)
  - [View OpenAPI schema](https://codebloom.patinanetwork.org/swagger-ui/index.html)
- Our main database is [PostgreSQL 16](https://www.postgresql.org/), which is controlled with [Flyway](https://www.red-gate.com/products/flyway/community/) migrations.
  - [Goto `db/`](./db/)
  - [View detailed documentation](./db/README.md)
- Our frontend which is written in [TypeScript](https://www.typescriptlang.org/) and [React 18](https://www.typescriptlang.org/) + [Vite](https://vite.dev/).
  - [Goto `js/`](./js/)
  - [View detailed documentation](./js/README.md)
- Our email templater which utilizes [React Email](https://react.email/) and is dynamically parsed & templated at runtime with [JSoup](https://jsoup.org/) in our backend.
  - [Goto `email/`](./email/)
  - [View detailed documentation](./email/README.md)
- Our CI/CD pipeline which utilizes [GitHub Actions](https://github.com/features/actions) with [TypeScript](https://typescriptlang.org/) + [Bun Shell](https://bun.com/docs/runtime/shell). These scripts utilize the [Notion SDK](https://github.com/makenotion/notion-sdk-js), the [DigitalOcean DoTs SDK](https://github.com/digitalocean/dots) and the [Octokit SDK](https://github.com/octokit/octokit.js).
  - [Goto `.github/`](./.github/)
  - [Goto `.github/scripts/` to view all Bun Shell scripts](./.github/scripts/)
  - [View detailed documentation](./.github/READMEFIRST.md)
- Our service gets deployed to two environments (production & staging) in [DigitalOcean App Platform](https://www.digitalocean.com/products/app-platform), which is defined using infrastructure-as-code. We containerize the main application with [Docker](https://www.docker.com/) and monitor metrics & logs with [Prometheus](https://prometheus.io/) & [OpenSearch](https://opensearch.org/) which get fed to [Grafana](https://grafana.com/).
  - [Goto `.github/scripts/redeploy/` to view Bun Shell redeploy scripts](./.github/scripts/redeploy/)
  - [Goto `infra/`](./infra/)
  - [View detailed documentation](./infra/README.md)
  - [View observability documentation](./docs/observability/README.md)
- Our internal tools (e.g. custom Ink CLI scripts & our internal standup bot) are currently being written in [Rust](https://rust-lang.org/) and [TypeScript](https://typescriptlang.org)
  - [Goto `internal/`](./internal/)
  - [View detailed documentation](./internal/README.md)
- Our secrets across all environments which are decryped with [git-crypt](https://github.com/AGWA/git-crypt).
  - [Goto `example.env`](./example.env)
  - [View detailed documentation](./docs/local/SECRET.md)

## Setup

Please go to [`docs/setup/README.md`](./docs/local/README.md) to begin setup instructions for local development.

## Authors

The current lead developer of the Codebloom development team is Arshadul Monir. If you have any inquires or questions, please direct them to him.

<!-- TODO: Add new authors -->
<!-- The current members of the core development team are: -->
<!---->
<!-- - Arshadul Monir -->

> [!NOTE]
> Codebloom is open-source and happily accepts contributions from the community. However, all pull requests are subject to review by the development team. Please click [here](./CONTRIBUTING.md) to learn more.

### Thank you <3

Thank you to all the previous members of the Codebloom development team that have helped make Codebloom possible <3

- Tahmid Ahmed (Previous lead developer, founding member)
- Alfardil Alam (Previous developer, founding member)
- Alisha Zaman (Previous developer, founding member)
- Angela Yu (Previous developer)
- Nancy Huang (Previous developer)

## Patina Network

Codebloom is a project under [Patina Network](https://www.patinanetwork.org/), a non-profit organization that is on a mission to address the unique struggles of AANHPI women and foster the diverse, inclusionary leaders of tomorrow.

Wherever you are on your career and allyship journey, [join us today](https://www.patinanetwork.org/) to make friends, build skills, and boldly celebrate what makes you unique — your patina!

[Learn more about Patina Network here](https://www.patinanetwork.org/)

## LICENSE

The license can be found under [`./LICENSE`](./LICENSE)
