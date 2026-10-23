import MetricsChartSkeleton from "@/app/user/[userId]/_components/UserProfile/MetricsChart/MetricsChartSkeleton";
import Paginator from "@/components/ui/table/Paginator";
import Toast from "@/components/ui/toast/Toast";
import { useUserMetricsQuery } from "@/lib/api/queries/user";
import { theme } from "@/lib/theme";
import { AreaChart } from "@mantine/charts";
import { Box, Divider } from "@mantine/core";
import "@mantine/charts/styles.css";
import d from "dayjs";

export default function MetricsChart({ userId }: { readonly userId: string }) {
  const { data, status, page, goBack, goForward, goTo } = useUserMetricsQuery({
    userId,
  });

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

  const userMetrics = data.payload;
  const pageData = userMetrics.items.map((item) => {
    return {
      ...item,
      createdAt: d(item.createdAt).utc().format("MMM DD, YYYY"),
    };
  });

  return (
    <>
      <Divider w={"70%"} />
      <Box w={"90%"}>
        <AreaChart
          h={250}
          data={pageData}
          dataKey="createdAt"
          data-testId = "metrics-area-chart"
          series={[
            { name: "points", label: "Points", color: theme.colors.patina[4] },
          ]}
          curveType="natural"
          tickLine="none"
          gridAxis="xy"
        />
      </Box>
      <Paginator
        pages={userMetrics?.pages ?? 0}
        currentPage={page}
        hasNextPage={userMetrics?.hasNextPage ?? false}
        goBack={goBack}
        goForward={goForward}
        goTo={goTo}
      />
    </>
  );
}
