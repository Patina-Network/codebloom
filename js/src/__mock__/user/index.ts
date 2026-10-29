import { ApiURL } from "@/lib/api/common/apiURL";
import { QuestionDifficulty, Tag } from "@/lib/api/types/schema";
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

const userSubmissionsUrl = ApiURL.create("/api/user/{userId}/submissions", {
  method: "GET",
  params: {
    userId: MOCK_USERS[0].id,
  },
});

export const MOCK_USER_SUBMISSIONS = [
  {
    id: "submission-1",
    userId: "user-1",
    questionSlug: "two-sum",
    questionNumber: 1,
    questionTitle: "Two Sum",
    questionLink: "https://leetcode.com/problems/two-sum",
    questionDifficulty: QuestionDifficulty.Easy,
    description: "<p>Given an array of integers, return indices.</p>",
    acceptanceRate: 0.49,
    pointsAwarded: 10,
    code: "def twoSum(nums, target):\n    pass",
    runtime: "50ms",
    memory: "16MB",
    language: "python3",
    createdAt: "2024-01-01T00:00:00Z",
    submittedAt: "2024-01-01T00:00:00Z",
    submissionId: null,
    topics: [],
  },
  {
    id: "submission-2",
    userId: "user-1",
    questionSlug: "add-two-numbers",
    questionNumber: 2,
    questionTitle: "Add Two Numbers",
    questionLink: "https://leetcode.com/problems/add-two-numbers",
    questionDifficulty: QuestionDifficulty.Medium,
    description: "<p>Add two numbers represented by linked lists.</p>",
    acceptanceRate: 0.49,
    pointsAwarded: 20,
    code: "def addTwoNumbers(l1, l2):\n    pass",
    runtime: "50ms",
    memory: "16MB",
    language: "python3",
    createdAt: "2024-01-02T00:00:00Z",
    submittedAt: "2024-01-02T00:00:00Z",
    submissionId: null,
    topics: [],
  },
];

export const userSubmissionsHandler = http.get(
  userSubmissionsUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      message: "Submissions loaded!",
      payload: {
        hasNextPage: false,
        pages: 1,
        pageSize: 20,
        items: MOCK_USER_SUBMISSIONS,
      },
    } satisfies ReturnType<typeof userSubmissionsUrl.res>);
  },
);

export const userSubmissionsEmptyHandler = http.get(
  userSubmissionsUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: true,
      message: "Submissions loaded!",
      payload: {
        hasNextPage: false,
        pages: 1,
        pageSize: 20,
        items: [],
      },
    } satisfies ReturnType<typeof userSubmissionsUrl.res>);
  },
);

export const userSubmissionsFailedHandler = http.get(
  userSubmissionsUrl.url.toString(),
  () => {
    return HttpResponse.json({
      success: false,
      message: "Failed to load submissions",
    } satisfies ReturnType<typeof userSubmissionsUrl.res>);
  },
);

export const userSubmissionsErrorHandler = http.get(
  userSubmissionsUrl.url.toString(),
  () => {
    return HttpResponse.error();
  },
);
