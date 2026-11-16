// Must load before any test imports frontend code; Playwright loads this config in every worker.
// Lives outside src/ because Playwright resolves `@/patches` to js/src/patches.ts.
// eslint-disable-next-line no-relative-import-paths/no-relative-import-paths
import "./patches";
import { defineConfig, devices } from "@playwright/test";

import { FRONTEND_URL } from "@/const";

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./src",
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: "html",
  snapshotPathTemplate: "{testDir}/__snapshots__/{testFilePath}/{arg}{ext}",
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL: FRONTEND_URL,

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "on-first-retry",
  },

  /* Configure projects for major browsers */
  projects: [
    // Each test consumes seeded one-time state (onboarding, school verification), so no retries.
    {
      name: "external",
      testDir: "./src/external",
      retries: 0,
      use: { ...devices["Desktop Chrome"], trace: "retain-on-failure" },
    },

    {
      name: "chromium",
      testIgnore: "external/**",
      use: { ...devices["Desktop Chrome"] },
    },

    {
      name: "firefox",
      testIgnore: "external/**",
      use: { ...devices["Desktop Firefox"] },
    },

    {
      name: "webkit",
      testIgnore: "external/**",
      use: { ...devices["Desktop Safari"] },
    },

    /* Test against mobile viewports. */
    // {
    //   name: 'Mobile Chrome',
    //   use: { ...devices['Pixel 5'] },
    // },
    // {
    //   name: 'Mobile Safari',
    //   use: { ...devices['iPhone 12'] },
    // },

    /* Test against branded browsers. */
    {
      name: "Microsoft Edge",
      testIgnore: "external/**",
      use: { ...devices["Desktop Edge"], channel: "msedge" },
    },
    {
      name: "Google Chrome",
      testIgnore: "external/**",
      use: { ...devices["Desktop Chrome"], channel: "chrome" },
    },
  ],

  webServer: [
    {
      command:
        "cd .. && ./mvnw -B -ntp spring-boot:run -Dspring-boot.run.profiles=ci",
      env: {
        SPRING_PROFILES_INCLUDE: "e2e",
      },
      url: "http://localhost:8080/api",
      stdout: "pipe",
      stderr: "pipe",
      wait: {
        stdout: /^Started\s+CodebloomApplication\s+in/,
      },
    },
    {
      command: "cd ../js && pnpm run dev",
      url: FRONTEND_URL,
      stdout: "pipe",
      stderr: "pipe",
      wait: {
        stdout: /^ready\s+in/,
      },
    },
  ],
});
