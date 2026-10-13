import {
  REFRESH_LEETCODE_TOKEN_FAILURE_MESSAGE,
  REFRESH_LEETCODE_TOKEN_SUCCESS_MESSAGE,
  refreshLeetcodeTokenFailureHandler,
  refreshLeetcodeTokenSuccessHandler,
} from "@/__mock__/admin";
import RefreshLeetcodeTokenModal from "@/app/admin/_components/leetcode/RefreshLeetcodeTokenModal";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("RefreshLeetcodeTokenModal", () => {
  const server = setupServer(refreshLeetcodeTokenSuccessHandler);
  let requestCount = 0;

  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    requestCount = 0;
    server.events.removeAllListeners();
    server.events.on("request:start", () => {
      requestCount++;
    });
  });

  it("should not call the API until the warning is confirmed", async () => {
    renderProviderFn?.(<RefreshLeetcodeTokenModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Refresh LeetCode Token" }),
    );

    expect(
      await screen.findByText(
        "DO NOT TRIGGER THIS UNLESS YOU KNOW WHAT YOU'RE DOING.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Too many refreshes can cause rate limiting."),
    ).toBeInTheDocument();
    expect(requestCount).toBe(0);
  });

  it("should call the API, notify, and close on confirm", async () => {
    renderProviderFn?.(<RefreshLeetcodeTokenModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Refresh LeetCode Token" }),
    );
    await user.click(await screen.findByRole("button", { name: "Refresh" }));

    expect(
      await screen.findByText(REFRESH_LEETCODE_TOKEN_SUCCESS_MESSAGE),
    ).toBeInTheDocument();
    expect(requestCount).toBe(1);
    await waitFor(() => {
      expect(
        screen.queryByRole("button", { name: "Refresh" }),
      ).not.toBeInTheDocument();
    });
  });

  it("should show the failure message when the API fails", async () => {
    server.use(refreshLeetcodeTokenFailureHandler);
    renderProviderFn?.(<RefreshLeetcodeTokenModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Refresh LeetCode Token" }),
    );
    await user.click(await screen.findByRole("button", { name: "Refresh" }));

    expect(
      await screen.findByText(REFRESH_LEETCODE_TOKEN_FAILURE_MESSAGE),
    ).toBeInTheDocument();
  });
});
