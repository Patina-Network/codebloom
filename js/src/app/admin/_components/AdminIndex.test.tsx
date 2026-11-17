import { getIncompleteQuestionsSuccessHandler } from "@/__mock__/admin";
import { announcementHandler } from "@/__mock__/announcement";
import { allLeaderboardMetadataSuccessHandler } from "@/__mock__/leaderboard";
import { getAllUsersHandler } from "@/__mock__/user";
import AdminIndex from "@/app/admin/_components/AdminIndex";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import { setupServer } from "msw/node";

describe("AdminIndex page successfully loads", () => {
  const server = setupServer(
    getAllUsersHandler,
    getIncompleteQuestionsSuccessHandler,
    allLeaderboardMetadataSuccessHandler,
    announcementHandler,
  );

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;

  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render the Admin Page title", () => {
    renderProviderFn?.(<AdminIndex />);

    expect(screen.getByText("Admin Page")).toBeInTheDocument();
    expect(screen.getByText("Admin Page")).toBeVisible();
  });

  it("should render the Leaderboard component eventually", async () => {
    renderProviderFn?.(<AdminIndex />);

    await waitFor(() => {
      expect(screen.getByText("leaderboard-name-1")).toBeInTheDocument();
      expect(screen.getByText("leaderboard-name-1")).toBeVisible();
    });
  });

  it("should render the Incomplete Questions component eventually", async () => {
    renderProviderFn?.(<AdminIndex />);
    await waitFor(() => {
      expect(screen.getByText("Test Question")).toBeInTheDocument();
      expect(screen.getByText("Test Question")).toBeVisible();
    });
  });

  it("should render the Users component eventually", async () => {
    renderProviderFn?.(<AdminIndex />);

    await waitFor(() => {
      expect(screen.getByTestId("user-admin-list-board")).toBeInTheDocument();
    });
  });
});
