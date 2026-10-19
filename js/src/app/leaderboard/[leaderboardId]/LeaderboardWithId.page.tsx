import { LeaderboardById } from "@/app/leaderboard/_components/Leaderboard";
import { LeaderboardMetadataById } from "@/app/leaderboard/_components/LeaderboardMetadata/LeaderboardMetadata";
import { Box } from "@mantine/core";
import { useParams } from "react-router";

export default function LeaderboardWithIdPage() {
  const { leaderboardId } = useParams();

  // This shouldn't happen, since no ID means `/leaderboard`,
  // which just links to the current leaderboard.
  if (!leaderboardId) {
    return <></>;
  }

  return (
    <>
      <Box>
        <LeaderboardMetadataById
          leaderboardId={leaderboardId}
          showAllLeaderboardButton
        />
        <LeaderboardById leaderboardId={leaderboardId} />
      </Box>
    </>
  );
}
