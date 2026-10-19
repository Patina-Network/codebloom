import SubmissionDetailsContent from "@/app/submission/[submissionId]/_components/SubmissionDetailsContent";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useParams } from "react-router";

export default function SubmissionDetailsPage() {
  const { submissionId } = useParams();

  if (!submissionId) {
    return <ToastWithRedirect to={-1} message={"Invalid submission ID."} />;
  }

  return <SubmissionDetailsContent submissionId={submissionId} />;
}
