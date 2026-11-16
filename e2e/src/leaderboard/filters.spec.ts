import { test, expect, type Page } from "@playwright/test";

import { E2E_LEADERBOARD_USERS } from "@/const";

const GWC_USER = E2E_LEADERBOARD_USERS.gwc;
const UNTAGGED_LEADER = E2E_LEADERBOARD_USERS.untaggedLeader;

function leaderboardResponse(page: Page, gwc: boolean) {
  return page.waitForResponse((response) => {
    const url = new URL(response.url());

    return (
      url.pathname === "/api/leaderboard/current/user/all" &&
      (url.searchParams.get("gwc") === "true") === gwc
    );
  });
}

test("filters are available when clicked on", async ({ page }) => {
  await page.goto("http://localhost:5173/");
  await page.getByTestId("transitional-button-Leaderboard").click();
  await page.getByRole("button", { name: "Filters" }).click();
  await expect(page.getByRole("menu", { name: "Filters" })).toBeVisible();
});

test("club filter narrows the leaderboard and clearing restores it", async ({
  page,
}) => {
  await page.goto("/leaderboard");
  await expect(page.getByText(UNTAGGED_LEADER, { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Filters" }).click();
  await page.getByRole("menuitem", { name: "Clubs" }).hover();

  const filtered = leaderboardResponse(page, true);
  await page.getByRole("menuitem", { name: /GWC - Hunter College/ }).click();
  expect((await filtered).ok()).toBe(true);

  await expect(page).toHaveURL(/[?&]gwc=true/);
  await expect(page.getByText(GWC_USER, { exact: true })).toBeVisible();
  await expect(page.getByText(UNTAGGED_LEADER, { exact: true })).toHaveCount(0);

  const cleared = leaderboardResponse(page, false);
  await page.getByRole("menuitem", { name: "Clear Filters" }).click();
  expect((await cleared).ok()).toBe(true);

  await expect(page).not.toHaveURL(/[?&]gwc=true/);
  await expect(page.getByText(UNTAGGED_LEADER, { exact: true })).toBeVisible();
});
