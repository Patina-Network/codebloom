import type { Environment, Type } from "types";

import { DockerClient, GitHubClient } from "@tahminator/pipeline";
import { $ } from "bun";
import { getEnvVariablesByPrefix } from "load-secrets/env/load";
import { backend } from "utils/run-backend-instance";
import { db } from "utils/run-local-db";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

process.env.TZ = "America/New_York";

const {
  environment,
  dockerUpload,
  getGhaOutput,
  githubOutputFile,
  type,
  prId,
} = await yargs(hideBin(process.argv))
  .option("environment", {
    choices: ["staging", "production"] satisfies Environment[],
    describe: "Deployment environment (staging or production)",
    demandOption: true,
  })
  .option("dockerUpload", {
    type: "boolean",
    default: false,
    demandOption: true,
  })
  .option("getGhaOutput", {
    type: "boolean",
    describe:
      "Enable GitHub Actions output to receive latest built tag version",
    default: false,
  })
  .option("githubOutputFile", {
    type: "string",
    describe: "Path to GITHUB_OUTPUT (passed in automatically in CI)",
    default: process.env.GITHUB_OUTPUT,
  })
  .option("type", {
    choices: ["standup-bot", "web"] satisfies Type[],
    describe: "Type to build",
    demandOption: true,
    default: "web" as Type,
  })
  .option("prId", {
    type: "string",
    default: "",
    coerce: (v: string) => (v === "" ? undefined : Number(v)),
  })
  .strict()
  .parse();

const tagPrefix = environment === "staging" ? "staging-" : "";

async function main() {
  const {
    dockerHubPat,
    dockerHubUsername,
    githubAppAppId,
    githubAppInstallationId,
    githubAppPrivateKey,
  } = parseCiEnv(process.env);

  if (type === "web") {
    const ciAppEnv = getEnvVariablesByPrefix("CI_APP_");
    const localDbEnv = await db.start();
    await backend.start(ciAppEnv);

    try {
      const $$ = $.env({
        ...process.env,
        ...ciAppEnv,
        ...localDbEnv,
      });
      await $$`pnpm --dir js run generate`;
    } finally {
      await backend.end();
      await db.end();
    }
  }

  // copy old tz format from build-image.sh
  const timestamp = new Date()
    .toLocaleString("en-US", {
      timeZone: process.env.TZ,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    })
    .replace(/(\d+)\/(\d+)\/(\d+),\s(\d+):(\d+):(\d+)/, "$3.$1.$2-$4.$5.$6");

  const gitSha = (await $`git rev-parse --short HEAD`.text()).trim();

  await using dockerClient = await DockerClient.create(
    dockerHubUsername,
    dockerHubPat,
  );

  const tags = [
    `${tagPrefix}latest`,
    `${tagPrefix}${timestamp}`,
    `${tagPrefix}${gitSha}`,
  ];

  console.log("Building image with following tags:");
  tags.forEach((tag) => console.log(tag));

  const buildArgs = {
    ...(environment === "staging" ?
      {
        VITE_STAGING: true,
      }
    : {}),
  };

  await dockerClient.buildImage({
    dockerRepository: type === "web" ? "codebloom" : "codebloom-standup-bot",
    dockerFileLocation:
      type === "web" ? "infra/Dockerfile" : "internal/standup-bot/Dockerfile",
    tags,
    shouldUpload: dockerUpload,
    buildArgs,
    platforms: ["linux/amd64"],
  });

  console.log("Image pushed successfully.");

  if (getGhaOutput && githubOutputFile) {
    const githubClient = await GitHubClient.createWithGithubAppToken({
      appId: githubAppAppId,
      installationId: githubAppInstallationId,
      privateKey: githubAppPrivateKey,
    });
    await githubClient.outputToGithubOutput({
      overrideGithubOutputFile: githubOutputFile,
      ctx: {
        tag: gitSha,
      },
    });

    if (prId !== undefined) {
      await githubClient.sendPrMessage({
        prId,
        owner: "Patina-Network",
        repository: "codebloom",
        message: `The image has been uploaded to https://hub.docker.com/r/patinanetwork/${type === "web" ? "codebloom" : "codebloom-standup-bot"}/tags under the following tags:

${tags.map((t) => `- \`${type === "web" ? "codebloom" : "codebloom-standup-bot"}:${t}\``).join("\n")}
`,
      });
    }
  }
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const dockerHubPat = (() => {
    const v = ciEnv["DOCKER_HUB_PAT"];
    if (!v) {
      throw new Error("Missing DOCKER_HUB_PAT from env");
    }
    return v;
  })();

  const dockerHubUsername = (() => {
    const v = ciEnv["DOCKER_HUB_USERNAME"];
    if (!v) {
      throw new Error("Missing DOCKER_HUB_USERNAME from env");
    }
    return v;
  })();

  const githubAppAppId = (() => {
    const v = ciEnv["_GITHUB_APP_APP_ID"];
    if (!v) {
      throw new Error("Missing _GITHUB_APP_APP_ID from env");
    }
    return v;
  })();

  const githubAppInstallationId = (() => {
    const v = ciEnv["_GITHUB_APP_INSTALLATION_ID"];
    if (!v) {
      throw new Error("Missing _GITHUB_APP_INSTALLATION_ID from env");
    }
    return v;
  })();

  const githubAppPrivateKey = (() => {
    const v = ciEnv["_GITHUB_APP_PEM_CONTENT"];
    if (!v) {
      throw new Error("Missing _GITHUB_APP_PEM_CONTENT from env");
    }
    return v;
  })();

  return {
    dockerHubPat,
    dockerHubUsername,
    githubAppAppId,
    githubAppInstallationId,
    githubAppPrivateKey,
  };
}

main();
