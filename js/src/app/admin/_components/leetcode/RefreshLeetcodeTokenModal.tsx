import { useRefreshLeetcodeTokenMutation } from "@/lib/api/queries/admin";
import { Button, Modal, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { notifications } from "@mantine/notifications";
import { FormEvent } from "react";

export default function RefreshLeetcodeTokenModal() {
  const [opened, { open, close }] = useDisclosure(false);
  const { mutate, status } = useRefreshLeetcodeTokenMutation();

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    mutate(undefined, {
      onSuccess: (data) => {
        notifications.show({
          message: data.message,
          color: data.success ? undefined : "red",
        });
        close();
      },
    });
  };

  return (
    <>
      <Button onClick={open} top={12} color="red" variant="outline">
        Refresh LeetCode Token
      </Button>
      <Modal opened={opened} onClose={close} title="Refresh LeetCode Token">
        <form onSubmit={onSubmit}>
          <Text mt={12}>
            Triggers an asynchronous re-steal of the LeetCode auth cookie.
          </Text>
          <Text mt={12} fw={700} c="red">
            DO NOT TRIGGER THIS UNLESS YOU KNOW WHAT YOU&apos;RE DOING.
          </Text>
          <Text mt={12}>Too many refreshes can cause rate limiting.</Text>
          <Button
            type="submit"
            size="xs"
            mt="sm"
            variant="outline"
            color="red"
            disabled={status === "pending"}
            loading={status === "pending"}
          >
            Refresh
          </Button>
        </form>
      </Modal>
    </>
  );
}
