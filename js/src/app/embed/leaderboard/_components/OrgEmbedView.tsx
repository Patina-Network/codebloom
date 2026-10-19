import OrgHeader from "@/app/embed/leaderboard/_components/OrgHeader";
import LeaderboardSkeleton from "@/app/leaderboard/_components/LeaderboardSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import LeaderboardCard from "@/components/ui/LeaderboardCard";
import CustomPagination from "@/components/ui/table/CustomPagination";
import SearchBox from "@/components/ui/table/SearchBox";
import Toast from "@/components/ui/toast/Toast";
import {
  useCurrentLeaderboardMetadataQuery,
  useCurrentLeaderboardUsersQuery,
} from "@/lib/api/queries/leaderboard";
import {
  formatLeaderboardDateRange,
  getUserProfileUrl,
} from "@/lib/helper/leaderboardDateRange";
import getOrdinal from "@/lib/helper/ordinal";
import { theme } from "@/lib/theme";
import {
  Box,
  Button,
  Center,
  Flex,
  Overlay,
  Stack,
  Text,
  Tooltip,
} from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { useEffect, useMemo } from "react";
import { FaArrowLeft, FaArrowRight, FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link, useSearchParams } from "react-router";

export default function OrgLeaderboardEmbed() {
  const [searchParams] = useSearchParams();

  const pageSizeParam = searchParams.get("pageSize");
  const pageSize = pageSizeParam ? Number(pageSizeParam) : undefined;

  const {
    status,
    isPlaceholderData,
    data,
    page,
    goTo,
    goBack,
    goForward,
    setSearchQuery,
    searchQuery,
    debouncedQuery,
    filters,
    onFilterReset,
  } = useCurrentLeaderboardUsersQuery({ pageSize });

  const metadataQuery = useCurrentLeaderboardMetadataQuery();

  const dateRange =
    metadataQuery.data?.success ?
      formatLeaderboardDateRange(metadataQuery.data.payload)
    : undefined;

  const activeFilter = useMemo(() => {
    const active = Object.typedEntries(filters).filter(
      ([, enabled]) => enabled,
    );

    return active.length === 1 ? active[0][0] : undefined;
  }, [filters]);

  useEffect(() => {
    const activeCount = Object.typedEntries(filters).filter(
      ([, enabled]) => enabled,
    ).length;

    if (activeCount > 1) {
      onFilterReset();
    }
  }, [filters, onFilterReset]);

  if (status === "pending") {
    return <LeaderboardSkeleton embedded />;
  }

  if (status === "error") {
    return <Toast message="Sorry, something went wrong." />;
  }

  if (!data.success) {
    return <p>Sorry, there are no users to display.</p>;
  }

  const pageData = data.payload;
  const [first, second, third] = pageData.items;

  return (
    <Flex
      p="md"
      direction="column"
      style={{
        borderRadius: "8px",
        border: "1px solid var(--mantine-color-dark-5)",
      }}
    >
      <OrgHeader orgTag={activeFilter} />
      <Center mb="xs">
        <Button
          component={Link}
          to="/"
          target="_blank"
          rel="noopener noreferrer"
        >
          Visit CodeBloom
        </Button>
      </Center>
      <Flex
        direction={{ base: "column", xs: "row" }}
        align={{ base: "center", xs: "flex-end" }}
        justify="center"
        gap="md"
        mb="md"
      >
        {page === 1 && second && !debouncedQuery && (
          <LeaderboardCard
            placeString={getOrdinal(second.index)}
            sizeOrder={2}
            discordName={second.discordName}
            startDate={dateRange?.startDate}
            endDate={dateRange?.endDate}
            leetcodeUsername={second.leetcodeUsername}
            totalScore={second.totalScore}
            nickname={second.nickname}
            width={"200px"}
            userId={second.id as string}
            isLoading={isPlaceholderData}
            embedded
          />
        )}
        {page === 1 && first && !debouncedQuery && (
          <LeaderboardCard
            placeString={getOrdinal(first.index)}
            startDate={dateRange?.startDate}
            endDate={dateRange?.endDate}
            sizeOrder={1}
            discordName={first.discordName}
            leetcodeUsername={first.leetcodeUsername}
            totalScore={first.totalScore}
            nickname={first.nickname}
            width={"200px"}
            userId={first.id as string}
            isLoading={isPlaceholderData}
            embedded
          />
        )}
        {page === 1 && third && !debouncedQuery && (
          <LeaderboardCard
            placeString={getOrdinal(third.index)}
            sizeOrder={3}
            discordName={third.discordName}
            leetcodeUsername={third.leetcodeUsername}
            totalScore={third.totalScore}
            nickname={third.nickname}
            startDate={dateRange?.startDate}
            endDate={dateRange?.endDate}
            width={"200px"}
            userId={third.id as string}
            isLoading={isPlaceholderData}
            embedded
          />
        )}
      </Flex>
      <SearchBox
        query={searchQuery}
        onChange={(event) => {
          setSearchQuery(event.currentTarget.value);
        }}
        placeholder={"Search for User"}
        smallPadding
      />
      <Box pos="relative" my="lg">
        {isPlaceholderData && (
          <Overlay zIndex={1000} backgroundOpacity={0.35} blur={4} />
        )}
        <Stack gap="md">
          {pageData.items.map((entry, index) => {
            if (page === 1 && !debouncedQuery && [0, 1, 2].includes(index))
              return null;
            return (
              <CodebloomCard
                key={entry.id}
                component={Link}
                to={getUserProfileUrl(entry.id, dateRange)}
                target="_blank"
                rel="noopener noreferrer"
                padding="lg"
                bg={theme.colors.dark[7]}
                styles={{
                  root: {
                    borderColor: theme.colors.dark[5],
                  },
                }}
                style={{
                  transition: "all 0.2s ease",
                  textDecoration: "none",
                }}
              >
                <Flex
                  direction="row"
                  justify="space-between"
                  align="center"
                  gap="md"
                >
                  <Flex align="center" gap="md">
                    <Text
                      size="xl"
                      fw={700}
                      c={theme.colors.patina[4]}
                      miw={50}
                    >
                      #{entry.index}
                    </Text>
                    <Flex direction="column" gap="xs">
                      <Stack gap="xs">
                        <Flex
                          direction={{ base: "column", xs: "row" }}
                          gap={{ base: "xs", xs: "md" }}
                          align={{ base: "flex-start", xs: "center" }}
                        >
                          <Flex align="center" gap={6}>
                            <FaDiscord size={16} />
                            <Text size="md" fw={600}>
                              {entry.discordName}
                            </Text>
                          </Flex>
                          <Flex align="center" gap={6}>
                            <SiLeetcode size={16} />
                            <Text size="md" fw={600}>
                              {entry.leetcodeUsername}
                            </Text>
                          </Flex>
                        </Flex>
                        {entry.nickname && (
                          <Flex align="center" gap={5}>
                            <Tooltip label="This user is a verified member of the Patina Discord server.">
                              <Flex align="center" gap={5}>
                                <IconCircleCheckFilled
                                  color={theme.colors.patina[4]}
                                  size={18}
                                />
                                <Text size="sm">{entry.nickname}</Text>
                              </Flex>
                            </Tooltip>
                          </Flex>
                        )}
                      </Stack>
                    </Flex>
                  </Flex>
                  <Text size="lg" fw={600} miw={100} ta="right">
                    {entry.totalScore} Pts
                  </Text>
                </Flex>
              </CodebloomCard>
            );
          })}
        </Stack>
      </Box>
      <Center my={"sm"}>
        <Flex direction={"row"} gap={"sm"}>
          <Button disabled={page === 1} onClick={goBack} size={"compact-sm"}>
            <FaArrowLeft />
          </Button>
          <CustomPagination
            goTo={goTo}
            pages={pageData.pages}
            currentPage={page}
          />
          <Button
            disabled={!pageData.hasNextPage || page >= pageData.pages}
            onClick={() => {
              if (pageData.hasNextPage || page >= pageData.pages) {
                goForward();
              }
            }}
            size={"compact-sm"}
          >
            <FaArrowRight />
          </Button>
        </Flex>
      </Center>
    </Flex>
  );
}
