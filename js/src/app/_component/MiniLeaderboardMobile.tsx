import MiniLeaderboardMobileSkeleton from "@/app/_component/skeletons/MiniLeaderboardMobileSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import LeaderboardCard from "@/components/ui/LeaderboardCard";
import TagList from "@/components/ui/tags/TagList";
import Toast from "@/components/ui/toast/Toast";
import { useCurrentLeaderboardUsersQuery } from "@/lib/api/queries/leaderboard";
import { Tag } from "@/lib/api/types/schema";
import { tagFF } from "@/lib/ff/tag";
import getOrdinal from "@/lib/helper/ordinal";
import { theme } from "@/lib/theme";
import {
  Button,
  Flex,
  Overlay,
  SegmentedControl,
  Text,
  Tooltip,
} from "@mantine/core";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { FaDiscord } from "react-icons/fa";
import { SiLeetcode } from "react-icons/si";
import { Link } from "react-router";

export default function MiniLeaderboardMobile() {
  const { data, status, filters, toggleFilter, isPlaceholderData } =
    useCurrentLeaderboardUsersQuery({ pageSize: 5, tieToUrl: false });

  if (status === "pending") {
    return <MiniLeaderboardMobileSkeleton />;
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
          mt="md"
          mb="xl"
        >
          {first && (
            <LeaderboardCard
              placeString={getOrdinal(first.index)}
              sizeOrder={1}
              discordName={first.discordName}
              leetcodeUsername={first.leetcodeUsername}
              nickname={first.nickname}
              totalScore={first.totalScore}
              width={"300px"}
              userId={first.id}
              isLoading={isPlaceholderData}
              tags={first.tags}
            />
          )}
          {second && (
            <LeaderboardCard
              placeString={getOrdinal(second.index)}
              sizeOrder={2}
              discordName={second.discordName}
              leetcodeUsername={second.leetcodeUsername}
              nickname={second.nickname}
              totalScore={second.totalScore}
              width={"300px"}
              userId={second.id}
              isLoading={isPlaceholderData}
              tags={second.tags}
            />
          )}
          {third && (
            <LeaderboardCard
              placeString={getOrdinal(third.index)}
              sizeOrder={3}
              discordName={third.discordName}
              leetcodeUsername={third.leetcodeUsername}
              nickname={third.nickname}
              totalScore={third.totalScore}
              width={"300px"}
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
                    gap="sm"
                    w="100%"
                  >
                    <Flex align="center" gap="sm" miw={0}>
                      <Text
                        size="lg"
                        fw={700}
                        c={theme.colors.patina[4]}
                        miw="35px"
                        fz="16px"
                      >
                        #{entry.index}
                      </Text>
                      <Flex direction="column" gap={4} miw={0}>
                        {entry.nickname && (
                          <Flex align="center" gap={6}>
                            <Tooltip label="This user is a verified member of the Patina Discord server.">
                              <Flex align="center" gap={6}>
                                <IconCircleCheckFilled
                                  color={theme.colors.patina[4]}
                                  size={16}
                                />
                                <Text fw={600} size="sm" truncate>
                                  {entry.nickname}
                                </Text>
                              </Flex>
                            </Tooltip>
                            {tagFF && entry.tags?.length > 0 && (
                              <TagList tags={entry.tags} size={14} gap="xs" />
                            )}
                          </Flex>
                        )}
                        <Flex gap="sm" wrap="wrap">
                          <Flex align="center" gap={4}>
                            <FaDiscord size={14} />
                            <Text size="xs" truncate maw={120}>
                              {entry.discordName}
                            </Text>
                          </Flex>
                          <Flex align="center" gap={4}>
                            <SiLeetcode size={14} />
                            <Text size="xs" truncate>
                              {entry.leetcodeUsername}
                            </Text>
                          </Flex>
                        </Flex>
                      </Flex>
                    </Flex>
                    <Text size="sm" fw={600} miw={70} ta="right">
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
          View all
        </Button>
      </div>
    </>
  );
}
