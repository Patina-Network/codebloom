describe("runtimeConfig", () => {
  let config: HTMLElement;
  let original: string | null;

  beforeEach(() => {
    const el = document.getElementById("codebloom-config");
    if (!el) {
      throw new Error("Test setup must provide #codebloom-config");
    }
    config = el;
    original = el.textContent;
    vi.resetModules();
  });

  afterEach(() => {
    config.textContent = original;
  });

  // `@/lib/config` reads the DOM once at import time, so each case needs a fresh
  // module instance via dynamic import; a static import would only see the setup default.
  it("reads feature flags from the embedded config", async () => {
    config.textContent = JSON.stringify({
      featureFlags: { duels: false, userMetrics: true },
    });

    const { runtimeConfig } = await import("@/lib/config");

    expect(runtimeConfig.featureFlags).toEqual({
      duels: false,
      userMetrics: true,
    });
  });

  it("fails fast when the embedded config is missing", async () => {
    config.textContent = "";

    await expect(import("@/lib/config")).rejects.toThrow(
      "Missing #codebloom-config runtime configuration",
    );
  });

  it("rejects config with non-boolean flag values", async () => {
    config.textContent = JSON.stringify({ featureFlags: { duels: "yes" } });

    await expect(import("@/lib/config")).rejects.toThrow();
  });
});
