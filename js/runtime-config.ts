import type { Plugin } from "vite";

import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const placeholder = "__CODEBLOOM_CONFIG__";

/**
 * Create codebloom-config by calling the Java backend to get fully transformed index.html
 * */
export function runtimeConfigPlugin(backendUrl: string): Plugin {
  async function inject(html: string): Promise<string> {
    const response = await fetch(new URL("/index.html", backendUrl), {
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) {
      throw new Error(`Backend HTML bootstrap failed: HTTP ${response.status}`);
    }

    const backendHtml = await response.text();
    const config = backendHtml.match(
      /<script\b[^>]*\bid=["']codebloom-config["'][^>]*>([\s\S]*?)<\/script>/i,
    )?.[1];
    if (!config || !html.includes(placeholder)) {
      throw new Error("Application HTML is missing its runtime configuration");
    }

    JSON.parse(config);
    return html.replace(placeholder, () => config);
  }

  return {
    name: "codebloom-runtime-config",
    apply: "serve",
    transformIndexHtml: {
      order: "pre",
      handler: inject,
    },
    configurePreviewServer(server) {
      const html = readFile(
        resolve(server.config.root, server.config.build.outDir, "index.html"),
        "utf8",
      );
      server.middlewares.use((request, response, next) => {
        const pathname = new URL(request.url ?? "/", "http://localhost")
          .pathname;
        const isHtml =
          pathname === "/" ||
          pathname === "/index.html" ||
          request.headers.accept?.includes("text/html");
        if (
          !isHtml ||
          pathname.startsWith("/api") ||
          (request.method !== "GET" && request.method !== "HEAD")
        ) {
          next();
          return;
        }
        void html
          .then(inject)
          .then((body) => {
            response.setHeader("Content-Type", "text/html; charset=utf-8");
            response.setHeader("Cache-Control", "no-store");
            response.end(request.method === "HEAD" ? undefined : body);
          })
          .catch(next);
      });
    },
  };
}
