import {
  CREATE_DISCORD_ANNOUNCEMENT_FAILURE_MESSAGE,
  CREATE_DISCORD_ANNOUNCEMENT_SUCCESS_MESSAGE,
  CREATE_DISCORD_ANNOUNCEMENT_SERVER_ERROR_MESSAGE,
  createDiscordAnnouncementPartialFailureHandler,
  createDiscordAnnouncementServerErrorHandler,
  createDiscordAnnouncementSuccessHandler,
} from "@/__mock__/admin";
import NewDiscordAnnouncementModal from "@/app/admin/_components/announcements/NewDiscordAnnouncementModal";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setupServer } from "msw/node";

describe("NewDiscordAnnouncementModal", () => {
  const server = setupServer(createDiscordAnnouncementSuccessHandler);
  let requestCount = 0;
  let requestBodies: unknown[] = [];

  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    requestCount = 0;
    requestBodies = [];
    server.events.removeAllListeners();
    server.events.on("request:start", async ({ request }) => {
      requestCount++;
      requestBodies.push(await request.clone().json());
    });
  });

  const openModal = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(
      screen.getByRole("button", { name: "New Discord Announcement" }),
    );
    return screen.findByRole("textbox", { name: /Announcement message/ });
  };

  it("should be closed until the button is clicked", async () => {
    renderProviderFn?.(<NewDiscordAnnouncementModal />);
    const user = userEvent.setup();

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();

    expect(await openModal(user)).toBeInTheDocument();
    expect(
      screen.getByText("Create new discord announcement"),
    ).toBeInTheDocument();
  });

  it("should not call the API when the message is only whitespace", async () => {
    renderProviderFn?.(<NewDiscordAnnouncementModal />);
    const user = userEvent.setup();

    const input = await openModal(user);
    await user.type(input, "   ");
    await user.click(screen.getByRole("button", { name: "Submit" }));

    expect(requestCount).toBe(0);
    expect(input).toBeInTheDocument();
  });

  it("should send the trimmed message, notify, close, and clear the field on success", async () => {
    renderProviderFn?.(<NewDiscordAnnouncementModal />);
    const user = userEvent.setup();

    const input = await openModal(user);
    await user.type(input, "  big news  ");
    await user.click(screen.getByRole("button", { name: "Submit" }));

    expect(
      await screen.findByText(CREATE_DISCORD_ANNOUNCEMENT_SUCCESS_MESSAGE),
    ).toBeInTheDocument();
    expect(requestCount).toBe(1);
    expect(requestBodies[0]).toEqual({ message: "big news" });
    await waitFor(() => {
      expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    });

    expect(await openModal(user)).toHaveValue("");
  });

  it("should notify and keep the modal open when the API reports failure", async () => {
    server.use(createDiscordAnnouncementPartialFailureHandler);
    renderProviderFn?.(<NewDiscordAnnouncementModal />);
    const user = userEvent.setup();

    const input = await openModal(user);
    await user.type(input, "big news");
    await user.click(screen.getByRole("button", { name: "Submit" }));

    expect(
      await screen.findByText(CREATE_DISCORD_ANNOUNCEMENT_FAILURE_MESSAGE),
    ).toBeInTheDocument();
    expect(input).toBeInTheDocument();
    expect(input).toHaveValue("big news");
  });

  it("should show the error message when the request fails", async () => {
    server.use(createDiscordAnnouncementServerErrorHandler);
    renderProviderFn?.(<NewDiscordAnnouncementModal />);
    const user = userEvent.setup();

    const input = await openModal(user);
    await user.type(input, "big news");
    await user.click(screen.getByRole("button", { name: "Submit" }));

    expect(
      await screen.findByText(CREATE_DISCORD_ANNOUNCEMENT_SERVER_ERROR_MESSAGE),
    ).toBeInTheDocument();
    expect(input).toBeInTheDocument();
  });
});
