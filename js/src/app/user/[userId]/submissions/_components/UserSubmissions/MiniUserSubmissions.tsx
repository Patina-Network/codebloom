import MiniUserSubmissionsSkeleton from "@/app/user/[userId]/submissions/_components/UserSubmissions/MiniUserSubmissionsSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import {
  langNameKey,
  langNameToIcon,
} from "@/components/ui/langname-to-icon/LangNameToIcon";
import Toast from "@/components/ui/toast/Toast";
import { useUserSubmissionsQuery } from "@/lib/api/queries/user";
import { ApiUtils } from "@/lib/api/utils";
import { timeDiff } from "@/lib/timeDiff";
import { Badge, Box, Overlay, Text, Group, Stack } from "@mantine/core";
import { Link } from "react-router";

export default function MiniUserSubmissions({ userId }: { userId: string }) {
  const { data, status, isPlaceholderData } = useUserSubmissionsQuery({
    userId,
    tieToUrl: true,
    pageSize: 5,
  });

  if (status === "pending") {
    return (
      <>
        <MiniUserSubmissionsSkeleton />
      </>
    );
  }

  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong when trying to fetch user's submissions. Please try again later." />
    );
  }

  if (!data.success) {
    return <>{data.message}</>;
  }

  const pageData = data.payload;

  return (
    <Box pos="relative" px="xs">
      {isPlaceholderData && <Overlay zIndex={1000} opacity={0.35} blur={4} />}
      <Stack gap="sm" my="sm">
        {pageData.items.length === 0 && (
          <>
            <CodebloomCard mih={80} w="100%" flex={1}>
              <Stack gap="sm" justify="center" align="center" h="100%">
                <Text fw={500} ta="center" c="dimmed">
                  Nothing found.
                </Text>
                <Text size="sm" ta="center" c="dimmed">
                  No submissions has been entered yet.
                </Text>
              </Stack>
            </CodebloomCard>
          </>
        )}
        {pageData.items.map((submission) => {
          const badgeDifficultyColor = (() => {
            if (submission.questionDifficulty === "Easy") {
              return undefined;
            }
            if (submission.questionDifficulty === "Medium") {
              return "yellow";
            }
            if (submission.questionDifficulty === "Hard") {
              return "red";
            }
            return undefined;
          })();
          const badgeAcceptedColor = (() => {
            const acceptanceRate = submission.acceptanceRate * 100;
            if (acceptanceRate >= 75) {
              return undefined;
            }
            if (acceptanceRate >= 50) {
              return "yellow";
            }
            if (acceptanceRate >= 0) {
              return "red";
            }
            return undefined;
          })();
          const LanguageIcon =
            langNameToIcon[submission.language as langNameKey] ||
            langNameToIcon["default"];
          return (
            <CodebloomCard
              key={submission.id}
              w="100%"
              maw={950}
              component={Link}
              to={`/submission/${submission.id}`}
              className="transition-all hover:brightness-110"
            >
              <Stack gap="xs">
                <Group justify="space-between" align="flex-start">
                  <Group gap="xs" flex={1} miw={0}>
                    <LanguageIcon size={20} width={20} height={20} />
                    <Text size="sm" fw={500} flex={1} lh={1.3}>
                      {submission.questionTitle}
                    </Text>
                  </Group>
                  <Text size="xs" c="dimmed">
                    {timeDiff(new Date(submission.submittedAt))}
                  </Text>
                </Group>
                <Group justify="space-between" wrap="wrap" gap="xs">
                  <Group gap="xs">
                    <Badge size="sm" color={badgeDifficultyColor}>
                      {submission.questionDifficulty}
                    </Badge>
                    <Badge size="sm" color={badgeAcceptedColor}>
                      {Math.round(submission.acceptanceRate * 100)}%
                    </Badge>
                  </Group>
                </Group>
                {submission.topics && submission.topics.length > 0 && (
                  <Group justify="space-between">
                    <Group gap="xs" wrap="wrap">
                      {submission.topics.map((topic) => (
                        <Badge
                          key={topic.id}
                          size="xs"
                          variant={"filled"}
                          color={"gray.4"}
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
                      {submission.pointsAwarded} pts
                    </Text>
                  </Group>
                )}
              </Stack>
            </CodebloomCard>
          );
        })}
      </Stack>
    </Box>
  );
}
