import type { UnknownApiResponse } from "@/lib/api/common/apiResponse";
import type { Api } from "@/lib/api/types";

import { ApiURL } from "@/lib/api/common/apiURL";
import { Tag } from "@/lib/api/types/schema";
import d from "dayjs";
import { http, HttpResponse } from "msw";
import { v4 as uuid } from "uuid";

const currentMetadata = ApiURL.create("/api/leaderboard/current/metadata", {
  method: "GET",
});

const currentLeaderboardUsers = ApiURL.create(
  "/api/leaderboard/current/user/all",
  {
    method: "GET",
  },
);

export const MOCK_LEADERBOARD_ID = uuid();
const metadataById = ApiURL.create(
  "/api/leaderboard/{leaderboardId}/metadata",
  {
    method: "GET",
    params: {
      leaderboardId: MOCK_LEADERBOARD_ID,
    },
  },
);

export const currentMetadataNoSyntaxHighlightingHandler = http.get(
  currentMetadata.url.toString(),
  () => {
    return HttpResponse.json({
      payload: {
        id: uuid(),
        createdAt: d().toISOString(),
        shouldExpireBy: d().add(10, "days").toISOString(),
        syntaxHighlightingLanguage: null,
        name: "haiii",
        deletedAt: null,
      },
      success: true,
      message: "Leaderboard metadata loaded!",
    } satisfies ReturnType<typeof currentMetadata.res>);
  },
);
export const metadataByIdNoSyntaxHighlightingHandler = http.get(
  metadataById.url.toString(),
  () => {
    return HttpResponse.json({
      payload: {
        id: uuid(),
        createdAt: d().toISOString(),
        shouldExpireBy: d().add(10, "days").toISOString(),
        syntaxHighlightingLanguage: null,
        name: "haiii",
        deletedAt: null,
      },
      success: true,
      message: "Leaderboard metadata loaded!",
    } satisfies ReturnType<typeof metadataById.res>);
  },
);

export const successfulLeaderboardHandlers = [
  http.get(currentMetadata.url.toString(), () => {
    return HttpResponse.json({
      payload: {
        id: uuid(),
        createdAt: d().toISOString(),
        shouldExpireBy: d().add(10, "days").toISOString(),
        syntaxHighlightingLanguage: "cpp",
        name: 'std::string november = "hello world"',
        deletedAt: null,
      },
      success: true,
      message: "Leaderboard metadata loaded!",
    } satisfies ReturnType<typeof currentMetadata.res>);
  }),
  http.get(metadataById.url.toString(), () => {
    return HttpResponse.json({
      payload: {
        id: uuid(),
        createdAt: d().toISOString(),
        shouldExpireBy: d().add(10, "days").toISOString(),
        syntaxHighlightingLanguage: "cpp",
        name: 'std::string november = "hello world"',
        deletedAt: null,
      },
      success: true,
      message: "Leaderboard metadata loaded!",
    } satisfies ReturnType<typeof metadataById.res>);
  }),
];

export const failedLeaderboardHandlers = [
  http.get(currentMetadata.url.toString(), () => {
    return HttpResponse.json({
      success: false,
      message: "Leaderboard metadata failed to load",
    } satisfies ReturnType<typeof currentMetadata.res>);
  }),
  http.get(metadataById.url.toString(), () => {
    return HttpResponse.json({
      success: false,
      message: "Leaderboard metadata failed to load",
    } satisfies ReturnType<typeof metadataById.res>);
  }),
];

export const catastrophicLeaderboardHandlers = [
  http.get(currentMetadata.url.toString(), () => {
    return HttpResponse.error();
  }),
  http.get(metadataById.url.toString(), () => {
    return HttpResponse.error();
  }),
];

export const currentLeaderboardUsersHandler = http.get(
  currentLeaderboardUsers.url.toString(),
  ({ request }) => {
    const url = new URL(request.url);
    const query = url.searchParams.get("query") || "";

    const allUsers = [
      {
        index: 1,
        id: "user-1",
        discordId: "discord-1",
        discordName: "aphrodite",
        leetcodeUsername: "aphrodite",
        nickname: null,
        admin: false,
        profileUrl: null,
        tags: [],
        achievements: [],
        totalScore: 120,
      },
      {
        index: 2,
        id: "user-2",
        discordId: "discord-2",
        discordName: "poseidon",
        leetcodeUsername: "poseidon",
        nickname: null,
        admin: false,
        profileUrl: null,
        tags: [],
        achievements: [],
        totalScore: 110,
      },
      {
        index: 3,
        id: "user-3",
        discordId: "discord-3",
        discordName: "hermes",
        leetcodeUsername: "hermes",
        nickname: null,
        admin: false,
        profileUrl: null,
        tags: [],
        achievements: [],
        totalScore: 100,
      },
    ];

    const filteredUsers =
      query ?
        allUsers.filter(
          (user) =>
            user.discordName.toLowerCase().includes(query.toLowerCase()) ||
            user.leetcodeUsername.toLowerCase().includes(query.toLowerCase()),
        )
      : allUsers;

    return HttpResponse.json({
      success: true,
      message: "Leaderboard users loaded!",
      payload: {
        hasNextPage: false,
        pages: 1,
        pageSize: 20,
        items: filteredUsers,
      },
    } satisfies ReturnType<typeof currentLeaderboardUsers.res>);
  },
);

const mockLeaderboardUser = (
  index: number,
  name: string,
  nickname: string | null,
  tags: Tag[],
): Api<"IndexedUserWithScoreDto"> => ({
  index,
  id: `user-${index}`,
  discordId: `discord-${index}`,
  discordName: name,
  leetcodeUsername: name,
  nickname,
  admin: false,
  profileUrl: null,
  tags: tags.map((tag) => ({
    id: `user-${index}-${tag}`,
    createdAt: "2026-01-01T00:00:00Z",
    userId: `user-${index}`,
    tag,
  })),
  achievements: [],
  totalScore: 200 - index * 10,
});

/** Mixes nicknames and tags so verified-nickname and tag rendering paths are exercised. */
export const currentLeaderboardUsersWithTagsHandler = http.get(
  currentLeaderboardUsers.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      message: "Leaderboard users loaded!",
      payload: {
        hasNextPage: false,
        pages: 1,
        pageSize: 20,
        items: [
          mockLeaderboardUser(1, "aphrodite", "Aphro", [Tag.Hunter]),
          mockLeaderboardUser(2, "poseidon", null, [Tag.Nyu]),
          mockLeaderboardUser(3, "hermes", null, []),
          mockLeaderboardUser(4, "athena", "Athena", [Tag.Hunter]),
          mockLeaderboardUser(5, "ares", null, [Tag.Nyu]),
          mockLeaderboardUser(6, "apollo", "Apollo", []),
        ],
      },
    } satisfies UnknownApiResponse<Api<"PageIndexedUserWithScoreDto">>);
  },
);

export const currentLeaderboardUsersFailedHandler = http.get(
  currentLeaderboardUsers.url.toString(),
  () => {
    return HttpResponse.json({
      success: false,
      message: "No users to display",
    } satisfies ReturnType<typeof currentLeaderboardUsers.res>);
  },
);

export const currentLeaderboardUsersErrorHandler = http.get(
  currentLeaderboardUsers.url.toString(),
  () => {
    return HttpResponse.error();
  },
);
