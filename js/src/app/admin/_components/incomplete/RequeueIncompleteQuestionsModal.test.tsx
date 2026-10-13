import {
  REQUEUE_INCOMPLETE_FAILURE_MESSAGE,
  REQUEUE_INCOMPLETE_SUCCESS_MESSAGE,
  requeueIncompleteFailureHandler,
  requeueIncompleteSuccessHandler,
} from "@/__mock__/admin";
import RequeueIncompleteQuestionsModal from "@/app/admin/_components/incomplete/RequeueIncompleteQuestionsModal";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("RequeueIncompleteQuestionsModal", () => {
  const server = setupServer(requeueIncompleteSuccessHandler);
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

  it("should not call the API until confirmed", async () => {
    renderProviderFn?.(<RequeueIncompleteQuestionsModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Requeue Incomplete Questions" }),
    );

    expect(
      await screen.findByRole("button", { name: "Requeue" }),
    ).toBeInTheDocument();
    expect(requestCount).toBe(0);
  });

  it("should call the API, notify with the queued count, and close", async () => {
    renderProviderFn?.(<RequeueIncompleteQuestionsModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Requeue Incomplete Questions" }),
    );
    await user.click(await screen.findByRole("button", { name: "Requeue" }));

    expect(
      await screen.findByText(REQUEUE_INCOMPLETE_SUCCESS_MESSAGE),
    ).toBeInTheDocument();
    expect(requestCount).toBe(1);
    await waitFor(() => {
      expect(
        screen.queryByRole("button", { name: "Requeue" }),
      ).not.toBeInTheDocument();
    });
  });

  it("should show the failure message when the API fails", async () => {
    server.use(requeueIncompleteFailureHandler);
    renderProviderFn?.(<RequeueIncompleteQuestionsModal />);
    const user = userEvent.setup();

    await user.click(
      screen.getByRole("button", { name: "Requeue Incomplete Questions" }),
    );
    await user.click(await screen.findByRole("button", { name: "Requeue" }));

    expect(
      await screen.findByText(REQUEUE_INCOMPLETE_FAILURE_MESSAGE),
    ).toBeInTheDocument();
  });
});
