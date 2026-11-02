import {
  MOCK_USER_SUBMISSIONS,
  userSubmissionsEmptyHandler,
  userSubmissionsErrorHandler,
  userSubmissionsFailedHandler,
  userSubmissionsHandler,
} from "@/__mock__/user";
import RecentSubmissions from "@/app/dashboard/_components/RecentSubmissions/RecentSubmissions";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import { setupServer } from "msw/node";

describe("RecentSubmissions", () => {
  const server = setupServer(userSubmissionsHandler);

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render 'Submissions' heading after successful API call", async () => {
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText("Submissions")).toBeInTheDocument();
    });
  });

  it("should render each submission title", async () => {
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      for (const q of MOCK_USER_SUBMISSIONS) {
        expect(screen.getByText(q.questionTitle)).toBeInTheDocument();
      }
    });
  });

  it("should link each submission to its submission page", async () => {
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    const first = MOCK_USER_SUBMISSIONS[0];
    await waitFor(() => {
      expect(
        screen.getByText(first.questionTitle).closest("a"),
      ).toHaveAttribute("href", `/submission/${first.id}`);
    });
  });

  it("should render difficulty and points for a submission", async () => {
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    const first = MOCK_USER_SUBMISSIONS[0];
    await waitFor(() => {
      expect(
        screen.getByText(`+${first.pointsAwarded} points`),
      ).toBeInTheDocument();
    });
    expect(
      screen.getAllByText(first.questionDifficulty).length,
    ).toBeGreaterThan(0);
  });

  it("should render 'View all' link to the user's profile", async () => {
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "View all" })).toHaveAttribute(
        "href",
        "/user/user-1",
      );
    });
  });

  it("should render empty message when user has no submissions", async () => {
    server.use(userSubmissionsEmptyHandler);
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      expect(screen.getByText(/no problems solved yet/i)).toBeInTheDocument();
    });
  });

  it("should render failed message when API returns success false", async () => {
    server.use(userSubmissionsFailedHandler);
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      expect(
        screen.getAllByText("Failed to load submissions").length,
      ).toBeGreaterThan(0);
    });
  });

  it("should render error message when API call fails", async () => {
    server.use(userSubmissionsErrorHandler);
    renderProviderFn?.(<RecentSubmissions userId="user-1" />);

    await waitFor(() => {
      expect(
        screen.getByText("Sorry, something went wrong. Please try again."),
      ).toBeInTheDocument();
    });
  });
});
