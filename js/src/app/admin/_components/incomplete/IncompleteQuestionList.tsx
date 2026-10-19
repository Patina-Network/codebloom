import IncompleteQuestionListSkeleton from "@/app/admin/_components/incomplete/IncompleteQuestionListSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import {
  langNameToIcon,
  langNameKey,
} from "@/components/ui/langname-to-icon/LangNameToIcon";
import { useIncompleteQuestionQuery } from "@/lib/api/queries/incomplete";
import { ApiUtils } from "@/lib/api/utils";
import { timeDiff } from "@/lib/timeDiff";
import {
  Badge,
  Box,
  Group,
  Overlay,
  ScrollArea,
  Stack,
  Text,
} from "@mantine/core";
import { Link } from "react-router";

/**
 * Renders a scrollable list of incomplete questions.
 */
export default function IncompleteQuestionList() {
  const { data, status, isPlaceholderData } = useIncompleteQuestionQuery();

  if (status === "pending") {
    return <IncompleteQuestionListSkeleton />;
  }

  if (status === "error") {
    return <div>Error</div>;
  }

  if (!data.success) {
    return <div>{data.message}</div>;
  }

  const pageData = data.payload;

  return (
    <Box mt={10} pos="relative" w="100%" maw={925} p="xs" mx="auto">
      <Box pos="relative">
        {isPlaceholderData && (
          <Overlay zIndex={1000} backgroundOpacity={0.35} blur={3} />
        )}
        <ScrollArea
          h="70vh"
          type="auto"
          scrollbarSize={8}
          offsetScrollbars
          scrollHideDelay={500}
          styles={{
            viewport: {
              padding: "0.5rem",
            },
          }}
        >
          <Stack gap="sm" align="center">
            {pageData.length === 0 && (
              <CodebloomCard mih={80} w="100%">
                <Stack gap="xs" justify="center" align="center" h="100%">
                  <Text fw={500} ta="center" c="dimmed">
                    Nothing found.
                  </Text>
                  <Text size="sm" ta="center" c="dimmed">
                    No incomplete questions have been found.
                  </Text>
                </Stack>
              </CodebloomCard>
            )}
            {pageData.map((submission) => {
              const badgeDifficultyColor = (() => {
                if (submission.questionDifficulty === "Easy") return undefined;
                if (submission.questionDifficulty === "Medium") return "yellow";
                if (submission.questionDifficulty === "Hard") return "red";
                return undefined;
              })();

              const badgeAcceptedColor = (() => {
                const rate = submission.acceptanceRate * 100;
                if (rate >= 75) return undefined;
                if (rate >= 50) return "yellow";
                if (rate >= 0) return "red";
                return undefined;
              })();

              const LanguageIcon =
                langNameToIcon[submission.language as langNameKey] ||
                langNameToIcon["default"];

              return (
                <CodebloomCard
                  key={submission.id}
                  w="100%"
                  component={Link}
                  to={`/submission/${submission.id}`}
                  className="transition-all hover:brightness-110"
                >
                  <Stack gap="xs">
                    <Group justify="space-between" align="flex-start">
                      <Group gap="xs" flex={1} miw={0}>
                        <LanguageIcon size={22} width={22} height={22} />
                        <Text fw={500} lh={1.3} flex={1}>
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
                      <Group justify="space-between" wrap="wrap">
                        <Group gap="xs" wrap="wrap">
                          {submission.topics.map((topic) => (
                            <Badge
                              key={topic.id}
                              size="xs"
                              variant="filled"
                              color="gray.4"
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
                  </Stack>
                </CodebloomCard>
              );
            })}
          </Stack>
        </ScrollArea>
      </Box>
    </Box>
  );
}
