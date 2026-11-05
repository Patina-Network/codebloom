import { UnknownApiResponse } from "@/lib/api/common/apiResponse";
import { ApiURL } from "@/lib/api/common/apiURL";
import { Empty } from "@/lib/api/common/empty";
import d from "dayjs";
import { http, HttpResponse } from "msw";

export const MOCK_LEADERBOARD_NAME = 'std::string name = "leaderboard name"';
export const MOCK_LEADERBOARD_LANGUAGE = "cpp";
export const MOCK_LEADERBOARD_EXPIRE = d().add(10, "days").toISOString();

const editLeaderboard = ApiURL.create("/api/admin/leaderboard/current", {
  method: "PUT",
});

export const editLeaderboardSuccessHandler = http.put(
  editLeaderboard.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: {},
      message: "Leaderboard updated successfully",
    } satisfies ReturnType<typeof editLeaderboard.res>);
  },
);

const refreshLeetcodeToken = ApiURL.create("/api/admin/leetcode/token", {
  method: "POST",
});

export const REFRESH_LEETCODE_TOKEN_SUCCESS_MESSAGE =
  "LeetCode token refresh triggered.";
export const REFRESH_LEETCODE_TOKEN_FAILURE_MESSAGE =
  "Failed to refresh LeetCode token.";

export const refreshLeetcodeTokenSuccessHandler = http.post(
  refreshLeetcodeToken.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: {},
      message: REFRESH_LEETCODE_TOKEN_SUCCESS_MESSAGE,
    } satisfies UnknownApiResponse<Empty>);
  },
);

export const refreshLeetcodeTokenFailureHandler = http.post(
  refreshLeetcodeToken.url.toString(),
  () => {
    return HttpResponse.json(
      {
        success: false,
        message: REFRESH_LEETCODE_TOKEN_FAILURE_MESSAGE,
      },
      { status: 500 },
    );
  },
);

const requeueIncompleteQuestions = ApiURL.create(
  "/api/admin/questions/incomplete/requeue",
  {
    method: "POST",
  },
);

export const REQUEUE_INCOMPLETE_SUCCESS_MESSAGE =
  "Queued 3 incomplete question(s).";
export const REQUEUE_INCOMPLETE_FAILURE_MESSAGE =
  "Failed to requeue incomplete questions.";

export const requeueIncompleteSuccessHandler = http.post(
  requeueIncompleteQuestions.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: {},
      message: REQUEUE_INCOMPLETE_SUCCESS_MESSAGE,
    } satisfies UnknownApiResponse<Empty>);
  },
);

export const requeueIncompleteFailureHandler = http.post(
  requeueIncompleteQuestions.url.toString(),
  () => {
    return HttpResponse.json(
      {
        success: false,
        message: REQUEUE_INCOMPLETE_FAILURE_MESSAGE,
      },
      { status: 500 },
    );
  },
);

const createDiscordAnnouncement = ApiURL.create(
  "/api/admin/discord/announcement/",
  {
    method: "POST",
  },
);

export const CREATE_DISCORD_ANNOUNCEMENT_SUCCESS_MESSAGE =
  "Announcement sent to all clubs.";
export const CREATE_DISCORD_ANNOUNCEMENT_FAILURE_MESSAGE =
  "Failed to send announcement to: Club 1";
export const CREATE_DISCORD_ANNOUNCEMENT_SERVER_ERROR_MESSAGE =
  "Failed to send announcement: server error.";

export const createDiscordAnnouncementSuccessHandler = http.post(
  createDiscordAnnouncement.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: {},
      message: CREATE_DISCORD_ANNOUNCEMENT_SUCCESS_MESSAGE,
    } satisfies UnknownApiResponse<Empty>);
  },
);

// 200 with success: false, which is how the backend reports clubs that failed.
export const createDiscordAnnouncementPartialFailureHandler = http.post(
  createDiscordAnnouncement.url.toString(),
  () => {
    return HttpResponse.json(
      {
        success: false,
        message: CREATE_DISCORD_ANNOUNCEMENT_FAILURE_MESSAGE,
      },
      { status: 500 },
    );
  },
);

export const createDiscordAnnouncementServerErrorHandler = http.post(
  createDiscordAnnouncement.url.toString(),
  () => {
    return HttpResponse.json(
      {
        success: false,
        message: CREATE_DISCORD_ANNOUNCEMENT_SERVER_ERROR_MESSAGE,
      },
      { status: 500 },
    );
  },
);

const toggleAdmin = ApiURL.create("/api/admin/user/admin/toggle", {
  method: "POST",
});

const TOGGLE_ADMIN_SUCCESS_MESSAGE =
  "User with Discord name of discord-4 is no longer an admin";

const TOGGLE_ADMIN_FAILURE_MESSAGE = "Failed to update admin status";

export const createToggleAdminSuccessHandler = http.post(
  toggleAdmin.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      payload: {
        id: "user-4",
        discordId: "discord-4",
        discordName: "persephone",
        leetcodeUsername: "persephone_lc",
        nickname: "Verified Persephone",
        admin: false,
        profileUrl: null,
        tags: [],
        achievements: [],
      },
      message: TOGGLE_ADMIN_SUCCESS_MESSAGE,
    } satisfies ReturnType<typeof toggleAdmin.res>);
  },
);

export const createToggleAdminFailureHandler = http.post(
  toggleAdmin.url.toString(),
  () => {
    return HttpResponse.json({
      success: false,
      message: TOGGLE_ADMIN_FAILURE_MESSAGE,
    } satisfies ReturnType<typeof toggleAdmin.res>);
  },
);
