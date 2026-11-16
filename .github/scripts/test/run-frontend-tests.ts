import { $ } from "bun";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

import { getEnvVariablesByPrefix } from "@/utils/env";
import { backend } from "@/utils/run-backend-instance";
import { parseDbEnv, startLocalDb } from "@/utils/run-local-db";
import { uploadFrontendTests } from "@/utils/upload";

const { shouldUploadCoverage } = await yargs(hideBin(process.argv))
  .option("shouldUploadCoverage", {
    type: "boolean",
    default: false,
  })
  .strict()
  .parse();

async function main() {
  const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");
  await using postgres = await startLocalDb();
  const localDbEnv = parseDbEnv(postgres);

  try {
    await backend.start({ ...ciAppEnv, ...localDbEnv });

    const $$ = $.env({
      ...process.env,
      ...ciAppEnv,
      ...localDbEnv,
    });
    await $$`pnpm --dir js run generate`;
    await $$`pnpm --dir js run test`;

    if (shouldUploadCoverage) {
      const { sonarToken } = parseCiEnv(process.env);

      await uploadFrontendTests(sonarToken);
    }
  } finally {
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
