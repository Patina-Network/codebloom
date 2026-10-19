import { useFetchPotdEmbedQuery } from "@/lib/api/queries/embed";
import {
  Badge,
  Button,
  Card,
  Center,
  Flex,
  Loader,
  Title,
} from "@mantine/core";
import { Link } from "react-router";

export default function PotdEmbedView() {
  const { data, status } = useFetchPotdEmbedQuery();

  if (status === "pending") {
    return (
      <Card
        withBorder
        radius="md"
        p="md"
        w="100%"
        h="100vh"
        display="flex"
        style={{
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <Flex
          direction={"row"}
          justify={"center"}
          align={"center"}
          w={"100%"}
          h={"100%"}
        >
          <Loader />
        </Flex>
      </Card>
    );
  }

  if (status === "error") {
    return (
      <Card
        withBorder
        radius="md"
        p="md"
        w="100%"
        h="100vh"
        display="flex"
        style={{
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <Flex
          direction={"row"}
          justify={"center"}
          align={"center"}
          w={"100%"}
          h={"100%"}
        >
          <Title order={6} ta={"center"}>
            Sorry, something went wrong. Please try again later.
          </Title>
        </Flex>
      </Card>
    );
  }

  if (!data.success) {
    return (
      <Card
        withBorder
        radius="md"
        p="md"
        w="100%"
        h="100vh"
        display="flex"
        style={{
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
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
      </Card>
    );
  }

  const json = data.payload;

  return (
    <Card
      withBorder
      radius="md"
      p="md"
      w="100%"
      h="100vh"
      display="flex"
      style={{
        flexDirection: "column",
        justifyContent: "center",
      }}
    >
      <Flex direction={"column"} gap={"xl"} m={"xl"}>
        <Flex direction={"column"} gap={"sm"} align={"center"}>
          <Title style={{ textAlign: "center" }} order={3}>
            Problem of the day
          </Title>
          <Title order={6}> POTD resets at 8:00 ET everyday</Title>
        </Flex>
        <Flex direction={"column"} gap={"sm"} align={"center"}>
          <Title style={{ textAlign: "center" }} order={4}>
            {json.title}
          </Title>
          <Badge
            variant={"gradient"}
            gradient={{ from: "blue.8", to: "green.8" }}
          >
            {json.multiplier}x multiplier
          </Badge>
        </Flex>
        <Center>
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
        </Center>
      </Flex>
    </Card>
  );
}
