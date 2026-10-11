import { Octokit } from "octokit";

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

export async function updatePrDescriptionWithTicket(
  ticketUrl: string,
  ticketId: number,
  prId: number,
  token?: string,
) {
  try {
    if (!token && !githubToken) {
      throw new Error("Some token should be set");
    }

    const client = new Octokit({
      auth: token ?? githubToken,
    });

    let body: string | null;
    try {
      const pr = await client.rest.pulls.get({
        owner,
        repo,
        pull_number: prId,
      });
      body = pr.data.body;
    } catch (e) {
      const s = JSON.stringify(e);
      throw new Error(`GitHub API Error\n\n${s}`, { cause: e });
    }

    const newBody = (() => {
      const ticketUrlLink = `## [${ticketId}](${ticketUrl})`;
      if (body == null) {
        return ticketUrlLink;
      } else {
        if (body.includes(ticketUrlLink)) {
          return body;
        }

        const bodyLines = body.split("\n");
        bodyLines.unshift(ticketUrlLink);
        bodyLines[0] = ticketUrlLink;
        return bodyLines.join("\n");
      }
    })();

    try {
      await client.rest.pulls.update({
        owner,
        repo,
        pull_number: prId,
        body: newBody,
      });
    } catch (e) {
      const s = JSON.stringify(e);
      throw new Error(`GitHub API Error\n\n${s}`, { cause: e });
    }
  } catch (e) {
    console.error("failed to update pr description with ticket", e);
    process.exit(1);
  }
}
