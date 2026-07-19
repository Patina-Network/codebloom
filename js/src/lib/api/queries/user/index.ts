import { ApiURL } from "@/lib/api/common/apiURL";
import { LeetcodeTopicEnum } from "@/lib/api/types/schema";
import { sleep } from "@/lib/api/utils/lag";
import { usePagination } from "@/lib/hooks/usePagination";
import useURLDateRange from "@/lib/hooks/useURLDateRange";
import { useURLState } from "@/lib/hooks/useUrlState";
import { notifications } from "@mantine/notifications";
import {
  keepPreviousData,
  useInfiniteQuery,
  useQuery,
} from "@tanstack/react-query";
import d from "dayjs";
import { useCallback, useEffect, useMemo } from "react";

/**
 * Fetch the metadata of the given user, such as Leetcode username, Discord name, and more.
 */
export const useUserProfileQuery = ({ userId }: { userId: string }) => {
  const apiURL = ApiURL.create("/api/user/{userId}/profile", {
    method: "GET",
    params: {
      userId,
    },
  });
  const { queryKey } = apiURL;

  return useQuery({
    queryKey,
    queryFn: () => fetchUserProfile(apiURL),
    placeholderData: keepPreviousData,
  });
};

/**
 * Fetch the user's submissions. This is a super query and as such,
 * also exports pagination and search capabilities.
 */
export const useUserSubmissionsQuery = ({
  userId,
  initialPage = 1,
  tieToUrl = false,
  pageSize = 20,
}: {
  userId: string;
  initialPage?: number;
  tieToUrl?: boolean;
  pageSize?: number;
}) => {
  const { page, goBack, goForward, goTo } = usePagination({
    initialPage: initialPage,
    tieToUrl: tieToUrl,
  });
  const [pointFilter, setPointFilter] = useURLState("pointFilter", false, {
    enabled: tieToUrl,
    debounce: 100,
  });
  const [searchQuery, setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );
  const {
    startDate,
    endDate,
    setStartDate,
    setEndDate,
    debouncedStartDate,
    debouncedEndDate,
  } = useURLDateRange(tieToUrl);

  const [_topics, _setTopics] = useURLState<string>("topics", "", {
    enabled: tieToUrl,
    debounce: 100,
  });

  useEffect(() => {
    goTo(1);
  }, [searchQuery, goTo]);

  const togglePointFilter = useCallback(() => {
    setPointFilter((prev) => !prev);
    goTo(1);
  }, [goTo, setPointFilter]);

  const topics = useMemo(
    () => _topics.split(",").filter(Boolean) as LeetcodeTopicEnum[],
    [_topics],
  );

  const setTopics = useCallback(
    (topics: LeetcodeTopicEnum[]) => {
      _setTopics(topics.join(","));
      goTo(1);
    },
    [goTo, _setTopics],
  );

  const clearTopics = useCallback(() => {
    _setTopics("");
    goTo(1);
  }, [goTo, _setTopics]);

  // if seconds are not included, use start of day and end of day
  const processedStartDate = useMemo(
    () =>
      debouncedStartDate ?
        debouncedStartDate.includes(":") ?
          d.utc(debouncedStartDate).toISOString()
        : d(debouncedStartDate).startOf("day").utc().toISOString()
      : undefined,
    [debouncedStartDate],
  );
  const processedEndDate = useMemo(
    () =>
      debouncedEndDate ?
        debouncedEndDate.includes(":") ?
          d.utc(debouncedEndDate).toISOString()
        : d(debouncedEndDate).endOf("day").utc().toISOString()
      : undefined,
    [debouncedEndDate],
  );

  const apiURL = ApiURL.create("/api/user/{userId}/submissions", {
    method: "GET",
    params: {
      userId,
    },
    queries: {
      page,
      query: debouncedQuery,
      pageSize,
      pointFilter,
      topics,
      startDate: processedStartDate,
      endDate: processedEndDate,
    },
  });

  const { queryKey } = apiURL;

  const query = useQuery({
    queryKey,
    queryFn: () => fetchUserSubmissions(apiURL),
    placeholderData: keepPreviousData,
  });

  useEffect(() => {
    if (query.status === "success" && !query.data.success) {
      notifications.show({
        message: query.data.message,
        color: "red",
      });
    }
  }, [query.data?.message, query.data?.success, query.status]);

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
    pointFilter,
    togglePointFilter,
    topics,
    setTopics,
    clearTopics,
    startDate,
    endDate,
    setStartDate,
    setEndDate,
  };
};

export const useGetAllUsersQuery = (
  {
    tieToUrl = false,
  }: {
    tieToUrl?: boolean;
  } = {
    tieToUrl: false,
  },
) => {
  const initialPage = 1;
  const pageSize = 5;

  const { page, goBack, goForward, goTo } = usePagination({
    initialPage: initialPage,
    tieToUrl: tieToUrl,
  });
  const [searchQuery, setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );

  useEffect(() => {
    goTo(1);
  }, [searchQuery, goTo]);

  const apiURL = ApiURL.create("/api/user/all", {
    method: "GET",
    queries: {
      page,
      query: debouncedQuery,
      pageSize,
    },
  });
  const { queryKey } = apiURL;

  const query = useQuery({
    queryKey,
    queryFn: () => fetchAllUsers(apiURL),
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
 * Fetch all users with infinite scroll support.
 * Uses useInfiniteQuery for pagination that loads more data as user scrolls.
 */
export const useGetAllUsersInfiniteQuery = ({
  tieToUrl = false,
  pageSize = 10,
}: {
  tieToUrl?: boolean;
  pageSize?: number;
} = {}) => {
  const [searchQuery, setSearchQuery, debouncedQuery] = useURLState(
    "query",
    "",
    {
      enabled: tieToUrl,
      debounce: 500,
    },
  );

  const baseApiURL = ApiURL.create("/api/user/all", {
    method: "GET",
    queries: {
      query: debouncedQuery,
      pageSize,
    },
  });

  const query = useInfiniteQuery({
    queryKey: baseApiURL.queryKey,
    queryFn: async ({ pageParam = 1 }) => {
      const apiURL = ApiURL.create("/api/user/all", {
        method: "GET",
        queries: {
          page: pageParam,
          query: debouncedQuery,
          pageSize,
        },
      });
      return fetchAllUsers(apiURL);
    },
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      if (!lastPage.success) return undefined;
      if (!lastPage.payload.hasNextPage) return undefined;
      return allPages.length + 1;
    },
    enabled: debouncedQuery.length > 0,
  });

  const allUsers = useMemo(() => {
    if (!query.data) return [];
    return query.data.pages.flatMap((page) =>
      page.success ? page.payload.items : [],
    );
  }, [query.data]);

  return {
    ...query,
    searchQuery,
    setSearchQuery,
    debouncedQuery,
    allUsers,
    pageSize,
  };
};

async function fetchUserProfile({
  url,
  method,
  res,
}: ApiURL<"/api/user/{userId}/profile", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}

async function fetchUserSubmissions({
  url,
  method,
  res,
}: ApiURL<"/api/user/{userId}/submissions", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(500);

  const json = res(await response.json());
  return json;
}

async function fetchAllUsers({
  url,
  method,
  res,
}: ApiURL<"/api/user/all", "get">) {
  const response = await fetch(url, {
    method,
  });

  await sleep(650);

  const json = res(await response.json());

  return json;
}
