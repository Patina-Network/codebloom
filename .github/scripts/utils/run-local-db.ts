import { LocalPostgresClient } from "@tahminator/pipeline";
import { $ } from "bun";

export function parseDbEnv(postgres: LocalPostgresClient) {
  const { host, port, database, user, password } = postgres.state;

  return {
    DATABASE_HOST: host,
    DATABASE_PORT: String(port),
    DATABASE_NAME: database,
    DATABASE_USER: user,
    DATABASE_PASSWORD: password,
  };
}

export async function startLocalDb(seed: "mock" | "e2e" = "mock") {
  const postgres = await LocalPostgresClient.create({ database: "codebloom" });

  try {
    const locations = `filesystem:./db/migration,filesystem:./db/${seed === "mock" ? "repeated" : "e2e"}`;

    await $.env({
      ...process.env,
      ...parseDbEnv(postgres),
    })`./mvnw -B -ntp flyway:migrate ${`-Dflyway.locations=${locations}`}`;

    return postgres;
  } catch (error) {
    await postgres[Symbol.asyncDispose]();
    throw error;
  }
}
