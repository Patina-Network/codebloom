import ProblemOfTheDaySkeleton from "@/app/dashboard/_components/ProblemOfTheDay/ProblemOfTheDaySkeleton";
import CodebloomCard from "@/components/ui/CodebloomCard";
import { useFetchPotdQuery } from "@/lib/api/queries/potd";
import { Badge, Button, Center, Flex, Title } from "@mantine/core";
import { Link } from "react-router-dom";

export default function ProblemOfTheDay() {
  const { data, status } = useFetchPotdQuery();

  const renderStatusCard = (message: string) => (
    <CodebloomCard miw={"31vw"} mih={"63vh"}>
      <Flex
        direction={"row"}
        justify={"center"}
        align={"center"}
        w={"100%"}
        h={"100%"}
      >
        <Title order={6} ta={"center"}>
          {message}
        </Title>
      </Flex>
    </CodebloomCard>
  );

  if (status === "pending") {
    return <ProblemOfTheDaySkeleton />;
  }

  if (status === "error") {
    return renderStatusCard(
      "Sorry, something went wrong. Please try again later.",
    );
  }

  if (!data.success) {
    return renderStatusCard(data.message);
  }

  const json = data.payload;

  return (
    <CodebloomCard miw="31vw" mih={{ base: "auto", md: "63vh" }}>
      <Center>
        <Title style={{ textAlign: "center" }} order={3}>
          Problem of the day
        </Title>
      </Center>
      <Center>
        <Title order={6}> POTD resets at 8:00 EDT everyday</Title>
      </Center>
      <Flex
        direction={"column"}
        gap={"sm"}
        align={"center"}
        justify={"center"}
        w={"100%"}
        h={"100%"}
      >
        <Title style={{ textAlign: "center" }} order={4}>
          {json.title}
        </Title>
        <Badge
          variant={"gradient"}
          gradient={{ from: "blue.8", to: "green.8" }}
        >
          {json.multiplier}x multiplier
        </Badge>
        <Button
          component={Link}
          to={`https://leetcode.com/problems/${json.slug}`}
          variant={"outline"}
          reloadDocument
          target="_blank"
          rel="noopener noreferrer"
        >
          Go to question
        </Button>
      </Flex>
    </CodebloomCard>
  );
}
