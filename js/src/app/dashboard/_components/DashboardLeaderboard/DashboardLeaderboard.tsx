import DashboardLeaderboardSkeleton from "@/app/dashboard/_components/DashboardLeaderboard/DashboardLeaderboardSkeleton";
import FilterTagsControl from "@/app/dashboard/_components/DashboardLeaderboard/FilterTagControls";
import MyCurrentPoints from "@/app/dashboard/_components/DashboardLeaderboard/MyCurrentPoints";
import { CurrentLeaderboardMetadata } from "@/app/leaderboard/_components/LeaderboardMetadata/LeaderboardMetadata";
import CodebloomCard from "@/components/ui/CodebloomCard";
import TagList from "@/components/ui/tags/TagList";
import {
  useCurrentLeaderboardMetadataQuery,
  useCurrentLeaderboardUsersQuery,
  useFixMyPointsPrefetch,
} from "@/lib/api/queries/leaderboard";
import { Api } from "@/lib/api/types";
import { ApiUtils } from "@/lib/api/utils";
import { tagFF } from "@/lib/ff/tag";
import {
  formatLeaderboardDateRange,
  getUserProfileUrl,
} from "@/lib/helper/leaderboardDateRange";
import { theme } from "@/lib/theme";
import {
  Button,
  Divider,
  Flex,
  Overlay,
  Text,
  Title,
  Tooltip,
} from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { useState } from "react";
import { FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export default function LeaderboardForDashboard({
  userId,
  userTags,
}: {
  userId: string;
  userTags: Api<"UserTag">[];
}) {
  // Hack to fix a race condition.
  useFixMyPointsPrefetch({ userId });

  const { data, status, filters, toggleFilter, isPlaceholderData } =
    useCurrentLeaderboardUsersQuery({
      pageSize: 5,
      tieToUrl: false,
    });

  const metadataQuery = useCurrentLeaderboardMetadataQuery();
  const dateRange =
    metadataQuery.data?.success ?
      formatLeaderboardDateRange(metadataQuery.data.payload)
    : undefined;
  const [selectedFilterKey, setSelectedFilterKey] = useState<
    string | undefined
  >();

  const onFilterSelected = (value: string | undefined) => {
    setSelectedFilterKey(value);
  };

  const renderCenteredStatusCard = (message: string) => (
    <CodebloomCard miw={"31vw"} mih={"63vh"}>
      <Flex
        direction={"row"}
        justify={"center"}
        align={"center"}
        w={"100%"}
        h={"100%"}
      >
        <Title order={6} ta={"center"}>
          {message}
        </Title>
      </Flex>
    </CodebloomCard>
  );

  if (status === "pending") {
    return <DashboardLeaderboardSkeleton />;
  }

  if (status === "error") {
    return renderCenteredStatusCard(
      "Sorry, something went wrong. Please try again later.",
    );
  }

  if (!data.success) {
    return renderCenteredStatusCard(data.message);
  }

  const leaderboardData = data.payload;

  if (leaderboardData.items.length == 0) {
    return renderCenteredStatusCard(
      "Oops! No users here yet. Crack your first problem and claim this space like a champ!",
    );
  }

  const inTop5 = !!leaderboardData.items.find((u) => u.id === userId);
  const filteredTags = ApiUtils.filterUnusedTags(userTags);

  return (
    <CodebloomCard miw={"31vw"} mih={"63vh"}>
      <Flex
        direction={{ base: "column", md: "row" }}
        justify={"space-between"}
        w={"100%"}
      >
        <CurrentLeaderboardMetadata syntaxStripSize={"md"} />
        <Button
          variant={"light"}
          component={Link}
          to={
            selectedFilterKey ?
              `/leaderboard?${selectedFilterKey}=true`
            : "/leaderboard"
          }
        >
          View all
        </Button>
      </Flex>
      <FilterTagsControl
        tags={filteredTags}
        filters={filters}
        toggleFilter={toggleFilter}
        onFilterSelected={onFilterSelected}
      />
      {!inTop5 && (
        <>
          <MyCurrentPoints
            userId={userId}
            startDate={dateRange?.startDate}
            endDate={dateRange?.endDate}
          />
          <Divider orientation={"horizontal"} />
        </>
      )}
      <Flex direction={"column"} gap={"md"} m={"xs"}>
        {isPlaceholderData && (
          <Overlay
            zIndex={1000}
            backgroundOpacity={0.55}
            blur={10}
            radius={"md"}
          />
        )}
        {leaderboardData.items.map((user, idx) => {
          const place = idx < 3 ? ((idx + 1) as 1 | 2 | 3) : undefined;
          const isMe = user.id === userId;

          const bgColor = (() => {
            if (isMe) return undefined;
            if (place === 1) return theme.colors.yellow[8];
            if (place === 2) return theme.colors.dark[2];
            if (place === 3) return theme.colors.orange[9];
            return undefined;
          })();

          const bgImage =
            isMe ? "linear-gradient(90deg, #283c86, #45a247)" : undefined;

          return (
            <CodebloomCard
              key={user.id}
              component={Link}
              to={getUserProfileUrl(user.id, dateRange)}
              padding="xs"
              style={{
                backgroundColor: bgColor,
                backgroundImage: bgImage,
                textDecoration: "none",
              }}
            >
              <Flex direction="row" justify="space-between" align="center">
                <Text fw={place != null ? 700 : 500} miw={32}>
                  {idx + 1}.
                </Text>
                <Flex
                  direction="column"
                  align="center"
                  gap={2}
                  style={{ flex: 1 }}
                >
                  {user.nickname && (
                    <Tooltip label="This user is a verified member of the Patina Discord server.">
                      <Flex align="center" gap={4}>
                        <IconCircleCheckFilled
                          color={theme.colors.patina[4]}
                          size={16}
                        />
                        <Text fw={600} size="sm">
                          {user.nickname}
                        </Text>
                        {tagFF && user.tags && user.tags.length > 0 && (
                          <TagList tags={user.tags} size={14} gap="xs" />
                        )}
                      </Flex>
                    </Tooltip>
                  )}
                  <Flex align="center" gap={4}>
                    <FaDiscord />
                    <Text size="sm">{user.discordName}</Text>
                    {!user.nickname &&
                      tagFF &&
                      user.tags &&
                      user.tags.length > 0 && (
                        <TagList tags={user.tags} size={14} gap="xs" />
                      )}
                  </Flex>
                  {user.leetcodeUsername && (
                    <Flex align="center" gap={4}>
                      <SiLeetcode />
                      <Text size="sm">{user.leetcodeUsername}</Text>
                    </Flex>
                  )}
                </Flex>
                <Text fw={place != null ? 700 : 500} miw={60} ta="right">
                  {user.totalScore}
                </Text>
              </Flex>
            </CodebloomCard>
          );
        })}
      </Flex>
    </CodebloomCard>
  );
}
