import type { Location } from "types";

import { DefaultArtifactClient } from "@actions/artifact";
import { $ } from "bun";
import path from "node:path";

import { backendExclusions, frontendExclusions } from "../../../exclusions";

const getDir = (loc: Location) => {
  return loc === "frontend" ?
      path.join(process.cwd(), "js/coverage/")
    : path.join(process.cwd(), "target/site/jacoco/");
};

export async function uploadBackendTests(sonarToken: string) {
  await uploadArtifact("backend-jacoco-report", "backend");

  await uploadToSonar(sonarToken, "backend");
  // await uploadToCodecov(codecovToken, "backend");
}

export async function uploadFrontendTests(sonarToken: string) {
  await uploadArtifact("frontend-coverage-report", "frontend");

  await uploadToSonar(sonarToken, "frontend");
  // await uploadToCodecov(codecovToken, "frontend");
}

async function _uploadToCodecov(token: string, loc: Location) {
  const dir = getDir(loc);

  try {
    const { exitCode } = Bun.spawnSync(["./codecov", "--help"]);
    if (exitCode != 0) {
      throw Error();
    }
  } catch {
    console.log("Codecov not installed, installing now...");
    await $`curl -Os https://cli.codecov.io/latest/linux/codecov`;
    await $`chmod +x codecov`;
  }

  const env = {
    ...process.env,
    CODECOV_TOKEN: token,
  };

  console.log("Setting up Codecov in CI...");
  const p1 = Bun.spawnSync(["./codecov", "upload-process"], {
    env,
  });

  if (p1.exitCode != 0) {
    console.error(p1.stderr);
    throw new Error(`Failed to load Codecov process\n\n${p1.stderr}`);
  }

  try {
    console.log(
      `Uploading reports from ${dir} to Codecov with flag: ${loc}...`,
    );
    const p2 = Bun.spawnSync([
      "./codecov",
      "do-upload",
      "--dir",
      dir,
      "--flag",
      loc,
    ]);
    if (p2.exitCode != 0) {
      throw new Error(`Failed to load Codecov process\n\n${p2.stderr}`);
    }
    console.log("Codecov upload successful.");
  } catch (error) {
    console.error("Failed to upload to Codecov:", error);
    throw error;
  }
}

async function uploadToSonar(token: string, loc: Location) {
  const dir = getDir(loc);

  console.log("Setting up Sonar in CI...");
  await $`pnpm i -g @sonar/scan`;

  const env = {
    ...process.env,
    SONAR_TOKEN: token,
  };

  const args = [
    "sonar-scanner-npm",
    "-Dsonar.host.url=https://sonarcloud.io",
    `-Dsonar.token=${token}`,
    `-Dsonar.projectKey=Patina-Network_codebloom_${loc}`,
    "-Dsonar.organization=patina-network",
    `-Dsonar.sources=${loc === "frontend" ? "./js/src" : "src/main/java"}`,
  ];

  if (loc === "frontend") {
    args.push(`-Dsonar.javascript.lcov.reportPaths=${dir}/lcov.info`);
    if (frontendExclusions.length) {
      args.push(`-Dsonar.coverage.exclusions=${frontendExclusions}`);
    }
  }

  if (loc === "backend") {
    args.push("-Dsonar.java.binaries=target/classes");
    args.push(
      "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml",
    );
    if (backendExclusions.length) {
      args.push(`-Dsonar.coverage.exclusions=${backendExclusions}`);
    }
  }

  const p = Bun.spawnSync(args, {
    env,
  });

  const stdoutText = p.stdout.toString();
  console.log(stdoutText);

  if (p.exitCode != 0) {
    const stderrText = p.stderr.toString();
    console.error(stderrText);
    throw new Error(`Failed to load Sonar process\n\n${stderrText}`);
  }
}

/*
 * can only run in github actions.
 */
async function uploadArtifact(artifactName: string, loc: Location) {
  const dir = getDir(loc);
  const client = new DefaultArtifactClient();

  const { id, size } = await client.uploadArtifact(
    artifactName,
    [dir],
    process.cwd(),
  );

  console.log(`Artifact ID ${id} uploaded (Size: ${size} bytes)`);
}
