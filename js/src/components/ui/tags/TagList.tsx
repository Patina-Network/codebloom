import AchievementCarousel from "@/components/ui/carousel/ItemCarousel";
import { Api } from "@/lib/api/types";
import { ApiUtils } from "@/lib/api/utils";
import { tagFF } from "@/lib/ff";
import {
  Image,
  Tooltip,
  Stack,
  Divider,
  Text,
  UnstyledButton,
} from "@mantine/core";
import { useMemo } from "react";

interface TagListProps {
  tags: Api<"UserTag">[];
  size?: number;
  gap?: string | number;
  expanded?: boolean;
  onTagClick?: (userTag: Api<"UserTag">) => void;
}

interface TagBadgeProps {
  userTag: Api<"UserTag">;
  size: number;
  onTagClick?: (userTag: Api<"UserTag">) => void;
}

export default function TagList({
  tags,
  size = 20,
  gap = "xs",
  expanded = false,
  onTagClick,
}: Readonly<TagListProps>) {
  const filteredTags = ApiUtils.filterUnusedTags(tags);
  const tagItems = useMemo(
    () =>
      filteredTags.map((userTag) => (
        <TagBadge
          key={userTag.id}
          userTag={userTag}
          size={size}
          onTagClick={onTagClick}
        />
      )),
    [filteredTags, size, onTagClick],
  );

  if (!tagFF) return null;

  const hasTags = tagItems.length > 0;

  if (!hasTags) return null;

  return (
    <Stack gap="sm" align="center">
      {expanded && <Divider w="70%" />}
      <Stack gap="sm" align="center">
        {expanded && (
          <Text size="sm" fw={500} c="dimmed">
            Leaderboard
          </Text>
        )}
        <AchievementCarousel
          items={tagItems}
          visibleCount={3}
          gap={gap}
          enableCarousel={expanded}
        />
      </Stack>
    </Stack>
  );
}

function TagBadge({ userTag, size, onTagClick }: Readonly<TagBadgeProps>) {
  const metadata = ApiUtils.getMetadataByTagEnum(userTag.tag);
  const image = (
    <Image
      src={metadata.icon}
      alt={metadata.alt}
      style={{ height: size, width: "auto" }}
    />
  );

  return (
    <Tooltip label={metadata.name} color="dark.4" withArrow position="top">
      {onTagClick ?
        <UnstyledButton
          type="button"
          aria-label={metadata.name}
          onClick={() => onTagClick(userTag)}
        >
          {image}
        </UnstyledButton>
      : image}
    </Tooltip>
  );
}
