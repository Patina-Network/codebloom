import { useVerifySchoolMutation } from "@/lib/api/queries/auth/school";
import { schoolVerificationFormSchema } from "@/lib/api/schema/school";
import {
  Alert,
  Box,
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
      size={"lg"}
      title="Verify your student email"
    >
      <Box p={"lg"}>
        <Text p="md">
          Verify your student email to gain access to school-specific
          competitions!
        </Text>
        <Alert title="CUNY students" color="gray" mx="md" mb="md">
          <Stack gap="xs" style={{ overflowWrap: "anywhere" }}>
            <Text size="sm">
              Use your college email alias, such as
              firstname.lastname##@stu-mail.hunter.cuny.edu for Hunter College.
              Your @login.cuny.edu address does not identify your college.
            </Text>
            <Text size="sm">
              Find your alias in CUNYfirst under email addresses. After
              submitting, sign in to Outlook with your @login.cuny.edu login and
              open the verification link while signed in to Codebloom.
            </Text>
          </Stack>
        </Alert>
        <form onSubmit={form.onSubmit(onSubmit)}>
          <TextInput
            {...form.getInputProps("email")}
            label="Student email"
            type="email"
            autoComplete="email"
            placeholder="Enter your school email"
            error={form.errors.email}
            p="md"
          />
          <Button
            type="submit"
            size="xs"
            ml="md"
            disabled={!form.isValid("email") || status === "pending"}
            loading={status === "pending"}
          >
            Submit
          </Button>
        </form>
      </Box>
    </Modal>
  );
}
