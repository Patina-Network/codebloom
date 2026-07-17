/**
 * See https://github.com/tahminator/codebloom/blob/main/.github/scripts/utils/upload.ts
 * for test exclusion usages.
 */

const backendBaseDir = "src/main/java/org/patinanetwork/codebloom";
const frontendBaseDir = "js/src";

export const backendExclusions = [
  `${backendBaseDir}/common/dto/**`,
  `${backendBaseDir}/playwright/PlaywrightProvider.java`,
  `${backendBaseDir}/CodebloomApplication.java`,
];
export const frontendExclusions = [
  `${frontendBaseDir}/**/*.test.ts`,
  `${frontendBaseDir}/**/*.test.tsx`,
  `${frontendBaseDir}/lib/api/queries/**/*.ts`,
  `${frontendBaseDir}/lib/api/schema/**/*.ts`,
];
