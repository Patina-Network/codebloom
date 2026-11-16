export const FRONTEND_URL = "http://localhost:5173";
export const STAGING_LAS_HOST =
  "stg.codebloom-leetcode-auth-service.vpn.patinanetwork.org:50051";
export const TWO_SUM_SUBMISSION_ID = 2196928698;

// Keep in sync with db/e2e/R__E2E_Create_e2e_data.SQL (ids, sessions, verifyKey).
export const E2E_USERS = {
  school: {
    id: "e17e7905-0871-4e15-bf1c-e653d8ce6412",
    sessionToken: "5fc6518f5da44d82a89ee3548b65f52e",
  },
  leetcode: {
    id: "b96dd61c-0e6f-49c6-8753-7458519f4ae8",
    sessionToken: "cd0ebcd1274e4eafaa365331b6c26857",
  },
} as const;

export const LEETCODE_VERIFY_KEY = "iamagoodbot";

// Keep in sync with db/e2e/R__E2E_Create_e2e_data.SQL: the only users on the current leaderboard.
export const E2E_LEADERBOARD_USERS = {
  untaggedLeader: "e2e-leader",
  gwc: "e2e-gwc",
} as const;

/*
 * The LeetCode account used by e2e is the account logged into staging LAS. To onboard a new one:
 *
 * 1. Add LEETCODE_VERIFY_KEY to its About Me at https://leetcode.com/settings/profile/.
 * 2. Submit TWO_SUM_SOLUTION (Python3) to https://leetcode.com/problems/two-sum/.
 * 3. Set TWO_SUM_SUBMISSION_ID to that accepted submission's id.
 * 4. Refresh the snapshot: `pnpm exec playwright test --project=external --update-snapshots`.
 */
export const TWO_SUM_SOLUTION = `class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:
        D = {}

        for i in range(len(nums)):
            if not(nums[i] in D):
                D[target - nums[i]] = i
            else:
                return [D[nums[i]], i]`;
