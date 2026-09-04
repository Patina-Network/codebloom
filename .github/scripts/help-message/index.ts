import { GitHubClient } from "@tahminator/pipeline";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { prId } = await yargs(hideBin(process.argv))
  .options("prId", {
    type: "number",
    describe: "Pull request number",
    demandOption: true,
  })
  .strict()
  .parse();

export async function main() {
  const { githubAppAppId, githubAppInstallationId, githubAppPrivateKey } =
    parseCiEnv(process.env);

  const githubClient = await GitHubClient.createWithGithubAppToken({
    appId: githubAppAppId,
    installationId: githubAppInstallationId,
    privateKey: githubAppPrivateKey,
  });

  await githubClient.sendPrMessage({
    prId,
    owner: "Patina-Network",
    repository: "codebloom",
    message: `
### Available PR Commands

There are currently none available at this time.
`.trim(),
  });
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
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
    githubAppAppId,
    githubAppInstallationId,
    githubAppPrivateKey,
  };
}

main();
