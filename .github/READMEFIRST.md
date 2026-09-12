# `.github/`

This directory holds the Codebloom infrastructure code. That includes workflows & composite actions directly interfacing with [GitHub Actions](https://github.com/features/actions) as well as scripts written in [TypeScript](https://www.typescriptlang.org/) and [Bun Shell](https://bun.com/docs/runtime/shell) to handle more complex logic.

[View `.github/scripts` which includes it's own documentation](./scripts/)

## Structure

```
.github/workflows
├── ai-command.yml              # wrapper that allows us to type /ai to run all AI agent commands at once
├── ai-review.yml               # Qodo review agent (includes custom logic to inject Notion task context to AI agent)
├── approval.yml                # DEPRECATED - used to handle custom approval rules not natively supported by GitHub
├── ci-cd.yml                   # Main CI/CD pipeline that runs tests, builds images, triggers deployments, and more.
├── copy-command.yml                 # command to copy prod db to staging db
├── e2e.yml                     # daily E2E tests
├── help-command.yml            # one-time command on PR launch to show all available slash commands
├── pr-verifications.yml        # validates PR & commits against Notion
└── slash.yml                   # helper workflow to trigger slash command invocations to the right workflow
```

<div align="center"><i>Last updated: 02/15/2026</i></div>

## Commands

> [!NOTE]
> Every PR now includes an automatic action that will publish the list of all slash commands. As such, this section may be out of date and deprecated soon.

### Staging

All PRs must be tested in our staging environment to make sure that it won't break anything (if there's a reason it cannot be tested in staging, it must be indicated in the PR description).

`/deploy` is registered, but its target `deploy-command.yml` workflow is missing from this checkout, so it cannot currently trigger staging deployment.

> [!NOTE]
> It should be a regular comment inside of the first page of the PR; writing the command as a review on a file or line(s) will not trigger deployment.

### AI Code Review

The following slash commands can be used in PR comments to trigger AI-powered code review:

| Command     | Description                                                                  |
| ----------- | ---------------------------------------------------------------------------- |
| `/ai`       | Triggers all AI review commands (`/review`, `/describe`, `/improve`) at once |
| `/review`   | Triggers an AI review of the PR changes                                      |
| `/describe` | Generates an AI-powered description of the PR                                |
| `/improve`  | Provides AI-powered suggestions                                              |

These commands use Qodo PR-Agent and integrate with Notion to include task context in reviews.
