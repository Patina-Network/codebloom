import type { Environment } from "@tahminator/pipeline";
import type { Type } from "types";

import { GitHubClient, Utils } from "@tahminator/pipeline";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { environment, newTagVersion, type } = await yargs(hideBin(process.argv))
  .option("newTagVersion", {
    type: "string",
    demandOption: true,
  })
  .option("environment", {
    choices: ["staging", "production"] satisfies Environment[],
    describe: "Deployment environment (staging or production)",
    demandOption: true,
  })
  .option("type", {
    choices: ["web", "standup-bot"] satisfies Type[],
    describe: "Service type to deploy",
    demandOption: true,
  })
  .strict()
  .parse();

async function main() {
  const ciEnv = await Utils.getEnvVariables(["ci"]);
  const { ghPat } = parseCiEnv(ciEnv);
  const ghClient = new GitHubClient(ghPat);

  if (type === "web") {
    await ghClient.updateK8sTagWithPR({
      manifestRepo: ["Patina-Network", "k8s-personal"],
      originRepo: ["Patina-Network", "codebloom"],
      kustomizationFilePath: `apps/${environment}/codebloom/kustomization.yaml`,
      imageName: "Patina-Network/codebloom",
      newTag: newTagVersion,
      environment,
    });
  }

  if (type === "standup-bot") {
    await ghClient.updateK8sTagWithPR({
      manifestRepo: ["Patina-Network", "k8s-personal"],
      originRepo: ["Patina-Network", "codebloom"],
      kustomizationFilePath: `apps/${environment}/codebloom-standup-bot/kustomization.yaml`,
      imageName: "Patina-Network/codebloom-standup-bot",
      newTag: newTagVersion,
      environment,
    });
  }
}

function parseCiEnv(ciEnv: Record<string, string>) {
  const ghPat = (() => {
    const v = ciEnv["GH_PAT"];
    if (!v) {
      throw new Error("Missing GH_PAT from .env.ci");
    }
    return v;
  })();

  return { ghPat };
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
