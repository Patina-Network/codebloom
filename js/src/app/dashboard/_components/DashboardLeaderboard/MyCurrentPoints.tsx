import TagList from "@/components/ui/tags/TagList";
import { useMyRecentLeaderboardData } from "@/lib/api/queries/leaderboard";
import { tagFF } from "@/lib/ff/tag";
import { getUserProfileUrl } from "@/lib/helper/leaderboardDateRange";
import { theme } from "@/lib/theme";
import { Flex, Skeleton, Text, Tooltip } from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export default function MyCurrentPoints({
  userId,
  startDate,
  endDate,
}: {
  userId: string;
  startDate?: string;
  endDate?: string;
}) {
  const { data, status } = useMyRecentLeaderboardData({ userId });

  if (status === "pending") {
    return (
      <Flex direction={"column"} gap={"md"} m={"xs"}>
        <Skeleton>
          <Flex
            direction={"row"}
            justify={"space-between"}
            style={{
              borderRadius: "4px",
              padding: "var(--mantine-spacing-xs)",
            }}
            p={"xs"}
          >
            <Text>Me.</Text>
            <Flex direction={"column"}>
              <Text ta="center">
                <FaDiscord
                  style={{
                    display: "inline",
                    marginLeft: "4px",
                    marginRight: "4px",
                  }}
                />
                tVal name
              </Text>
              <Text ta="center">
                <SiLeetcode
                  style={{
                    display: "inline",
                    marginLeft: "4px",
                    marginRight: "4px",
                  }}
                />
                tVal name
              </Text>
            </Flex>
            <Text>tVal score</Text>
          </Flex>
        </Skeleton>
      </Flex>
    );
  }

  if (status === "error") {
    return (
      <Text>Sorry, failed to fetch user data from current leaderboard.</Text>
    );
  }

  if (!data.success) {
    return <Text>{data.message}</Text>;
  }

  const userData = data.payload;

  const dateRange =
    startDate ? { startDate, endDate: endDate ?? undefined } : undefined;

  return (
    <Flex
      component={Link}
      direction={"column"}
      gap={"md"}
      m={"xs"}
      to={getUserProfileUrl(userId, dateRange)}
      className="group transition-all"
    >
      <Flex
        direction={"row"}
        justify={"space-between"}
        bg={"dark.4"}
        p={"xs"}
        style={{
          borderRadius: "4px",
        }}
        className="transition-all hover:!bg-blue-500"
      >
        <Text>Me</Text>
        <Flex direction={"column"}>
          {(userData.nickname ||
            (tagFF && userData.tags && userData.tags.length > 0)) && (
            <Flex align="center" justify="center" gap={5}>
              {userData.nickname && (
                <Tooltip
                  label={
                    "This user is a verified member of the Patina Discord server."
                  }
                >
                  <Text ta="center">
                    <IconCircleCheckFilled
                      style={{
                        display: "inline",
                      }}
                      color={theme.colors.patina[4]}
                      z={5000000}
                      size={20}
                    />{" "}
                    {userData.nickname}
                  </Text>
                </Tooltip>
              )}
              {userData.nickname &&
                tagFF &&
                userData.tags &&
                userData.tags.length > 0 && (
                  <TagList tags={userData.tags} size={16} gap="xs" />
                )}
            </Flex>
          )}
          <Flex align="center" justify="center" gap={5}>
            <Text ta="center">
              <FaDiscord
                style={{
                  display: "inline",
                  marginLeft: "4px",
                  marginRight: "4px",
                }}
              />
              {userData.discordName}
            </Text>
            {!userData.nickname &&
              tagFF &&
              userData.tags &&
              userData.tags.length > 0 && (
                <TagList tags={userData.tags} size={16} gap="xs" />
              )}
          </Flex>
          {userData.leetcodeUsername && (
            <Text ta="center">
              <SiLeetcode
                style={{
                  display: "inline",
                  marginLeft: "4px",
                  marginRight: "4px",
                }}
              />
              {userData.leetcodeUsername}
            </Text>
          )}
        </Flex>
        <Text>{userData.totalScore}</Text>
      </Flex>
    </Flex>
  );
}
