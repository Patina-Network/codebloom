import classes from "@/app/submission/[submissionId]/_components/SubmissionDetailsContent.module.css";
import SubmissionDetailsContentSkeleton from "@/app/submission/[submissionId]/_components/SubmissionDetailsContentSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import {
  langNameKey,
  langNameToIcon,
} from "@/components/ui/langname-to-icon/LangNameToIcon";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useSubmissionDetailsQuery } from "@/lib/api/queries/submissions";
import { ApiUtils } from "@/lib/api/utils";
import { capitalize } from "@/lib/helper/capitalize";
import { theme } from "@/lib/theme";
import { timeDiff } from "@/lib/timeDiff";
import {
  ActionIcon,
  Anchor,
  Badge,
  Box,
  Button,
  CopyButton,
  Flex,
  Group,
  SimpleGrid,
  Spoiler,
  Stack,
  Text,
  Title,
  Tooltip,
} from "@mantine/core";
import {
  IconCheck,
  IconCircleCheckFilled,
  IconClock,
  IconCopy,
  IconDatabase,
  IconExternalLink,
  IconHourglassHigh,
} from "@tabler/icons-react";
import { ReactNode } from "react";
import { FaDiscord } from "react-icons/fa";
import { Link } from "react-router-dom";
import ShikiHighlighter from "react-shiki";

