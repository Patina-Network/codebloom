import { useVerifySchoolMutation } from "@/lib/api/queries/auth/school";
import { schoolVerificationFormSchema } from "@/lib/api/schema/school";
import {
  Accordion,
  Group,
  Button,
  Modal,
  Stack,
  Text,
  TextInput,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { zodResolver } from "mantine-form-zod-resolver";
import { z } from "zod";

type SchoolModalProps = {
  enabled: boolean;
  toggle: () => void;
};

export default function SchoolEmailModal({
  enabled,
  toggle,
}: SchoolModalProps) {
  const { mutate, status } = useVerifySchoolMutation();
  const form = useForm({
    validate: zodResolver(schoolVerificationFormSchema),
    initialValues: {
      email: "",
    },
  });

  const onSubmit = (values: z.infer<typeof schoolVerificationFormSchema>) => {
    const id = notifications.show({
      message: "Verifying email... ",
      color: "blue",
    });
    mutate(
      { email: values.email },
      {
        onSuccess: async (data) => {
          notifications.update({
            id,
            message: data.message,
            color: data.success ? undefined : "red",
          });
          if (data.success) {
            form.reset();
            toggle();
          }
        },
      },
    );
  };

  return (
    <Modal
      opened={enabled}
      onClose={toggle}
      size="lg"
      padding="lg"
      title="Verify your student email"
    >
      <form onSubmit={form.onSubmit(onSubmit)}>
        <Stack gap="lg">
          <Text size="sm" c="dimmed">
            Join your school's leaderboard and compete in school-specific
            competitions.
          </Text>
          <TextInput
            {...form.getInputProps("email")}
            label="Student email"
            type="email"
            autoComplete="email"
            placeholder="Enter your school email"
          />
          <Accordion variant="contained" radius="md">
            <Accordion.Item value="cuny">
              <Accordion.Control>
                How do I enroll if I'm a CUNY student?
              </Accordion.Control>
              <Accordion.Panel>
                <Stack gap="sm" style={{ overflowWrap: "anywhere" }}>
                  <Text size="sm">
                    Please use your college email alias: e.g. If you are a
                    student at Hunter College, your email alias will look like:
                    firstname.lastname##@stu-mail.hunter.cuny.edu
                  </Text>
                  <Text size="sm">
                    You can find your alias in CUNYfirst under email addresses.
                    After submitting, sign in to Outlook with your
                    @login.cuny.edu login and open the verification link while
                    signed in to Codebloom.
                  </Text>
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          </Accordion>
          <Group justify="flex-end">
            <Button
              type="submit"
              disabled={!form.isValid("email") || status === "pending"}
              loading={status === "pending"}
            >
              Submit
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
