import MiniLeaderboardSkeleton from "@/app/_component/skeletons/MiniLeaderboardSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import LeaderboardCard from "@/components/ui/LeaderboardCard";
import TagList from "@/components/ui/tags/TagList";
import Toast from "@/components/ui/toast/Toast";
import { useCurrentLeaderboardUsersQuery } from "@/lib/api/queries/leaderboard";
import { Tag } from "@/lib/api/types/schema";
import { tagFF } from "@/lib/ff";
import getOrdinal from "@/lib/helper/ordinal";
import { theme } from "@/lib/theme";
import {
  Button,
  Flex,
  Overlay,
  SegmentedControl,
  Text,
  Tooltip,
  Stack,
} from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export default function MiniLeaderboardDesktop() {
  const { data, status, filters, toggleFilter, isPlaceholderData } =
    useCurrentLeaderboardUsersQuery({ pageSize: 5, tieToUrl: false });

  if (status === "pending") {
    return <MiniLeaderboardSkeleton />;
  }

  if (status === "error") {
    return <Toast message="Sorry, something went wrong." />;
  }

  if (!data.success) {
    return <Toast message={data.message} />;
  }

  const leaderboardData = data.payload;

  if (leaderboardData.items.length == 0) {
    return <p>Sorry, there are no users to display.</p>;
  }

  const [first, second, third] = leaderboardData.items;
  return (
    <>
      <SegmentedControl
        value={filters.Patina ? "patina" : "all"}
        w={"100%"}
        variant={"light"}
        data={[
          { label: "All", value: "all" },
          { label: "Patina", value: "patina" },
        ]}
        onChange={() => toggleFilter(Tag.Patina)}
      />
      <div style={{ position: "relative" }}>
        {isPlaceholderData && (
          <Overlay
            zIndex={1000}
            backgroundOpacity={0.55}
            blur={10}
            radius={"md"}
          />
        )}
        <Flex
          direction={{ base: "column", sm: "row" }}
          align={{ base: "center", sm: "flex-end" }}
          justify="center"
          gap="md"
          mb="xl"
          mt="md"
        >
          {second && (
            <LeaderboardCard
              placeString={getOrdinal(second.index)}
              sizeOrder={2}
              discordName={second.discordName}
              leetcodeUsername={second.leetcodeUsername}
              totalScore={second.totalScore}
              nickname={second.nickname}
              width={"200px"}
              userId={second.id}
              isLoading={isPlaceholderData}
              tags={second.tags}
            />
          )}
          {first && (
            <LeaderboardCard
              placeString={getOrdinal(first.index)}
              sizeOrder={1}
              discordName={first.discordName}
              leetcodeUsername={first.leetcodeUsername}
              totalScore={first.totalScore}
              nickname={first.nickname}
              width={"200px"}
              userId={first.id}
              isLoading={isPlaceholderData}
              tags={first.tags}
            />
          )}
          {third && (
            <LeaderboardCard
              placeString={getOrdinal(third.index)}
              sizeOrder={3}
              discordName={third.discordName}
              leetcodeUsername={third.leetcodeUsername}
              totalScore={third.totalScore}
              nickname={third.nickname}
              width={"200px"}
              userId={third.id}
              isLoading={isPlaceholderData}
              tags={third.tags}
            />
          )}
        </Flex>
        {leaderboardData.items.length > 3 && (
          <Flex direction="column" gap="xs" mt="md" mb="md">
            {leaderboardData.items.map((entry, index) => {
              if ([0, 1, 2].includes(index)) return null;
              return (
                <CodebloomCard
                  key={entry.id}
                  component={Link}
                  to={`/user/${entry.id}`}
                  padding="lg"
                  bg={theme.colors.dark[7]}
                  styles={{
                    root: {
                      borderColor: theme.colors.dark[3],
                    },
                  }}
                  style={{
                    transition: "all 0.2s ease",
                    cursor: "pointer",
                    textDecoration: "none",
                  }}
                >
                  <Flex
                    justify="space-between"
                    align="center"
                    gap="md"
                    w="100%"
                  >
                    <Flex align="center" gap="md" miw={0}>
                      <Text
                        size="lg"
                        fw={700}
                        c={theme.colors.patina[4]}
                        miw={50}
                      >
                        #{entry.index}
                      </Text>
                      <Flex direction="column" gap="xs" miw={0}>
                        <Stack gap="xs">
                          <Flex
                            direction={{ base: "column", xs: "row" }}
                            gap={{ base: "xs", xs: "md" }}
                            align={{ base: "flex-start", xs: "center" }}
                          >
                            <Flex align="center" gap={6}>
                              <FaDiscord size={16} />
                              <Text size="md" fw={600}>
                                {entry.discordName}
                              </Text>
                            </Flex>
                            <Flex align="center" gap={6}>
                              <SiLeetcode size={16} />
                              <Text size="md" fw={600}>
                                {entry.leetcodeUsername}
                              </Text>
                            </Flex>
                          </Flex>
                          {(entry.nickname ||
                            (tagFF && entry.tags && entry.tags.length > 0)) && (
                            <Flex align="center" gap={5}>
                              {entry.nickname && (
                                <Tooltip label="This user is a verified member of the Patina Discord server.">
                                  <Flex align="center" gap={5}>
                                    <IconCircleCheckFilled
                                      color={theme.colors.patina[4]}
                                      size={18}
                                    />
                                    <Text size="sm">{entry.nickname}</Text>
                                  </Flex>
                                </Tooltip>
                              )}
                              {tagFF && entry.tags && entry.tags.length > 0 && (
                                <TagList tags={entry.tags} size={16} gap="xs" />
                              )}
                            </Flex>
                          )}
                        </Stack>
                      </Flex>
                    </Flex>
                    <Text size="md" fw={600} miw={90} ta="right">
                      {entry.totalScore} Pts
                    </Text>
                  </Flex>
                </CodebloomCard>
              );
            })}
          </Flex>
        )}
        <Button
          variant={"light"}
          w={"100%"}
          component={Link}
          to={`/leaderboard?patina=${filters.Patina}`}
        >
          View All
        </Button>
      </div>
    </>
  );
}
