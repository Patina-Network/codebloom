import { themeOverride } from "@/lib/theme";
import {
  createTheme,
  MantineProvider,
  mergeThemeOverrides,
  Modal,
} from "@mantine/core";
import { Notifications } from "@mantine/notifications";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, RenderResult } from "@testing-library/react";
import { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";

/**
 * Some Mantine components like Modals use Transition components to render an animation.
 * RTL does not wait for the transition to finish before querying for elements.
 * This test theme override is needed to set the duration of these transitions to 0.
 * See https://help.mantine.dev/q/portals-testing
 */
const testTheme = mergeThemeOverrides(
  themeOverride,
  createTheme({
    components: {
      Modal: Modal.extend({
        defaultProps: {
          transitionProps: {
            duration: 0,
          },
        },
      }),
    },
  }),
);

export namespace TestUtilTypes {
  export type RenderWithAllProvidersFn = (
    ui: ReactElement,
    initialPath?: string,
  ) => RenderResult<
    typeof import("@testing-library/dom/types/queries"),
    HTMLElement,
    HTMLElement
  >;
}

export class TestUtils {
  static getRenderWithAllProvidersFn(): TestUtilTypes.RenderWithAllProvidersFn {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    return (ui: ReactElement, initialPath?: string) => {
      return render(
        <QueryClientProvider client={queryClient}>
          <MantineProvider theme={testTheme} forceColorScheme={"dark"}>
            <MemoryRouter
              initialEntries={initialPath ? [initialPath] : undefined}
            >
              {ui}
            </MemoryRouter>
            <Notifications />
          </MantineProvider>
        </QueryClientProvider>,
      );
    };
  }
}
