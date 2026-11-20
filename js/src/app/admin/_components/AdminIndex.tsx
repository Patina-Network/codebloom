import DeleteAnnouncementButton from "@/app/admin/_components/announcements/DeleteAnnouncementButton";
import NewAnnouncementModal from "@/app/admin/_components/announcements/NewAnnouncementModal";
import NewDiscordAnnouncementModal from "@/app/admin/_components/announcements/NewDiscordAnnouncementModal";
import IncompleteQuestionList from "@/app/admin/_components/incomplete/IncompleteQuestionList";
import RequeueIncompleteQuestionsModal from "@/app/admin/_components/incomplete/RequeueIncompleteQuestionsModal";
import AllLeaderboardAdmin from "@/app/admin/_components/leaderboards/pagination/AllLeaderboardAdmin";
import RefreshLeetcodeTokenModal from "@/app/admin/_components/leetcode/RefreshLeetcodeTokenModal";
import UserAdminList from "@/app/admin/_components/users/UserAdminList";
import { Flex, SimpleGrid, Title } from "@mantine/core";

export default function AdminIndex() {
  return (
    <Flex w={"98vw"} mih={"100vh"} direction={"column"}>
      <Title order={1} ta="center" mb={"sm"}>
        Admin Page
      </Title>
      <Flex
        w={"100%"}
        direction={"column"}
        justify={"center"}
        mb="lg"
        ta="center"
      >
        <Flex w={"100%"} direction={"row"} justify={"center"} mb={"xs"}>
          <NewAnnouncementModal />
        </Flex>
        <Flex w={"100%"} direction={"row"} justify={"center"}>
          <DeleteAnnouncementButton />
        </Flex>
        <Flex w={"100%"} direction={"row"} justify={"center"} mb={"xs"}>
          <NewDiscordAnnouncementModal />
        </Flex>
        <Flex w={"100%"} direction={"row"} justify={"center"} mb={"xs"}>
          <RefreshLeetcodeTokenModal />
        </Flex>
      </Flex>
      <SimpleGrid w={"100%"} cols={{ base: 1, md: 3 }} spacing={"sm"}>
        <Flex w={"100%"} direction={"column"} ta={"center"}>
          <Title order={4}>Leaderboards</Title>
          <AllLeaderboardAdmin />
        </Flex>
        <Flex w={"100%"} direction={"column"} ta={"center"}>
          <Title order={4}>Incomplete Questions</Title>
          <RequeueIncompleteQuestionsModal />
          <IncompleteQuestionList />
        </Flex>
        <Flex w={"100%"} direction={"column"} ta={"center"}>
          <Title order={4}>Users</Title>
          <UserAdminList />
        </Flex>
      </SimpleGrid>
    </Flex>
  );
}
