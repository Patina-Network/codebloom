import DateRangePopover from "@/app/user/[userId]/submissions/_components/DateRangePopover/DateRangePopover";
import TopicFilterPopover from "@/app/user/[userId]/submissions/_components/TopicFilters/TopicFilterPopover";
import DateRangeIndicator from "@/app/user/[userId]/submissions/_components/UserSubmissions/DateRangeIndicator";
import UserSubmissionsSkeleton from "@/app/user/[userId]/submissions/_components/UserSubmissions/UserSubmissionsSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import FilterDropdown from "@/components/ui/dropdown/FilterDropdown";
import FilterDropdownItem from "@/components/ui/dropdown/FilterDropdownItem";
import {
  langNameKey,
  langNameToIcon,
} from "@/components/ui/langname-to-icon/LangNameToIcon";
import Paginator from "@/components/ui/table/Paginator";
import SearchBox from "@/components/ui/table/SearchBox";
import Toast from "@/components/ui/toast/Toast";
import { useUserSubmissionsQuery } from "@/lib/api/queries/user";
import { ApiUtils } from "@/lib/api/utils";
import { timeDiff } from "@/lib/timeDiff";
import { Badge, Box, Overlay, Text, Stack, Group, Flex } from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";
import { useMemo } from "react";
import { Link } from "react-router";