export default function SubmissionDetailsContent({
  submissionId,
}: {
  submissionId: string;
}) {
  const { data, status } = useSubmissionDetailsQuery({ submissionId });

  if (status === "pending") {
    return <SubmissionDetailsContentSkeleton />;
  }

  if (status === "error") {
    return <Toast message="Sorry, something went wrong." />;
  }

  if (!data.success) {
    return <ToastWithRedirect message={data.message} to={"/dashboard"} />;
  }

  const {
    questionTitle,
    questionNumber,
    questionLink,
    pointsAwarded,
    questionDifficulty,
    description,
    acceptanceRate,
    discordName,
    nickname,
    code,
    runtime,
    memory,
    language,
    userId,
    submittedAt,
    topics,
  } = data.payload;

  const badgeAcceptedColor = (() => {
    const ac = acceptanceRate * 100;
    if (ac >= 75) {
      return undefined;
    }
    if (ac >= 50) {
      return "yellow";
    }
    return "red";
  })();

  const badgeDifficultyColor = (() => {
    if (questionDifficulty === "Easy") {
      return undefined;
    }
    if (questionDifficulty === "Medium") {
      return "yellow";
    }
    if (questionDifficulty === "Hard") {
      return "red";
    }
    return undefined;
  })();

  const parsedLanguage = (() => {
    if (!language) return undefined;
    return language === "python3" ? "python" : language;
  })();
  const isProcessing =
    !code || !language || language === "Unknown" || !runtime || !memory;

  const submittedDate = new Date(submittedAt);
  const hasSubmittedDate = !Number.isNaN(submittedDate.getTime());
  const authorName = nickname ?? discordName ?? "Unknown user";

  const stats: { label: string; value: ReactNode }[] = [
    {
      label: "Difficulty",
      value: <Badge color={badgeDifficultyColor}>{questionDifficulty}</Badge>,
    },
    {
      label: "Acceptance",
      value: (
        <Badge color={badgeAcceptedColor}>
          {Math.round(acceptanceRate * 100)}%
        </Badge>
      ),
    },
    {
      label: "Points",
      value: (
        <Text fw={700} lh={1}>
          {pointsAwarded ?? "—"}
        </Text>
      ),
    },
  ];
  if (hasSubmittedDate) {
    stats.push({
      label: "Submitted",
      value: (
        <Tooltip label={submittedDate.toLocaleString()}>
          <Text fw={700} lh={1}>
            {timeDiff(submittedDate)}
          </Text>
        </Tooltip>
      ),
    });
  }

  return (
    <>
      <DocumentTitle title={`CodeBloom - ${questionTitle}`} />
      <DocumentDescription
        description={`CodeBloom - View ${authorName}'s solution for ${questionTitle}`}
      />
      <Box maw={1100} mx="auto" p="lg">
        <Stack gap="lg">
          <CodebloomCard padding="lg" radius="lg">
            <Stack gap="md">
              <Flex
                justify="space-between"
                align="flex-start"
                gap="md"
                wrap="wrap"
              >
                <Stack gap={6} miw={0} flex="1 1 20rem">
                  <Title order={2} lh={1.25} className={classes.title}>
                    <Text span inherit c="dimmed">
                      {questionNumber}.
                    </Text>{" "}
                    <span>{questionTitle}</span>
                  </Title>
                  <Group gap={6}>
                    <Text c="dimmed">Solved by</Text>
                    <Anchor
                      component={Link}
                      to={`/user/${userId}`}
                      underline="hover"
                      fw={600}
                      c={nickname ? "patina.4" : "blue.5"}
                      className={classes.author}
                    >
                      {nickname ?
                        <Tooltip label="This user is a verified member of the Patina Discord server.">
                          <IconCircleCheckFilled
                            size={18}
                            color={theme.colors.patina[4]}
                          />
                        </Tooltip>
                      : <FaDiscord size={18} />}
                      <span>{authorName}</span>
                    </Anchor>
                  </Group>
                </Stack>
                <Button
                  component="a"
                  href={questionLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  variant="light"
                  rightSection={<IconExternalLink size={16} />}
                >
                  Open on LeetCode
                </Button>
              </Flex>
              <SimpleGrid cols={{ base: 2, sm: stats.length }} spacing="sm">
                {stats.map((stat) => (
                  <Stack key={stat.label} gap={8} className={classes.stat}>
                    <Text size="xs" c="dimmed" tt="uppercase" fw={600}>
                      {stat.label}
                    </Text>
                    {stat.value}
                  </Stack>
                ))}
              </SimpleGrid>
              {topics.length > 0 && (
                <Group gap="xs">
                  {topics.map((topic) => (
                    <Badge
                      key={topic.id}
                      size="sm"
                      variant="light"
                      color="gray"
                    >
                      {
                        ApiUtils.getTopicEnumMetadataByTopicEnum(topic.topic)
                          .name
                      }
                    </Badge>
                  ))}
                </Group>
              )}
            </Stack>
          </CodebloomCard>
          <CodebloomCard padding="lg" radius="lg">
            <Title order={4} mb="sm">
              Problem
            </Title>
            <Spoiler
              maxHeight={220}
              showLabel="Show full problem"
              hideLabel="Show less"
              classNames={{
                root: classes.spoilerRoot,
                control: classes.spoilerControl,
              }}
            >
              <div
                dangerouslySetInnerHTML={{
                  __html: description ?? "No available description found.",
                }}
                className={classes.description}
              />
            </Spoiler>
          </CodebloomCard>
          <CodebloomCard padding="lg" radius="lg">
            {isProcessing ?
              <Stack align="center" gap="sm" py="xl">
                <IconHourglassHigh size={36} color={theme.colors.patina[4]} />
                <Title order={4} c="patina.4">
                  Data Currently Not Available
                </Title>
                <Text size="sm" c="dimmed" ta="center">
                  This submission is still being processed.
                  <br />
                  Please check back later.
                </Text>
              </Stack>
            : <SolutionCode
                code={code.trim()}
                language={language}
                parsedLanguage={parsedLanguage}
                runtime={runtime}
                memory={memory}
              />
            }
          </CodebloomCard>
        </Stack>
      </Box>
    </>
  );
}

function SolutionCode({
  code,
  language,
  parsedLanguage,
  runtime,
  memory,
}: {
  code: string;
  language: string;
  parsedLanguage: string | undefined;
  runtime: string;
  memory: string;
}) {
  const LanguageIcon =
    langNameToIcon[language as langNameKey] ?? langNameToIcon.default;

  return (
    <Stack gap="md">
      <Flex justify="space-between" align="center" gap="sm" wrap="wrap">
        <Group gap="xs">
          <LanguageIcon size={22} width={22} height={22} />
          <Title order={4}>{capitalize(language)}</Title>
        </Group>
        <Group gap="lg">
          <Group gap={6}>
            <IconClock size={16} color="var(--mantine-color-dimmed)" />
            <Text size="sm" c="dimmed">
              Runtime
            </Text>
            <Text size="sm" fw={600}>
              {runtime}
            </Text>
          </Group>
          <Group gap={6}>
            <IconDatabase size={16} color="var(--mantine-color-dimmed)" />
            <Text size="sm" c="dimmed">
              Memory
            </Text>
            <Text size="sm" fw={600}>
              {memory}
            </Text>
          </Group>
          <CopyButton value={code}>
            {({ copied, copy }) => (
              <Tooltip label={copied ? "Copied" : "Copy code"}>
                <ActionIcon
                  variant="subtle"
                  color={copied ? "patina" : "gray"}
                  onClick={copy}
                  aria-label="Copy code"
                >
                  {copied ?
                    <IconCheck size={16} />
                  : <IconCopy size={16} />}
                </ActionIcon>
              </Tooltip>
            )}
          </CopyButton>
        </Group>
      </Flex>
      <ShikiHighlighter
        language={parsedLanguage}
        theme="github-dark-dimmed"
        showLanguage={false}
        showLineNumbers
        className={classes.code}
      >
        {code}
      </ShikiHighlighter>
    </Stack>
  );
}
