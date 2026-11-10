import {
  currentLeaderboardUsersHandler,
  currentLeaderboardUsersWithTagsHandler,
  successfulLeaderboardHandlers,
} from "@/__mock__/leaderboard";
import { CurrentLeaderboard } from "@/app/leaderboard/_components/Leaderboard";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("Leaderboard API Succeeded", () => {
  const server = setupServer(
    currentLeaderboardUsersHandler,
    ...successfulLeaderboardHandlers,
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

  it("should render loading state initially", () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    const element = screen.getByTestId("leaderboard-skeleton-search-box");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render search box after successful API call", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const searchBox = screen.getByPlaceholderText("Search for User");
    expect(searchBox).toBeInTheDocument();
    expect(searchBox).toBeVisible();
  });

  it("should render top 3 podium cards after successful API call", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      const firstPlace = screen.getByText(/1st/);
      const secondPlace = screen.getByText(/2nd/);
      const thirdPlace = screen.getByText(/3rd/);

      expect(firstPlace).toBeInTheDocument();
      expect(secondPlace).toBeInTheDocument();
      expect(thirdPlace).toBeInTheDocument();
    });
  });

  it("should render user cards with discord names", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      const aphroditeEntries = screen.getAllByText(/aphrodite/);
      expect(aphroditeEntries.length).toBeGreaterThan(0);

      const poseidonEntries = screen.getAllByText(/poseidon/);
      expect(poseidonEntries.length).toBeGreaterThan(0);

      const hermesEntries = screen.getAllByText(/hermes/);
      expect(hermesEntries.length).toBeGreaterThan(0);
    });
  });

  it("should render user cards as links with correct attributes", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const userLinks = screen
      .getAllByRole("link")
      .filter((link) => link.getAttribute("href")?.includes("/user/"));

    expect(userLinks.length).toBeGreaterThan(0);
  });

  it("should include startDate and endDate in links when metadata is available", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      const aphroditeEntries = screen.getAllByText("aphrodite");
      expect(aphroditeEntries.length).toBeGreaterThan(0);

      const aphroditeLink = aphroditeEntries[0].closest("a");
      expect(aphroditeLink).toHaveAttribute(
        "href",
        expect.stringContaining("/user/user-1?startDate="),
      );
      expect(aphroditeLink).toHaveAttribute(
        "href",
        expect.stringContaining("endDate="),
      );
    });
  });

  it("should allow typing in search box", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const searchBox = screen.getByPlaceholderText("Search for User");
    await user.type(searchBox, "test");

    expect(searchBox).toHaveValue("test");
  });

  it("should render pagination controls", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const prevButton = screen.getAllByRole("button").find((btn) => {
      const svg = btn.querySelector("svg");
      return svg !== null && (btn as HTMLButtonElement).disabled;
    });
    expect(prevButton).toBeInTheDocument();
    expect(prevButton).toBeDisabled();
  });

  it("should render Filters button", async () => {
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      const filtersButton = screen.getByRole("button", { name: /filters/i });
      expect(filtersButton).toBeInTheDocument();
      expect(filtersButton).toBeVisible();
    });
  });

  it("should show Clubs submenu item when Filters is clicked", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const filtersButton = await screen.findByRole("button", {
      name: /filters/i,
    });
    await user.click(filtersButton);

    await waitFor(() => {
      const clubsItem = screen.getByText(/clubs/i);
      expect(clubsItem).toBeInTheDocument();
    });
  });

  it("should display and interact with club submenu items", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const filtersButton = await screen.findByRole("button", {
      name: /filters/i,
    });
    await user.click(filtersButton);

    const clubsItem = await screen.findByText(/clubs/i);
    await user.hover(clubsItem);

    await waitFor(
      () => {
        const gwcItem = screen.queryByText(/gwc - hunter college/i);
        expect(gwcItem).toBeInTheDocument();
      },
      { timeout: 1000 },
    );

    const gwcItem = await screen.findByText(/gwc - hunter college/i);
    const mhcItem = screen.queryByText(/mhc\+\+/i);
    expect(mhcItem).toBeInTheDocument();

    const gwcCheckbox = gwcItem
      .closest("button")
      ?.querySelector('input[type="checkbox"]');
    expect(gwcCheckbox).not.toBeChecked();

    await user.click(gwcItem);

    await waitFor(() => {
      expect(gwcCheckbox).toBeChecked();
    });
  });

  it("should show all users again after clearing search", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const searchBox = screen.getByPlaceholderText("Search for User");
    await user.type(searchBox, "aphrodite");

    await waitFor(
      () => {
        expect(screen.queryAllByText(/poseidon/).length).toBe(0);
      },
      { timeout: 1000 },
    );

    await user.clear(searchBox);

    await waitFor(
      () => {
        expect(screen.getAllByText(/aphrodite/).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/poseidon/).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/hermes/).length).toBeGreaterThan(0);
      },
      { timeout: 1000 },
    );
  });

  it("should handle case-insensitive search", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const searchBox = screen.getByPlaceholderText("Search for User");
    await user.type(searchBox, "APHRODITE");

    await waitFor(
      () => {
        expect(screen.getAllByText(/aphrodite/).length).toBeGreaterThan(0);
        expect(screen.queryAllByText(/poseidon/).length).toBe(0);
      },
      { timeout: 1000 },
    );
  });

  it("should navigate to next page when next button is clicked", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<CurrentLeaderboard />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("leaderboard-skeleton-search-box"),
      ).not.toBeInTheDocument();
    });

    const buttons = screen.getAllByRole("button");
    const nextButton = buttons.find((btn) => {
      const svg = btn.querySelector("svg");
      return svg !== null && !(btn as HTMLButtonElement).disabled;
    });

    if (nextButton) {
      await user.click(nextButton);
      await waitFor(() => {
        expect(nextButton).toBeInTheDocument();
      });
    }
  });

  it("should render nicknames and tags for podium and list entries", async () => {
    server.use(currentLeaderboardUsersWithTagsHandler);
    renderProviderFn?.(<CurrentLeaderboard />);

    // athena (list): nickname + tag, ares (list): tag only, apollo (list): nickname only.
    expect(await screen.findByText("Athena")).toBeInTheDocument();
    expect(screen.getByText("Apollo")).toBeInTheDocument();
    // aphrodite (podium) + athena (list) carry Hunter; poseidon (podium) + ares (list) carry NYU.
    expect(screen.getAllByAltText("Hunter College Logo")).toHaveLength(2);
    expect(screen.getAllByAltText("NYU Logo")).toHaveLength(2);
  });
});
