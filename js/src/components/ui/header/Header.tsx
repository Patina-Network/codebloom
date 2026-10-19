import AvatarButton from "@/components/ui/auth/AvatarButton";
import SkeletonButton from "@/components/ui/auth/SkeletonButton";
import TransitionalButtons from "@/components/ui/button/transitonal/TransitionalButtons";
import HeaderContainer from "@/components/ui/header/container/HeaderContainer";
import { MM } from "@/components/wrapper";
import { useAuthQuery } from "@/lib/api/queries/auth";
import {
  Box,
  Burger,
  Button,
  Drawer,
  Flex,
  Group,
  Text,
  Title,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import { motion } from "motion/react";
import { Link } from "react-router";

const navButtons = [
  { to: "/", label: "Home" },
  { to: "/dashboard", label: "Dashboard" },
  { to: "/leaderboard", label: "Leaderboard" },
  { to: "/user/all", label: "Search" },
];

export default function Header() {
  const { data, status } = useAuthQuery();
  const [drawerOpened, { toggle: toggleDrawer, close: closeDrawer }] =
    useDisclosure(false);

  const renderButton = (className?: string) => {
    if (status === "pending") {
      return <SkeletonButton />;
    }

    if (status === "error") {
      return <Text c="red">Sorry, something went wrong.</Text>;
    }

    if (data && data.user && data.session) {
      const profileUrl = data.user.profileUrl;
      const initial =
        data.user.nickname ? data.user.nickname.charAt(0).toUpperCase() : "?";
      return (
        <AvatarButton
          src={profileUrl ?? ""}
          initial={initial}
          userId={data.user.id}
        />
      );
    }

    return (
      <Link to="/login" className={className}>
        <Button className={className}>Login</Button>
      </Link>
    );
  };

  return (
    <>
      <HeaderContainer>
        {({ logoSize, textOpacity, textWidth, fontSize }) => (
          <>
            <Link to="/">
              <Group>
                <motion.img
                  src={"/logo.png"}
                  style={{ width: logoSize, height: logoSize }}
                  alt="Logo"
                />
                <MM.Box
                  style={{
                    opacity: textOpacity,
                    maxWidth: textWidth,
                    overflow: "hidden",
                  }}
                >
                  <Title>
                    <MM.Text
                      gradient={{ from: "patina.4", to: "patina.8" }}
                      variant="gradient"
                      size="lg"
                      style={{
                        fontSize,
                      }}
                    >
                      CodeBloom
                    </MM.Text>
                  </Title>
                </MM.Box>
              </Group>
            </Link>
            <Box visibleFrom="sm">
              <TransitionalButtons buttons={navButtons} />
            </Box>
            <Group visibleFrom="sm">{renderButton()}</Group>
            <Burger
              opened={drawerOpened}
              onClick={toggleDrawer}
              hiddenFrom="sm"
              aria-label={"Menu button"}
            />
          </>
        )}
      </HeaderContainer>
      <Drawer
        opened={drawerOpened}
        onClose={closeDrawer}
        withCloseButton={false}
        size="50%"
        title="Navigation"
        styles={{
          header: {
            justifyContent: "center",
          },
          title: {
            textAlign: "center",
            fontWeight: 700,
            fontSize: "var(--mantine-font-size-lg)",
            marginBottom: "var(--mantine-spacing-xs)",
          },
        }}
      >
        <Flex direction="column" align="center" gap="xs" w="100%">
          {navButtons.map(({ to, label }) => (
            <Button
              component={Link}
              to={to}
              size={"compact-md"}
              variant="transparent"
              fullWidth
              key={to}
              onClick={closeDrawer}
            >
              {label}
            </Button>
          ))}
          <Flex justify="center" w="100%" mt="xs">
            {renderButton("full-width")}
          </Flex>
        </Flex>
      </Drawer>
    </>
  );
}
