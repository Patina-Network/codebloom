import SubmissionDetailsContentSkeleton from "@/app/submission/[submissionId]/_components/SubmissionDetailsContentSkeleton";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen } from "@testing-library/react";

describe("SubmissionDetailsContentSkeleton", () => {
  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;

  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render question title skeleton", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const element = screen.getByTestId("submission-skeleton-question-title");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render question link button skeleton", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const element = screen.getByTestId(
      "submission-skeleton-question-link-button",
    );
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render question solved by skeleton", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const element = screen.getByTestId(
      "submission-skeleton-question-solved-by",
    );
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render question information skeletons", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const info1 = screen.getByTestId("submission-skeleton-question-points");
    const info2 = screen.getByTestId("submission-skeleton-question-difficulty");
    const info3 = screen.getByTestId(
      "submission-skeleton-question-acceptance-rate",
    );

    expect(info1).toBeInTheDocument();
    expect(info1).toBeVisible();
    expect(info2).toBeInTheDocument();
    expect(info2).toBeVisible();
    expect(info3).toBeInTheDocument();
    expect(info3).toBeVisible();
  });

  it("should render question details skeleton", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const element = screen.getByTestId("submission-skeleton-question-details");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render user solution skeleton", () => {
    renderProviderFn?.(<SubmissionDetailsContentSkeleton />);
    const element = screen.getByTestId("submission-skeleton-user-solution");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });
});
