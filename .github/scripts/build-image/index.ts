import type { Environment } from "types";

import { $ } from "bun";
import { getEnvVariables } from "load-secrets/env/load";
import { backend } from "utils/run-backend-instance";
import { db } from "utils/run-local-db";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

process.env.TZ = "America/New_York";

const { environment, dockerUpload, getGhaOutput, githubOutputFile } =
  await yargs(hideBin(process.argv))
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
    .strict()
    .parse();

const tagPrefix = environment === "staging" ? "staging-" : "";
const serverProfiles = environment === "staging" ? "stg" : "prod";

async function main() {
  try {
    const ciEnv = await getEnvVariables(["ci"]);
    const { dockerHubPat } = parseCiEnv(ciEnv);
    const localDbEnv = await db.start();
    const ciAppEnv = await getEnvVariables(["ci-app"]);

    await backend.start(ciAppEnv);

    const $$ = $.env({
      ...process.env,
      ...ciAppEnv,
      ...localDbEnv,
    });
    await $$`pnpm --dir js run generate`;

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

    const tags = [
      `patinanetwork/codebloom:${tagPrefix}latest`,
      `patinanetwork/codebloom:${tagPrefix}${timestamp}`,
      `patinanetwork/codebloom:${tagPrefix}${gitSha}`,
    ];

    console.log("Building image with following tags:");
    tags.forEach((tag) => console.log(tag));

    if (dockerHubPat) {
      console.log("DOCKER_HUB_PAT found");
    } else {
      console.log("DOCKER_HUB_PAT missing or empty");
    }

    await $`echo ${dockerHubPat} | docker login -u patinanetwork --password-stdin`;

    try {
      await $`docker buildx create --use --name codebloom-builder`;
    } catch {
      await $`docker buildx use codebloom-builder`;
    }

    const buildMode = dockerUpload ? "--push" : "--load";

    const viteStagingArg =
      serverProfiles === "stg" ? ["--build-arg", "VITE_STAGING=true"] : [];

    const tagArgs = tags.flatMap((tag) => ["--tag", tag]);

    await $`docker buildx build ${buildMode} \
              --platform linux/amd64 \
              --file infra/Dockerfile \
              --build-arg SERVER_PROFILES=${serverProfiles} \
              --build-arg COMMIT_SHA=${gitSha} \
              --cache-from=type=gha \
              --cache-to=type=gha,mode=max \
              ${viteStagingArg} \
              ${tagArgs} \
              .`;

    console.log("Image pushed successfully.");

    if (getGhaOutput && githubOutputFile) {
      console.log("Outputting image tag...");
      const w = Bun.file(githubOutputFile).writer();
      await w.write(`tag<<EOF\n${tagPrefix}${gitSha}\nEOF\n`);
      await w.flush();
      await w.end();
    }
  } finally {
    await backend.end();
    await db.end();
  }
}

function parseCiEnv(ciEnv: Record<string, string>) {
  const dockerHubPat = (() => {
    const v = ciEnv["DOCKER_HUB_PAT"];
    if (!v) {
      throw new Error("Missing DOCKER_HUB_PAT from .env.ci");
    }
    return v;
  })();

  return { dockerHubPat };
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
