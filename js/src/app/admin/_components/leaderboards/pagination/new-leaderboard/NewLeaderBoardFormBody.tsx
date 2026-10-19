import { adminSchema } from "@/app/admin/_components/types";
import { useCreateLeaderboardMutation } from "@/lib/api/queries/admin";
import { Modal, Button, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import d from "dayjs";
import { zod4Resolver } from "mantine-form-zod-resolver";
import { useState } from "react";
import { z } from "zod";

interface NewLeaderboardFormProps {
  currentLeaderboardName: string;
}

function NewLeaderboardForm({
  currentLeaderboardName,
}: NewLeaderboardFormProps) {
  const [isModalOpen, setModalOpen] = useState(false);
  const { mutate, status } = useCreateLeaderboardMutation();
  const schema = adminSchema(currentLeaderboardName);
  const form = useForm<z.infer<typeof schema>>({
    validate: zod4Resolver(schema),
    initialValues: {
      name: "",
      confirmation: "",
      shouldExpireBy: null,
      syntaxHighlightingLanguage: null,
    },
    transformValues: ({
      shouldExpireBy,
      syntaxHighlightingLanguage,
      ...values
    }) => ({
      ...values,
      shouldExpireBy: shouldExpireBy ? d(shouldExpireBy).toISOString() : null,
      syntaxHighlightingLanguage: syntaxHighlightingLanguage,
    }),
  });
  const toggleModal = () => {
    setModalOpen((prev) => !prev);
  };

  const onSubmit = (values: z.infer<ReturnType<typeof adminSchema>>) => {
    notifications.show({
      message: "Please wait, this can take a while...",
      color: "blue",
    });
    mutate(
      { ...values },
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
      <Button variant="outline" onClick={toggleModal} mt={"auto"}>
        Create
      </Button>
      <Modal
        opened={isModalOpen}
        onClose={toggleModal}
        title="Create New Leaderboard"
      >
        <form onSubmit={form.onSubmit(onSubmit)}>
          <TextInput
            {...form.getInputProps("name")}
            label="New leaderboard name"
            mb="sm"
            withAsterisk
          />
          <DateTimePicker
            {...form.getInputProps("shouldExpireBy")}
            label="When should leaderboard expire?"
            valueFormat="DD MMM YYYY hh:mm:ss A"
            aria-label="When should leaderboard expire?"
            withSeconds
            clearable
            mb="sm"
            timePickerProps={{
              format: "12h",
            }}
          />
          <TextInput
            {...form.getInputProps("syntaxHighlightingLanguage")}
            label="Syntax highlighting language"
            placeholder="cpp"
            error={form.errors.syntaxHighlightingLanguage}
            mb="sm"
          />
          <TextInput
            {...form.getInputProps("confirmation")}
            label={`Type "${currentLeaderboardName}" to confirm`}
            error={form.errors.confirmation}
            withAsterisk
          />
          <Button
            type="submit"
            size="xs"
            mt="sm"
            variant="outline"
            disabled={!form.isValid("confirmation") || status === "pending"}
            loading={status === "pending"}
          >
            Submit
          </Button>
        </form>
      </Modal>
    </>
  );
}

export default NewLeaderboardForm;
