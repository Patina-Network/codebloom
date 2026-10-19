import PrettyCounter from "@/components/ui/pretty-counter/PrettyCounter";
import SyntaxStrip, {
  SyntaxStripSize,
} from "@/components/ui/syntax/SyntaxStrip";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import {
  useCurrentLeaderboardMetadataQuery,
  useLeaderboardMetadataByIdQuery,
} from "@/lib/api/queries/leaderboard";
import useCountdown from "@/lib/hooks/useCountdown";
import { Box, Button, Center, Skeleton, Title } from "@mantine/core";
import { useEffect } from "react";
import { Link } from "react-router";

type LeaderboardMetadataOptions = {
  showClock?: boolean;
  showAllLeaderboardButton?: boolean;
  syntaxStripSize?: SyntaxStripSize;
};

export function CurrentLeaderboardMetadata(
  props: LeaderboardMetadataOptions = {},
) {
  const query = useCurrentLeaderboardMetadataQuery();
  return <LeaderboardMetadata query={query} {...props} />;
}

export function LeaderboardMetadataById({
  leaderboardId,
  ...props
}: LeaderboardMetadataOptions & { leaderboardId: string }) {
  const query = useLeaderboardMetadataByIdQuery(leaderboardId);
  return <LeaderboardMetadata query={query} {...props} />;
}

function LeaderboardMetadata({
  query,
  showClock = false,
  showAllLeaderboardButton = false,
  syntaxStripSize,
}: LeaderboardMetadataOptions & {
  query: ReturnType<typeof useCurrentLeaderboardMetadataQuery>;
}) {
  const { data, status } = query;
  const [countdown, reset] = useCountdown(-10);

  useEffect(() => {
    if (status === "success" && data.success && data.payload.shouldExpireBy) {
      const shouldExpireByDate = new Date(data.payload.shouldExpireBy);
      const expireSeconds = (shouldExpireByDate.getTime() - Date.now()) / 1000;
      reset(expireSeconds);
    }
  }, [status, data, reset]);

  if (status === "pending") {
    return (
      <>
        <Center>
          <Title order={4} mb={"sm"} ta={"center"}>
            <Skeleton visible data-testid="leaderboard-skeleton-name">
              Really long leaderboard name
            </Skeleton>
          </Title>
        </Center>
        <Center>
          {showClock && (
            <Title order={4} mb={"sm"} ta={"center"}>
              <Skeleton visible data-testid="leaderboard-skeleton-timer">
                Long timer val
              </Skeleton>
            </Title>
          )}
        </Center>
        <Center>
          {showAllLeaderboardButton && (
            <Title order={4} mb={"sm"} ta={"center"}>
              <Skeleton visible data-testid="leaderboard-skeleton-all-button">
                All leaderboards !
              </Skeleton>
            </Title>
          )}
        </Center>
      </>
    );
  }

  if (status === "error") {
    return (
      <Title
        order={3}
        style={{
          fontSize: "1rem",
          fontWeight: "bold",
          marginBottom: "1rem",
        }}
        className="text-center sm:text-lg"
      >
        Sorry, something went wrong.
      </Title>
    );
  }

  if (!data.success) {
    return (
      <Title
        order={3}
        style={{
          fontSize: "1rem",
          fontWeight: "bold",
          marginBottom: "1rem",
        }}
        className="text-center sm:text-lg"
      >
        {data.message}
      </Title>
    );
  }

  const leaderboardData = data.payload;

  return (
    <>
      <DocumentTitle title={`CodeBloom - ${leaderboardData.name}`} />
      <DocumentDescription
        description={`CodeBloom - View your rank in the leaderboard`}
      />
      <Title
        order={4}
        ta={"center"}
        mb={"xs"}
        data-testid="LeaderboardMetadata-title"
      >
        {leaderboardData.syntaxHighlightingLanguage ?
          <SyntaxStrip
            size={syntaxStripSize}
            name={leaderboardData.name}
            language={leaderboardData.syntaxHighlightingLanguage}
          />
        : leaderboardData.name}
      </Title>
      <Title order={6} ta={"center"} mb={0}>
        {showClock && leaderboardData.shouldExpireBy && countdown > 0 && (
          <PrettyCounter size={"lg"} time={countdown} />
        )}
      </Title>
      {showAllLeaderboardButton && (
        <Box ta={"center"} my="md">
          <Button
            component={Link}
            to="/leaderboard/all"
            data-testid="ShowAllLeaderboardsButton"
          >
            View All Leaderboards
          </Button>
        </Box>
      )}
    </>
  );
}
