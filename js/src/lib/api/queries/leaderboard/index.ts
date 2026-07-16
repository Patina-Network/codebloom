import { ApiURL } from "@/lib/api/common/apiURL";
import { ApiUtils } from "@/lib/api/utils";
import { sleep } from "@/lib/api/utils/lag";
import { useFilters } from "@/lib/hooks/useFilters";
import { usePagination } from "@/lib/hooks/usePagination";
import { useURLState } from "@/lib/hooks/useUrlState";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { useCallback } from "react";

/**
 * Fetch the users on the current leaderboard. This is a super query
 * that also exposes pagination.
 */
export const useCurrentLeaderboardUsersQuery = (
  {
    initialPage = 1,
    pageSize = 20,
    tieToUrl = true,
  }: {
    initialPage?: number;
    pageSize?: number;
    tieToUrl?: boolean;
  } = {
    initialPage: 1,
    pageSize: 20,
    tieToUrl: true,
  },
) => {
  const { filters, toggleFilter, clearFilters, isAnyFilterEnabled } =
    useFilters({
      onFilterChange: () => {
        goTo(1);
      },
    });
  const { page, goBack, goForward, goTo } = usePagination({
    initialPage: initialPage,
    tieToUrl: tieToUrl,
  });

  /**
   * We wrap _setSearchQuery with a setSearchQuery because we need to run a side effect anytime we update the query.
   */
  const [searchQuery, _setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );
  const [globalIndex, setGlobalIndex] = useURLState("globalIndex", false, {
    enabled: tieToUrl,
    debounce: 100,
  });

  /**
   * Abstracted function so that we can also reset the page back to 1 whenever we update the query.
   * TODO - Move these side effects within the useURLState function, which will make it easier to deal with.
   */
  const setSearchQuery = useCallback(
    (query: string) => {
      _setSearchQuery(query);
      goTo(1);
    },
    [_setSearchQuery, goTo],
  );

  const toggleGlobalIndex = useCallback(() => {
    setGlobalIndex((prev) => !prev);
    goTo(1);
  }, [setGlobalIndex, goTo]);

  const apiURL = ApiURL.create("/api/leaderboard/current/user/all", {
    method: "GET",
    queries: {
      page,
      pageSize,
      query: debouncedQuery,
      globalIndex,
      ...Object.typedFromEntries(
        Object.typedEntries(filters).map(([tagEnum, filterEnabled]) => {
          const metadata = ApiUtils.getMetadataByTagEnum(tagEnum);

          return [metadata.apiKey, filterEnabled];
        }),
      ),
    },
  });
  const { queryKey } = apiURL;

  const query = useQuery({
    queryKey,
    queryFn: () => fetchLeaderboardUsers(apiURL),
    placeholderData: keepPreviousData,
  });

  const onFilterReset = useCallback(() => {
    clearFilters();
    goTo(1);

    if (globalIndex) {
      toggleGlobalIndex();
    }
  }, [clearFilters, goTo, globalIndex, toggleGlobalIndex]);

  return {
    ...query,
    page,
    globalIndex,
    goBack,
    goForward,
    goTo,
    searchQuery,
    setSearchQuery,
    filters,
    toggleFilter,
    isAnyFilterEnabled,
    onFilterReset,
    debouncedQuery,
    pageSize,
    toggleGlobalIndex,
  };
};

/**
 * Fetch a list of all leaderboards. This is a super query that
 * also exposes pagination.
 */
export const useAllLeaderboardsMetadataQuery = ({
  initialPage = 1,
  pageSize = 20,
  tieToUrl = true,
}: {
  initialPage?: number;
  pageSize?: number;
  tieToUrl?: boolean;
}) => {
  const { page, goBack, goForward, goTo } = usePagination({
    initialPage: initialPage,
    tieToUrl: tieToUrl,
  });
  /**
   * We wrap _setSearchQuery with a setSearchQuery because we need to run a side effect anytime we update the query.
   */
  const [searchQuery, _setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );

  /**
   * Abstracted function so that we can also reset the page back to 1 whenever we update the query.
   * TODO - Move these side effects within the useURLState function, which will make it easier to deal with.
   */
  const setSearchQuery = useCallback(
    (query: string) => {
      _setSearchQuery(query);
      goTo(1);
    },
    [_setSearchQuery, goTo],
  );

  const apiURL = ApiURL.create("/api/leaderboard/all/metadata", {
    method: "GET",
    queries: {
      page,
      pageSize,
      query: debouncedQuery,
    },
  });
  const { queryKey } = apiURL;

  const query = useQuery({
    queryKey,
    queryFn: () => fetchAllLeaderboardsMetadata(apiURL),
    placeholderData: keepPreviousData,
  });

  return {
    ...query,
    page,
    goBack,
    goForward,
    goTo,
    searchQuery,
    setSearchQuery,
    debouncedQuery,
    pageSize,
  };
};

/**
 * Fetch the users from the given leaderboard ID. This is a super query
 * that also exposes pagination.
 */
