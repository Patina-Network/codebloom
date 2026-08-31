import { $ } from "bun";
import { getEnvVariablesFromProcess } from "load-secrets/env/load";
import { sendDiscordMessage } from "utils/discord/send-message";
import { backend } from "utils/run-backend-instance";
import { db } from "utils/run-local-db";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { actionUrl, skipDiscordMessage } = await yargs(hideBin(process.argv))
  .option("actionUrl", {
    type: "string",
    demandOption: true,
  })
  .option("skipDiscordMessage", {
    type: "boolean",
    demandOption: true,
    default: false,
  })
  .strict()
  .parse();

async function main() {
  const { discordToken, discordChannelId } = parseCiEnv(process.env);
  try {
    const ciAppEnv = getEnvVariablesFromProcess("CI_APP_");

    //type-gen
    try {
      const dbEnv = await db.start();
      const env = { ...process.env, ...ciAppEnv, ...dbEnv };
      await backend.start(env);
      await $.env(env)`pnpm --dir js i --frozen-lockfile`;
      await $.env(env)`pnpm --dir js run generate`;
      await $.env(env)`pnpm --dir js run typecheck`;
    } finally {
      await backend.end();
      await db.end();
    }

    const dbEnv = await db.start();

    const c$ = $.env({
      ...process.env,
      ...dbEnv,
      ...ciAppEnv,
      CI: "true",
    });

    await c$`pnpm --dir e2e i --frozen-lockfile`;
    await c$`pnpm --dir e2e e2e`;

    if (!skipDiscordMessage) {
      await sendDiscordMessage(
        discordToken,
        discordChannelId,
        `The daily E2E test has succeeded. View more details here: ${actionUrl}`,
      );
    }
  } catch (e) {
    if (!skipDiscordMessage) {
      await sendDiscordMessage(
        discordToken,
        discordChannelId,
        `The daily E2E test has failed. View more details here: ${actionUrl}`,
      );
    }
    throw e;
  } finally {
    await db.end();
  }
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });

function parseCiEnv(env: Record<string, string | undefined>) {
  const discordToken = env["DISCORD_TOKEN"];
  if (!discordToken) {
    throw new Error("Missing DISCORD_TOKEN from .env.ci");
  }

  const discordChannelId = env["DISCORD_CHANNEL_ID"];
  if (!discordChannelId) {
    throw new Error("Missing DISCORD_CHANNEL_ID from .env.ci");
  }

  return { discordToken, discordChannelId };
}
