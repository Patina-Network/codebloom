import { schoolFF } from "@/lib/ff";
import { Menu, Avatar } from "@mantine/core";
import { Link } from "react-router";

const DEFAULT_AVATAR_URL =
  "https://assets.leetcode.com/users/default_avatar.jpg";

export default function AvatarDropdown({
  src,
  initial,
  userId,
}: {
  src: string;
  initial?: string;
  userId: string;
}) {
  const showInitial = !src || src === DEFAULT_AVATAR_URL;

  return (
    <Menu shadow="md" width={"xl"}>
      <Menu.Target>
        {showInitial ?
          <Avatar component="button" color="green" size={"2.75rem"}>
            {initial}
          </Avatar>
        : <Avatar component="button" src={src} size={"2.75rem"} />}
      </Menu.Target>
      <Menu.Dropdown>
        {schoolFF && (
          <>
            {userId && (
              <Menu.Item component={Link} to={`/user/${userId}`}>
                My Profile
              </Menu.Item>
            )}
            <Menu.Item component={Link} to={"/settings"}>
              Settings
            </Menu.Item>
            <Menu.Divider />
          </>
        )}
        <Menu.Item
          component={Link}
          to={"/api/auth/logout"}
          reloadDocument
          color="red"
        >
          Logout
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
