import { GitHubClient } from "@tahminator/pipeline";
import { $ } from "bun";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

import { sendDiscordMessage } from "@/utils/discord/send-message";
import { getEnvVariablesByPrefix } from "@/utils/env";
import { backend } from "@/utils/run-backend-instance";
import { parseDbEnv, startLocalDb } from "@/utils/run-local-db";

const { actionUrl, skipDiscordMessage, sha } = await yargs(hideBin(process.argv))
  .option("actionUrl", { type: "string", demandOption: true })
  .option("skipDiscordMessage", { type: "boolean", default: false })
  .option("sha", { type: "string", default: "" })
  .strict()
  .parse();

async function main() {
  const {
    githubApp,
    discord,
    codebloomEmailHost,
    codebloomEmailPort,
    codebloomEmailType,
    codebloomEmailUsername,
    codebloomEmailPassword,
    e2eEmailImapHost,
    e2eEmailImapPort,
    e2eEmailUsername,
    e2eEmailPassword,
  } = parseCiEnv(process.env);
  const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");

  const github = githubApp ? await GitHubClient.createWithGithubAppToken(githubApp) : undefined;

  const repo = { owner: "Patina-Network", repository: "codebloom" };

  const check = github
    ? await github.statusCheck({
        ...repo,
        action: "create",
        sha,
        name: "E2E tests",
        status: "in_progress",
        detailsUrl: actionUrl,
        output: { title: "Running E2E tests", summary: "Testing this PR's head commit." },
      })
    : undefined;

  if (github && !check) {
    throw new Error("Failed to create E2E status check.");
  }

  let passed = false;

  try {
    await $`pnpm --dir js i --frozen-lockfile`;
    await $`pnpm --dir e2e i --frozen-lockfile`;
    await $`pnpm --dir e2e run e2e-install`;
    await $`pnpm --dir e2e exec playwright install --with-deps chrome msedge`;

    // The backend loads the real school verification template from its resources.
    await $`pnpm --dir email i --frozen-lockfile`;
    await $`cd email && ./email.sh`;

    {
      await using postgres = await startLocalDb("e2e");
      const env = {
        ...process.env,
        ...ciAppEnv,
        ...parseDbEnv(postgres),
        CODEBLOOM_EMAIL_HOST: codebloomEmailHost,
        CODEBLOOM_EMAIL_PORT: codebloomEmailPort,
        CODEBLOOM_EMAIL_TYPE: codebloomEmailType,
        CODEBLOOM_EMAIL_USERNAME: codebloomEmailUsername,
        CODEBLOOM_EMAIL_PASSWORD: codebloomEmailPassword,
        E2E_EMAIL_IMAP_HOST: e2eEmailImapHost,
        E2E_EMAIL_IMAP_PORT: e2eEmailImapPort,
        E2E_EMAIL_USERNAME: e2eEmailUsername,
        E2E_EMAIL_PASSWORD: e2eEmailPassword,
        CI: "true",
        VITE_MOCK: "false",
        SERVER_URL: "http://localhost:5173",
      };

      try {
        await backend.start({ ...env, SPRING_PROFILES_INCLUDE: "e2e" });
        await $.env(env)`pnpm --dir js run generate`;
        await $.env(env)`pnpm --dir js run typecheck`;
        await $.env(env)`pnpm --dir e2e run typecheck`;
      } finally {
        await backend.end();
      }

      await $.env(env)`pnpm --dir e2e exec playwright test`;
    }

    passed = true;
  } finally {
    if (github && check) {
      await github.statusCheck({
        ...repo,
        action: "update",
        checkRunId: check.id,
        detailsUrl: actionUrl,
        status: "completed",
        conclusion: passed ? "success" : "failure",
        output: {
          title: passed ? "E2E tests passed" : "E2E tests failed",
          summary: passed ? "All E2E tests passed." : "See the workflow logs for details.",
        },
      });
    }

    if (discord) {
      await sendDiscordMessage(
        discord.token,
        discord.channel,
        `The daily E2E test has ${passed ? "succeeded" : "failed"}. View more details here: ${actionUrl}`,
      );
    }
  }
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const githubApp = (() => {
    if (!sha) {
      return undefined;
    }

    const appId = ciEnv["_GITHUB_APP_APP_ID"];
    if (!appId) {
      throw new Error("Missing _GITHUB_APP_APP_ID from .env.ci");
    }

    const installationId = ciEnv["_GITHUB_APP_INSTALLATION_ID"];
    if (!installationId) {
      throw new Error("Missing _GITHUB_APP_INSTALLATION_ID from .env.ci");
    }

    const privateKey = ciEnv["_GITHUB_APP_PEM_CONTENT"];
    if (!privateKey) {
      throw new Error("Missing _GITHUB_APP_PEM_CONTENT from .env.ci");
    }

    return { appId, installationId, privateKey };
  })();

  const discord = (() => {
    if (skipDiscordMessage) {
      return undefined;
    }

    const token = ciEnv["DISCORD_TOKEN"];
    if (!token) {
      throw new Error("Missing DISCORD_TOKEN from .env.ci");
    }

    const channel = ciEnv["DISCORD_CHANNEL_ID"];
    if (!channel) {
      throw new Error("Missing DISCORD_CHANNEL_ID from .env.ci");
    }

    return { token, channel };
  })();

  const codebloomEmailHost = (() => {
    const v = ciEnv["CI_APP_CODEBLOOM_EMAIL_HOST"];
    if (!v) {
      throw new Error("Missing CI_APP_CODEBLOOM_EMAIL_HOST from .env.ci");
    }
    return v;
  })();

  const codebloomEmailPort = (() => {
    const v = ciEnv["CI_APP_CODEBLOOM_EMAIL_PORT"];
    if (!v) {
      throw new Error("Missing CI_APP_CODEBLOOM_EMAIL_PORT from .env.ci");
    }
    return v;
  })();

  const codebloomEmailType = (() => {
    const v = ciEnv["CI_APP_CODEBLOOM_EMAIL_TYPE"];
    if (!v) {
      throw new Error("Missing CI_APP_CODEBLOOM_EMAIL_TYPE from .env.ci");
    }
    return v;
  })();

  const codebloomEmailUsername = (() => {
    const v = ciEnv["CI_APP_CODEBLOOM_EMAIL_USERNAME"];
    if (!v) {
      throw new Error("Missing CI_APP_CODEBLOOM_EMAIL_USERNAME from .env.ci");
    }
    return v;
  })();

  const codebloomEmailPassword = (() => {
    const v = ciEnv["CI_APP_CODEBLOOM_EMAIL_PASSWORD"];
    if (!v) {
      throw new Error("Missing CI_APP_CODEBLOOM_EMAIL_PASSWORD from .env.ci");
    }
    return v;
  })();

  const e2eEmailImapHost = (() => {
    const v = ciEnv["E2E_EMAIL_IMAP_HOST"];
    if (!v) {
      throw new Error("Missing E2E_EMAIL_IMAP_HOST from .env.ci");
    }
    return v;
  })();

  const e2eEmailImapPort = (() => {
    const v = ciEnv["E2E_EMAIL_IMAP_PORT"];
    if (!v) {
      throw new Error("Missing E2E_EMAIL_IMAP_PORT from .env.ci");
    }
    return v;
  })();

  const e2eEmailUsername = (() => {
    const v = ciEnv["E2E_EMAIL_USERNAME"];
    if (!v) {
      throw new Error("Missing E2E_EMAIL_USERNAME from .env.ci");
    }
    return v;
  })();

  const e2eEmailPassword = (() => {
    const v = ciEnv["E2E_EMAIL_PASSWORD"];
    if (!v) {
      throw new Error("Missing E2E_EMAIL_PASSWORD from .env.ci");
    }
    return v;
  })();

  return {
    githubApp,
    discord,
    codebloomEmailHost,
    codebloomEmailPort,
    codebloomEmailType,
    codebloomEmailUsername,
    codebloomEmailPassword,
    e2eEmailImapHost,
    e2eEmailImapPort,
    e2eEmailUsername,
    e2eEmailPassword,
  };
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
