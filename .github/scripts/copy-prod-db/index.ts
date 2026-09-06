import { $ } from "bun";
import { getEnvVariablesByPrefix } from "load-secrets/env/load";
import { updateCommitStatus } from "utils/update-commit-status";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

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

    const dbCreds = getEnvVariablesByPrefix("DB_MIGRATIOR_");

    const prodDb: Record<string, string> = {
      ...dbCreds,
      DATABASE_NAME: "codebloom-prod",
    };
    const stagingDb: Record<string, string> = {
      ...dbCreds,
      DATABASE_NAME: "codebloom-stg",
    };

    const flywayEnv = {
      ...stagingDb,
    };

    const pgEnv = {
      PROD_DATABASE_HOST: prodDb.DATABASE_HOST,
      PROD_DATABASE_PORT: prodDb.DATABASE_PORT,
      PROD_DATABASE_USER: prodDb.DATABASE_USER,
      PROD_DATABASE_PASSWORD: prodDb.DATABASE_PASSWORD,
      PROD_DATABASE_NAME: prodDb.DATABASE_NAME,
      STAGING_DATABASE_HOST: stagingDb.DATABASE_HOST,
      STAGING_DATABASE_PORT: stagingDb.DATABASE_PORT,
      STAGING_DATABASE_USER: stagingDb.DATABASE_USER,
      STAGING_DATABASE_PASSWORD: stagingDb.DATABASE_PASSWORD,
      STAGING_DATABASE_NAME: stagingDb.DATABASE_NAME,
    };

    console.log("Cleaning staging database...");
    await $.env(flywayEnv)`./mvnw flyway:clean -Dflyway.cleanDisabled=false`;

    console.log("Copying production database to staging...");
    // extensions are provisioned by `platform-infra`,
    // and Pulumi doesnt support provisioning extensions via each table's service acct
    // (instead, it goes thru root).
    //
    // this means, we cant try to add them back in this script, or it will fail the entire transaction.
    await $.env(pgEnv)`PGPASSWORD="$PROD_DATABASE_PASSWORD" pg_dump \
      --host="$PROD_DATABASE_HOST" \
      --port="$PROD_DATABASE_PORT" \
      --username="$PROD_DATABASE_USER" \
      --dbname="$PROD_DATABASE_NAME" \
      --verbose \
      --clean \
      --if-exists \
      --format=plain \
      | sed -E '/SET transaction_timeout/d; /^(DROP|CREATE|ALTER) EXTENSION/d; /^COMMENT ON EXTENSION/d' \
      | PGPASSWORD="$STAGING_DATABASE_PASSWORD" psql \
          --host="$STAGING_DATABASE_HOST" \
          --port="$STAGING_DATABASE_PORT" \
          --username="$STAGING_DATABASE_USER" \
          --dbname="$STAGING_DATABASE_NAME" \
          --echo-errors \
          --single-transaction`;

    console.log("Cleaning unneccesary data...");
    await $.env(pgEnv)`PGPASSWORD="$STAGING_DATABASE_PASSWORD" psql \
      --host="$STAGING_DATABASE_HOST" \
      --port="$STAGING_DATABASE_PORT" \
      --username="$STAGING_DATABASE_USER" \
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
