import { $ } from "bun";

import { pink } from "@/utils/colors";

let fe: Bun.Subprocess<"ignore", Bun.BunFile, "inherit"> | undefined;

async function start(env: Record<string, string>) {
  try {
    const $$ = $.env({ ...process.env, ...env });
    await $$`pnpm --v`;

    await $$`pnpm --dir js i --frozen-lockfile`;
    await $$`pnpm --dir js run generate`;
    const logFile = Bun.file("frontend.log");
    fe = Bun.spawn(["pnpm", "--dir", "js", "dev"], {
      env: { ...process.env, ...env },
      stdout: logFile,
    });

    console.log("Waiting for frontend to become ready.");

    let ready = false;
    const attempts = 30;

    for (let i = 1; i <= attempts; i++) {
      try {
        const res = await fetch("http://localhost:5173");

        if (res.ok) {
          console.log("Frontend is up!");
          ready = true;
          break;
        }
      } catch (_) {
        /* empty */
      }

      console.log(`Waiting for frontend... (${i}/${attempts})`);
      await Bun.sleep(2000);
    }

    if (!ready) {
      console.error("Frontend failed to start in time.");
      await end();
      process.exit(1);
    }

    console.log("frontend ready");
  } catch (e) {
    console.error(e);
    await end();
  }
}

async function end() {
  if (fe) {
    if (!fe.killed) {
      fe.kill();
    }
    console.log(pink("=== FRONTEND LOGS ==="));
    const logs = await Bun.file("frontend.log").text();
    logs
      .split("\n")
      .filter((s) => s.length > 0)
      .forEach((line) => console.log(pink(line)));
    console.log(pink("=== FRONTEND LOGS END ==="));
  }
}

export const frontend = {
  start,
  end,
};
