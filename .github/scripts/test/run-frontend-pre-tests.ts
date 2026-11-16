import { $ } from "bun";

import { getEnvVariablesByPrefix } from "@/utils/env";
import { backend } from "@/utils/run-backend-instance";
import { parseDbEnv, startLocalDb } from "@/utils/run-local-db";

async function main() {
  const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");
  await using postgres = await startLocalDb();
  const localDbEnv = parseDbEnv(postgres);

  await backend.start({ ...ciAppEnv, ...localDbEnv });

  const $$ = $.env({
    ...process.env,
    ...ciAppEnv,
    ...localDbEnv,
  });
  //type
  try {
    await $$`pnpm --dir js run generate`;
    await $$`pnpm --dir js run typecheck`;
  } finally {
    await backend.end();
  }

  // fmt
  await $`pnpm --dir js run prettier`;

  // lint
  await $`pnpm --dir js run lint`;

  // compile
  await $`pnpm --dir js run build`;
}

main()
  .then(() => {
    process.exit();
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
