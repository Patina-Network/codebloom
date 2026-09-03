import type { Environment } from "types";

import { $ } from "bun";
import { getEnvVariablesByPrefix } from "load-secrets/env/load";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { environment, sha } = await yargs(hideBin(process.argv))
  .option("environment", {
    choices: ["staging", "production"] satisfies Environment[],
    describe: "Deployment environment (staging or production)",
    default: "staging" satisfies Environment as Environment,
  })
  .option("sha", {
    type: "string",
    describe: "Commit SHA (required for staging)",
    default: "",
  })
  .check(({ sha, environment }) => {
    if (environment === "staging" && !sha) {
      throw new Error(
        "SHA must be available in ENV if script is being run in staging environment.",
      );
    } else {
      return true;
    }
  })
  .strict()
  .parse();

export async function main() {
  // make sure you checkout the repo first.

  if (environment === "staging") {
    await $`git fetch origin main:main`;
    const diffOutput = await $`git diff --name-only main...${sha}`.text();
    const files = diffOutput.split("\n");

    const hasDbChanges = files.some((file) => file.startsWith("db/"));
    if (!hasDbChanges) {
      console.log("in staging, skipping db migration.");
      return;
    }
  }

  const appEnv =
    environment === "staging" ?
      getEnvVariablesByPrefix("STAGING_DB_MIGRATIOR_")
    : {
        ...getEnvVariablesByPrefix("DB_MIGRATIOR_"),
        DATABASE_NAME: "codebloom-prod",
      };

  await $.env({
    ...process.env,
    ...appEnv,
  })`bash -c "./mvnw flyway:validate -Dflyway.locations=filesystem:./db/migration -Dflyway.ignoreMigrationPatterns='*:pending'"`;
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
