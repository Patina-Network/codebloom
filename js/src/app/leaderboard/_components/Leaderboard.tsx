import LeaderboardSkeleton from "@/app/leaderboard/_components/LeaderboardSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import FilterDropdown from "@/components/ui/dropdown/FilterDropdown";
import FilterDropdownItem from "@/components/ui/dropdown/FilterDropdownItem";
import LeaderboardCard from "@/components/ui/LeaderboardCard";
import CustomPagination from "@/components/ui/table/CustomPagination";
import SearchBox from "@/components/ui/table/SearchBox";
import TagList from "@/components/ui/tags/TagList";
import Toast from "@/components/ui/toast/Toast";
import {
  useCurrentLeaderboardMetadataQuery,
  useCurrentLeaderboardUsersQuery,
  useLeaderboardMetadataByIdQuery,
  useLeaderboardUsersByIdQuery,
} from "@/lib/api/queries/leaderboard";
import { ApiUtils } from "@/lib/api/utils";
import { schoolFF, tagFF } from "@/lib/ff";
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
  Divider,
  Flex,
  Image,
  Overlay,
  Stack,
  Text,
  Tooltip,
  Menu,
} from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { FaArrowLeft, FaArrowRight, FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export function CurrentLeaderboard() {
  const query = useCurrentLeaderboardUsersQuery();
  const metadataQuery = useCurrentLeaderboardMetadataQuery();

  const dateRange =
    metadataQuery.data?.success ?
      formatLeaderboardDateRange(metadataQuery.data.payload)
    : undefined;

  return (
    <LeaderboardIndex
      query={query}
      startDate={dateRange?.startDate}
      endDate={dateRange?.endDate}
    />
  );
}

export function LeaderboardById({ leaderboardId }: { leaderboardId: string }) {
  const query = useLeaderboardUsersByIdQuery({ leaderboardId });
  const metadataQuery = useLeaderboardMetadataByIdQuery(leaderboardId);

  const dateRange =
    metadataQuery.data?.success ?
      formatLeaderboardDateRange(metadataQuery.data.payload)
    : undefined;

  return (
    <LeaderboardIndex
      query={query}
      startDate={dateRange?.startDate}
      endDate={dateRange?.endDate}
    />
  );
}

function LeaderboardIndex({
  query,
  startDate,
  endDate,
}: {
  query: ReturnType<typeof useCurrentLeaderboardUsersQuery>;
  startDate?: string;
  endDate?: string;
}) {
  const {
    data,
    status,
    goTo,
    page,
    goBack,
    goForward,
    setSearchQuery,
    searchQuery,
    debouncedQuery,
    filters,
    toggleFilter,
    globalIndex,
    toggleGlobalIndex,
    isPlaceholderData,
    isAnyFilterEnabled,
    onFilterReset,
  } = query;
  const isMobile = useMediaQuery("(max-width: 48em)");

  if (status === "pending") {
    return <LeaderboardSkeleton />;
  }

  if (status === "error") {
    return <Toast message="Sorry, something went wrong." />;
  }

  if (!data.success) {
    return <p>Sorry, there are no users to display.</p>;
  }

  const pageData = data.payload;
  const [first, second, third] = pageData.items;
  const cardItems = pageData.items.filter((_, index) => {
    if (page === 1 && !debouncedQuery && [0, 1, 2].includes(index)) {
      return false;
    }
    return true;
  });

  const dateRange = startDate ? { startDate, endDate } : undefined;

  return (
    <>
      <Flex
        direction={{ base: "column", xs: "row" }}
        align={{ base: "center", xs: "flex-end" }}
        justify="center"
        gap="md"
        mb="xl"
      >
        {page === 1 &&
          !debouncedQuery &&
          (isMobile ? [first, second, third] : [second, first, third]).map(
            (entry) =>
              entry && (
                <LeaderboardCard
                  key={entry.id}
                  placeString={getOrdinal(entry.index)}
                  sizeOrder={entry.index as 1 | 2 | 3}
                  discordName={entry.discordName}
                  leetcodeUsername={entry.leetcodeUsername}
                  totalScore={entry.totalScore}
                  nickname={entry.nickname}
                  width={"300px"}
                  userId={entry.id}
                  tags={entry.tags}
                  isLoading={isPlaceholderData}
                  startDate={startDate}
                  endDate={endDate}
                />
              ),
          )}
      </Flex>
      <Flex
        justify="space-between"
        align="center"
        mb="md"
        direction={{ base: "column", sm: "row" }}
        gap="md"
      >
        <FilterDropdown
          style={{ marginLeft: "auto", display: "block" }}
          buttonName="Filters"
        >
          {schoolFF &&
            ApiUtils.getAllSupportedTagEnums().map((tagEnum) => (
              <FilterDropdownItem
                key={tagEnum}
                name={() => {
                  const metadata = ApiUtils.getMetadataByTagEnum(tagEnum);

                  return (
                    <Flex gap={"xs"} align={"center"}>
                      {metadata.shortName}
                      <Image
                        src={metadata.icon}
                        alt={metadata.alt}
                        style={{ height: "2em", width: "auto" }}
                      />
                    </Flex>
                  );
                }}
                value={filters[tagEnum]}
                toggle={() => toggleFilter(tagEnum)}
              />
            ))}
          <Divider my="sm" />
          <Menu.Sub
            offset={8}
            closeDelay={200}
            position={isMobile ? "bottom-start" : "right-start"}
          >
            <Menu.Sub.Target>
              <Menu.Sub.Item closeMenuOnClick={false}>Clubs</Menu.Sub.Item>
            </Menu.Sub.Target>
            <Menu.Sub.Dropdown>
              {Object.typedEntries(ApiUtils.getAllParentTags())
                .filter(([, childTags]) => childTags.length > 0)
                .map(([parentTag, childTags]) => (
                  <Box key={parentTag}>
                    <Menu.Label>
                      {ApiUtils.getMetadataByTagEnum(parentTag).name}
                    </Menu.Label>
                    {childTags.map((childTag) => (
                      <FilterDropdownItem
                        key={childTag}
                        name={() => {
                          const metadata =
                            ApiUtils.getMetadataByTagEnum(childTag);

                          return (
                            <Flex gap={"xs"} align={"center"}>
                              {metadata.shortName}
                              <Image
                                src={metadata.icon}
                                alt={metadata.alt}
                                style={{ height: "2em", width: "auto" }}
                              />
                            </Flex>
                          );
                        }}
                        value={filters[childTag as keyof typeof filters]}
                        toggle={() =>
                          toggleFilter(childTag as keyof typeof filters)
                        }
                      />
                    ))}
                  </Box>
                ))}
            </Menu.Sub.Dropdown>
          </Menu.Sub>
          <FilterDropdownItem
            value={globalIndex}
            toggle={toggleGlobalIndex}
            disabled={!isAnyFilterEnabled}
            switchMode
            name={
              <Flex gap="xs" align="center">
                Toggle Global Rank
              </Flex>
            }
          />
          <Menu.Item
            closeMenuOnClick={false}
            onClick={onFilterReset}
            disabled={!isAnyFilterEnabled && !globalIndex}
            color="red"
          >
            Clear Filters
          </Menu.Item>
        </FilterDropdown>
      </Flex>
      <SearchBox
        query={searchQuery}
        onChange={(event) => {
          setSearchQuery(event.currentTarget.value);
        }}
        placeholder={"Search for User"}
      />
      <Box pos="relative" my="lg">
        {isPlaceholderData && (
          <Overlay zIndex={1000} backgroundOpacity={0.35} blur={4} />
        )}
        <Stack gap="md">
          {cardItems.map((entry) => (
            <CodebloomCard
              key={entry.id}
              component={Link}
              to={getUserProfileUrl(entry.id, dateRange)}
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
                <Flex align="center" gap="xs">
                  <Text
                    size="xl"
                    fw={700}
                    c={theme.colors.patina[4]}
                    miw={{
                      sm: 0,
                      md: 50,
                    }}
                  >
                    {entry.index}
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
                      {(entry.nickname ||
                        (tagFF && entry.tags?.length > 0)) && (
                        <Flex align="center" gap={5}>
                          {entry.nickname && (
                            <Tooltip label="This user is a verified member of the Patina Discord server.">
                              <Flex align="center" gap={5}>
                                <IconCircleCheckFilled
                                  color={theme.colors.patina[4]}
                                  size={18}
                                />
                                <Text size="sm">{entry.nickname}</Text>
                              </Flex>
                            </Tooltip>
                          )}
                          {entry.nickname &&
                            tagFF &&
                            entry.tags?.length > 0 && (
                              <Divider orientation="vertical" h={20} />
                            )}
                          {tagFF && entry.tags?.length > 0 && (
                            <TagList tags={entry.tags} size={16} gap="xs" />
                          )}
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
          ))}
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
    </>
  );
}
