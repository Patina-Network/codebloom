import DashboardLeaderboard from "@/app/dashboard/_components/DashboardLeaderboard/DashboardLeaderboard";
import OnboardingLeetcodeUser from "@/app/dashboard/_components/OnboardingLeetcodeUser";
import ProblemOfTheDay from "@/app/dashboard/_components/ProblemOfTheDay/ProblemOfTheDay";
import RecentSubmissions from "@/app/dashboard/_components/RecentSubmissions/RecentSubmissions";
import RefreshSubmissions from "@/app/dashboard/_components/RefreshSubmissions";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAuthQuery } from "@/lib/api/queries/auth";
import { Center, Flex, Loader, Title, Box } from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";

export default function DashboardPage() {
  const smallPhone = useMediaQuery("(min-width: 48em)");
  const { data, status } = useAuthQuery();

  if (status === "pending") {
    return (
      <div className="flex flex-col items-center justify-center w-screen h-screen">
        <Loader />
      </div>
    );
  }

  if (status === "error") {
    return <Toast message="Sorry, something went wrong." />;
  }

  const authenticated = !!data.user && !!data.session;

  if (!authenticated) {
    return (
      <ToastWithRedirect to="/login" message="You are not authenticated!" />
    );
  }

  const onboarded = !!data.user.leetcodeUsername;
  const schoolRegistered = !!data.user.schoolEmail;

  if (!onboarded) {
    return (
      <ToastWithRedirect
        to="/onboarding"
        message="Please finish the onboarding to gain access to the dashboard."
      />
    );
  }
  return (
    <>
      <DocumentTitle title={`CodeBloom - Dashboard`} />
      <DocumentDescription
        description={`CodeBloom - Refresh your latest submissions`}
      />
      <Box mx="-lg">
        <Flex p={"32px"} direction={"column"} gap={"lg"} w="100%" mih="90vh">
          <Center>
            <Title order={2}>Dashboard</Title>
          </Center>
          <Center>
            {!data.user.leetcodeUsername ?
              <OnboardingLeetcodeUser />
            : <RefreshSubmissions schoolRegistered={schoolRegistered} />}
          </Center>
          <Flex direction={smallPhone ? "row" : "column"} gap={"md"}>
            <Flex direction={"column"} flex={1}>
              <ProblemOfTheDay />
            </Flex>
            <Flex direction={"column"} flex={1}>
              <DashboardLeaderboard
                userId={data.user.id}
                userTags={data.user.tags}
              />
            </Flex>
            <Flex direction={"column"} flex={1}>
              <RecentSubmissions userId={data.user.id} />
            </Flex>
          </Flex>
        </Flex>
      </Box>
    </>
  );
}
