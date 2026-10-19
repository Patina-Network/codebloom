import { useAuthCallbackInfo } from "@/app/login/hooks";
import LoginButton from "@/components/ui/auth/LoginButton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAuthQuery } from "@/lib/api/queries/auth";
import { Button, Center, Loader, Space, Text } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { useEffect } from "react";
import { Link } from "react-router";

export default function LoginPage() {
  // This handles any sort of callback info we get from the backend.
  const { success, message } = useAuthCallbackInfo();
  const { data, status } = useAuthQuery();

  useEffect(() => {
    if (typeof success === "boolean" && message) {
      notifications.show({
        message,
        color: success ? "green" : "red",
      });
    }
  }, [message, success]);

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

  if (authenticated) {
    return (
      <ToastWithRedirect
        to="/dashboard"
        message="You are already authenticated"
      />
    );
  }

  return (
    <>
      <DocumentTitle title={`CodeBloom - Login`} />
      <DocumentDescription description={`CodeBloom - Log into your account`} />
      <Center style={{ height: "100vh" }}>
        <CodebloomCard
          style={{
            width: 500,
            textAlign: "center",
          }}
        >
          <Text fw={400} size="xl">
            Welcome to CodeBloom!
          </Text>
          <LoginButton />
          <Space h="sm" />
          <Link to="/" reloadDocument>
            <Button size="xs" variant="subtle" style={{ fontSize: "12px" }}>
              Go Back
            </Button>
          </Link>
        </CodebloomCard>
      </Center>
    </>
  );
}
