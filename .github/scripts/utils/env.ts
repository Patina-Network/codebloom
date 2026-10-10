/** Return defined environment variables matching a prefix, with the prefix removed. */
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
