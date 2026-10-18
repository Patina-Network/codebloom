import MetricsChartSkeleton from "@/app/user/[userId]/_components/UserProfile/MetricsChart/MetricsChartSkeleton";
import Toast from "@/components/ui/toast/Toast";
import { useUserProfileQuery } from "@/lib/api/queries/user";

export default function MetricsChart({ userId }: { userId: string }) {
  const { data, status } = useUserProfileQuery({ userId });

  if (status === "pending") {
    return <MetricsChartSkeleton />;
  }

  if (status === "error") {
    return (
      <Toast message="Sorry, something went wrong. Please try again later." />
    );
  }

  if (!data.success) {
    return <Toast message={data.message} />;
  }
}
