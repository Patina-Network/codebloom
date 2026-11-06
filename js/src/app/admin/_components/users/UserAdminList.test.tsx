import {
  createToggleAdminSuccessHandler,
  createToggleAdminFailureHandler,
} from "@/__mock__/admin";
import {
  getAllUsersErrorHandler,
  getAllUsersFailedHandler,
  getAllUsersHandler,
} from "@/__mock__/user";
import UserAdminList from "@/app/admin/_components/users/UserAdminList.tsx";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("UserAdminList API Succeeded", () => {
  const server = setupServer(getAllUsersHandler);

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should initially render a loading state", () => {
    renderProviderFn?.(<UserAdminList />);

    const element = screen.getByTestId("user-admin-list-skeleton-board");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render user admin list board and filter button eventually", async () => {
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByTestId("user-admin-list-board")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByTestId("user-admin-list-filter")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText("aphrodite_lc")).toBeInTheDocument();
    });
  });

  it("should show only usernames that match the search query", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByRole("textbox")).toBeInTheDocument();
    });

    await user.type(screen.getByRole("textbox"), "aphrodite");

    await waitFor(() => {
      expect(screen.getByText("aphrodite_lc")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.queryByText("poseidon_lc")).not.toBeInTheDocument();
    });
  });

  it("should render admin badge colors correctly", async () => {
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByTestId("aphrodite_lc-button")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByTestId("persephone_lc-button")).toBeInTheDocument();
    });

    expect(screen.getByTestId("aphrodite_lc-button")).toHaveStyle(
      "--button-bg: var(--mantine-color-gray-filled)",
    );

    expect(screen.getByTestId("persephone_lc-button")).not.toHaveStyle(
      "--button-bg: var(--mantine-color-gray-filled)",
    );
  });

  it("should switch admin status from chosen user upon click", async () => {
    const user = userEvent.setup();
    server.use(createToggleAdminSuccessHandler);
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByTestId("persephone_lc-button")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("persephone_lc-button"));

    await waitFor(() => {
      expect(
        screen.getByText(
          /User with Discord name of discord-4 is no longer an admin/i,
        ),
      ).toBeInTheDocument();
    });
  });

  it("should show admins first", async () => {
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByText("aphrodite_lc")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText("persephone_lc")).toBeInTheDocument();
    });

    expect(
      screen
        .getByText("persephone_lc")
        .compareDocumentPosition(screen.getByText("aphrodite_lc")),
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });

  it("should show non-admins too if filter is off but only admins if filter is on", async () => {
    const user = userEvent.setup();
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "Filters" }),
      ).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Filters" }));

    await waitFor(() => {
      expect(
        screen.getByTestId("user-admin-list-filter-switch"),
      ).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("user-admin-list-filter-switch"));

    await waitFor(() => {
      expect(screen.getByText("persephone_lc")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.queryByText("aphrodite_lc")).not.toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Filters" }));

    await waitFor(() => {
      expect(
        screen.getByTestId("user-admin-list-filter-switch"),
      ).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("user-admin-list-filter-switch"));

    await waitFor(() => {
      expect(screen.getByText("persephone_lc")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText("aphrodite_lc")).toBeInTheDocument();
    });
  });

  it("should show error message when toggle API errors", async () => {
    const user = userEvent.setup();
    server.use(createToggleAdminFailureHandler);
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByTestId("persephone_lc-button")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("persephone_lc-button"));

    await waitFor(() => {
      expect(
        screen.getByText("Failed to update admin status"),
      ).toBeInTheDocument();
    });
  });

  it("should have error div when API errors", async () => {
    server.use(getAllUsersErrorHandler);
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByText("Error")).toBeInTheDocument();
    });
  });

  it("should have a fail message when API errors", async () => {
    server.use(getAllUsersFailedHandler);
    renderProviderFn?.(<UserAdminList />);

    await waitFor(() => {
      expect(screen.getByText("No users to display")).toBeInTheDocument();
    });
  });
});
