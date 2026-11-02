import UserTagsSkeleton from "@/app/user/[userId]/_components/UserProfile/UserTags/UserTagsSkeleton";
import TagList from "@/components/ui/tags/TagList";
import UserAchievement from "@/components/ui/tags/UserAchievement";
import Toast from "@/components/ui/toast/Toast";
import { useUserProfileQuery } from "@/lib/api/queries/user";
import { Group, Text, Stack } from "@mantine/core";
import { IconAlertTriangle } from "@tabler/icons-react";
import { useNavigate } from "react-router-dom";

export default function UserTags({ userId }: { userId: string }) {
  const navigate = useNavigate();
  const { data, status } = useUserProfileQuery({ userId });

  if (status === "pending") {
    return <UserTagsSkeleton />;
  }

  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong when trying to fetch the user's tags. Please try again later." />
    );
  }

  if (!data.success) {
    return (
      <Group gap="xs">
        <IconAlertTriangle color="red" />
        <Text fw={500} ff="Inter, sans-serif">
          Error Fetching Tags
        </Text>
      </Group>
    );
  }

  const { tags, achievements } = data.payload;

  return (
    <Stack gap="md" w="100%">
      <TagList
        tags={tags}
        size={40}
        gap="xs"
        expanded={true}
        onTagClick={(userTag) =>
          navigate(`/leaderboard?${userTag.tag.toLowerCase()}=true`)
        }
      />
      <UserAchievement achievements={achievements} size={40} gap="xs" />
    </Stack>
  );
}
