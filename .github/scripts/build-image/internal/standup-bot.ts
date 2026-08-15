import { $ } from "bun";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

process.env.TZ = "America/New_York";

const { dockerUpload, getGhaOutput, githubOutputFile } = await yargs(
  hideBin(process.argv),
)
  .option("dockerUpload", {
    type: "boolean",
    default: false,
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

async function main() {
  const { dockerHubPat } = parseCiEnv(process.env);

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
    `tahminator/codebloom-standup-bot:latest`,
    `tahminator/codebloom-standup-bot:${timestamp}`,
    `tahminator/codebloom-standup-bot:${gitSha}`,
  ];

  console.log("Building image with following tags:");
  tags.forEach((tag) => console.log(tag));

  if (dockerHubPat) {
    console.log("DOCKER_HUB_PAT found");
  } else {
    console.log("DOCKER_HUB_PAT missing or empty");
  }

  await $`echo ${dockerHubPat} | docker login -u tahminator --password-stdin`;

  try {
    await $`docker buildx create --use --name codebloom-standup-bot-builder`;
  } catch {
    await $`docker buildx use codebloom-standup-bot-builder`;
  }

  const buildMode = dockerUpload ? "--push" : "--load";

  const tagArgs = tags.flatMap((tag) => ["--tag", tag]);

  console.log(`cwd is ${process.cwd()}`);

  await $`docker buildx build ${buildMode} \
              --platform linux/amd64 \
              --file internal/standup-bot/Dockerfile \
              --cache-from=type=gha \
              --cache-to=type=gha,mode=max \
              ${tagArgs} \
              .`;

  console.log("Image pushed successfully.");

  if (getGhaOutput && githubOutputFile) {
    console.log("Outputting image tag...");
    const w = Bun.file(githubOutputFile).writer();
    await w.write(`tag<<EOF\n${gitSha}\nEOF\n`);
    await w.flush();
    await w.end();
  }
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
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
