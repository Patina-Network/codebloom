import UserProfilePage from "@/app/user/[userId]/UserProfile.page";
import { TestUtils, TestUtilTypes } from "@/lib/test";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router";

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserverMock;

const routes = (
  <Routes>
    <Route path="/user/:userId" element={<UserProfilePage />} />
  </Routes>
);

describe("UserProfilePage", () => {
  describe("without date range", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Recent Submissions heading", () => {
      renderProviderFn?.(routes, "/user/user-1");

      expect(screen.getByText("Recent Submissions")).toBeInTheDocument();
    });

    it("should render View All button", () => {
      renderProviderFn?.(routes, "/user/user-1");

      const viewAllButton = screen.getByRole("link", { name: "View All" });
      expect(viewAllButton).toBeInTheDocument();
    });

    it("should have View All link without date params", () => {
      renderProviderFn?.(routes, "/user/user-1");

      const viewAllLink = screen.getByRole("link", { name: "View All" });
      expect(viewAllLink).toHaveAttribute("href", "/user/user-1/submissions");
    });

    it("should not render Clear button without date params", () => {
      renderProviderFn?.(routes, "/user/user-1");

      expect(
        screen.queryByRole("button", { name: "Clear" }),
      ).not.toBeInTheDocument();
    });
  });

  describe("with date range", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Clear button with date params", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1?startDate=2025-01-01T00:00:00.000Z&endDate=2025-02-01T00:00:00.000Z",
      );

      expect(screen.getByRole("button", { name: "Clear" })).toBeInTheDocument();
    });

    it("should have View All link with date params", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1?startDate=2025-01-01T00:00:00.000Z&endDate=2025-02-01T00:00:00.000Z",
      );

      const viewAllLink = screen.getByRole("link", { name: "View All" });
      expect(viewAllLink).toHaveAttribute(
        "href",
        expect.stringContaining("/user/user-1/submissions?startDate="),
      );
      expect(viewAllLink).toHaveAttribute(
        "href",
        expect.stringContaining("endDate="),
      );
    });

    it("should remove date params when Clear is clicked", async () => {
      const user = userEvent.setup();
      renderProviderFn?.(
        routes,
        "/user/user-1?startDate=2025-01-01T00:00:00.000Z&endDate=2025-02-01T00:00:00.000Z",
      );

      const clearButton = screen.getByRole("button", {
        name: "Clear",
      });
      await user.click(clearButton);

      await waitFor(() => {
        expect(
          screen.queryByRole("button", { name: "Clear" }),
        ).not.toBeInTheDocument();
      });
    });
  });

  describe("with only startDate", () => {
    let renderProviderFn: TestUtilTypes.RenderWithAllProvidersFn | null = null;
    beforeEach(() => {
      renderProviderFn = TestUtils.getRenderWithAllProvidersFn();
    });

    it("should render Clear button with only startDate", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1?startDate=2025-01-01T00:00:00.000Z",
      );

      expect(screen.getByRole("button", { name: "Clear" })).toBeInTheDocument();
    });

    it("should have View All link with startDate only", () => {
      renderProviderFn?.(
        routes,
        "/user/user-1?startDate=2025-01-01T00:00:00.000Z",
      );

      const viewAllLink = screen.getByRole("link", { name: "View All" });
      expect(viewAllLink).toHaveAttribute(
        "href",
        expect.stringContaining("/user/user-1/submissions?startDate="),
      );
      expect(viewAllLink).not.toHaveAttribute(
        "href",
        expect.stringContaining("endDate="),
      );
    });
  });
});
