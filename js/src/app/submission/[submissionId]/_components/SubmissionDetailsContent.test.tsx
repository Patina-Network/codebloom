import {
  submissionDetailsHandler,
  submissionDetailsFailedHandler,
  submissionDetailsErrorHandler,
  submissionDetailsProcessingHandler,
  MOCK_SUBMISSION_ID,
} from "@/__mock__/submissions";
import SubmissionDetailsContent from "@/app/submission/[submissionId]/_components/SubmissionDetailsContent";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
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
});
