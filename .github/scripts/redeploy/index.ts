import type { Environment } from "types";

import { _migrateDb } from "redeploy/db";
import yargs from "yargs";
import { hideBin } from "yargs/helpers";

const { environment, sha } = await yargs(hideBin(process.argv))
  .option("environment", {
    choices: ["staging", "production"] satisfies Environment[],
    describe: "Deployment environment (staging or production)",
    default: "staging" satisfies Environment as Environment,
  })
  .option("sha", {
    type: "string",
    describe: "Commit SHA (required for staging)",
    default: "",
  })
  .check(({ sha, environment }) => {
    if (environment === "staging" && !sha) {
      throw new Error(
        "SHA must be available in ENV if script is being run in staging environment.",
      );
    } else {
      return true;
    }
  })
  .strict()
  .parse();

async function main() {
  await _migrateDb({
    environment,
    sha,
  });
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
