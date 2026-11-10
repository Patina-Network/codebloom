/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

// eslint-disable-next-line no-relative-import-paths/no-relative-import-paths -- Vite config is outside src/.
import { runtimeConfigPlugin } from "./runtime-config";

const backendUrl = process.env.CODEBLOOM_BACKEND_URL ?? "http://localhost:8080";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tsconfigPaths(), runtimeConfigPlugin(backendUrl)],
  envDir: "../",
  server: {
    proxy: {
      "/api": {
        target: backendUrl,
        // This is not traditional, but it allows the authenticated redirect to go back to the SPA.
        changeOrigin: false,
      },
    },
  },
  resolve: {
    alias: {
      "@tabler/icons-react": "@tabler/icons-react/dist/esm/icons/index.mjs",
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    coverage: {
      enabled: true,
      provider: "istanbul",
      reporter: ["lcov"],
    },
    setupFiles: ["src/lib/test/defaults.tsx"],
  },
});
