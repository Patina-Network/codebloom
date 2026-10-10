import { GitHubClient } from "@tahminator/pipeline";
import { $ } from "bun";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

import { getEnvVariablesByPrefix } from "@/utils/env";

const AUTHORIZED_USERS = ["tahminator", "angelayu0530"];

const { runUrl, sha, username } = await yargs(hideBin(process.argv))
  .option("runUrl", {
    type: "string",
    describe: "Workflow run URL for the status check",
    demandOption: true,
  })
  .option("sha", {
    type: "string",
    describe: "PR head commit SHA",
    demandOption: true,
  })
  .option("username", {
    type: "string",
    describe: "Username of the person who triggered the command",
    demandOption: true,
  })
  .strict()
  .parse();

export async function main() {
  const { githubAppAppId, githubAppInstallationId, githubAppPrivateKey } = parseCiEnv(process.env);

  const ghClient = await GitHubClient.createWithGithubAppToken({
    appId: githubAppAppId,
    installationId: githubAppInstallationId,
    privateKey: githubAppPrivateKey,
  });

  const repo = { owner: "Patina-Network", repository: "codebloom" };
  const check = await ghClient.statusCheck({
    ...repo,
    action: "create",
    sha,
    name: "Copy Production DB to Staging",
    status: "in_progress",
    detailsUrl: runUrl,
    output: {
      title: "Checking authorization",
      summary:
        "Verifying the caller's membership in @Patina-Network/dino and the copy allowlist...",
    },
  });
  if (!check) {
    throw new Error("Failed to create database copy status check.");
  }
  const update = {
    ...repo,
    action: "update" as const,
    checkRunId: check.id,
    detailsUrl: runUrl,
  };

  try {
    const isTeamMember = await ghClient.isTeamMember({
      org: "Patina-Network",
      teamSlug: "dino",
      username,
    });
    if (!isTeamMember) {
      throw new Error(
        "Only members of @Patina-Network/dino can copy the production database to staging.",
      );
    }

    if (!AUTHORIZED_USERS.includes(username)) {
      throw new Error(
        `Only ${AUTHORIZED_USERS.join(", ")} can copy the production database to staging.`,
      );
    }

    await ghClient.statusCheck({
      ...update,
      status: "in_progress",
      output: {
        title: "Copying production database",
        summary: "Cleaning codebloom-stg and restoring it from codebloom-prod.",
      },
    });

    await copyProdToStaging();

    await ghClient.statusCheck({
      ...update,
      status: "completed",
      conclusion: "success",
      output: {
        title: "Database copy successful",
        summary: "codebloom-prod was copied to codebloom-stg and cleaned.",
      },
    });
  } catch (error) {
    await ghClient
      .statusCheck({
        ...update,
        status: "completed",
        conclusion: "failure",
        output: {
          title: "Database copy failed",
          summary: "Database copy failed. See the workflow logs for details.",
        },
      })
      .catch((statusError) => {
        console.error("Failed to report database copy failure:", statusError);
      });
    throw error;
  }
}

async function copyProdToStaging() {
  const dbCreds = getEnvVariablesByPrefix("DB_MIGRATIOR_");

  const prodDb: Record<string, string> = {
    ...dbCreds,
    DATABASE_NAME: "codebloom-prod",
  };
  const stagingDb: Record<string, string> = {
    ...dbCreds,
    DATABASE_NAME: "codebloom-stg",
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
  await $.env(stagingDb)`./mvnw -B -ntp flyway:clean -Dflyway.cleanDisabled=false`;

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
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const githubAppAppId = ciEnv["_GITHUB_APP_APP_ID"];
  if (!githubAppAppId) {
    throw new Error("Missing _GITHUB_APP_APP_ID from env");
  }

  const githubAppInstallationId = ciEnv["_GITHUB_APP_INSTALLATION_ID"];
  if (!githubAppInstallationId) {
    throw new Error("Missing _GITHUB_APP_INSTALLATION_ID from env");
  }

  const githubAppPrivateKey = ciEnv["_GITHUB_APP_PEM_CONTENT"];
  if (!githubAppPrivateKey) {
    throw new Error("Missing _GITHUB_APP_PEM_CONTENT from env");
  }

  return { githubAppAppId, githubAppInstallationId, githubAppPrivateKey };
}

if (import.meta.main) {
  await main();
}
