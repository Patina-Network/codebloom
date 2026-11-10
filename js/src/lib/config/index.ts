import type { UiFeatureFlags } from "@/lib/api/types/complex";

import { z } from "zod";

const runtimeConfigSchema = z.object({
  featureFlags: z.record(z.boolean()),
});

function readRuntimeConfig() {
  const json = document.getElementById("codebloom-config")?.textContent;
  if (!json) {
    throw new Error("Missing #codebloom-config runtime configuration");
  }

  const { featureFlags } = runtimeConfigSchema.parse(JSON.parse(json));
  return { featureFlags: featureFlags as UiFeatureFlags };
}

export const runtimeConfig = readRuntimeConfig();
