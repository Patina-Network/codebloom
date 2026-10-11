import { Octokit } from "@octokit/rest";

const githubToken = (() => {
  const v = process.env.GH_TOKEN;
  return v;
})();

const [owner, repo] = (() => {
  const v = process.env.GITHUB_REPOSITORY;
  if (!v) {
    throw new Error("GITHUB_REPOSITORY is required");
  }
  return v.split("/") as [string, string];
})();

export async function sendMessage(prId: number, message: string, token?: string) {
  try {
    if (!token && !githubToken) {
      throw new Error("Some token should be set");
    }

    const client = new Octokit({
      auth: token ?? githubToken,
    });

    try {
      await client.rest.issues.createComment({
        issue_number: prId,
        owner,
        repo,
        body: message,
      });
    } catch (e) {
      const s = JSON.stringify(e);
      throw new Error(`GitHub API Error\n\n${s}`, { cause: e });
    }
  } catch (e) {
    console.error("Failed to post GitHub error message\n", e);
    process.exit(1);
  }
}
