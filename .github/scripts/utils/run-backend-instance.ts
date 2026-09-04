import { $ } from "bun";

import { cyan } from "@/../utils/colors";

let be: Bun.Subprocess<"ignore", Bun.BunFile, "inherit"> | undefined;

async function start(env: Record<string, string>) {
  try {
    console.log("Starting backend instance...");

    await $`java -version`;
    await $`javac -version`;
    console.log(`JAVA_HOME=${process.env.JAVA_HOME}`);

    const logFile = Bun.file("backend.log");
    be = Bun.spawn(
      ["./mvnw", "-Dspring-boot.run.profiles=ci", "spring-boot:run"],
      {
        env: { ...process.env, ...env },
        stdout: logFile,
      },
    );

    console.log("Waiting for backend to become ready.");

    let ready = false;
    const attempts = 30;

    for (let i = 1; i <= attempts; i++) {
      try {
        const response = await fetch("http://localhost:8080/api");
        const data = (await response.json()) as { success: boolean };

        if (data.success === true) {
          console.log("Backend is up!");
          ready = true;
          break;
        }
      } catch (_) {
        /* empty */
      }

      console.log(`Waiting for backend... (${i}/${attempts})`);
      await Bun.sleep(2000);
    }

    if (!ready) {
      console.error("Backend failed to start in time.");
      await end();
      process.exit(1);
    }

    console.log("backend ready");
  } catch (e) {
    console.error(e);
    end();
  }
}

async function end() {
  if (be) {
    if (!be.killed) {
      be.kill();

      const exitedInTime = await Promise.race([
        be.exited.then(() => true),
        Bun.sleep(5000).then(() => false),
      ]);

      if (!exitedInTime) {
        console.warn(
          "Backend did not exit after SIGTERM within 5s, sending SIGKILL...",
        );
        be.kill("SIGKILL");
        await be.exited;
      }
    }
    console.log(cyan("=== BACKEND LOGS ==="));
    const logs = await Bun.file("backend.log").text();
    logs
      .split("\n")
      .filter((s) => s.length > 0)
      .forEach((line) => console.log(cyan(line)));
    console.log(cyan("=== BACKEND LOGS END ==="));
  }
}

export const backend = {
  start,
  end,
};
