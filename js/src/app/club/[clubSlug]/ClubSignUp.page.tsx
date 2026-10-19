import ClubSignUpForm from "@/app/club/[clubSlug]/_components/ClubSignUpForm";
import Toast from "@/components/ui/toast/Toast";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAuthQuery } from "@/lib/api/queries/auth";
import { Box, Loader } from "@mantine/core";
import { useParams } from "react-router";

export default function ClubSignupPage() {
  const { data, status } = useAuthQuery();
  const { clubSlug } = useParams<{ clubSlug: string }>();

  if (status === "pending") {
    return <Loader />;
  }
  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong. Please try again later." />
    );
  }

  const authenticated = !!data.user && !!data.session;
  if (!authenticated) {
    return (
      <ToastWithRedirect to="/login" message="You are not authenticated!" />
    );
  }

  // Potentially return a club list page in the future
  if (clubSlug === undefined) {
    return <ToastWithRedirect to="/" message="Club Slug missing!" />;
  }

  return (
    <Box
      style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <Box>
        <ClubSignUpForm
          userId={data.user.id}
          userTags={data.user.tags}
          clubSlug={clubSlug}
        />
      </Box>
    </Box>
  );
}
