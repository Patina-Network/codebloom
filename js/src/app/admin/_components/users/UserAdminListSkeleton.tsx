import { Box, Flex, Skeleton } from "@mantine/core";

export default function UserAdminListSkeleton() {
  return (
    <Box p={"xs"} data-testId={"user-admin-list-skeleton-board"}>
      <Skeleton height={35} width="20%" mb="md" mt="md" ml="auto" />
      <Skeleton height={35} width="100%" mb="md" mt="md" />
      <Skeleton height={50} mb="sm" radius="md" />
      {[...Array(5)].map((_, i) => (
        <Skeleton key={i} height={60} mb="sm" radius="md">
          <Box p={"md"} />
        </Skeleton>
      ))}
      <Flex p="xl" justify="center">
        {[...Array(8)].map((_, i) => (
          <Skeleton key={i} height={30} width={30} mr={8} />
        ))}
      </Flex>
    </Box>
  );
}