export default function UserSubmissions({ userId }: { userId: string }) {
  const {
    data,
    status,
    page,
    goBack,
    goForward,
    isPlaceholderData,
    goTo,
    searchQuery,
    setSearchQuery,
    pointFilter,
    togglePointFilter,
    topics,
    setTopics,
    clearTopics,
    startDate,
    endDate,
    setStartDate,
    setEndDate,
  } = useUserSubmissionsQuery({
    userId,
    tieToUrl: true,
    pageSize: 15,
  });

  const selectedTopicsSet = useMemo(() => new Set(topics), [topics]);

  const isMobile = useMediaQuery("(max-width: 768px)");

  const getDifficultyBadgeColor = (difficulty: string) => {
    if (difficulty === "Medium") {
      return "yellow";
    }
    if (difficulty === "Hard") {
      return "red";
    }
    return undefined;
  };

  const getAcceptedBadgeColor = (acceptanceRate: number) => {
    if (acceptanceRate >= 75) {
      return undefined;
    }
    if (acceptanceRate >= 50) {
      return "yellow";
    }
    return "red";
  };

  if (status === "pending") {
    return (
      <>
        <UserSubmissionsSkeleton />
      </>
    );
  }

  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong when trying to fetch user's submissions. Please try again later." />
    );
  }

  const pageData = data.payload;

  const filterDropdown = (
    <Box pos="relative" display="inline-block">
      <FilterDropdown buttonName="Filters">
        <TopicFilterPopover
          value={topics}
          selectedTopicsSet={selectedTopicsSet}
          onChange={setTopics}
          onClear={clearTopics}
        />
        <FilterDropdownItem
          value={pointFilter}
          toggle={togglePointFilter}
          switchMode
          name={
            <Flex gap="0.5rem" align="center">
              Points Received
            </Flex>
          }
        />
        <DateRangePopover
          startDate={startDate}
          endDate={endDate}
          onStartDateChange={setStartDate}
          onEndDateChange={setEndDate}
        />
      </FilterDropdown>
      <DateRangeIndicator startDate={startDate} endDate={endDate} />
    </Box>
  );

  return (
    <Box
      mt={10}
      pos="relative"
      px={isMobile ? "xs" : undefined}
      w={isMobile ? undefined : "100%"}
      maw={isMobile ? undefined : 925}
      p={isMobile ? undefined : "xs"}
    >
      {!isMobile && (
        <Box display="block" style={{ textAlign: "right" }}>
          {filterDropdown}
        </Box>
      )}
      <Group
        justify="space-between"
        align="flex-end"
        mb="sm"
        gap="sm"
        pt={isMobile ? undefined : 10}
      >
        <Box flex={1} miw={0}>
          <SearchBox
            pt={isMobile ? undefined : 0}
            query={searchQuery}
            onChange={(event) => setSearchQuery(event.currentTarget.value)}
            placeholder="Search for submission title"
            w={isMobile ? "100%" : undefined}
          />
        </Box>
        {isMobile && filterDropdown}
      </Group>
      <Box pos="relative">
        {isPlaceholderData && (
          <Overlay zIndex={1000} backgroundOpacity={0.35} blur={3} />
        )}
        <Stack gap="sm" my="sm" align={isMobile ? undefined : "center"}>
          {!pageData || pageData.items.length === 0 ?
            <CodebloomCard mih={80} w="100%" flex={isMobile ? 1 : undefined}>
              <Stack gap="xs" justify="center" align="center" h="100%">
                <Text fw={500} ta="center" c="dimmed">
                  Nothing found.
                </Text>
                <Text size="sm" ta="center" c="dimmed">
                  No submissions has been entered yet.
                </Text>
              </Stack>
            </CodebloomCard>
          : pageData.items.map((submission) => {
              const badgeDifficultyColor = getDifficultyBadgeColor(
                submission.questionDifficulty,
              );
              const badgeAcceptedColor = getAcceptedBadgeColor(
                submission.acceptanceRate * 100,
              );
              const LanguageIcon =
                langNameToIcon[submission.language as langNameKey] ||
                langNameToIcon["default"];
              return (
                <CodebloomCard
                  key={submission.id}
                  p={isMobile ? "sm" : "md"}
                  w="100%"
                  component={Link}
                  to={`/submission/${submission.id}`}
                  className="transition-all hover:brightness-110"
                >
                  <Stack gap="xs">
                    <Group justify="space-between" align="flex-start">
                      <Group gap="xs" flex={1} miw={0}>
                        <LanguageIcon
                          size={isMobile ? 20 : 22}
                          width={isMobile ? 20 : 22}
                          height={isMobile ? 20 : 22}
                        />
                        <Text
                          size={isMobile ? "sm" : undefined}
                          fw={500}
                          lh={1.3}
                          flex={1}
                        >
                          {submission.questionTitle}
                        </Text>
                      </Group>
                      <Text size="xs" c="dimmed">
                        {timeDiff(new Date(submission.submittedAt))}
                      </Text>
                    </Group>
                    <Group gap="xs" wrap="wrap">
                      <Badge size="sm" color={badgeDifficultyColor}>
                        {submission.questionDifficulty}
                      </Badge>
                      <Badge size="sm" color={badgeAcceptedColor}>
                        {Math.round(submission.acceptanceRate * 100)}%
                      </Badge>
                    </Group>
                    {submission.topics && submission.topics.length > 0 && (
                      <Group justify="space-between">
                        <Group gap="xs" wrap="wrap">
                          {submission.topics.map((topic) => (
                            <Badge
                              key={topic.id}
                              size="xs"
                              variant={isMobile ? "light" : "filled"}
                              color={isMobile ? "gray" : "gray.4"}
                            >
                              {
                                ApiUtils.getTopicEnumMetadataByTopicEnum(
                                  topic.topic,
                                ).name
                              }
                            </Badge>
                          ))}
                        </Group>
                        <Text size="sm" fw={500}>
                          {submission.pointsAwarded} Pts
                        </Text>
                      </Group>
                    )}
                    {!isMobile &&
                      !(submission.topics && submission.topics.length > 0) && (
                        <Group justify="space-between">
                          <Text size="xs" c="dimmed">
                            -
                          </Text>
                          <Text size="sm" fw={500}>
                            {submission.pointsAwarded} Pts
                          </Text>
                        </Group>
                      )}
                  </Stack>
                </CodebloomCard>
              );
            })
          }
        </Stack>
      </Box>
      <Paginator
        pages={pageData?.pages ?? 0}
        currentPage={page}
        hasNextPage={pageData?.hasNextPage ?? false}
        goBack={goBack}
        goForward={goForward}
        goTo={goTo}
      />
    </Box>
  );
}
