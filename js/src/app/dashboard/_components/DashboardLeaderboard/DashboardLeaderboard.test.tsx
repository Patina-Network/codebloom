import {
  currentLeaderboardUsersErrorHandler,
  currentLeaderboardUsersFailedHandler,
  currentLeaderboardUsersHandler,
  currentLeaderboardUsersWithTagsHandler,
  successfulLeaderboardHandlers,
} from "@/__mock__/leaderboard";
import LeaderboardForDashboard from "@/app/dashboard/_components/DashboardLeaderboard/DashboardLeaderboard";
import { ApiURL } from "@/lib/api/common/apiURL";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { cleanup, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";

// user-1 exists in currentLeaderboardUsersHandler (aphrodite, poseidon, hermes are user-1/2/3)
const MOCK_USER_IN_TOP5 = "user-1";
const MOCK_USER_NOT_IN_TOP5 = "not-in-leaderboard-user";

const makeMyPointsUrl = (userId: string) =>
  ApiURL.create("/api/leaderboard/current/user/{userId}", {
    method: "GET",
    params: { userId },
  }).url.toString();

const makeMyPointsHandler = (userId: string) =>
  http.get(makeMyPointsUrl(userId), () =>
    HttpResponse.json({
      success: true,
      message: "User leaderboard data loaded!",
      payload: {
        id: userId,
        discordId: `discord-${userId}`,
        discordName: "zeus",
        leetcodeUsername: null,
        nickname: null,
        admin: false,
        profileUrl: null,
        tags: [],
        achievements: [],
        totalScore: 5,
      },
    }),
  );

const currentLeaderboardUsersUrl = ApiURL.create(
  "/api/leaderboard/current/user/all",
  { method: "GET" },
);

describe("LeaderboardForDashboard", () => {
  const server = setupServer(
    currentLeaderboardUsersHandler,
    ...successfulLeaderboardHandlers,
    makeMyPointsHandler(MOCK_USER_IN_TOP5),
    makeMyPointsHandler(MOCK_USER_NOT_IN_TOP5),
  );

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
    cleanup();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render skeleton before API resolves", () => {
    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    expect(screen.queryByText("View all")).not.toBeInTheDocument();
  });

  it("should render users after successful API call", async () => {
    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(screen.getByText("View all")).toBeInTheDocument();
    });

    expect(screen.getAllByText("aphrodite").length).toBeGreaterThan(0);
    expect(screen.getAllByText("poseidon").length).toBeGreaterThan(0);
    expect(screen.getAllByText("hermes").length).toBeGreaterThan(0);
  });

  it("should show error message when API errors", async () => {
    server.use(currentLeaderboardUsersErrorHandler);

    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(
        screen.getByText(
          "Sorry, something went wrong. Please try again later.",
        ),
      ).toBeInTheDocument();
    });
  });

  it("should show failure message when API returns success false", async () => {
    server.use(currentLeaderboardUsersFailedHandler);

    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(screen.getByText("No users to display")).toBeInTheDocument();
    });
  });

  it("should show empty state when no users in leaderboard", async () => {
    server.use(
      http.get(currentLeaderboardUsersUrl.url.toString(), () =>
        HttpResponse.json({
          success: true,
          message: "Loaded!",
          payload: { hasNextPage: false, pages: 1, pageSize: 5, items: [] },
        }),
      ),
    );

    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(screen.getByText(/No users here yet/i)).toBeInTheDocument();
    });
  });

  it("should not show MyCurrentPoints when user is in top 5", async () => {
    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(screen.getByText("View all")).toBeInTheDocument();
    });

    expect(screen.queryByText("Me")).not.toBeInTheDocument();
  });

  it("should show MyCurrentPoints when user is not in top 5", async () => {
    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_NOT_IN_TOP5} userTags={[]} />,
    );

    await waitFor(() => {
      expect(screen.getByText("Me")).toBeInTheDocument();
    });
  });

  it("should render tags beside nicknames or discord names", async () => {
    server.use(currentLeaderboardUsersWithTagsHandler);
    renderProviderFn?.(
      <LeaderboardForDashboard userId={MOCK_USER_IN_TOP5} userTags={[]} />,
    );

    // aphrodite/athena: tag beside nickname; poseidon/ares: tag beside discord name.
    expect(await screen.findByText("Aphro")).toBeInTheDocument();
    expect(screen.getAllByAltText("Hunter College Logo")).toHaveLength(2);
    expect(screen.getAllByAltText("NYU Logo")).toHaveLength(2);
  });
});
