import CodebloomCard from "@/components/ui/CodebloomCard";
import { Box, Flex, SimpleGrid, Skeleton, Stack } from "@mantine/core";

export default function SubmissionDetailsContentSkeleton() {
  return (
    <Box maw={1100} mx="auto" p="lg">
      <Stack gap="lg">
        <CodebloomCard padding="lg" radius="lg">
          <Stack gap="md">
            <Flex justify="space-between" align="flex-start" gap="md">
              <Stack gap={6} flex={1}>
                <Skeleton
                  w="60%"
                  h="2rem"
                  data-testid="submission-skeleton-question-title"
                />
                <Skeleton
                  w="14rem"
                  h="1.25rem"
                  data-testid="submission-skeleton-question-solved-by"
                />
              </Stack>
              <Skeleton
                w="10rem"
                h="2.25rem"
                radius="md"
                data-testid="submission-skeleton-question-link-button"
              />
            </Flex>
            <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="sm">
              <Skeleton
                h="4rem"
                radius="md"
                data-testid="submission-skeleton-question-difficulty"
              />
              <Skeleton
                h="4rem"
                radius="md"
                data-testid="submission-skeleton-question-acceptance-rate"
              />
              <Skeleton
                h="4rem"
                radius="md"
                data-testid="submission-skeleton-question-points"
              />
              <Skeleton h="4rem" radius="md" />
            </SimpleGrid>
          </Stack>
        </CodebloomCard>
        <CodebloomCard
          padding="lg"
          radius="lg"
          data-testid="submission-skeleton-question-details"
        >
          <Skeleton w="6rem" h="1.5rem" mb="md" />
          <Skeleton w="100%" h="1rem" mb="xs" />
          <Skeleton w="90%" h="1rem" mb="xs" />
          <Skeleton w="95%" h="1rem" mb="md" />
          <Skeleton w="100%" h="6rem" />
        </CodebloomCard>
        <CodebloomCard
          padding="lg"
          radius="lg"
          data-testid="submission-skeleton-user-solution"
        >
          <Flex justify="space-between" align="center" mb="md">
            <Skeleton w="8rem" h="1.75rem" />
            <Skeleton w="16rem" h="1.25rem" />
          </Flex>
          <Skeleton w="100%" h="20rem" />
        </CodebloomCard>
      </Stack>
    </Box>
  );
}
