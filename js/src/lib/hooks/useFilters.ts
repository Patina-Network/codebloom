import { Tag } from "@/lib/api/types/schema";
import { ApiUtils } from "@/lib/api/utils";
import { useCallback, useMemo, useRef } from "react";
import { useSearchParams } from "react-router-dom";
import { useImmer } from "use-immer";

export type TagEnumToBooleanFilterObject = Record<Tag, boolean>;

export type ToggleTagEnumFn = (tagEnum: Tag) => void;

function getUrlKey(tagEnum: Tag) {
  return ApiUtils.getMetadataByTagEnum(tagEnum).apiKey;
}

/**
 * React hook that manages user tag filters.
 *
 * Each supported `Tag` is represented as a boolean in the `filters` object.
 * When a filter is toggled, both the internal state and the URL search params update
 * to reflect the new value.
 *
 * Furthermore, this hook automatically initializes the filter state from the current
 * URL params and updates whenever filters are toggled.
 *
 * @param {Object} options - Configuration options for the hook.
 * @param {(tagEnum: Tag) => void} options.onFilterChange - (Optional) When `toggleFilter` is called, `onFilterChange` will be triggered 
 * to run any side-effects.
 *
 * @returns An object containing:
 * - `filters`: an object with each key of `Tag` mapping to a value to its current enabled/disabled state
 * - `toggleFilter`: a function to toggle an individual tag filter
 *
 * @example
 * ```tsx
 * const { filters, toggleFilter } = useFilters();
 *
 * return (
      <SegmentedControl
        value={filters.Patina ? "patina" : "all"}
        data={[
          { label: "All", value: "all" },
          { label: "Patina", value: "patina" },
        ]}
        onChange={() => toggleFilter(Tag.Patina)}
      />
 * );
 * ```
 */
function syncSearchParams(
  prev: URLSearchParams,
  nextFilters: TagEnumToBooleanFilterObject,
) {
  const allTags = Object.typedKeys(nextFilters);
  const activeKeys = allTags
    .filter((tag) => nextFilters[tag])
    .map((tag) => getUrlKey(tag));

  for (const tag of allTags) {
    prev.delete(getUrlKey(tag));
  }

  if (activeKeys.length > 0) {
    prev.set("filters", activeKeys.join(","));
  } else {
    prev.delete("filters");
  }

  return prev;
}

export function useFilters({
  onFilterChange,
}: {
  onFilterChange?: (tagEnum: Tag) => void;
} = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useImmer<TagEnumToBooleanFilterObject>(() => {
    const activeFilters = new Set(
      (searchParams.get("filters") ?? "")
        .split(",")
        .map((key) => key.trim())
        .filter(Boolean),
    );

    const kvTuples = ApiUtils.getAllTagEnums().map((tagEnum) => {
      const key = getUrlKey(tagEnum);
      return [
        tagEnum,
        activeFilters.has(key) || searchParams.get(key) === "true",
      ];
    }) as [Tag, boolean][];

    return Object.typedFromEntries(kvTuples);
  });
  const latestFilters = useRef(filters);

  const toggleFilter: ToggleTagEnumFn = useCallback(
    (tagEnum: Tag) => {
      const nextFilters = {
        ...latestFilters.current,
        [tagEnum]: !latestFilters.current[tagEnum],
      };
      latestFilters.current = nextFilters;

      setFilters((prev) => {
        prev[tagEnum] = nextFilters[tagEnum];
      });
      setSearchParams((prev) => syncSearchParams(prev, nextFilters));
      onFilterChange?.(tagEnum);
    },
    [onFilterChange, setFilters, setSearchParams],
  );

  const clearFilters = useCallback(() => {
    const activeTags = Object.typedKeys(latestFilters.current).filter(
      (tag) => latestFilters.current[tag],
    );
    if (activeTags.length === 0) return;

    const nextFilters = { ...latestFilters.current };
    for (const tagEnum of activeTags) {
      nextFilters[tagEnum] = false;
    }
    latestFilters.current = nextFilters;

    setFilters((prev) => {
      for (const tagEnum of activeTags) {
        prev[tagEnum] = false;
      }
    });
    setSearchParams((prev) => syncSearchParams(prev, nextFilters));

    for (const tagEnum of activeTags) {
      onFilterChange?.(tagEnum);
    }
  }, [onFilterChange, setFilters, setSearchParams]);

  const isAnyFilterEnabled = useMemo(
    () => Object.values(filters).some(Boolean),
    [filters],
  );

  return { filters, toggleFilter, clearFilters, isAnyFilterEnabled };
}
