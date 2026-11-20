import {
  authHandler,
  authWithSchoolHandler,
  authErrorHandler,
  authUnauthenticatedHandler,
} from "@/__mock__/settings";
import SettingsPage from "@/app/settings/Settings.page";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("SettingsPage API Succeeded", () => {
  const server = setupServer(authHandler);

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
    renderProviderFn?.(<SettingsPage />);

    const element = screen.getByTestId("settings-skeleton-title");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render Settings heading after successful API call", async () => {
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByText("Settings")).toBeInTheDocument();
    });
  });

  it("should not render skeleton after successful API call", async () => {
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(
        screen.queryByTestId("settings-skeleton-title"),
      ).not.toBeInTheDocument();
    });
  });

  it("should render school verify settings card after successful API call", async () => {
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByText("Verify School")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "Verify Now" }),
      ).toBeInTheDocument();
    });
  });

  it("should render with school already verified when schoolEmail exists", async () => {
    server.use(authWithSchoolHandler);
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByText("Verify School")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "You are already verified!" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "You are already verified!" }),
      ).toBeDisabled();
    });
  });

  it("should render change profile picture card after successful API call", async () => {
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(screen.getByText("Change Profile Picture")).toBeInTheDocument();
      expect(
        screen.getByText(
          "To update your profile picture, please change your icon on LeetCode.",
        ),
      ).toBeInTheDocument();
    });
  });

  it("should render contact email link in change profile picture card", async () => {
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      const emailLink = screen.getByRole("link", {
        name: "codebloom@patinanetwork.org",
      });
      expect(emailLink).toBeInTheDocument();
      expect(emailLink).toHaveAttribute(
        "href",
        "mailto:codebloom@patinanetwork.org",
      );
    });
  });

  describe.each([
    {
      name: "Log Out All Sessions",
      modalText: /are you sure you want to log out of all sessions/i,
    },
    {
      name: "Deactivate Account",
      modalText: /are you sure you want to deactivate your account/i,
    },
  ])("$name card", ({ name, modalText }) => {
    it("should render card after successful API call", async () => {
      renderProviderFn?.(<SettingsPage />);

      await waitFor(() => {
        expect(screen.getByRole("heading", { name })).toBeInTheDocument();
        expect(screen.getByRole("button", { name })).toBeInTheDocument();
      });
    });

    it("should render button as enabled", async () => {
      renderProviderFn?.(<SettingsPage />);

      await waitFor(() => {
        expect(screen.getByRole("button", { name })).not.toBeDisabled();
      });
    });

    it("should open modal when button is clicked, close on cancel", async () => {
      const user = userEvent.setup();
      renderProviderFn?.(<SettingsPage />);

      await waitFor(() => {
        expect(
          screen.queryByTestId("settings-skeleton-title"),
        ).not.toBeInTheDocument();
      });

      await user.click(screen.getByRole("button", { name }));

      await waitFor(() => {
        expect(screen.getByText(modalText)).toBeInTheDocument();
      });

      await user.click(screen.getByRole("button", { name: "Cancel" }));

      await waitFor(() => {
        expect(screen.queryByText(modalText)).not.toBeInTheDocument();
      });
    });
  });

  it("should render error message when API call fails", async () => {
    server.use(authErrorHandler);
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/sorry, something went wrong/i),
      ).toBeInTheDocument();
    });
  });

  it("should redirect when user is not authenticated", async () => {
    server.use(authUnauthenticatedHandler);
    renderProviderFn?.(<SettingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/you are not authenticated/i),
      ).toBeInTheDocument();
    });
  });
});
