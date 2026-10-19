import { useCreateAnnouncementLeaderboardMutation } from "@/lib/api/queries/admin";
import { newAnnouncementSchema } from "@/lib/api/schema/admin";
import { Box, Button, Modal, Switch, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import d from "dayjs";
import { zod4Resolver } from "mantine-form-zod-resolver";
import { useState } from "react";
import { z } from "zod";

export default function NewAnnouncementModal() {
  const [isModalOpen, setModalOpen] = useState(false);
  const { mutate, status } = useCreateAnnouncementLeaderboardMutation();
  const form = useForm({
    validate: zod4Resolver(newAnnouncementSchema),
    initialValues: {
      message: "",
      showTimer: false,
      expiresAt: d().toString(),
    },
    transformValues: ({ expiresAt, ...values }) => ({
      ...values,
      expiresAt: d(expiresAt).toISOString(),
    }),
  });

  const toggleModal = () => {
    setModalOpen((prev) => !prev);
  };

  const onSubmit = (data: z.infer<typeof newAnnouncementSchema>) => {
    mutate(
      { ...data },
      {
        onSuccess: async (data) => {
          notifications.show({
            message: data.message,
            color: data.success ? undefined : "red",
          });
          if (data.success) {
            form.reset();
            setModalOpen(false);
          }
        },
      },
    );
  };

  return (
    <>
      <Button onClick={toggleModal} top={12}>
        New Announcement
      </Button>
      <Modal
        opened={isModalOpen}
        onClose={toggleModal}
        title="Create new announcement"
      >
        <form onSubmit={form.onSubmit(onSubmit)}>
          <TextInput
            {...form.getInputProps("message")}
            label="Announcement message"
            error={form.errors.message}
            withAsterisk
            mb={"sm"}
          />
          <DateTimePicker
            {...form.getInputProps("expiresAt")}
            label={"When should announcement expire?"}
            valueFormat="DD MMM YYYY hh:mm:ss A"
            aria-label="When should announcement expire?"
            withSeconds
            clearable
            withAsterisk
            timePickerProps={{
              format: "12h",
            }}
          />
          <Box my={"sm"}>
            <Switch
              {...form.getInputProps("showTimer")}
              label={"Show timer?"}
            />
          </Box>
          <Button
            type="submit"
            size="xs"
            mt="sm"
            variant="outline"
            disabled={status === "pending"}
          >
            Submit
          </Button>
        </form>
      </Modal>
    </>
  );
}
