import { ApiURL } from "@codebloom-frontend/lib/api/common/apiURL";
import { expect, test } from "@playwright/test";

import { FRONTEND_URL, TWO_SUM_SOLUTION, TWO_SUM_SUBMISSION_ID } from "@/const";

test("leetcode submissionDetails (authenticated) still returns our submission", async ({
  request,
}) => {
  const api = ApiURL.create("/api/e2e/leetcode/submission/{submissionId}", {
    method: "GET",
    origin: FRONTEND_URL,
    params: { submissionId: TWO_SUM_SUBMISSION_ID },
  });
  const response = await request.get(api.url.toString());
  const body = api.res(await response.json());

  if (!response.ok() || !body.success) {
    throw new Error(
      `Submission request failed (${response.status()}): ${body.message}`,
    );
  }

  const details = body.payload;

  expect(
    details.code,
    `Submission ${TWO_SUM_SUBMISSION_ID} is not TWO_SUM_SOLUTION from e2e/src/const.ts. See the onboarding steps there.`,
  ).toBe(TWO_SUM_SOLUTION);

  // Percentiles drift as other people submit, so only their types are checked.
  expect(typeof details.runtimePercentile).toBe("number");
  expect(typeof details.memoryPercentile).toBe("number");

  const stable = {
    fields: Object.keys(details).sort(),
    code: details.code,
    lang: details.lang,
    runtime: details.runtime,
    runtimeDisplay: details.runtimeDisplay,
    memory: details.memory,
    memoryDisplay: details.memoryDisplay,
  };

  expect(JSON.stringify(stable, null, 2)).toMatchSnapshot(
    "submission-details.json",
  );
});
