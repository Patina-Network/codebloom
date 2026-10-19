import RecentSubmissionsSkeleton from "@/app/dashboard/_components/RecentSubmissions/RecentSubmissionsSkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import { useUserSubmissionsQuery } from "@/lib/api/queries/user";
import { Badge, Box, Button, Center, Flex, Text, Title } from "@mantine/core";
import { Link } from "react-router";

export default function RecentSubmissions({ userId }: { userId: string }) {
  const { data, status } = useUserSubmissionsQuery({ userId });

  if (status === "pending") {
    return <RecentSubmissionsSkeleton />;
  }

  if (status === "error") {
    return (
      <CodebloomCard miw={"31vw"} mih={"63vh"}>
        <Flex
          direction={"row"}
          justify={"center"}
          align={"center"}
          w={"100%"}
          h={"100%"}
        >
          <Title order={6} ta={"center"}>
            Sorry, something went wrong. Please try again.
          </Title>
        </Flex>
      </CodebloomCard>
    );
  }

  if (!data.success) {
    return (
      <CodebloomCard miw={"31vw"} mih={"63vh"}>
        <Flex
          direction={"row"}
          justify={"center"}
          align={"center"}
          w={"100%"}
          h={"100%"}
        >
          <Title order={6} ta={"center"}>
            {data.message}
          </Title>
        </Flex>
      </CodebloomCard>
    );
  }

  const questions = data.payload.items;
  const dashboardQuestions = questions.slice(0, 5);

  if (!questions.length) {
    return (
      <CodebloomCard miw={"31vw"} mih={"63vh"}>
        <Flex
          direction={"row"}
          justify={"center"}
          align={"center"}
          w={"100%"}
          h={"100%"}
        >
          <Title order={6} ta={"center"}>
            Oops! No problems solved yet—tackle one to start racking up points!
          </Title>
        </Flex>
      </CodebloomCard>
    );
  }

  return (
    <CodebloomCard miw={"31vw"} mih={"63vh"}>
      <Flex
        direction={{ base: "column", md: "row" }}
        justify={"space-between"}
        w={"100%"}
      >
        <Center>
          <Title order={4} className="text-center" pb={"sm"}>
            Submissions
          </Title>
        </Center>
        <Button variant={"light"} component={Link} to={`/user/${userId}`}>
          View all
        </Button>
      </Flex>
      {dashboardQuestions.map((q, idx) => {
        const badgeColor = (() => {
          if (q.questionDifficulty === "Easy") {
            return undefined;
          }
          if (q.questionDifficulty === "Medium") {
            return "yellow";
          }
          if (q.questionDifficulty === "Hard") {
            return "red";
          }
          return undefined;
        })();

        return (
          <Flex
            component={Link}
            to={`/submission/${q.id}`}
            key={idx}
            direction={"row"}
            justify={"space-between"}
            style={{
              borderRadius: "4px",
            }}
            p={"lg"}
            className="group"
          >
            <Text className="transition-all group-hover:!text-blue-500">
              {idx + 1}.
            </Text>
            <Flex direction={"column"}>
              <Text
                ta="center"
                className="transition-all group-hover:!text-blue-500"
              >
                {q.questionTitle}
              </Text>
              <Box ta={"center"}>
                <Badge
                  ta="center"
                  color={badgeColor}
                  className="transition-all group-hover:!bg-blue-500"
                >
                  {q.questionDifficulty}
                </Badge>
              </Box>
            </Flex>
            <Text
              ta={"center"}
              className="transition-all group-hover:!text-blue-500"
            >
              +{q.pointsAwarded} points
            </Text>
          </Flex>
        );
      })}
    </CodebloomCard>
  );
}
