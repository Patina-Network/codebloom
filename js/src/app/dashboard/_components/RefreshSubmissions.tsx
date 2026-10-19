import { useUsersTotalPoints } from "@/lib/api/queries/leaderboard";
import useCountdown from "@/lib/hooks/useCountdown";
import {
  Button,
  Center,
  Text,
  CloseButton,
  Box,
  Modal,
  Group,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { useLocalStorage, useDisclosure } from "@mantine/hooks";
import { notifications } from "@mantine/notifications";
import { Link } from "react-router";

export default function RefreshSubmissions({
  schoolRegistered,
  userId,
}: {
  schoolRegistered: boolean;
  userId?: string;
}) {
  const { mutate, isPending } = useUsersTotalPoints(userId);
  const [countdown, resetCountdown] = useCountdown(0);

  const isDisabled = countdown > 0;

  const [hideBtn, setHideBtn] = useLocalStorage({
    key: "hideUniversityButton",
    defaultValue: false,
    getInitialValueInEffect: true,
  });

  const [confirmOpen, { open, close }] = useDisclosure(false);
  const xClicked = () => {
    open();
  };
  const confirmHidden = () => {
    setHideBtn(true);
    close();
  };

  const form = useForm({
    initialValues: {
      leetcodeUsername: "",
    },
  });

  const onSubmit = () => {
    mutate(undefined, {
      onSuccess: ({ success, message: responseMessage }) => {
        // Hack to avoid breaking the standard ApiResponse flow.
        if (typeof responseMessage === "number") {
          resetCountdown(responseMessage);
          return;
        }

        notifications.show({
          message: responseMessage,
          color: success ? undefined : "red",
        });
      },
    });
  };

  return (
    <>
      <Modal
        opened={confirmOpen}
        onClose={close}
        withCloseButton={false}
        centered
      >
        <Text fw={700} ta="center">
          Are you sure you want to hide this button permanently?
        </Text>
        <Text size="sm" c="dimmed" ta="center" mt="xs">
          You can always register your university email by clicking on the
          profile icon on the top right & going to the settings page
        </Text>
        <Group justify="center" mt="md">
          <Button variant="default" onClick={close}>
            Cancel
          </Button>
          <Button color="red" onClick={confirmHidden}>
            Confirm
          </Button>
        </Group>
      </Modal>
      <form onSubmit={form.onSubmit(onSubmit)}>
        <Button
          fullWidth
          radius="md"
          mt="md"
          mb="md"
          size="md"
          onClick={onSubmit}
          loading={isPending}
          disabled={isDisabled}
          ta={"center"}
        >
          {isDisabled ?
            `Please wait ${countdown} seconds to refresh again.`
          : "Refresh your latest submissions!"}
        </Button>
        {!schoolRegistered && !hideBtn && (
          <Center>
            <Box pos="relative" display="inline-block">
              <Button
                component={Link}
                variant="light"
                to="/settings"
                size="sm"
                mb="sm"
              >
                Go to settings & register your university email
              </Button>
              <CloseButton
                aria-label="Hide university button"
                size="sm"
                pos="absolute"
                top={-8}
                right={-8}
                bg="red.7"
                c="white"
                onClick={xClicked}
                radius="xl"
              />
            </Box>
          </Center>
        )}
        <Text
          c={"dimmed"}
          style={{
            maxWidth: "640px",
          }}
          ta={"center"}
        >
          Please note that we automatically query Leetcode's APIs behind the
          scenes to make sure you can stay on that LeetCode grind without having
          to press this button everytime!
        </Text>
      </form>
    </>
  );
}
