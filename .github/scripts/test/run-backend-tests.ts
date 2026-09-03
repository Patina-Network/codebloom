import { $ } from "bun";
import { getEnvVariablesByPrefix } from "load-secrets/env/load";
import { backend } from "utils/run-backend-instance";
import { frontend } from "utils/run-frontend-instance";
import { db } from "utils/run-local-db";
import { uploadBackendTests } from "utils/upload";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { shouldUploadCoverage } = await yargs(hideBin(process.argv))
  .option("shouldUploadCoverage", {
    type: "boolean",
    default: false,
  })
  .strict()
  .parse();

async function main() {
  try {
    const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");
    const localDbEnv = await db.start();

    // backend starts so we can generate schema, then kill it.
    await backend.start({ ...ciAppEnv, ...localDbEnv });
    await frontend.start(ciAppEnv);
    await backend.end();

    if (!localDbEnv) {
      throw new Error("Local db empty when it should not be");
    }

    const $$ = $.env({
      ...process.env,
      ...ciAppEnv,
      ...localDbEnv,
    });

    await $`./mvnw -B install -D skipTests --no-transfer-progress`;

    await $`./mvnw -B exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"`;

    await $`cd email && pnpm i --frozen-lockfile && ./email.sh && cd ..`;

    await $$`./mvnw clean verify -Dspring.profiles.active=ci`;

    if (shouldUploadCoverage) {
      const { sonarToken } = parseCiEnv(process.env);
      await uploadBackendTests(sonarToken);
    }
  } finally {
    await db.end();
    await frontend.end();
    await backend.end();
  }
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const sonarToken = (() => {
    const v = ciEnv["SONAR_TOKEN"];
    if (!v) {
      throw new Error("Missing SONAR_TOKEN from .env.ci");
    }
    return v;
  })();

  return { sonarToken };
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
