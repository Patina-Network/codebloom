import { expect, test } from "@playwright/test";

import { E2E_USERS, LEETCODE_VERIFY_KEY, STAGING_LAS_HOST } from "@/const";
import { LasClient } from "@/lib/las";

test("leetcode onboarding: verify key in About Me links the account", async ({
  context,
  page,
}) => {
  await using las = new LasClient(STAGING_LAS_HOST);
  const { username } = await las.getToken();

  await context.addCookies([
    {
      name: "session_token",
      value: E2E_USERS.leetcode.sessionToken,
      domain: "localhost",
      path: "/",
    },
  ]);

  await page.goto("/onboarding");

  await expect(page.getByText("Set LeetCode Username")).toBeVisible();
  await expect(
    page.getByText(LEETCODE_VERIFY_KEY, { exact: true }),
  ).toBeVisible();

  await page.getByLabel("Username").fill(username);
  await page.getByRole("button", { name: "Submit" }).click();

  await expect(page.getByText("Leetcode username has been set!")).toBeVisible();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();

  const validate = await page.request.get("/api/auth/validate");
  const { payload } = await validate.json();

  expect(payload.user.leetcodeUsername).toBe(username);
});
