# `email/`

This directory contains a [React Email](https://react.email/) application which is used to write email templates using [React JSX](https://react.dev/).

These templates are then built into `*.html` files which are then loaded at runtime to dynamically insert variables before sending the email off. View [Backend Implementation](#backend-implementation) below for details.

### Requirements

- [pnpm](https://pnpm.io/)
    > It is **STRONGLY** recommended that you follow the setup directions for `pnpm` inside of [docs/setup/README.md](../../docs/setup/README.md)

### Run

1. Run `pnpm i` to install all dependencies
1. Run `just email-dev` from the root directory to run the React Email dev server with sane defaults
1. Run `pnpm build` to build all React Email files under `emails/`
1. If you would like to build & copy all template files to the backend, run `just email-gen` from the root directory

# Backend Implementation

_Last updated: 02/15/2026_

[`ReactEmailTemplater.java`](../src/main/java/org/patinanetwork/codebloom/common/email/template/ReactEmailTemplater.java) is the wrapper that allows the backend to use React Email templates.

> [!NOTE]
> Find [`ReactEmailTemplaterImpl.java`](../src/main/java/org/patinanetwork/codebloom/common/email/template/ReactEmailTemplaterImpl.java) here

## Implementation

1. `ReactEmailTemplater` will load the classpath HTML resource into a string.

1. This will then be passed into [`JSoup`](https://jsoup.org/) so we can use DOM operations to insert our variables into the template.

1. Once complete, we will then return the modified template back as a string.

## Examples

- [`ReactEmailTemplaterTest.java`](../src/test/java/org/patinanetwork/codebloom/reactEmail/ReactEmailTemplaterTest.java)
- [`AuthController.java`](../src/main/java/org/patinanetwork/codebloom/api/auth/AuthController.java#L122-L168) where the school email template is used to send school verification emails
