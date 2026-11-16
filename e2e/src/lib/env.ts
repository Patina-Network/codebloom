export function requireEnv(name: string): string {
  const value = process.env[name];

  if (!value) {
    throw new Error(
      `Missing env ${name}. Set it in .env.shared.yaml or the root .env locally (see example.env), or as a repo secret in CI.`,
    );
  }

  return value;
}
