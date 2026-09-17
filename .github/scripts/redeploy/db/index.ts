import { GitHubClient } from "@tahminator/pipeline";
import { _migrateDb } from "redeploy/db/util";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { runUrl, sha, username } = await yargs(hideBin(process.argv))
  .option("runUrl", {
    type: "string",
    describe: "Workflow run URL for the status check",
    demandOption: true,
  })
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

  const repo = { owner: "Patina-Network", repository: "codebloom" };
  const check = await ghClient.statusCheck({
    ...repo,
    action: "create",
    sha,
    name: "Migrate staging DB",
    status: "in_progress",
    detailsUrl: runUrl,
    output: {
      title: "Checking authorization",
      summary:
        "Verifying the caller's membership in @Patina-Network/codebloom...",
    },
  });
  if (!check) {
    throw new Error("Failed to create staging migration status check.");
  }
  const update = {
    ...repo,
    action: "update" as const,
    checkRunId: check.id,
    detailsUrl: runUrl,
  };

  try {
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

    await ghClient.statusCheck({
      ...update,
      status: "in_progress",
      output: {
        title: "Running staging migrations",
        summary:
          "Checking database changes and applying pending migrations to codebloom-stg.",
      },
    });

    const result = await _migrateDb({ environment: "staging", sha });
    await ghClient.statusCheck({
      ...update,
      status: "completed",
      conclusion: result,
      output: {
        title:
          result === "skipped" ? "Migration skipped" : "Migration successful",
        summary:
          result === "skipped" ?
            "No changes under db/ were found when compared with main."
          : "Staging database migrations completed successfully.",
      },
    });
  } catch (error) {
    await ghClient
      .statusCheck({
        ...update,
        status: "completed",
        conclusion: "failure",
        output: {
          title: "Staging migration failed",
          summary:
            "Staging migration failed. See the workflow logs for details.",
        },
      })
      .catch((statusError) => {
        console.error("Failed to report migration failure:", statusError);
      });
    throw error;
  }
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
