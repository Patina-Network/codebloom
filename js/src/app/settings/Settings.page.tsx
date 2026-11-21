import ChangeImageSettingsCard from "@/app/settings/_components/ChangeImageSettingsCard";
import DeactivateAccountCard from "@/app/settings/_components/DeactivateAccountCard";
import LogoutAllSessionsCard from "@/app/settings/_components/LogoutAllSessionsCard";
import SchoolVerifySettingsCard from "@/app/settings/_components/SchoolVerifySettingsCard";
import SettingsSkeleton from "@/app/settings/_components/SettingsSkeleton";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAuthQuery } from "@/lib/api/queries/auth";
import { useBackendCallbackParams } from "@/lib/hooks/useBackendCallbackParams";
import { Box, Center, Stack, Title } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { useEffect } from "react";

export default function SettingsPage() {
  const { data, status } = useAuthQuery();
  const { success, message } = useBackendCallbackParams();

  useEffect(() => {
    if (typeof success === "boolean" && message) {
      notifications.show({
        message,
        color: success ? "green" : "red",
      });
    }
  }, [message, success]);

  if (status === "pending") {
    return <SettingsSkeleton />;
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

  const schoolExists = data.user.schoolEmail !== null;

  return (
    <>
      <DocumentTitle title={`CodeBloom - Settings`} />
      <DocumentDescription description={`CodeBloom - Settings`} />
      <Box mih={"90vh"} p={"lg"}>
        <Box>
          <Center>
            <Title order={3} p="md">
              Settings
            </Title>
          </Center>
          <Stack gap="xl">
            <SchoolVerifySettingsCard schoolExists={schoolExists} />
            <ChangeImageSettingsCard />
            <LogoutAllSessionsCard />
            <DeactivateAccountCard />
          </Stack>
        </Box>
      </Box>
    </>
  );
}
