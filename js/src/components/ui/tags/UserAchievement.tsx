import AchievementCarousel from "@/components/ui/carousel/ItemCarousel";
import { Api } from "@/lib/api/types";
import { AchievementPlaceEnum } from "@/lib/api/types/schema";
import { ApiUtils } from "@/lib/api/utils";
import { Image, Tooltip, Box, Text, Stack, Divider } from "@mantine/core";
import dayjs from "dayjs";
import { useMemo } from "react";
import { Link } from "react-router-dom";

type AchievementDto = Api<"AchievementDto">;
interface UserAchievementProps {
  achievements?: AchievementDto[];
  size: number;
  gap?: string | number;
  showHeader?: boolean;
}

const PLACE_CONFIG = {
  [AchievementPlaceEnum.ONE]: "🥇",
  [AchievementPlaceEnum.TWO]: "🥈",
  [AchievementPlaceEnum.THREE]: "🥉",
} as const;

export default function UserAchievement({
  achievements = [],
  size,
  gap = "xs",
  showHeader = true,
}: UserAchievementProps) {
  const achievementItems = useMemo(
    () =>
      achievements
        .filter((achievement) => achievement.active && achievement.place)
        .map((achievement) =>
          achievement.leaderboard ?
            <LeaderboardAchievementBadge
              key={achievement.id}
              achievement={achievement}
              size={size}
            />
          : <GlobalTrophyBadge
              key={achievement.id}
              achievement={achievement}
            />,
        ),
    [achievements, size],
  );

  if (!achievementItems.length) return <></>;

  return (
    <Stack gap="md" align="center">
      <Divider w="70%" />
      {showHeader && (
        <Text size="sm" fw={500} c="dimmed">
          Achievements
        </Text>
      )}
      <AchievementCarousel
        items={achievementItems}
        visibleCount={3}
        gap={gap}
        enableCarousel={true}
      />
    </Stack>
  );
}

interface AchievementBadgeProps {
  achievement: AchievementDto;
}

function GlobalTrophyBadge({ achievement }: AchievementBadgeProps) {
  const emoji = PLACE_CONFIG[achievement.place];

  return (
    <Tooltip
      label={`${achievement.title} - Received on ${dayjs(
        achievement.createdAt,
      ).format("MMM D, YYYY")}`}
      withArrow
      position="top"
    >
      <Box
        style={{ position: "relative", display: "inline-block" }}
        component={Link}
        to={`/leaderboard/${achievement.leaderboardId}`}
      >
        <Text fz={34} lh={1}>
          🏆
        </Text>
        <Text
          style={{
            position: "absolute",
            top: -13,
            right: -7,
            fontSize: 22,
            lineHeight: 1,
          }}
        >
          {emoji}
        </Text>
      </Box>
    </Tooltip>
  );
}

interface LeaderboardAchievementBadgeProps {
  achievement: AchievementDto;
  size: number;
}

function LeaderboardAchievementBadge({
  achievement,
  size,
}: LeaderboardAchievementBadgeProps) {
  if (!achievement.leaderboard) return <></>;

  const emoji = PLACE_CONFIG[achievement.place];

  const metadata = ApiUtils.getTagMetadataFromLeaderboard(
    achievement.leaderboard,
  );
  return (
    <Tooltip
      label={`${achievement.title} - Received on ${dayjs(
        achievement.createdAt,
      ).format("MMM D, YYYY")}`}
      withArrow
      position="top"
    >
      <Box
        style={{ position: "relative", display: "inline-block" }}
        component={Link}
        to={`/leaderboard/${achievement.leaderboardId}?${achievement.leaderboard.toLowerCase()}=true`}
      >
        <Image
          src={metadata.icon}
          alt={metadata.alt}
          style={{ height: size, width: "auto", cursor: "pointer" }}
        />
        <Text
          style={{
            position: "absolute",
            top: -10,
            right: -7,
            fontSize: 22,
            lineHeight: 1,
          }}
        >
          {emoji}
        </Text>
      </Box>
    </Tooltip>
  );
}
