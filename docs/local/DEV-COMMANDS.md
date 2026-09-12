> [!NOTE]
> Link to Justfile can be found [here](../../Justfile)

# Shared

`just dev` - Will run both the backend and frontend development server at the same time.

`just devd` - Will run both the backend and frontend development server at the same time, but the backend will be in debug mode. See `just backend-dev-debug`.

# Database

`just drop` - Will drop your local database's public schema using the credentials provided in `.env`

`just migrate` - Will migrate your local database using the credentials provided in `.env`

`just migrate-prod` - Will manually migrate production database using the credentials provided in `.env.production`

> [!WARNING]
> Should be used sparingly

`just migrate-stg` - Migrate staging using `.env.staging` and versioned migrations only.

`just copy-stg` currently references the missing `infra/copy-prod-db.sh` and does not work. The implemented production-to-staging copy is the `/copy` PR command, backed by [copy-command.yml](../../.github/workflows/copy-command.yml) and [copy-prod-db/index.ts](../../.github/scripts/copy-prod-db/index.ts). It replaces staging data and then sanitizes it.

# Frontend

`just dev` - Will run both the backend and frontend development server at the same time.

`just frontend-install` - Download any missing frontend dependencies. An alias for `cd js && pnpm i`.

`just frontend-dev` - Will only start the frontend Vite dev server.

`just type-gen` - Regenerate the `schema.ts` file.

> [!WARNING]
> The backend must be running

`just frontend-test` - Run the entire frontend test suite - linters, autoformatters, typechecking, etc

# Backend

`just backend-install` - Builds and installs Spring backend. An alias for `./mvnw install -DskipTests=true`.

`just backend-dev` - Will only start the backend Spring dev server.

`just backend-dev-debug` - Will only start the backend Spring dev server, but will wait for a JVM debugger to attach to port 5005 first.

`just backend-test` - Check Spotless formatting, then run Checkstyle and Maven verification with the `ci` profile, loading `.env` and `.env.shared`.

After `just backend-test`, view the JaCoCo report at `target/site/jacoco/index.html`. There is currently no `backend-coverage` recipe.

`just backend-spotless` - Runs the backend formatter (currently Spotless with Palantir Java Formatter) and indicates whether or not you need to run the formatter on any files.

`just backend-spotless-fix` - Runs the backend formatter (currently Spotless with Palantir Java Formatter) and will write to any files that have not been formatted yet.

# React Email

`just email-dev` - Run the React Email dev server.

`just email-gen` - Will run `email/email.sh` which will build all React Email components into HTML and pass them into the backend's static folder.

> [!WARNING]
> This is mainly used in development.
