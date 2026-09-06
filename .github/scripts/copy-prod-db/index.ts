import { $ } from "bun";
import { updateCommitStatus } from "utils/update-commit-status";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

import { getEnvVariables } from "../load-secrets/env/load";

const AUTHORIZED_USERS = ["tahminator", "angelayu0530"];

const { runUrl, username, sha } = await yargs(hideBin(process.argv))
  .options("runUrl", {
    type: "string",
    describe: "Run url for action",
  })
  .options("username", {
    type: "string",
    describe: "Username of person who triggered action",
    demandOption: true,
  })
  .options("sha", {
    type: "string",
    describe: "Commit SHA",
    demandOption: true,
  })
  .strict()
  .parse();

async function main() {
  try {
    await updateCommitStatus({
      sha,
      state: "pending",
      description: "Database copy in progress...",
      targetUrl: runUrl,
      context: "Copy Production DB to Staging",
    });

    if (!AUTHORIZED_USERS.includes(username)) {
      throw new Error("You are not authorized!");
    }

    const env = await getEnvVariables(["production"], {
      baseDir: "infra",
    });

    const flywayEnv = {
      ...env,
      DATABASE_NAME: env.STAGING_DATABASE_NAME,
    };

    const pgEnv = {
      ...env,
      PGPASSWORD: env.DATABASE_PASSWORD,
    };

    console.log("Cleaning staging database...");
    await $.env(flywayEnv)`./mvnw flyway:clean -Dflyway.cleanDisabled=false`;

    console.log("Copying production database to staging...");
    await $.env(pgEnv)`PGPASSWORD="$DATABASE_PASSWORD" pg_dump \
      --host="$DATABASE_HOST" \
      --port="$DATABASE_PORT" \
      --username="$DATABASE_USER" \
      --dbname="$PRODUCTION_DATABASE_NAME" \
      --verbose \
      --clean \
      --if-exists \
      --format=plain \
      | sed '/SET transaction_timeout/d' \
      | PGPASSWORD="$DATABASE_PASSWORD" psql \
          --host="$DATABASE_HOST" \
          --port="$DATABASE_PORT" \
          --username="$DATABASE_USER" \
          --dbname="$STAGING_DATABASE_NAME" \
          --echo-errors \
          --single-transaction`;

    console.log("Cleaning unneccesary data...");
    await $.env(pgEnv)`PGPASSWORD="$DATABASE_PASSWORD" psql \
      --host="$DATABASE_HOST" \
      --port="$DATABASE_PORT" \
      --username="$DATABASE_USER" \
      --dbname="$STAGING_DATABASE_NAME" \
      --set ON_ERROR_STOP=on \
      --file ./infra/clean-stg-db.SQL`;

    await updateCommitStatus({
      sha,
      state: "success",
      description: "Database copy completed successfully",
      targetUrl: runUrl,
      context: "Copy Production DB to Staging",
    });
  } catch (e) {
    await updateCommitStatus({
      sha,
      state: "failure",
      description: "Database copy failed",
      targetUrl: runUrl,
      context: "Copy Production DB to Staging",
    });
    throw e;
  }
}

main()
  .then(() => {
    process.exit();
  })
  .catch(() => {
    process.exit(1);
  });
