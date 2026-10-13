import { ApiURL } from "@/lib/api/common/apiURL";
import { Page } from "@/lib/api/common/page";
import { Api } from "@/lib/api/types";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export const useToggleAdminMutation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    // Check the mutation function regarding `metadata`.
    mutationFn: toggleUserAdmin,
    onMutate: async (newData) => {
      const { userId, toggleTo, metadata } = newData;
      // res is used to get response type out
      const { res: _res, queryKey } = ApiURL.create("/api/user/all", {
        method: "GET",
        queries: {
          page: metadata.page,
          query: metadata.debouncedQuery,
          pageSize: 5,
        },
      });
      type AllUsersResponse = ReturnType<typeof _res>;

      queryClient.cancelQueries({
        queryKey,
      });

      const previousApiResponse =
        queryClient.getQueryData<AllUsersResponse>(queryKey);

      // Impossible, just handle it to make TS happy and narrow the type.
      if (!previousApiResponse || !previousApiResponse.success) {
        return;
      }

      // Find the user and insert the new admin status to it's object.
      const newUsers: Api<"UserDto">[] = previousApiResponse.payload.items.map(
        (user) => (user.id === userId ? { ...user, admin: toggleTo } : user),
      );

      // Replace the data (users array) with the newUsers, and keep the rest of the Page items the same.
      //
      // Remember, ApiResponse<Page<T>> has a type that looks like this:
      //
      // {
      //  success: boolean;
      //  message: string;
      //  // This `data` is `Page`.
      //  data: {
      //    page: number;
      //    pageSize: number;
      //    ...
      //    // This is the actual array of users.
      //    data: T
      //  };
      // }
      const newPage: Page<Api<"UserDto">[]> = {
        ...previousApiResponse.payload,
        items: newUsers,
      };

      // Insert this new Page type, and keep the success and message from the previous API response.
      queryClient.setQueryData<AllUsersResponse>(queryKey, {
        ...previousApiResponse,
        payload: newPage,
      });

      // This is context that is passed on, so we can hold onto it and possibly put it back if needed.
      return { previousApiResponse };
    },
    onError: (_, newData, ctx) => {
      const { metadata } = newData;
      // res is used to get response type out
      const { res: _res, queryKey } = ApiURL.create("/api/user/all", {
        method: "GET",
        queries: {
          page: metadata.page,
          query: metadata.debouncedQuery,
          pageSize: 5,
        },
      });
      type AllUsersResponse = ReturnType<typeof _res>;

      // cant happen
      if (!ctx?.previousApiResponse) {
        return;
      }

      queryClient.setQueryData<AllUsersResponse>(
        queryKey,
        // Shallow copy because sometimes there is a bug with the object passed in by reference.
        { ...ctx?.previousApiResponse },
      );
    },
    onSettled: () => {
      queryClient.invalidateQueries({
        queryKey: ApiURL.prefix("/api/user/all"),
      });
    },
  });
};

async function toggleUserAdmin({
  userId,
  toggleTo,
}: {
  userId: string;
  toggleTo: boolean;
  /**
   * This metadata property isn't used in the fetcher function at all.
   * Instead, it is used in order to pinpoint exactly what data we will be mutating
   * in the optimistic update.
   */
  metadata: {
    page: number;
    debouncedQuery: string;
  };
}) {
  // If you want to test the optimistic updates, uncomment the lines below.
  //
  // await new Promise((res, rej) => {
  //   setTimeout(() => {
  //     res(-1);
  //   }, 10000);
  // });
  // throw new Error("hi");

  const { url, method, req, res } = ApiURL.create(
    "/api/admin/user/admin/toggle",
    {
      method: "POST",
    },
  );

  const response = await fetch(url, {
    method,
    body: req({
      id: userId,
      toggleTo,
    }),
  });

  const json = res(await response.json());

  return json;
}

export async function createLeaderboard(leaderboard: { name: string }) {
  const { url, method, req, res } = ApiURL.create(
    "/api/admin/leaderboard/create",
    {
      method: "POST",
    },
  );
  const response = await fetch(url, {
    method,
    body: req(leaderboard),
  });

  const json = res(await response.json());
  return json;
}

export const useCreateLeaderboardMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createLeaderboard,
    onSuccess: async (data) => {
      if (data.success) {
        queryClient.invalidateQueries({
          queryKey: ApiURL.prefix("/api/leaderboard"),
        });
      }
    },
  });
};

/**
 * Create a brand new announcement, invalidating the previous announcement.
 */
export const useCreateAnnouncementLeaderboardMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAnnouncement,
    onSuccess: async (data) => {
      if (data.success) {
        queryClient.invalidateQueries({
          queryKey: ApiURL.prefix("/api/announcement"),
        });
      }
    },
  });
};

async function createAnnouncement({
  message,
  expiresAt,
  showTimer,
}: {
  message: string;
  expiresAt: string;
  showTimer: boolean;
}) {
  const { url, method, req, res } = ApiURL.create(
    "/api/admin/announcement/create",
    {
      method: "POST",
    },
  );
  const response = await fetch(url, {
    method,
    body: req({
      message,
      expiresAt,
      showTimer,
    }),
  });

  return res(await response.json());
}

/**
 * Disable the current announcement.
 */
export const useDeleteAnnouncementMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteAnnouncement,
    onSuccess: async (data) => {
      if (data.success) {
        queryClient.invalidateQueries({
          queryKey: ApiURL.prefix("/api/announcement"),
        });
      }
    },
  });
};

async function deleteAnnouncement({ id }: { id: string }) {
  const { url, method, req, res } = ApiURL.create(
    "/api/admin/announcement/disable",
    {
      method: "POST",
    },
  );
  const response = await fetch(url, {
    method,
    body: req({
      id,
    }),
  });

  return res(await response.json());
}

/**
 * Trigger a refresh of the LeetCode auth token.
 */
export const useRefreshLeetcodeTokenMutation = () => {
  return useMutation({
    mutationFn: refreshLeetcodeToken,
  });
};

async function refreshLeetcodeToken() {
  const { url, method, res } = ApiURL.create("/api/admin/leetcode/token", {
    method: "POST",
  });
  const response = await fetch(url, {
    method,
  });

  return res(await response.json());
}

/**
 * Edit current leaderboard
 */

export const useEditLeaderboardMutation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: editLeaderboard,
    onSuccess: async (data) => {
      if (data.success) {
        queryClient.invalidateQueries({
          queryKey: ApiURL.prefix("/api/leaderboard"),
        });
      }
    },
  });
};

async function editLeaderboard(leaderboard: {
  name: string;
  shouldExpireBy: string | null;
  syntaxHighlightingLanguage: string | null;
}) {
  const { url, method, req, res } = ApiURL.create(
    "/api/admin/leaderboard/current",
    {
      method: "PUT",
    },
  );
  const response = await fetch(url, {
    method,
    body: req(leaderboard),
  });

  return res(await response.json());
}