export const useLeaderboardUsersByIdQuery = ({
  initialPage = 1,
  pageSize = 20,
  tieToUrl = true,
  leaderboardId,
}: {
  initialPage?: number;
  pageSize?: number;
  tieToUrl?: boolean;
  leaderboardId: string;
}) => {
  const { filters, toggleFilter, clearFilters, isAnyFilterEnabled } =
    useFilters({
      onFilterChange: () => {
        goTo(1);
      },
    });
  const { page, goBack, goForward, goTo } = usePagination({
    initialPage: initialPage,
    tieToUrl: tieToUrl,
  });

  /**
   * We wrap _setSearchQuery with a setSearchQuery because we need to run a side effect anytime we update the query.
   */
  const [searchQuery, _setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );
  const [globalIndex, setGlobalIndex] = useURLState("globalIndex", false, {
    enabled: tieToUrl,
    debounce: 100,
  });

  /**
   * Abstracted function so that we can also reset the page back to 1 whenever we update the query.
   * TODO - Move these side effects within the useURLState function, which will make it easier to deal with.
   */
  const setSearchQuery = useCallback(
    (query: string) => {
      _setSearchQuery(query);
      goTo(1);
    },
    [_setSearchQuery, goTo],
  );

  const toggleGlobalIndex = useCallback(() => {
    setGlobalIndex((prev) => !prev);
    goTo(1);
  }, [goTo, setGlobalIndex]);

  const onFilterReset = useCallback(() => {
    clearFilters();
    goTo(1);

    if (globalIndex) {
      toggleGlobalIndex();
    }
  }, [clearFilters, goTo, globalIndex, toggleGlobalIndex]);

  const apiURL = ApiURL.create("/api/leaderboard/{leaderboardId}/user/all", {
    method: "GET",
    params: {
      leaderboardId,
    },
    queries: {
      query: debouncedQuery,
      page,
      pageSize,
      globalIndex,
      ...Object.fromEntries(
        Object.typedEntries(filters).map(([tagEnum, filterEnabled]) => {
          const metadata = ApiUtils.getMetadataByTagEnum(tagEnum);

          return [metadata.apiKey, filterEnabled];
        }),
      ),
    },
  });
  const { queryKey } = apiURL;

  const query = useQuery({
    queryKey,
    queryFn: () => fetchLeaderboardUsersByLeaderboardId(apiURL),
    placeholderData: keepPreviousData,
  });

  return {
    ...query,
    page,
    filters,
    toggleFilter,
    globalIndex,
    goBack,
    goForward,
    goTo,
    searchQuery,
    setSearchQuery,
    debouncedQuery,
    pageSize,
    toggleGlobalIndex,
    isAnyFilterEnabled,
    onFilterReset,
  };
};

/**
 * Fetch the details about a leaderboard (excluding users)
 */
export const useCurrentLeaderboardMetadataQuery = () => {
  const apiURL = ApiURL.create("/api/leaderboard/current/metadata", {
    method: "GET",
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => getCurrentLeaderboardMetadata(apiURL),
  });
};

/**
 * Fetch the details about a leaderboard by ID (excluding users)
 */
export const useLeaderboardMetadataByIdQuery = (leaderboardId: string) => {
  const apiURL = ApiURL.create("/api/leaderboard/{leaderboardId}/metadata", {
    method: "GET",
    params: {
      leaderboardId,
    },
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => getLeaderboardMetadataById(apiURL),
  });
};

/**
 * Query the backend to fetch points. If it returns a successful search,
 * we invalidate all the queries to see if anything has changed.
 *
 * @param userId optional; can be used to optimize query invalidation
 */
export const useUsersTotalPoints = (userId?: string | undefined) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: updateTotalPoints,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ApiURL.prefix("/api/leetcode/submission"),
      });
      queryClient.invalidateQueries({
        queryKey: ApiURL.prefix("/api/leaderboard"),
      });
      queryClient.invalidateQueries({
        queryKey:
          userId ?
            ApiURL.prefix("/api/user/{userId}/submissions", userId)
          : ApiURL.prefix("/api/user"),
      });
      queryClient.invalidateQueries({
        queryKey: ApiURL.prefix("/api/leetcode/potd"),
      });
    },
  });
};

/**
 * To patch a rendering bug in the interim, prefetching is used to avoid any loading states.
 */
export const useFixMyPointsPrefetch = ({ userId }: { userId: string }) => {
  const queryClient = useQueryClient();
  const apiURL = ApiURL.create("/api/leaderboard/current/user/{userId}", {
    method: "GET",
    params: {
      userId,
    },
  });
  const { queryKey } = apiURL;

  queryClient.prefetchQuery({
    queryKey,
    queryFn: () => getMyRecentLeaderboardData(apiURL),
  });
  return;
};

/**
 * Fetch the specific submission data of the authenticated user.
 */
export const useMyRecentLeaderboardData = ({ userId }: { userId: string }) => {
  const apiURL = ApiURL.create("/api/leaderboard/current/user/{userId}", {
    method: "GET",
    params: {
      userId,
    },
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => getMyRecentLeaderboardData(apiURL),
  });
};

async function fetchLeaderboardUsers({
  url,
  method,
  res,
}: ApiURL<"/api/leaderboard/current/user/all", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(800);

  const json = res(await response.json());

  return json;
}

async function fetchLeaderboardUsersByLeaderboardId({
  url,
  res,
  method,
}: ApiURL<"/api/leaderboard/{leaderboardId}/user/all", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(800);

  const json = res(await response.json());

  return json;
}

async function getCurrentLeaderboardMetadata({
  url,
  method,
  res,
}: ApiURL<"/api/leaderboard/current/metadata", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}

async function getLeaderboardMetadataById({
  url,
  method,
  res,
}: ApiURL<"/api/leaderboard/{leaderboardId}/metadata", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}

async function updateTotalPoints() {
  const { url, method, res } = ApiURL.create("/api/leetcode/check", {
    method: "POST",
  });
  const response = await fetch(url, {
    method,
  });

  const json = res(await response.json());

  // returns wait-until rate limit number.
  // TODO - Make it a header
  if (response.status === 429) {
    return { ...json, message: Number(json.message) };
  }

  return json;
}

export async function getMyRecentLeaderboardData({
  url,
  method,
  res,
}: ApiURL<"/api/leaderboard/current/user/{userId}", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}

async function fetchAllLeaderboardsMetadata({
  url,
  method,
  res,
}: ApiURL<"/api/leaderboard/all/metadata", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}
