import type { Environment, Type } from "types";

import { GitHubClient } from "@tahminator/pipeline";
import { _migrateDb } from "redeploy/db";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { environment, sha, type } = await yargs(hideBin(process.argv))
  .option("environment", {
    choices: ["staging", "production"] satisfies Environment[],
    describe: "Deployment environment (staging or production)",
    default: "staging" satisfies Environment as Environment,
  })
  .option("sha", {
    type: "string",
    describe: "Commit SHA",
    default: "",
  })
  .option("type", {
    choices: ["standup-bot", "web"] satisfies Type[],
    describe: "Type to build",
    demandOption: true,
    default: "web" as Type,
  })
  .strict()
  .parse();

async function main() {
  const resolvedSha = resolveSha(sha, process.env.GITHUB_SHA);

  const { githubAppAppId, githubAppInstallationId, githubAppPrivateKey } =
    parseCiEnv(process.env);

  const ghClient = await GitHubClient.createWithGithubAppToken({
    appId: githubAppAppId,
    installationId: githubAppInstallationId,
    privateKey: githubAppPrivateKey,
  });

  if (type === "web") {
    await _migrateDb({
      environment,
      sha: resolvedSha,
    });
  }

  await ghClient.updateK8sTagWithPR({
    originRepo: ["Patina-Network", "codebloom"],
    manifestRepo: ["Patina-Network", "k8s-manifests"],
    kustomizationFilePath: `base/${environment}/${type === "web" ? "codebloom" : "codebloom-standup-bot"}/kustomization.yaml`,
    imageName: `patinanetwork/${type === "web" ? "codebloom" : "codebloom-standup-bot"}`,
    environment: `${environment}`,
    newTag: environment === "staging" ? `staging-${resolvedSha}` : resolvedSha,
  });
}

function resolveSha(cliSha: string, githubSha: string | undefined) {
  const source = (cliSha || githubSha || "").trim();

  if (!source) {
    throw new Error("Missing deployment SHA. Provide --sha or set GITHUB_SHA.");
  }

  return source.slice(0, 7);
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const githubAppAppId = (() => {
    const v = ciEnv["GITHUB_APP_APP_ID"];
    if (!v) {
      throw new Error("Missing GITHUB_APP_APP_ID from .env.ci");
    }
    return v;
  })();

  const githubAppInstallationId = (() => {
    const v = ciEnv["GITHUB_APP_INSTALLATION_ID"];
    if (!v) {
      throw new Error("Missing GITHUB_APP_INSTALLATION_ID from .env.ci");
    }
    return v;
  })();

  const githubAppPrivateKey = (() => {
    const v = ciEnv["GITHUB_APP_PEM_CONTENT"];
    if (!v) {
      throw new Error("Missing GITHUB_APP_PEM_CONTENT from .env.ci");
    }
    return v;
  })();

  return { githubAppAppId, githubAppInstallationId, githubAppPrivateKey };
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
