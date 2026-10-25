import { useUserMetricsQuery } from "@/lib/api/queries/user";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";

const metrics = {
  success: true,
  message: "Metrics loaded!",
  payload: {
    items: [
      {
        id: "metric-1",
        userId: "user-1",
        points: 500,
        createdAt: "2026-10-01T00:00:00.000Z",
      },
    ],
    pages: 2,
    hasNextPage: true,
  },
};

describe("useUserMetricsQuery placeholders", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(Response.json(metrics))
        .mockImplementation(() => new Promise<Response>(() => {})),
    );
  });

  afterEach(() => {
    cleanup();
    queryClient.clear();
    vi.unstubAllGlobals();
  });

  function wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  }

  it("retains metrics while fetching another page for the same user", async () => {
    const { result } = renderHook(
      () => useUserMetricsQuery({ userId: "user-1" }),
      { wrapper },
    );

    expect(result.current.data).toBeUndefined();
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    act(() => result.current.goForward());

    expect(result.current.page).toBe(2);
    expect(result.current.isFetching).toBe(true);
    expect(result.current.isPlaceholderData).toBe(true);
    expect(result.current.data).toEqual(metrics);
  });

  it.each([false, true])(
    "clears metrics when switching users (page request pending: %s)",
    async (changePage) => {
      const { result, rerender } = renderHook(
        ({ userId }) => useUserMetricsQuery({ userId }),
        { initialProps: { userId: "user-1" }, wrapper },
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      if (changePage) {
        act(() => result.current.goForward());
        expect(result.current.isPlaceholderData).toBe(true);
      }

      rerender({ userId: "user-2" });

      expect(result.current.isPending).toBe(true);
      expect(result.current.isPlaceholderData).toBe(false);
      expect(result.current.data).toBeUndefined();
    },
  );
});
