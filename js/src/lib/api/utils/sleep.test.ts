import { sleep } from "@/lib/api/utils/lag";
import { afterEach, describe, expect, it, vi } from "vitest";

describe("sleep", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllEnvs();
  });

  it("skips delay in test environment", async () => {
    let resolved = false;
    sleep(10_000).then(() => {
      resolved = true;
    });

    await vi.advanceTimersByTimeAsync(0);

    expect(resolved).toBe(true);
    expect(vi.getTimerCount()).toBe(0);
  });

  it("waits when not in test environment", async () => {
    vi.stubEnv("MODE", "production");

    let resolved = false;
    void sleep(1_000).then(() => {
      resolved = true;
    });

    await vi.advanceTimersByTimeAsync(0);
    expect(resolved).toBe(false);

    await vi.advanceTimersByTimeAsync(1_000);
    expect(resolved).toBe(true);
  });
});
