import {
  submissionDetailsHandler,
  submissionDetailsFailedHandler,
  submissionDetailsErrorHandler,
  submissionDetailsProcessingHandler,
  submissionDetailsSparseHandler,
  submissionDetailsVerifiedHandler,
  MOCK_SUBMISSION_ID,
} from "@/__mock__/submissions";
import SubmissionDetailsContent from "@/app/submission/[submissionId]/_components/SubmissionDetailsContent";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("SubmissionDetailsContent API Succeeded", () => {
  const server = setupServer(submissionDetailsHandler);

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render loading state initially", () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    const element = screen.getByTestId("submission-skeleton-question-title");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render question title after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("Two Sum")).toBeInTheDocument();
    });
  });

  it("should render question link button after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      const link = screen.getByRole("link", { name: /open on leetcode/i });
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute(
        "href",
        "https://leetcode.com/problems/two-sum",
      );
      expect(link).toHaveAttribute("target", "_blank");
      expect(link).toHaveAttribute("rel", "noopener noreferrer");
    });
  });

  it("should render discord name after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("aphrodite")).toBeInTheDocument();
    });
  });

  it("should render points awarded after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("10")).toBeInTheDocument();
    });
  });

  it("should render difficulty badge after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("Easy")).toBeInTheDocument();
    });
  });

  it("should render acceptance rate badge after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("49%")).toBeInTheDocument();
    });
  });

  it("should link author name to user profile after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "aphrodite" })).toHaveAttribute(
        "href",
        "/user/user-1",
      );
    });
  });

  it("should render language after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("Python3")).toBeInTheDocument();
    });
  });

  it("should render runtime after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("50ms")).toBeInTheDocument();
    });
  });

  it("should render memory after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("16MB")).toBeInTheDocument();
    });
  });

  it("should not render skeleton after successful API call", async () => {
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(
        screen.queryByTestId("submission-skeleton-question-title"),
      ).not.toBeInTheDocument();
    });
  });

  it("should render processing state when submission data is not yet available", async () => {
    server.use(submissionDetailsProcessingHandler);
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(
        screen.getByText("Data Currently Not Available"),
      ).toBeInTheDocument();
      expect(
        screen.getByText(/this submission is still being processed/i),
      ).toBeInTheDocument();
    });
  });

  it("should render error message when API call fails", async () => {
    server.use(submissionDetailsErrorHandler);
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(
        screen.getByText(/sorry, something went wrong/i),
      ).toBeInTheDocument();
    });
  });

  it("should redirect when submission is not found", async () => {
    server.use(submissionDetailsFailedHandler);
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByText("Submission not found.")).toBeInTheDocument();
    });
  });

  it("should render verified nickname, submitted time and topics when available", async () => {
    server.use(submissionDetailsVerifiedHandler);
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "Aphrodite" })).toHaveAttribute(
        "href",
        "/user/user-1",
      );
    });
    expect(screen.queryByText("aphrodite")).not.toBeInTheDocument();
    expect(screen.getByText("Submitted")).toBeInTheDocument();
    expect(screen.getByText("2d ago")).toBeInTheDocument();
    expect(screen.getByText("Array")).toBeInTheDocument();
  });

  it("should render fallbacks when optional submission fields are missing", async () => {
    server.use(submissionDetailsSparseHandler);
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    await waitFor(() => {
      expect(
        screen.getByRole("link", { name: "Unknown user" }),
      ).toBeInTheDocument();
    });
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(
      screen.getByText("No available description found."),
    ).toBeInTheDocument();
    expect(screen.getByText("Csharp")).toBeInTheDocument();
    expect(screen.queryByText("Submitted")).not.toBeInTheDocument();
  });

  it("should copy the solution code to the clipboard", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(
      <SubmissionDetailsContent submissionId={MOCK_SUBMISSION_ID} />,
    );

    const copyButton = await screen.findByRole("button", {
      name: "Copy code",
    });
    await user.click(copyButton);

    await waitFor(async () => {
      expect(await navigator.clipboard.readText()).toBe(
        "def twoSum(nums, target):\n    pass",
      );
    });
    expect(await screen.findByText("Copied")).toBeInTheDocument();
  });
});
