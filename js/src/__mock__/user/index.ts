import { ApiURL } from "@/lib/api/common/apiURL";
import { Tag } from "@/lib/api/types/schema";
import { http, HttpResponse } from "msw";

const allUsersUrl = ApiURL.create("/api/user/all", {
  method: "GET",
});

export const MOCK_USERS = [
  {
    id: "user-1",
    discordId: "discord-1",
    discordName: "aphrodite",
    leetcodeUsername: "aphrodite_lc",
    nickname: null,
    admin: false,
    profileUrl: null,
    tags: [
      {
        id: "tag-1",
        createdAt: "2024-01-01T00:00:00Z",
        userId: "user-1",
        tag: Tag.Patina,
      },
    ],
    achievements: [],
  },
  {
    id: "user-2",
    discordId: "discord-2",
    discordName: "poseidon",
    leetcodeUsername: "poseidon_lc",
    nickname: "Verified Poseidon",
    admin: false,
    profileUrl: null,
    tags: [],
    achievements: [],
  },
  {
    id: "user-3",
    discordId: "discord-3",
    discordName: "hermes",
    leetcodeUsername: null,
    nickname: null,
    admin: false,
    profileUrl: null,
    tags: [],
    achievements: [],
  },
];

export const getAllUsersHandler = http.get(
  allUsersUrl.url.toString(),
  ({ request }) => {
    const url = new URL(request.url);
    const query = url.searchParams.get("query") || "";

    const q = query.toLowerCase();
    const filteredUsers =
      query ?
        MOCK_USERS.filter(
          (user) =>
            user.discordName.toLowerCase().includes(q) ||
            (user.leetcodeUsername ?? "").toLowerCase().includes(q),
        )
      : MOCK_USERS;

    return HttpResponse.json({
      success: true,
      message: "Users loaded!",
      payload: {
        hasNextPage: false,
        pages: 1,
        pageSize: 5,
        items: filteredUsers,
      },
    } satisfies ReturnType<typeof allUsersUrl.res>);
  },
);

export const getAllUsersFailedHandler = http.get(
  allUsersUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: false,
      message: "No users to display",
    } satisfies ReturnType<typeof allUsersUrl.res>);
  },
);

export const getAllUsersErrorHandler = http.get(
  allUsersUrl.url.toString(),
  () => {
    return HttpResponse.error();
  },
);

const userProfileUrl = ApiURL.create("/api/user/{userId}/profile", {
  method: "GET",
  params: {
    userId: MOCK_USERS[0].id,
  },
});

export const getUserProfileHandler = http.get(
  userProfileUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      message: "User profile loaded!",
      payload: MOCK_USERS[0],
    } satisfies ReturnType<typeof userProfileUrl.res>);
  },
);
