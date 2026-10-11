import { Octokit } from "@octokit/rest";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

import { sendMessage } from "@/utils/send-message";

const { prId } = await yargs(hideBin(process.argv))
  .option("prId", {
    type: "number",
    describe: "Pull request number",
    demandOption: true,
  })
  .strict()
  .parse();

const [owner, repo] = (() => {
  const v = process.env.GITHUB_REPOSITORY;
  if (!v) throw new Error("GITHUB_REPOSITORY is required");
  return v.split("/") as [string, string];
})();

async function main() {
  const client = new Octokit({ auth: process.env.GH_TOKEN });
  const { data } = await client.rest.pulls.get({
    owner,
    repo,
    pull_number: prId,
  });
  const { body } = data;

  const descriptionContent = (() => {
    const match = (body ?? "").match(/## Description of changes([\s\S]*?)(?=\n##|$)/);
    return (match?.[1] ?? "").trim();
  })();

  if (descriptionContent) {
    console.log("PR description is filled out");
    return;
  }

  await sendMessage(
    prId,
    `
### PR Description Required
Please fill out the \`Description of changes\` section of the PR.`.trim(),
  );

  console.error("PR description is empty.");
  process.exit(1);
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
