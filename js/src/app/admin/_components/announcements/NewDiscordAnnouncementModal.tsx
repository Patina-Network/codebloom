import { useCreateDiscordAnnouncementMutation } from "@/lib/api/queries/admin";
import { newDiscordAnnouncementSchema } from "@/lib/api/schema/admin";
import { Button, Modal, TextInput } from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { zodResolver } from "mantine-form-zod-resolver";
import { useState } from "react";
import { z } from "zod";

export default function NewDiscordAnnouncementModal() {
  const [isModalOpen, setModalOpen] = useState(false);
  const { mutate, status } = useCreateDiscordAnnouncementMutation();
  const form = useForm({
    validate: zodResolver(newDiscordAnnouncementSchema),
    initialValues: {
      message: "",
    },
  });

  const toggleModal = () => {
    form.clearErrors();
    setModalOpen((prev) => !prev);
  };

  const onSubmit = (data: z.infer<typeof newDiscordAnnouncementSchema>) => {
    mutate(
      { ...data, message: data.message.trim() },
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
        onError: (error) => {
          notifications.show({
            message: error.message,
            color: "red",
          });
        },
      },
    );
  };

  return (
    <>
      <Button onClick={toggleModal} top={12}>
        New Discord Announcement
      </Button>
      <Modal
        opened={isModalOpen}
        onClose={toggleModal}
        title="Create new discord announcement"
      >
        <form onSubmit={form.onSubmit(onSubmit)}>
          <TextInput
            {...form.getInputProps("message")}
            label="Announcement message"
            error={form.errors.message}
            withAsterisk
            mb={"sm"}
          />
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
