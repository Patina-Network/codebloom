import MetricsChart from "@/app/user/[userId]/_components/UserProfile/MetricsChart/MetricsChart.tsx";
import { ApiURL } from "@/lib/api/common/apiURL";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { cleanup, screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { v4 as uuid } from "uuid";

const FIXED_USER_ID = "metrics-test-user";

const userMetricsUrl = ApiURL.create("/api/user/{userId}/metrics", {
  method: "GET",
  params: { userId: FIXED_USER_ID },
});

const MOCK_METRICS = [
  {
    id: "metric-1",
    userId: FIXED_USER_ID,
    points: 500,
    createdAt: new Date(2026, 9, 1, 0).toISOString(),
  },
  {
    id: "metric-2",
    userId: FIXED_USER_ID,
    points: 1500,
    createdAt: new Date(2026, 9, 2, 0).toISOString(),
  },
  {
    id: "metric-3",
    userId: FIXED_USER_ID,
    points: 2000,
    createdAt: new Date(2026, 9, 3, 0).toISOString(),
  },
  {
    id: "metric-4",
    userId: FIXED_USER_ID,
    points: 3000,
    createdAt: new Date(2026, 9, 4, 0).toISOString(),
  },
  {
    id: "metric-5",
    userId: FIXED_USER_ID,
    points: 3000,
    createdAt: new Date(2026, 9, 5, 0).toISOString(),
  },
  {
    id: "metric-6",
    userId: FIXED_USER_ID,
    points: 3000,
    createdAt: new Date(2026, 9, 6, 0).toISOString(),
  },
];

const userMetricsSuccessHandler = http.get(userMetricsUrl.url.toString(), () =>
  HttpResponse.json({
    success: true,
    message: "Metrics loaded!",
    payload: {
      hasNextPage: false,
      pages: 1,
      items: MOCK_METRICS,
      success: true,
    },
  }),
);

const userMetricsErrorHandler = http.get(userMetricsUrl.url.toString(), () =>
  HttpResponse.error(),
);

const mockUseUserMetricsQuery = vi.fn();

vi.mock("@/lib/api/queries/user", () => ({
  useUserMetricsQuery: (...args: unknown[]) => mockUseUserMetricsQuery(...args),
}));

const BASE_QUERY_RESULT = {
  data: undefined,
  status: "pending",
  page: 1,
  goBack: vi.fn(),
  goForward: vi.fn(),
  goTo: vi.fn(),
};

describe("MetricsChart succeeded", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    mockUseUserMetricsQuery.mockReturnValue(BASE_QUERY_RESULT);
  });

  it("should render skeleton metrics chart initially", () => {
    const MOCK_USER_ID = uuid();
    renderProviderFn?.(<MetricsChart userId={MOCK_USER_ID} />);
    const element = screen.getByTestId("user-metrics-chart-skeleton");
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should render AreaChart eventually", async () => {
    mockUseUserMetricsQuery.mockReturnValue({
      ...BASE_QUERY_RESULT,
      status: "success",
      data: {
        payload: { items: [], pages: 1, hasNextPage: false },
        success: true,
      },
    });
    const MOCK_USER_ID = uuid();
    renderProviderFn?.(<MetricsChart userId={MOCK_USER_ID} />);
    const element = await screen.findByTestId(/metrics-area-chart.*$/);
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should show data message toast and not render AreaChart when data is not successful", async () => {
    const mockMessage = "Data not success.";
    mockUseUserMetricsQuery.mockReturnValue({
      ...BASE_QUERY_RESULT,
      status: "success",
      data: {
        payload: { items: [], pages: 1, hasNextPage: false },
        success: false,
        message: mockMessage,
      },
    });
    const MOCK_USER_ID = uuid();
    renderProviderFn?.(<MetricsChart userId={MOCK_USER_ID} />);

    await waitFor(() => {
      expect(screen.getByText(mockMessage)).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(
        screen.queryByTestId(/metrics-area-chart.*$/),
      ).not.toBeInTheDocument();
    });
  });
});

describe("UserMetricsChart with successful API", () => {
  const server = setupServer(userMetricsSuccessHandler);

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
    cleanup();
    vi.clearAllMocks();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();

    mockUseUserMetricsQuery.mockReturnValue({
      ...BASE_QUERY_RESULT,
      status: "success",
      data: {
        payload: { items: MOCK_METRICS, pages: 2, hasNextPage: true },
        success: true,
      },
    });
  });

  it("should have formatted dates for chart after successful API call", async () => {
    renderProviderFn?.(<MetricsChart userId={FIXED_USER_ID} />);

    const element = await screen.findByTestId(
      /metrics-area-chart Oct 01, 2026.*$/,
    );
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });

  it("should have correct userMetrics pages and hasNextPage based on data payload", async () => {
    renderProviderFn?.(<MetricsChart userId={FIXED_USER_ID} />);

    const element = await screen.findByTestId(
      "metrics-area-chart Oct 01, 2026 pages2 hasNextPagetrue",
    );
    expect(element).toBeInTheDocument();
    expect(element).toBeVisible();
  });
});

describe("UserMetricsChart error state", () => {
  const server = setupServer(userMetricsErrorHandler);

  beforeAll(() => server.listen());
  afterEach(() => {
    server.resetHandlers();
    cleanup();
    vi.clearAllMocks();
  });
  afterAll(() => server.close());

  let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
  beforeEach(() => {
    renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    mockUseUserMetricsQuery.mockReturnValue({
      ...BASE_QUERY_RESULT,
      status: "error",
      data: undefined,
    });
  });

  it("should show error toast when API errors", async () => {
    renderProviderFn?.(<MetricsChart userId={FIXED_USER_ID} />);

    await waitFor(() => {
      expect(
        screen.getByText(/sorry, something went wrong/i),
      ).toBeInTheDocument();
    });
  });
});
