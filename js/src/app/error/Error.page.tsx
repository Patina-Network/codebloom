import { Illustration } from "@/app/error/IIllustration";
import DocumentDescription from "@/components/ui/title/DocumentDescription";
import DocumentTitle from "@/components/ui/title/DocumentTitle";
import { useReporter } from "@/lib/reporter";
import { CustomErrorResponse } from "@/lib/reporter/types";
import { Box, Button, Flex, Group, Text, Title } from "@mantine/core";
import { isRouteErrorResponse, Link, useRouteError } from "react-router";

export default function ErrorPage() {
  const unknownError = useRouteError();
  const { err } = useReporter();

  // fallback
  if (!isRouteErrorResponse(unknownError)) {
    err(`Something went wrong and we could not determine the shape of the resulting error object.

Error, attempting to convert to string: ${String(unknownError)}
    `);

    return (
      <Flex p={"160px"} justify={"center"} h={"100vh"}>
        <Flex pos={"relative"} align={"center"}>
          <Box pos={"absolute"} inset={0} opacity={0.75} c={"dark"} />
          <Flex
            style={{ textAlign: "center" }}
            pos={"relative"}
            gap={"md"}
            direction={"column"}
          >
            <Title ta={"center"}>Womp Womp.</Title>
            <Flex direction={"column"} wrap={"wrap"} align={"center"}>
              <Text c="dimmed" size="lg" ta="center">
                Something went wrong. The developers have been notified; you may
                try again now or at another time.
              </Text>
            </Flex>
            <Group justify="center">
              <Button size="md" component={Link} to={"/"}>
                Go back to the home page
              </Button>
            </Group>
          </Flex>
        </Flex>
      </Flex>
    );
  }

  const error = unknownError as CustomErrorResponse | string | undefined;

  if (!error || typeof error === "string") {
    err(
      error ??
        `Something went wrong and we have no stack trace to tell you why.`,
    );
    return (
      <Flex p={"160px"} justify={"center"} h={"100vh"}>
        <Flex pos={"relative"} align={"center"}>
          <Box pos={"absolute"} inset={0} opacity={0.75} c={"dark"} />
          <Flex
            style={{ textAlign: "center" }}
            pos={"relative"}
            gap={"md"}
            direction={"column"}
          >
            <Title ta={"center"}>Womp Womp.</Title>
            <Flex direction={"column"} wrap={"wrap"} align={"center"}>
              <Text c="dimmed" size="lg" ta="center">
                Something went wrong. The developers have been notified; you may
                try again now or at another time.
              </Text>
            </Flex>
            <Group justify="center">
              <Button size="md" component={Link} to={"/"}>
                Go back to the home page
              </Button>
            </Group>
          </Flex>
        </Flex>
      </Flex>
    );
  }

  // 4xx errors are not reported
  if (error.status.toString().startsWith("4")) {
    return (
      <Flex p={"160px"} justify={"center"} h={"100vh"}>
        <Flex pos={"relative"} align={"center"}>
          <Box
            pos={"absolute"}
            inset={0}
            opacity={0.75}
            c={"dark"}
            component={Illustration}
          />
          <Flex
            style={{ textAlign: "center" }}
            pos={"relative"}
            gap={"md"}
            direction={"column"}
          >
            <Title ta={"center"}>Womp Womp.</Title>
            <Flex direction={"column"} wrap={"wrap"} align={"center"}>
              <Text c="dimmed" size="lg" ta="center">
                Unfortunately, this is only a 404 page. You may have mistyped
                the address, or the page has been moved to another URL.
              </Text>
            </Flex>
            <Group justify="center">
              <Button size="md" component={Link} to={"/"}>
                Go back to the home page
              </Button>
            </Group>
          </Flex>
        </Flex>
      </Flex>
    );
  }

  err(
    error?.error?.stack ??
      `Something went wrong and we could not determine the shape of the resulting error object.

Error, attempting to convert to string: ${String(error)}
    `,
  );

  // This should, in theory, catch all other runtime errors.
  return (
    <>
      <DocumentTitle title={`CodeBloom - 404 Page`} />
      <DocumentDescription description={`CodeBloom - 404 Error Page`} />
      <Flex p={"160px"} justify={"center"} h={"100vh"}>
        <Flex pos={"relative"} align={"center"}>
          <Box pos={"absolute"} inset={0} opacity={0.75} c={"dark"} />
          <Flex
            style={{ textAlign: "center" }}
            pos={"relative"}
            gap={"md"}
            direction={"column"}
          >
            <Title ta={"center"}>Womp Womp.</Title>
            <Flex direction={"column"} wrap={"wrap"} align={"center"}>
              <Text c="dimmed" size="lg" ta="center">
                Something went wrong. The developers have been notified; you may
                try again now or at another time.
              </Text>
            </Flex>
          </Flex>
        </Flex>
      </Flex>
    </>
  );
}
