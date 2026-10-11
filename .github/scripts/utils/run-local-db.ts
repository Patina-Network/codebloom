import { $ } from "bun";

import { brightMagenta } from "@/utils/colors";

async function start() {
  try {
    console.log("Starting postgres container...");

    await $`docker rm -f codebloom-db`;

    await $`docker run -d \
        --name codebloom-db \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=postgres \
        -e POSTGRES_DB=codebloom \
        -p 5440:5432 \
        mirror.gcr.io/library/postgres:16-alpine`;

    console.log("Waiting for postgres to become ready.");

    let ready = false;
    const attempts = 30;

    for (let i = 1; i <= attempts; i++) {
      const check = await $`docker exec codebloom-db pg_isready -U postgres`.quiet().nothrow();

      if (check.exitCode === 0) {
        console.log("postgres is ready!");
        ready = true;
        break;
      }

      console.log(`Waiting for postgres... (${i}/${attempts})`);
      await Bun.sleep(2000);
    }

    if (!ready) {
      console.error("postgres failed to start in time.");
      await end();
      process.exit(1);
    }

    const env = {
      DATABASE_HOST: "localhost",
      DATABASE_PORT: "5440",
      DATABASE_NAME: "codebloom",
      DATABASE_USER: "postgres",
      DATABASE_PASSWORD: "postgres",
    };

    console.log("postres started, running migrations...");

    await $.env(env)`./mvnw -B -ntp flyway:migrate -Dflyway.locations=filesystem:./db`;

    console.log("postgres ready");

    return env;
  } catch (e) {
    console.error(e);
    await end();
  }
}

async function end() {
  console.log("Stopping and removing postgres container...");

  console.log(brightMagenta("=== DB LOGS ==="));
  const logs = await $`docker logs codebloom-db`.text();
  logs
    .split("\n")
    .filter((s) => s.length > 0)
    .forEach((line) => console.log(brightMagenta(line)));
  console.log(brightMagenta("=== DB LOGS END ==="));

  await $`docker stop codebloom-db`.quiet().nothrow();
  await $`docker rm codebloom-db`.quiet().nothrow();

  delete process.env.DATABASE_HOST;
  delete process.env.DATABASE_PORT;
  delete process.env.DATABASE_NAME;
  delete process.env.DATABASE_USER;
  delete process.env.DATABASE_PASSWORD;
}

export const db = {
  start,
  end,
};
