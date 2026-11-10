import "@testing-library/jest-dom";
import "@/patches";

import type { UiFeatureFlags } from "@/lib/api/types/complex";

// Mirrors the backend-rendered #codebloom-config element so flag modules can read it on import.
const runtimeConfig = document.createElement("script");
runtimeConfig.id = "codebloom-config";
runtimeConfig.type = "application/json";
runtimeConfig.textContent = JSON.stringify({
  featureFlags: {
    duels: true,
    userMetrics: true,
  } satisfies UiFeatureFlags,
});
document.head.append(runtimeConfig);

// Mock ResizeObserver for Mantine ScrollArea
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserverMock;

// https://stackoverflow.com/a/42685938
beforeAll(() => {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(), // Deprecated
      removeListener: vi.fn(), // Deprecated
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
});
