import { $ } from "bun";

type Opts = {
  baseDir?: string;
  mask_PLZ_DO_NOT_TURN_OFF_UNLESS_YOU_KNOW_WHAT_UR_DOING?: boolean;
};

let isGitCryptUnlocked = false;

/**
 * @param environments - List of environment files to load.
 * @param baseDir - Directory of environment variable.
 * @param mask_PLZ_DO_NOT_TURN_OFF_UNLESS_YOU_KNOW_WHAT_UR_DOING - Should variables be masked. Defaults to `true`. __NOTE: This will only work in a GitHub Action runner.__
 *
 * @returns a map of the loaded environments as a key and value inside of a map.
 *
 * _Please note that duplicate environment variables will be overwritten, so
 * the order in which you define `environments` does matter._
 */
export async function getEnvVariables(
  environments: string[],
  opts?: Opts,
): Promise<Record<string, string>> {
  const {
    baseDir = "",
    mask_PLZ_DO_NOT_TURN_OFF_UNLESS_YOU_KNOW_WHAT_UR_DOING = true,
  } = opts ?? {};
  if (!isGitCryptUnlocked) {
    await $`git-crypt unlock`.nothrow();
    isGitCryptUnlocked = true;
  }

  const loaded = new Map<string, string>();

  for (const env of environments) {
    const path = baseDir ? `${baseDir}/.env.${env}` : `.env.${env}`;
    const envFile = Bun.file(path);
    if (await envFile.exists()) {
      console.log(`Loading ${envFile.name}`);

      const content = await envFile.text();
      const lines = content.split("\n").filter((s) => s.length > 0);

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith("#")) continue;

        const match = trimmed.split("=").filter((s) => s.length > 0);
        if (match.length === 2) {
          const [key, value] = match as [string, string];
          const cleanKey = key.trim();
          let cleanValue = value.trim();
          if (
            (cleanValue.startsWith('"') && cleanValue.endsWith('"')) ||
            (cleanValue.startsWith("'") && cleanValue.endsWith("'"))
          ) {
            cleanValue = cleanValue.slice(1, -1);
          }

          if (!loaded.has(cleanKey)) {
            loaded.set(cleanKey, cleanValue);
          }
        }
      }
    } else {
      console.warn(`Warning: ${envFile.name} not found`);
    }
  }

  if (mask_PLZ_DO_NOT_TURN_OFF_UNLESS_YOU_KNOW_WHAT_UR_DOING) {
    for (const [varName, value] of loaded.entries()) {
      if (value === "true" || value === "false" || value === "") {
        console.log(`Not masking ${varName}: true/false/empty value`);
        continue;
      }

      console.log(`Masking ${varName}`);
      console.log(`::add-mask::${value}`);
    }
  }

  return Object.fromEntries(loaded);
}

/**
 * @param prefix - Prefix to filter `envObject` keys by.
 * @param envObject - Object to read variables from. Defaults to `process.env`.
 *
 * @returns a map of the matching variables, keyed without the `prefix`.
 *
 * _Please note that keys are stripped of `prefix` before being returned, so
 * `PREFIX_FOO` becomes `FOO`._
 */
export function getEnvVariablesByPrefix(
  prefix: string,
  envObject: Record<string, string | undefined> = process.env,
): Record<string, string> {
  const result: Record<string, string> = {};

  for (const [key, value] of Object.entries(envObject)) {
    if (key.startsWith(prefix) && value !== undefined) {
      result[key.slice(prefix.length)] = value;
    }
  }

  return result;
}
