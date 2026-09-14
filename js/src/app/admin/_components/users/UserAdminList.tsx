import UserAdminListSkeleton from "@/app/admin/_components/users/UserAdminListSkeleton";
import Paginator from "@/components/ui/table/Paginator";
import SearchBox from "@/components/ui/table/SearchBox";
import { useToggleAdminMutation } from "@/lib/api/queries/admin";
import { useGetAllUsersQuery } from "@/lib/api/queries/user";
import { theme } from "@/lib/theme";
import { Box, Button, Overlay, Table, Text, Tooltip } from "@mantine/core";
import { notifications } from "@mantine/notifications";
import { IconCircleCheckFilled } from "@tabler/icons-react";
import { FaDiscord } from "react-icons/fa";

/**
 * This function renders a list of users of which the toggle button launches a modal that allows you to
 * modify the user's `Admin` status.
 */
export default function UserAdminList() {
  const {
    data,
    status,
    page,
    goBack,
    goForward,
    isPlaceholderData,
    goTo,
    searchQuery,
    setSearchQuery,
    debouncedQuery,
  } = useGetAllUsersQuery();

  const { mutate } = useToggleAdminMutation();

  if (status === "pending") {
    return <UserAdminListSkeleton />;
  }

  if (status === "error") {
    return <div>Error</div>;
  }

  if (!data.success) {
    return <div>{data.message}</div>;
  }

  const pageData = data.payload;

  // Sort users so admins appear first
  const sortedItems = [...pageData.items].sort((a, b) => {
    if (a.admin === b.admin) return 0;
    return a.admin ? -1 : 1;
  });

  const onToggle = (userId: string, currentAdminStatus: boolean) => {
    // Check the mutate function origin on why we
    // need this metadata object as well.
    mutate(
      {
        userId,
        toggleTo: !currentAdminStatus,
        metadata: {
          page,
          debouncedQuery,
        },
      },
      {
        onSuccess: (data) => {
          if (!data.success) {
            return notifications.show({
              color: "red",
              message: data.message,
            });
          }

          notifications.show({
            color: data.payload.admin ? undefined : "red",
            message: data.message,
          });
        },
      },
    );
  };

  return (
    <>
      <SearchBox
        query={searchQuery}
        onChange={(event) => {
          setSearchQuery(event.currentTarget.value);
        }}
        placeholder={"Search for user"}
        m={"lg"}
        mb={0}
      />
      <Box style={{ overflowX: "auto" }} m={"lg"} mt={0}>
        <Table
          verticalSpacing={"lg"}
          horizontalSpacing={"xs"}
          withRowBorders={false}
          striped
          pos={"relative"}
        >
          {isPlaceholderData && (
            <Overlay zIndex={1000} backgroundOpacity={0.35} blur={4} />
          )}
          <Table.Thead>
            <Table.Tr>
              <Table.Th ta={"center"}>Discord</Table.Th>
              <Table.Th ta={"center"}>Leetcode</Table.Th>
              <Table.Th ta={"center"}>Toggle</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {pageData.items.length == 0 && (
              <Table.Tr>
                <Table.Td colSpan={100}>
                  <Text fw={500} ta="center">
                    Nothing found
                  </Text>
                </Table.Td>
              </Table.Tr>
            )}
            {sortedItems.map((user, index) => {
              const adminBadgeColor = (() => {
                if (user.admin) {
                  return undefined;
                }

                return "gray";
              })();

              return (
                <Table.Tr key={index}>
                  <Table.Td>
                    {user.nickname ?
                      <Tooltip
                        label={
                          "This user is a verified member of the Patina Discord server."
                        }
                      >
                        <Text
                          ta="center"
                          className="transition-all group-hover:text-white-500"
                        >
                          <IconCircleCheckFilled
                            style={{
                              display: "inline",
                            }}
                            color={theme.colors.patina[4]}
                            z={5000000}
                            size={20}
                          />{" "}
                          {user.nickname}
                        </Text>
                      </Tooltip>
                    : <Text
                        ta="center"
                        className="transition-all group-hover:text-white-500"
                      >
                        <FaDiscord
                          style={{
                            display: "inline",
                            marginLeft: "4px",
                            marginRight: "4px",
                          }}
                        />
                        {user.discordName}
                      </Text>
                    }
                  </Table.Td>
                  <Table.Td>{user.leetcodeUsername}</Table.Td>
                  <Table.Td>
                    <Button
                      ta="center"
                      color={adminBadgeColor}
                      onClick={() => onToggle(user.id, user.admin)}
                    >
                      {user.admin ? "✓" : "×"}
                    </Button>
                  </Table.Td>
                </Table.Tr>
              );
            })}
          </Table.Tbody>
        </Table>
      </Box>
      <Paginator
        pages={pageData.pages}
        currentPage={page}
        hasNextPage={pageData.hasNextPage}
        goBack={goBack}
        goForward={goForward}
        goTo={goTo}
      />
    </>
  );
}
