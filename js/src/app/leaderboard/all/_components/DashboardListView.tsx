import CodebloomCard from "@/components/ui/CodebloomCard";
import CustomPagination from "@/components/ui/table/CustomPagination";
import SearchBox from "@/components/ui/table/SearchBox";
import ToastWithRedirect from "@/components/ui/toast/ToastWithRedirect";
import { useAllLeaderboardsMetadataQuery } from "@/lib/api/queries/leaderboard";
import { timeDiff } from "@/lib/timeDiff";
import {
  Box,
  Button,
  Center,
  Flex,
  Loader,
  Overlay,
  Table,
  Title,
  Tooltip,
} from "@mantine/core";
import { FaArrowLeft, FaArrowRight } from "react-icons/fa";
import { Link } from "react-router";

export default function DashboardListView() {
  const {
    data,
    status,
    goTo,
    page,
    goBack,
    goForward,
    setSearchQuery,
    searchQuery,
    isPlaceholderData,
  } = useAllLeaderboardsMetadataQuery({});

  if (status === "pending") {
    return (
      <Box>
        <CodebloomCard>
          <Flex
            direction={"row"}
            justify={"center"}
            align={"center"}
            w={"100%"}
            h={"100%"}
          >
            <Loader />
          </Flex>
        </CodebloomCard>
      </Box>
    );
  }

  if (status === "error") {
    return (
      <ToastWithRedirect
        to="/"
        message="Sorry, something went wrong. Please try again later."
      />
    );
  }

  if (!data.success) {
    return <>{data.message}</>;
  }

  const pageData = data.payload;

  return (
    <Box>
      <CodebloomCard>
        <Center>
          <Title order={3}>All Leaderboards</Title>
        </Center>
        <SearchBox
          query={searchQuery}
          onChange={(event) => {
            setSearchQuery(event.currentTarget.value);
          }}
          placeholder={"Search by leaderboard name"}
        />
        <Box style={{ overflowX: "auto" }} maw={"100%"} miw={"66%"}>
          <Table
            verticalSpacing={"lg"}
            horizontalSpacing={"xs"}
            withRowBorders={false}
            striped
            my={"sm"}
            pos={"relative"}
          >
            {isPlaceholderData && (
              <Overlay zIndex={1000} backgroundOpacity={0.35} blur={4} />
            )}
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th>Created</Table.Th>
                <Table.Th>Ended</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {pageData.items.map((leaderboard, index) => {
                const isCurrentLeaderboard = leaderboard.deletedAt === null;

                return (
                  <Table.Tr key={index}>
                    <Table.Td>
                      <Flex
                        direction={"column"}
                        component={Link}
                        to={
                          isCurrentLeaderboard ? "/leaderboard" : (
                            `/leaderboard/${leaderboard.id}`
                          )
                        }
                        className="group"
                      >
                        <span className="transition-all group-hover:text-blue-500 w-max">
                          {leaderboard.name}
                        </span>
                      </Flex>
                    </Table.Td>
                    <Table.Td>
                      <Tooltip
                        events={{ hover: true, focus: true, touch: true }}
                        label={new Date(leaderboard.createdAt).toLocaleString()}
                      >
                        <span>{timeDiff(new Date(leaderboard.createdAt))}</span>
                      </Tooltip>
                    </Table.Td>
                    <Table.Td>
                      {!isCurrentLeaderboard && leaderboard.deletedAt ?
                        <Tooltip
                          label={new Date(
                            leaderboard.deletedAt,
                          ).toLocaleString()}
                          events={{ hover: true, focus: true, touch: true }}
                        >
                          <span>
                            {timeDiff(new Date(leaderboard.deletedAt))}
                          </span>
                        </Tooltip>
                      : "Currently running"}
                    </Table.Td>
                  </Table.Tr>
                );
              })}
            </Table.Tbody>
          </Table>
        </Box>
        <Center my={"sm"}>
          <Flex direction={"row"} gap={"sm"}>
            <Button disabled={page === 1} onClick={goBack} size={"compact-sm"}>
              <FaArrowLeft />
            </Button>
            <CustomPagination
              goTo={goTo}
              pages={pageData.pages}
              currentPage={page}
            />
            <Button
              disabled={!pageData.hasNextPage || page >= pageData.pages}
              onClick={() => {
                if (pageData.hasNextPage || page >= pageData.pages) {
                  goForward();
                }
              }}
              size={"compact-sm"}
            >
              <FaArrowRight />
            </Button>
          </Flex>
        </Center>
      </CodebloomCard>
    </Box>
  );
}
