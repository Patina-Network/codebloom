import ProfilePicture from "@/app/user/[userId]/_components/UserProfile/ProfilePicture/ProfilePicture";
import UserProfileHeader from "@/app/user/[userId]/_components/UserProfile/UserProfileHeader";
import UserTags from "@/app/user/[userId]/_components/UserProfile/UserTags/UserTags";
import MetricsChart from "@/app/user/[userId]/_components/UserProfile/MetricsChart/MetricsChart";
import MiniUserSubmissions from "@/app/user/[userId]/submissions/_components/UserSubmissions/MiniUserSubmissions";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { getUserSubmissionsUrl } from "@/lib/helper/leaderboardDateRange";
import { Center, Button, Flex, Box, Text } from "@mantine/core";
import { Link, useParams, useSearchParams } from "react-router-dom";

export default function UserProfilePage() {
  const { userId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const startDate = searchParams.get("startDate") ?? undefined;
  const endDate = searchParams.get("endDate") ?? undefined;
  const hasDateRange = !!startDate;

  if (!userId) {
    return <ToastWithRedirect to={-1} message={"This user ID is not valid."} />;
  }

  const dateRange = startDate ? { startDate, endDate } : undefined;
  const viewAllUrl = getUserSubmissionsUrl(userId, dateRange);

  const clearDateRange = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete("startDate");
    newParams.delete("endDate");
    setSearchParams(newParams);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  return (
    <>
      <Center>
        <Flex
          direction={{ base: "column", sm: "row" }}
          w="100%"
          gap="md"
          maw="1440px"
          p={{ base: "md", sm: "5%" }}
          pt="md"
          wrap="wrap"
        >
          <Box
            bg="gray.9"
            w="100%"
            p="md"
            bdrs="md"
            style={{
              flex: "1 1 260px",
              minWidth: 0,
              boxShadow: "0 4px 10px rgba(0,0,0,.5)",
            }}
          >
            <Flex direction="column" align="center" gap="sm" pt="20px">
              <ProfilePicture userId={userId} />
              <UserProfileHeader userId={userId} />
              <UserTags userId={userId} />
              <MetricsChart userId={userId}/>
            </Flex>
          </Box>
          <Box
            bg="gray.9"
            w="100%"
            p="md"
            bdrs="md"
            style={{
              flex: "3 1 360px",
              minWidth: 0,
              boxShadow: "0 4px 10px rgba(0,0,0,.5)",
            }}
          >
            <Flex
              direction={{ base: "column", md: "row" }}
              justify={{ base: "center", md: "space-between" }}
              w="100%"
              align="center"
              px="md"
              gap="xs"
            >
              <Text size="clamp(1.2rem, 2vw, 2em)" fw={700} c="white">
                Recent Submissions
              </Text>
              <Button variant="light" component={Link} to={viewAllUrl}>
                View All
              </Button>
            </Flex>
            {hasDateRange && startDate && (
              <Flex
                align="center"
                justify="flex-start"
                gap="sm"
                px="md"
                mt="xs"
              >
                <Text size="sm" c="dimmed" style={{ wordBreak: "break-word" }}>
                  {endDate ?
                    `Viewing submissions from ${formatDate(startDate)} to ${formatDate(endDate)}`
                  : `Viewing submissions from ${formatDate(startDate)}`}
                </Text>
                <Button
                  variant="outline"
                  color="red"
                  size="compact-xs"
                  onClick={clearDateRange}
                  style={{ flexShrink: 0 }}
                >
                  Clear
                </Button>
              </Flex>
            )}
            <Box>
              <MiniUserSubmissions
                userId={userId}
                key={`${startDate ?? ""}-${endDate ?? ""}`}
              />
            </Box>
          </Box>
        </Flex>
      </Center>
    </>
  );
}
