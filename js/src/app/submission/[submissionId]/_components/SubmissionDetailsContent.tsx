import classes from "@/app/submission/[submissionId]/_components/SubmissionDetailsContent.module.css";
import SubmissionDetailsContentSkeleton from "@/app/submission/[submissionId]/_components/SubmissionDetailsContentSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useSubmissionDetailsQuery } from "@/lib/api/queries/submissions";
import { capitalize } from "@/lib/helper/capitalize";
import { Badge, Box, Button, Center, Flex, Text, Title } from "@mantine/core";
import { FaDiscord } from "react-icons/fa";
import { FiExternalLink } from "react-icons/fi";
import { useNavigate } from "react-router";
import { Link } from "react-router";
import ShikiHighlighter from "react-shiki";

export default function SubmissionDetailsContent({
  submissionId,
}: {
  submissionId: string;
}) {
  const navigate = useNavigate();

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
    questionLink,
    pointsAwarded,
    questionDifficulty,
    description,
    acceptanceRate,
    discordName,
    code,
    runtime,
    memory,
    language,
    userId,
  } = data.payload;

  const badgeAcceptedColor = (() => {
    const ac = acceptanceRate * 100;
    if (ac >= 75) {
      return undefined;
    }
    if (ac >= 50) {
      return "yellow";
    }
    if (ac >= 30) {
      return "red";
    }
    return undefined;
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

  return (
    <>
      <DocumentTitle title={`CodeBloom - ${questionTitle}`} />
      <DocumentDescription
        description={`CodeBloom - View ${discordName}'s solution for ${questionTitle}`}
      />
      <Box p={"lg"}>
        <Center>
          <Title mt="lg" mb="lg" order={3}>
            {questionTitle}
            <Button
              component={Link}
              to={questionLink}
              reloadDocument
              target="_blank"
              rel="noopener noreferrer"
              mx={"sm"}
              variant={"light"}
            >
              <FiExternalLink size={20} color="green" />
            </Button>
          </Title>
        </Center>
        <div style={{ padding: "0rem" }}>
          <Center>
            <Title order={3}>
              Solved by{" "}
              <FaDiscord
                style={{
                  display: "inline",
                  color: "var(--mantine-color-blue-5)",
                }}
              />{" "}
              <Title display={"inline"} c={"blue.5"} order={3}>
                {discordName}
              </Title>
            </Title>
          </Center>
          <Center>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <Title fw={700} order={4}>
                Points:
              </Title>
              <Text size="lg">{pointsAwarded}</Text>
            </div>
          </Center>
          <Center>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <Title fw={700} order={4}>
                Difficulty:
              </Title>
              <Badge ta="center" color={badgeDifficultyColor}>
                {questionDifficulty}
              </Badge>
            </div>
          </Center>
          <Center>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <Title fw={700} order={4}>
                Acceptance Rate:
              </Title>
              <Badge ta={"center"} color={badgeAcceptedColor}>
                {Math.round(acceptanceRate * 100)}%
              </Badge>
            </div>
          </Center>
          <Center mt={"xs"}>
            <Button
              onClick={() => {
                navigate(`/user/${userId}`);
              }}
            >
              ← Go to profile
            </Button>
          </Center>
          <CodebloomCard shadow="xs" padding="lg" radius="lg" mt="xl">
            <div
              dangerouslySetInnerHTML={{
                __html: description ?? "No available description found.",
              }}
              style={{
                overflow: "auto",
                minWidth: 0,
              }}
              className={classes.description}
            />
          </CodebloomCard>
          <CodebloomCard shadow="xs" padding="lg" radius="lg" mt="xl">
            {isProcessing ?
              <Flex
                direction={"column"}
                align={"center"}
                gap={"lg"}
                justify={"center"}
                py={"xl"}
              >
                <Title order={4} style={{ color: "rgb(75,233,167)" }}>
                  Data Currently Not Available
                </Title>
                <Text
                  size="sm"
                  style={{ color: "rgb(75,233,167)" }}
                  ta="center"
                >
                  This submission is still being processed.
                  <br />
                  Please check back later.
                </Text>
              </Flex>
            : <>
                <Flex direction={"column"} gap={"md"} align={"center"}>
                  <Title order={3}>{capitalize(language ?? "Unknown")}</Title>
                  <Text>Runtime: {runtime ?? ""}</Text>
                  <Text>Memory: {memory ?? ""}</Text>
                </Flex>
                <ShikiHighlighter
                  language={parsedLanguage}
                  theme="github-dark-dimmed"
                >
                  {code.trim() ?? "No code available."}
                </ShikiHighlighter>
              </>
            }
          </CodebloomCard>
        </div>
      </Box>
    </>
  );
}
