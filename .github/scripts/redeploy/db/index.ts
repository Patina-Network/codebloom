import type { Environment } from "types";

import { $ } from "bun";
import { getEnvVariablesByPrefix } from "load-secrets/env/load";

export async function _migrateDb({
  environment,
  sha,
}: {
  environment: Environment;
  sha?: string;
}): Promise<void> {
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

  const migratorEnv = getEnvVariablesByPrefix("DB_MIGRATIOR_");
  const DATABASE_NAME =
    environment === "production" ? "codebloom-prod" : "codebloom-stg";

  await $.env({
    ...migratorEnv,
    DATABASE_NAME,
  })`./mvnw flyway:migrate -Dflyway.locations=filesystem:./db/migration`;
}
