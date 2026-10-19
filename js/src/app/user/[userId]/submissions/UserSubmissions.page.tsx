import UserSubmissions from "@/app/user/[userId]/submissions/_components/UserSubmissions/UserSubmissions";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { getUserProfileUrl } from "@/lib/helper/leaderboardDateRange";
import { Button, Flex } from "@mantine/core";
import { useParams, useNavigate, useSearchParams } from "react-router";

export default function UserSubmissionsPage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const startDate = searchParams.get("startDate") ?? undefined;
  const endDate = searchParams.get("endDate") ?? undefined;

  if (!userId) {
    return <ToastWithRedirect to={-1} message={"This User ID is not valid."} />;
  }

  const dateRange = startDate ? { startDate, endDate } : undefined;
  const profileUrl = getUserProfileUrl(userId, dateRange);

  return (
    <>
      <Flex
        direction={"column"}
        align={"center"}
        miw={"98vw"}
        mih={"90vh"}
        p={"lg"}
      >
        <Flex justify="center" align="center" w="100%" maw={925} mt="xs">
          <Button
            variant={"outline"}
            onClick={() => {
              navigate(profileUrl);
            }}
          >
            ← Go to profile
          </Button>
        </Flex>
        <UserSubmissions userId={userId} />
      </Flex>
    </>
  );
}
