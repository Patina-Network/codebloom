import { DuelWithIdBody } from "@/app/duel/_components/DuelBody";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAuthQuery } from "@/lib/api/queries/auth";
import { Flex, Loader } from "@mantine/core";
import { useParams } from "react-router";

export default function DuelPage() {
  const { lobbyCode } = useParams<{ lobbyCode: string }>();
  const { data, status } = useAuthQuery();

  if (!lobbyCode) {
    return (
      <ToastWithRedirect
        to={"/duel"}
        message={"Please join or create a party first."}
      />
    );
  }

  if (status === "pending") {
    return (
      <Flex
        direction={"column"}
        align={"center"}
        justify={"center"}
        w={"98vw"}
        h={"90vh"}
      >
        <Loader />
      </Flex>
    );
  }

  if (status === "error") {
    return (
      <ToastWithRedirect to={-1} message={"Sorry, something went wrong."} />
    );
  }

  const authenticated = !!data.user && !!data.session;

  if (!authenticated) {
    return (
      <ToastWithRedirect to={"/login"} message={"Please log in to continue."} />
    );
  }

  const onboarded = !!data.user.leetcodeUsername;

  if (!onboarded) {
    return (
      <ToastWithRedirect
        to="/onboarding"
        message="Please finish the onboarding to gain access."
      />
    );
  }

  return <DuelWithIdBody code={lobbyCode} currentUser={data.user} />;
}
