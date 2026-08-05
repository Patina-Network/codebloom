// THIS MUST BE FIRST. NEVER MOVE THIS.
import "@/patches";
import "@mantine/core/styles.css";
import ReactQueryProvider from "@/lib/queryProvider";
import { router } from "@/lib/router";
import "@mantine/notifications/styles.css";
import { themeOverride } from "@/lib/theme";
import { MantineProvider } from "@mantine/core";
import "@/index.css";
import "@mantine/dates/styles.css";
import { Notifications } from "@mantine/notifications";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import dayjs from "dayjs";
import customParseFormat from "dayjs/plugin/customParseFormat";
import utc from "dayjs/plugin/utc";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router-dom";

dayjs.extend(utc);
dayjs.extend(customParseFormat);

if (import.meta.env.VITE_MOCK === "true") {
  const { launchMockServer } = await import("@/__mock__");
  await launchMockServer();
}

// eslint-disable-next-line @typescript-eslint/no-non-null-assertion
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ReactQueryProvider>
      <MantineProvider theme={themeOverride} forceColorScheme={"dark"}>
        <RouterProvider router={router} />
        <Notifications />
      </MantineProvider>
      <ReactQueryDevtools />
    </ReactQueryProvider>
  </StrictMode>,
);
