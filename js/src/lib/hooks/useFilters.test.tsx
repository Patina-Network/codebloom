import { Tag } from "@/lib/api/types/schema";
import { useFilters } from "@/lib/hooks/useFilters";
import { act, renderHook, screen } from "@testing-library/react";
import { PropsWithChildren } from "react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

function createWrapper(initialEntry = "/") {
  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <MemoryRouter initialEntries={[initialEntry]}>
        <LocationProbe />
        {children}
      </MemoryRouter>
    );
  };
}

function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location-search">{location.search}</output>;
}

describe("useFilters", () => {
  it("initializes every tag as disabled when the URL has no filters", () => {
    const { result } = renderHook(() => useFilters(), {
      wrapper: createWrapper(),
    });

    expect(Object.values(result.current.filters)).not.toContain(true);
    expect(result.current.isAnyFilterEnabled).toBe(false);
  });

  it("initializes filters from the filters parameter and individual tag parameters", () => {
    const { result } = renderHook(() => useFilters(), {
      wrapper: createWrapper(
        "/leaderboard?filters=patina,%20nyu,,unknown&hunter=true",
      ),
    });

    expect(result.current.filters[Tag.Patina]).toBe(true);
    expect(result.current.filters[Tag.Nyu]).toBe(true);
    expect(result.current.filters[Tag.Hunter]).toBe(true);
    expect(result.current.filters[Tag.Baruch]).toBe(false);
    expect(result.current.isAnyFilterEnabled).toBe(true);
  });

  it("toggles a filter on and off, updates the URL, and invokes the callback", () => {
    const onFilterChange = vi.fn();
    const { result } = renderHook(() => useFilters({ onFilterChange }), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.toggleFilter(Tag.Patina);
    });

    expect(result.current.filters[Tag.Patina]).toBe(true);
    expect(result.current.isAnyFilterEnabled).toBe(true);
    expect(screen.getByTestId("location-search").textContent).toBe(
      "?filters=patina",
    );
    expect(onFilterChange).toHaveBeenCalledWith(Tag.Patina);

    act(() => {
      result.current.toggleFilter(Tag.Patina);
    });

    expect(result.current.filters[Tag.Patina]).toBe(false);
    expect(result.current.isAnyFilterEnabled).toBe(false);
    expect(screen.getByTestId("location-search").textContent).toBe("");
    expect(onFilterChange).toHaveBeenCalledTimes(2);
  });

  it("migrates existing direct tag parameters into the filters parameter", () => {
    const { result } = renderHook(() => useFilters(), {
      wrapper: createWrapper("/leaderboard?hunter=true"),
    });

    act(() => {
      result.current.toggleFilter(Tag.Patina);
    });

    expect(screen.getByTestId("location-search").textContent).toBe(
      "?filters=patina%2Chunter",
    );
  });

  it("does nothing when clearFilters is called with no active filters", () => {
    const onFilterChange = vi.fn();
    const { result } = renderHook(() => useFilters({ onFilterChange }), {
      wrapper: createWrapper("/leaderboard"),
    });

    act(() => {
      result.current.clearFilters();
    });

    expect(onFilterChange).not.toHaveBeenCalled();
    expect(screen.getByTestId("location-search").textContent).toBe("");
  });

  it("clears every active filter and invokes the callback for each active tag", () => {
    const onFilterChange = vi.fn();
    const { result } = renderHook(() => useFilters({ onFilterChange }), {
      wrapper: createWrapper("/leaderboard?filters=patina,nyu"),
    });

    act(() => {
      result.current.clearFilters();
    });

    expect(result.current.filters[Tag.Patina]).toBe(false);
    expect(result.current.filters[Tag.Nyu]).toBe(false);
    expect(result.current.isAnyFilterEnabled).toBe(false);
    expect(screen.getByTestId("location-search").textContent).toBe("");
    expect(onFilterChange).toHaveBeenCalledTimes(2);
    expect(onFilterChange).toHaveBeenCalledWith(Tag.Patina);
    expect(onFilterChange).toHaveBeenCalledWith(Tag.Nyu);
  });
});
