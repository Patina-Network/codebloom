import { useRequeueIncompleteQuestionsMutation } from "@/lib/api/queries/incomplete";
import { Button, Modal, Text } from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { notifications } from "@mantine/notifications";
import { FormEvent } from "react";

export default function RequeueIncompleteQuestionsModal() {
  const [opened, { open, close }] = useDisclosure(false);
  const { mutate, status } = useRequeueIncompleteQuestionsMutation();

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
      <Button onClick={open} variant="outline" size="xs" mx="auto">
        Requeue Incomplete Questions
      </Button>
      <Modal
        opened={opened}
        onClose={close}
        title="Requeue Incomplete Questions"
      >
        <form onSubmit={onSubmit}>
          <Text mt={12}>
            Queues incomplete questions without an active job and restarts jobs
            stuck processing for at least one hour with a fresh retry budget.
            Already queued jobs and jobs processing for less than an hour are
            left unchanged.
          </Text>
          <Text mt={12}>
            Each job calls LeetCode. If jobs have been failing because the auth
            token expired, refresh the LeetCode token first.
          </Text>
          <Button
            type="submit"
            size="xs"
            mt="sm"
            variant="outline"
            disabled={status === "pending"}
            loading={status === "pending"}
          >
            Requeue
          </Button>
        </form>
      </Modal>
    </>
  );
}
