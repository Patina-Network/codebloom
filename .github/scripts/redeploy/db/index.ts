import { GitHubClient } from "@tahminator/pipeline";
import { _migrateDb } from "redeploy/db/util";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { sha, username } = await yargs(hideBin(process.argv))
  .option("sha", {
    type: "string",
    describe: "PR head commit SHA",
    demandOption: true,
  })
  .option("username", {
    type: "string",
    describe: "Username of the person who triggered the command",
    demandOption: true,
  })
  .strict()
  .parse();

export async function main() {
  const { githubAppAppId, githubAppInstallationId, githubAppPrivateKey } =
    parseCiEnv(process.env);

  const ghClient = await GitHubClient.createWithGithubAppToken({
    appId: githubAppAppId,
    installationId: githubAppInstallationId,
    privateKey: githubAppPrivateKey,
  });

  const isTeamMember = await ghClient.isTeamMember({
    org: "Patina-Network",
    teamSlug: "codebloom",
    username,
  });
  if (!isTeamMember) {
    throw new Error(
      "Only members of @Patina-Network/codebloom can migrate the staging database.",
    );
  }

  await _migrateDb({ environment: "staging", sha });
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const githubAppAppId = ciEnv["_GITHUB_APP_APP_ID"];
  if (!githubAppAppId) {
    throw new Error("Missing _GITHUB_APP_APP_ID from env");
  }

  const githubAppInstallationId = ciEnv["_GITHUB_APP_INSTALLATION_ID"];
  if (!githubAppInstallationId) {
    throw new Error("Missing _GITHUB_APP_INSTALLATION_ID from env");
  }

  const githubAppPrivateKey = ciEnv["_GITHUB_APP_PEM_CONTENT"];
  if (!githubAppPrivateKey) {
    throw new Error("Missing _GITHUB_APP_PEM_CONTENT from env");
  }

  return { githubAppAppId, githubAppInstallationId, githubAppPrivateKey };
}

if (import.meta.main) {
  await main();
}
