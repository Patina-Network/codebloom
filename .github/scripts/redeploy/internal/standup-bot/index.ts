import { getEnvVariables } from "load-secrets/env/load";
import { initClient } from "redeploy/internal/coolify";
import {
  _getOrCreateStandupBotResource,
  _triggerStandupBotDeployment,
  _updateStandupBotAppEnvs,
} from "redeploy/internal/standup-bot/utils";

async function main() {
  // should already be called
  // await $`git-crypt unlock`;

  const { bearerAuth, serverUrl, projectUuid, serverUuid } = parseCiEnv(
    process.env,
  );

  const client = await initClient(bearerAuth, serverUrl);

  const appUuid = await _getOrCreateStandupBotResource({
    client,
    projectUuid,
    serverUuid,
  });

  const appEnv = await getEnvVariables(["production"], {
    baseDir: "internal/standup-bot",
  });

  await _updateStandupBotAppEnvs({
    client,
    appUuid,
    envs: appEnv,
  });

  const pendingDeploymentId = await _triggerStandupBotDeployment({
    client,
    appUuid,
  });

  console.log("Deployment ID:", pendingDeploymentId);

  let ready = false;
  const attempts = 60;

  for (let i = 1; i <= attempts; i++) {
    const res = await client.deployments.get({
      uuid: pendingDeploymentId,
    });

    const phase = res.status;

    console.log("Deployment phase:", phase);

    if (phase === "finished") {
      console.log("Deployment has completed!");
      ready = true;
      break;
    }

    if (phase === "failed" || phase === "error" || phase === "cancelled") {
      console.log(`Deployment failed with phase ${phase}`);
      process.exit(1);
    }

    console.log(`Waiting for deployment to complete... (${i}/${attempts})`);
    await Bun.sleep(10000);
  }

  if (!ready) {
    console.error("Deployment did not reach a valid state within 10 minutes.");
    process.exit(1);
  }
}

function parseCiEnv(ciEnv: Record<string, string | undefined>) {
  const bearerAuth = (() => {
    const v = ciEnv["COOLIFY_BEARER_AUTH"];
    if (!v) {
      throw new Error("Missing COOLIFY_BEARER_AUTH from .env.ci");
    }
    return v;
  })();
  const serverUrl = (() => {
    const v = ciEnv["COOLIFY_SERVER_URL"];
    if (!v) {
      throw new Error("Missing COOLIFY_SERVER_URL from .env.ci");
    }
    return v;
  })();
  const serverUuid = (() => {
    const v = ciEnv["COOLIFY_SERVER_UUID"];
    if (!v) {
      throw new Error("Missing COOLIFY_SERVER_UUID from .env.ci");
    }
    return v;
  })();
  const projectUuid = (() => {
    const v = ciEnv["COOLIFY_PROJECT_UUID"];
    if (!v) {
      throw new Error("Missing COOLIFY_PROJECT_UUID from .env.ci");
    }
    return v;
  })();

  return { bearerAuth, serverUrl, serverUuid, projectUuid };
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
