import { _checkCommits } from "notion/commits";
import { checkNotionPrAndGetTask } from "notion/pr";
import { _validateNotionTask } from "notion/pr/validate-ticket";
import { getNotionClient } from "notion/sdk";
import { _updateNotionTaskWithPrLink } from "notion/task";
import { updatePrDescriptionWithTicket } from "utils/update-pr-description";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

export * from "./pr";

const {
  prId,
  getGhaOutput,
  githubOutput: githubOutputFile,
  validateTicket,
} = await yargs(hideBin(process.argv))
  .option("prId", {
    type: "number",
    describe: "Pull request number",
    demandOption: true,
  })
  .option("getGhaOutput", {
    type: "boolean",
    describe: "Enable GitHub Actions output",
    default: false,
  })
  .option("githubOutput", {
    type: "string",
    describe: "Path to GITHUB_OUTPUT",
    default: process.env.GITHUB_OUTPUT,
  })
  .option("validateTicket", {
    type: "boolean",
    describe: "Validate ticket fields",
    default: true,
  })
  .strict()
  .parse();

async function main() {
  console.log(`GET_GHA_OUTPUT=${getGhaOutput}`);

  const { notionDbId, notionSecret } = parseCiEnv(process.env);
  const client = getNotionClient(notionSecret);

  const { taskId, taskContent, task, taskPublicUrl } =
    await checkNotionPrAndGetTask(client, prId, notionDbId);

  console.log(taskContent);

  if (validateTicket) {
    await _validateNotionTask(task, taskContent, prId);
  }

  await _checkCommits(taskId, prId);

  await _updateNotionTaskWithPrLink(client, task, taskId, prId);
  if (taskPublicUrl) {
    await updatePrDescriptionWithTicket(taskPublicUrl, taskId, prId);
  }

  if (getGhaOutput && githubOutputFile) {
    console.log("Outputting Notion context...");
    const w = Bun.file(githubOutputFile).writer();
    await w.write(`context<<EOF\n${taskContent}\nEOF\n`);
    await w.flush();
    await w.end();
  }
}

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

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
