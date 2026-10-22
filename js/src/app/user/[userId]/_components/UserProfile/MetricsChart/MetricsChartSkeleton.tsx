import { Flex, Skeleton, Divider } from "@mantine/core";

export default function MetricsChartSkeleton() {
  return (
    <>
      <Flex direction={"column"} align={"center"} gap={"sm"} w={"100%"}>
        <Divider w={"70%"} />
        <Skeleton height={250} width={"70%"} mx={"md"} />
      </Flex>
    </>
  );
}
