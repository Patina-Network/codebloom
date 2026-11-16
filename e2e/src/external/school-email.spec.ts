import { expect, test } from "@playwright/test";

import { E2E_USERS } from "@/const";
import { uniqueInboxAddress, waitForEmailHtml } from "@/lib/inbox";

test("school email: magic link received over real SMTP verifies the user", async ({
  context,
  page,
}) => {
  test.setTimeout(180_000);

  await context.addCookies([
    {
      name: "session_token",
      value: E2E_USERS.school.sessionToken,
      domain: "localhost",
      path: "/",
    },
  ]);

  const email = uniqueInboxAddress("school");

  await page.goto("/settings");
  await page.getByRole("button", { name: "Verify Now" }).click();

  const dialog = page.getByRole("dialog", {
    name: "Verify your student email",
  });
  await dialog.getByLabel("Student email").fill(email);
  await dialog.getByRole("button", { name: "Submit" }).click();

  await expect(
    page.getByText("Magic link sent! Check your school inbox to continue."),
  ).toBeVisible();

  const html = await waitForEmailHtml({ to: email, timeoutMs: 120_000 });

  const mail = await context.newPage();
  await mail.setContent(html);
  const verifyLink = mail.locator("#input-verifyUrl-href").first();

  await expect(
    verifyLink,
    "school-email.html no longer has #input-verifyUrl-href; update this test and ReactEmailTemplaterImpl together",
  ).toHaveAttribute("href", /\/api\/auth\/school\/verify\?state=/);

  // The email links use target="_blank", so the verification opens a new page.
  const [verified] = await Promise.all([
    context.waitForEvent("page"),
    verifyLink.click(),
  ]);

  await expect(verified).toHaveURL(/\/settings\?success=true/);
  await expect(
    verified.getByText("The email has been verified!").first(),
  ).toBeVisible();

  const validate = await page.request.get("/api/auth/validate");
  const { payload } = await validate.json();

  expect(payload.user.schoolEmail).toBe(email);
  expect(payload.user.tags.map((tag: { tag: string }) => tag.tag)).toContain(
    "Nyu",
  );
});
