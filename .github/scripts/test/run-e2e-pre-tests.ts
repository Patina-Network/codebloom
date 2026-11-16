import { $ } from "bun";

import { getEnvVariablesByPrefix } from "@/utils/env";
import { backend } from "@/utils/run-backend-instance";
import { parseDbEnv, startLocalDb } from "@/utils/run-local-db";

async function main() {
  const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");

  await $`pnpm --dir js i --frozen-lockfile`;
  await $`pnpm --dir e2e i --frozen-lockfile`;

  await using postgres = await startLocalDb("e2e");
  const env = {
    ...process.env,
    ...ciAppEnv,
    ...parseDbEnv(postgres),
    SPRING_PROFILES_INCLUDE: "e2e",
  };

  try {
    await backend.start(env);
    await $.env(env)`pnpm --dir js run generate`;
    await $.env(env)`pnpm --dir e2e run test`;
  } finally {
    await backend.end();
  }
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
