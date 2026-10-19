import UserSubmissionsPage from "@/app/user/[userId]/submissions/UserSubmissions.page";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen } from "@testing-library/react";
import { Route, Routes } from "react-router";

const routes = (
  <Routes>
    <Route path="/user/:userId/submissions" element={<UserSubmissionsPage />} />
  </Routes>
);

describe("UserSubmissionsPage", () => {
  describe("without date range", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Go to profile button", () => {
      renderProviderFn?.(routes, "/user/user-1/submissions");

      expect(
        screen.getByRole("button", { name: "← Go to profile" }),
      ).toBeInTheDocument();
    });

    it("should render skeleton stack of submissions initially", () => {
      renderProviderFn?.(routes, "/user/user-1/submissions");

      const element = screen.getByTestId(
        "user-profile-skeleton-submissions-stack",
      );
      expect(element).toBeInTheDocument();
      expect(element).toBeVisible();
    });
  });

  describe("with date range", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Go to profile button with date range", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1/submissions?startDate=2025-01-01T00:00:00.000Z&endDate=2025-02-01T00:00:00.000Z",
      );

      expect(
        screen.getByRole("button", { name: "← Go to profile" }),
      ).toBeInTheDocument();
    });
  });

  describe("with only startDate", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Go to profile button with startDate only", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1/submissions?startDate=2025-01-01T00:00:00.000Z",
      );

      expect(
        screen.getByRole("button", { name: "← Go to profile" }),
      ).toBeInTheDocument();
    });
  });
});
