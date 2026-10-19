import ClubSignUpSkeleton from "@/app/club/[clubSlug]/_components/ClubSignUpSkeleton";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import {
  useClubQuery,
  useVerifyPasswordMutation,
} from "@/lib/api/queries/club";
import { clubVerificationFormSchema } from "@/lib/api/schema/club";
import { Api } from "@/lib/api/types";
import {
  Button,
  Center,
  Stack,
  Text,
  Card,
  Title,
  TextInput,
  Image,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import { notifications } from "@mantine/notifications";
import { zod4Resolver } from "mantine-form-zod-resolver";
import { useState } from "react";
import { Link } from "react-router";
import z from "zod";

type ClubSignUpProps = {
  userId: string;
  userTags: Api<"UserTag">[];
  clubSlug: string;
};

export default function ClubSignUpForm({
  userId,
  userTags,
  clubSlug,
}: ClubSignUpProps) {
  const { data, status } = useClubQuery({
    clubSlug,
  });

  const { mutate } = useVerifyPasswordMutation(clubSlug);
  const [imgError, setImgError] = useState(false);

  const form = useForm({
    validate: zod4Resolver(clubVerificationFormSchema),
    initialValues: {
      password: "",
    },
  });

  const onSubmit = (values: z.infer<typeof clubVerificationFormSchema>) => {
    const id = notifications.show({
      message: "Verifying password... ",
      color: "blue",
    });
    mutate(
      {
        userId: userId,
        password: values.password,
        clubSlug: clubSlug,
      },
      {
        onSuccess: async (data) => {
          notifications.update({
            id,
            message: data.message,
            color: data.success ? undefined : "red",
          });

          if (data.success) {
            form.reset();
          }
        },
      },
    );
  };

  if (status === "pending") {
    return <ClubSignUpSkeleton />;
  }

  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong. Please try again later." />
    );
  }

  if (!data.success) {
    return <ToastWithRedirect to="/" message={data.message} />;
  }

  const club = data.payload;
  const clubTag = data.payload.tag;

  const hasTag = userTags.some(
    ({ tag }) => tag.toString() === clubTag.toString(),
  );

  return (
    <Center style={{ minHeight: "70vh", padding: 16 }}>
      <Card
        shadow="sm"
        radius="lg"
        p="xl"
        withBorder
        miw={360}
        maw={480}
        w="100%"
      >
        <Stack gap="md">
          {club.splashIconUrl && !imgError && (
            <Image
              src={club.splashIconUrl}
              alt=""
              radius="md"
              w={160}
              maw={200}
              fit="contain"
              mx="auto"
              onError={() => setImgError(true)}
            />
          )}
          {!hasTag ?
            <>
              <Title order={2} ta="center">
                Register for {club.name ?? "this club"}
              </Title>
              <Text c="dimmed" ta="center">
                Enter the club password to continue.
              </Text>
              <form onSubmit={form.onSubmit(onSubmit)}>
                <TextInput
                  {...form.getInputProps("password")}
                  placeholder="Enter the club password"
                  error={form.errors.email}
                  pb="md"
                />
                <Button type="submit" size="xs" disabled={hasTag}>
                  Submit
                </Button>
              </form>
            </>
          : <>
              <Title order={2} ta="center">
                You already are verified for {club.name ?? "this club"}!
              </Title>
              <Button
                component={Link}
                to="/dashboard"
                radius="md"
                fullWidth
                mt="md"
              >
                Go to Dashboard
              </Button>
            </>
          }
        </Stack>
      </Card>
    </Center>
  );
}
