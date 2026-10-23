import MetricsChart from "@/app/user/[userId]/_components/UserProfile/MetricsChart/MetricsChart.tsx";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, cleanup, waitFor } from "@testing-library/react";
import { v4 as uuid } from "uuid";

describe("MetricsChart succeeded", () => {
  afterEach(() => {
    cleanup();
  });

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
  });

  it("should render skeleton metrics chart initially", () => {
    const MOCK_USER_ID = uuid();
    renderProviderFn?.(<MetricsChart userId={MOCK_USER_ID} />);
    const element = screen.getByTestId("user-metrics-chart-skeleton");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render AreaChart eventually", async () => {
    const MOCK_USER_ID = uuid();
    renderProviderFn?.(<MetricsChart userId={MOCK_USER_ID} />);
    await waitFor(() => {
      const element = screen.getByTestId("metrics-area-chart");
      expect(element).toBeInTheDocument();
      expect(element).toBeVisible();
    });
  });
});
