import { adminSchema } from "@/app/admin/_components/types";
import { useEditLeaderboardMutation } from "@/lib/api/queries/admin";
import { Modal, Button, TextInput } from "@mantine/core";
import { DateTimePicker } from "@mantine/dates";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import d from "dayjs";
import { zod4Resolver } from "mantine-form-zod-resolver";
import { useState } from "react";
import { z } from "zod";

interface EditLeaderboardFormProps {
  currentLeaderboardName: string;
  currentLeaderboardExpire: string | null;
  currentLeaderboardLanguage: string | null;
  disableButton: boolean;
}

function EditLeaderboardForm({
  currentLeaderboardName,
  currentLeaderboardExpire,
  currentLeaderboardLanguage,
  disableButton,
}: Readonly<EditLeaderboardFormProps>) {
  const [isModalOpen, setModalOpen] = useState(false);
  const { mutate, status } = useEditLeaderboardMutation();
  const toggleModal = () => {
    setModalOpen((prev) => !prev);
  };
  const form = useForm({
    initialValues: {
      name: currentLeaderboardName,
      confirmation: "",
      syntaxHighlightingLanguage: currentLeaderboardLanguage,
      shouldExpireBy: currentLeaderboardExpire,
    },
    transformValues: ({
      syntaxHighlightingLanguage,
      shouldExpireBy,
      ...values
    }) => ({
      ...values,
      syntaxHighlightingLanguage: syntaxHighlightingLanguage,
      shouldExpireBy: shouldExpireBy ? d(shouldExpireBy).toISOString() : null,
    }),
    validate: zod4Resolver(adminSchema(currentLeaderboardName)),
  });

  const onSubmit = (values: z.infer<ReturnType<typeof adminSchema>>) => {
    notifications.show({
      color: "blue",
      message: "Please wait, this can take a while...",
    });
    mutate(
      { ...values },
      {
        onSuccess: async (data) => {
          notifications.show({
            color: data.success ? undefined : "red",
            message: data.message,
          });
          if (data.success) {
            form.setValues({
              name: values.name,
              confirmation: "",
              shouldExpireBy: values.shouldExpireBy,
              syntaxHighlightingLanguage: values.syntaxHighlightingLanguage,
            });
            setModalOpen(false);
          }
        },
      },
    );
  };

  return (
    <>
      <Button
        variant="outline"
        onClick={toggleModal}
        mt={"auto"}
        disabled={disableButton}
      >
        Edit
      </Button>
      <Modal
        opened={isModalOpen}
        onClose={toggleModal}
        title="Edit Leaderboard"
      >
        <form onSubmit={form.onSubmit(onSubmit)}>
          <TextInput
            {...form.getInputProps("name")}
            data-testid={"Name field"}
            label="Updated name"
            mb="sm"
            withAsterisk
          />
          <DateTimePicker
            {...form.getInputProps("shouldExpireBy")}
            data-testid={"Date time field"}
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
            data-testid={"Language field"}
            label="Syntax highlighting language"
            placeholder="cpp"
            error={form.errors.syntaxHighlightingLanguage}
            mb="sm"
          />
          <TextInput
            {...form.getInputProps("confirmation")}
            data-testid={"Confirmation field"}
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

export default EditLeaderboardForm;
