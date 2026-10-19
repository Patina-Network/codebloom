import CodebloomCard from "@/components/ui/CodebloomCard";
import TagList from "@/components/ui/tags/TagList";
import { Api } from "@/lib/api/types";
import { tagFF } from "@/lib/ff";
import { getUserProfileUrl } from "@/lib/helper/leaderboardDateRange";
import { OrdinalString } from "@/lib/helper/ordinal";
import { theme } from "@/lib/theme";
import { Text, Tooltip, Flex, LoadingOverlay, Stack } from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { CSSProperties } from "react";
import { FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export default function LeaderboardCard({
  placeString,
  discordName,
  leetcodeUsername,
  sizeOrder,
  totalScore,
  width,
  userId,
  nickname,
  tags,
  isLoading = false,
  embedded = false,
  startDate,
  endDate,
}: {
  placeString: OrdinalString;
  sizeOrder: 1 | 2 | 3;
  discordName: string;
  leetcodeUsername: string | null;
  totalScore: number;
  width: CSSProperties["width"];
  userId: string;
  nickname: string | null;
  tags?: Api<"UserTag">[];
  isLoading?: boolean;
  embedded?: boolean;
  startDate?: string;
  endDate?: string;
}) {
  const isTopThree = sizeOrder <= 3;
  const borderColor = (() => {
    if (placeString === "1st") return theme.colors.yellow[4];
    if (placeString === "2nd") return theme.colors.gray[4];
    if (placeString === "3rd") return theme.colors.orange[9];
    return undefined;
  })();

  const boxShadow = (() => {
    if (placeString === "1st")
      return "0 0 15px rgba(250, 204, 21, 0.4), 0 0 30px rgba(250, 204, 21, 0.2), 0 0 45px rgba(34, 197, 94, 0.1)";
    if (placeString === "2nd")
      return "0 0 12px rgba(203, 213, 225, 0.35), 0 0 25px rgba(148, 163, 184, 0.2)";
    if (placeString === "3rd")
      return "0 0 12px rgba(217, 119, 6, 0.25), 0 0 25px rgba(217, 119, 6, 0.12)";
    return undefined;
  })();

  const height = (() => {
    switch (sizeOrder) {
      case 1:
        return embedded ? "150px" : "225px";
      case 2:
        return embedded ? "140px" : "215px";
      case 3:
        return embedded ? "135px" : "210px";
    }
  })();
  const displayTags = tags?.slice(0, 3);

  const dateRange =
    startDate ? { startDate, endDate: endDate ?? undefined } : undefined;

  return (
    <CodebloomCard
      padding={embedded ? "xs" : "md"}
      style={{
        height,
        width,
        boxShadow,
        borderColor,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "space-between",
        position: "relative",
      }}
      component={Link}
      to={getUserProfileUrl(userId, dateRange)}
      reloadDocument={embedded}
      target={embedded ? "_blank" : undefined}
      rel={embedded ? "noopener noreferrer" : undefined}
    >
      <LoadingOverlay visible={isLoading} />
      <Text
        ta="center"
        size={embedded ? "lg" : "xl"}
        fw={isTopThree ? 800 : 500}
      >
        {placeString}
      </Text>
      <Stack gap={embedded ? 0 : 4} align="center" my="auto">
        {nickname && (
          <Tooltip
            label={
              "This user is a verified member of the Patina Discord server."
            }
          >
            <Text
              ta="center"
              fw={isTopThree ? 700 : 600}
              truncate
              style={{
                fontSize:
                  embedded ?
                    `clamp(.75rem, ${100 / (nickname.length + 5)}vw, 0.25rem)`
                  : `clamp(1rem, ${100 / (nickname.length + 5)}vw, 1.25rem)`,
              }}
            >
              <IconCircleCheckFilled
                style={{ display: "inline" }}
                color={theme.colors.patina[4]}
                size={embedded ? 13 : 24}
              />{" "}
              {nickname}
            </Text>
          </Tooltip>
        )}
        <Flex align="center" justify="center" gap="xs">
          <Text
            ta="center"
            fw={isTopThree ? 600 : 500}
            truncate
            style={{
              fontSize:
                embedded ?
                  `clamp(0.9rem, ${100 / (discordName.length + 5)}vw, 0.5rem)`
                : `clamp(0.9rem, ${100 / (discordName.length + 5)}vw, 1.1rem)`,
            }}
          >
            <FaDiscord style={{ display: "inline" }} /> {discordName}
          </Text>
          {tagFF && displayTags && displayTags.length > 0 && (
            <TagList tags={displayTags} size={14} gap="xs" />
          )}
        </Flex>
        <Text
          ta="center"
          fw={isTopThree ? 500 : 400}
          style={{ fontSize: "0.95rem" }}
        >
          <SiLeetcode style={{ display: "inline" }} /> {leetcodeUsername}
        </Text>
      </Stack>
      <Text
        ta="center"
        fw={isTopThree ? 700 : 500}
        size={embedded ? "md" : "lg"}
      >
        {totalScore} Points
      </Text>
    </CodebloomCard>
  );
}
