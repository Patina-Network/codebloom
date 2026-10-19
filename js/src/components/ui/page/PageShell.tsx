import BannerParent from "@/components/ui/banner/BannerParent";
import { Footer } from "@/components/ui/footer/Footer";
import Header from "@/components/ui/header/Header";
import { Flex, Box } from "@mantine/core";
import { ReactNode } from "react";
import { useLocation } from "react-router";

export default function PageShell({
  children,
  hideHeader = false,
  hideFooter = false,
}: {
  children: ReactNode;
  hideHeader?: boolean;
  hideFooter?: boolean;
}) {
  const location = useLocation();
  const embedPath = location.pathname.startsWith("/embed");

  return (
    <Flex direction={"column"} mih={"100vh"}>
      {!embedPath && <BannerParent />}
      {!hideHeader && <Header />}
      <Box p={embedPath ? undefined : "lg"} flex={1}>
        {children}
      </Box>
      {!hideFooter && <Footer />}
    </Flex>
  );
}
