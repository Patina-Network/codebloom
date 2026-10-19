import MiniLeaderboard from "@/app/_component/MiniLeaderboard";
import MiniLeaderboardMobile from "@/app/_component/MiniLeaderboardMobile";
import OurFeatures from "@/app/_component/OurFeatures";
import SchoolSection from "@/app/_component/SchoolSection";
import { CurrentLeaderboardMetadata } from "@/app/leaderboard/_components/LeaderboardMetadata/LeaderboardMetadata";
import {
  Badge,
  Box,
  Button,
  Center,
  Container,
  Flex,
  Group,
  Stack,
  Title,
} from "@mantine/core";
import { useRef } from "react";
import { Link } from "react-router";

export default function AboutUs() {
  const targetSectionRef = useRef<HTMLDivElement>(null);

  const scrollToSection = () => {
    if (targetSectionRef.current) {
      targetSectionRef.current.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    }
  };

  return (
    <Box mx="-lg">
      <Flex direction={"row"} h={"92vh"} w={"100%"} visibleFrom="lg">
        <Flex
          align={"center"}
          justify={"center"}
          h={"100%"}
          direction={"column"}
          w={"50%"}
        >
          <Title order={3} style={{ color: "#4cffb0", alignItems: "center" }}>
            Level Up Your Coding with
          </Title>
          <Title order={3}>Patina's LeetCode Challenge!</Title>
          <Box p="xs">
            <Center>
              <Group gap="sm">
                <Button variant="white" onClick={scrollToSection}>
                  Learn More
                </Button>
                <Link to="/login">
                  <Button>Get Started</Button>
                </Link>
              </Group>
            </Center>
          </Box>
        </Flex>
        <Flex
          align={"center"}
          justify={"center"}
          h={"100%"}
          direction={"column"}
          w={"50%"}
        >
          <Box pos={"relative"} p={"xs"}>
            <CurrentLeaderboardMetadata showClock syntaxStripSize={"md"} />
            <MiniLeaderboard />
          </Box>
        </Flex>
      </Flex>
      <Flex h={"80vh"} w={"100%"} hiddenFrom="lg">
        <Stack align={"center"} justify={"center"} h={"100%"} w={"100%"}>
          <Badge
            variant={"gradient"}
            gradient={{ from: "green", to: "cyan", deg: 90 }}
            size={"sm"}
            m={"md"}
          >
            Celebrating CodeBloom's 1 Year Anniversary! 🎉
          </Badge>
          <Title order={5} style={{ color: "#4cffb0" }}>
            Level Up Your Coding with
          </Title>
          <Title order={5}>Patina's LeetCode Challenge!</Title>
          <Box p="1rem">
            <Center>
              <Group gap="sm">
                <Button variant="white" onClick={scrollToSection}>
                  Learn More
                </Button>
                <Link to="/login">
                  <Button>Get Started</Button>
                </Link>
              </Group>
            </Center>
          </Box>
        </Stack>
      </Flex>
      <Container hiddenFrom={"lg"}>
        <CurrentLeaderboardMetadata showClock syntaxStripSize={"lg"} />
        <MiniLeaderboardMobile />
      </Container>
      <Box px="lg">
        <SchoolSection />
      </Box>
      <Box p="2rem" ref={targetSectionRef}>
        <OurFeatures />
      </Box>
    </Box>
  );
}
