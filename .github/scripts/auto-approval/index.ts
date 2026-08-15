import type { RestEndpointMethodTypes } from "@octokit/rest";

import { checkNotionPrAndGetTask } from "notion/pr";
import { getNotionClient } from "notion/sdk";
import { Octokit } from "octokit";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const AUTHORIZED_USERS = ["tahminator", "Arshadul-Monir"];

const {
  githubToken,
  repo: rawRepo,
  prId,
} = await yargs(hideBin(process.argv))
  .option("githubToken", {
    type: "string",
    describe: "GitHub token",
    default: process.env.GH_TOKEN,
    demandOption: true,
  })
  .option("repo", {
    type: "string",
    describe: "Repository in owner/repo form",
    default: process.env.GITHUB_REPOSITORY,
    demandOption: true,
  })
  .option("prId", {
    type: "number",
    describe: "Pull request number",
    demandOption: true,
  })
  .strict()
  .parse();

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const notionDbId = (() => {
    const v = ciEnv["NOTION_TASK_DB_ID"];
    if (!v) {
      throw new Error("Missing NOTION_TASK_DB_ID from .env.ci");
    }
    return v;
  })();

  const notionSecret = (() => {
    const v = ciEnv["NOTION_SECRET"];
    if (!v) {
      throw new Error("Missing NOTION_SECRET from .env.ci");
    }
    return v;
  })();

  return {
    notionDbId,
    notionSecret,
  };
}

const { notionDbId, notionSecret } = parseCiEnv(process.env);
const notionClient = getNotionClient(notionSecret);

const taskAndPr = await checkNotionPrAndGetTask(notionClient, prId, notionDbId);

const priorityProp = taskAndPr.task.properties["Priority"];
const taskPriority =
  priorityProp?.type === "select" ? priorityProp.select?.name : undefined;

const [owner, repo] = (() => {
  const v = rawRepo;
  if (!v) {
    throw new Error("GITHUB_REPOSITORY is required");
  }
  return v.split("/") as [string, string];
})();

async function main() {
  const client = new Octokit({
    auth: githubToken,
  });

  let res: RestEndpointMethodTypes["pulls"]["get"]["response"];
  try {
    res = await client.rest.pulls.get({
      owner,
      repo,
      pull_number: prId,
    });
  } catch (e) {
    const s = JSON.stringify(e);
    throw new Error(`GitHub API Error\n\n${s}`);
  }

  const { login: username } = res.data.user;

  const isCollaborator = await client.rest.repos
    .checkCollaborator({ owner, repo, username })
    .then(() => true)
    .catch(() => false);

  const isSupportTicketOverride = taskPriority === "Support" && isCollaborator;
  const isOwnerOverride = AUTHORIZED_USERS.includes(username);

  if (isSupportTicketOverride || isOwnerOverride) {
    try {
      await client.rest.pulls.createReview({
        owner,
        repo,
        pull_number: prId,
        event: "APPROVE",
      });
    } catch (e) {
      const s = JSON.stringify(e);
      throw new Error(`GitHub API Error\n\n${s}`);
    }
  } else {
    console.log("Skipping auto approval...");
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
