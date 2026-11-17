import { Tag } from "@/lib/api/types/schema";
import { ApiUtils } from "@/lib/api/utils";
import { useCallback, useMemo } from "react";
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
export function useFilters({
  onFilterChange,
}:
  | {
      onFilterChange?: (tagEnum: Tag) => void;
    }
  | undefined = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useImmer<TagEnumToBooleanFilterObject>(() => {
    const rawFilters = searchParams.get("filters");
    const activeFilters = new Set(
      rawFilters ?
        rawFilters
          .split(",")
          .map((key) => key.trim())
          .filter(Boolean)
      : [],
    );
    // NOTE: We allow all enums so that if a user manually forces a param into the URL
    // (e.g. our backend loading the frontend to take screenshots) it WILL be allowed. However, it will NOT
    // be generated as a filter list or usable in the UI whatsoever.
    const kvTuples = ApiUtils.getAllTagEnums().map((tagEnum) => {
      const key = getUrlKey(tagEnum);
      return [
        tagEnum,
        activeFilters.has(key) || searchParams.get(key) === "true",
      ];
    }) as [Tag, boolean][]; // slight type trickery because TS
    // doesn't know which type is a key and which is a value; we help it out here a little.

    return Object.typedFromEntries(kvTuples);
  });

  const toggleFilter: ToggleTagEnumFn = useCallback(
    (tagEnum: Tag) => {
      const nextFilters = {
        ...filters,
        [tagEnum]: !filters[tagEnum],
      };
      setFilters((prev) => {
        prev[tagEnum] = !prev[tagEnum];
      });
      setSearchParams((prev) => {
        const activeKeys = Object.typedKeys(nextFilters)
          .filter((tag) => nextFilters[tag])
          .map((tag) => getUrlKey(tag));
        for (const tag of Object.typedKeys(nextFilters)) {
          prev.delete(getUrlKey(tag));
        }
        if (activeKeys.length > 0) {
          prev.set("filters", activeKeys.join(","));
        } else {
          prev.delete("filters");
        }
        return prev;
      });
      onFilterChange?.(tagEnum);
    },
    [filters, onFilterChange, setFilters, setSearchParams],
  );

  const clearFilters = useCallback(() => {
    const activeTags = Object.typedKeys(filters).filter((tag) => filters[tag]);
    if (activeTags.length === 0) return;

    setFilters((prev) => {
      for (const tagEnum of activeTags) {
        prev[tagEnum] = false;
      }
    });

    setSearchParams((prev) => {
      prev.delete(FILTERS_PARAM_KEY);
      for (const tagEnum of activeTags) {
        prev.delete(getUrlKey(tagEnum));
      }
      return prev;
    });

    for (const tagEnum of activeTags) {
      onFilterChange?.(tagEnum);
    }
  }, [filters, onFilterChange, setFilters, setSearchParams]);

  const isAnyFilterEnabled: boolean = useMemo(() => {
    return Object.values(filters).some(Boolean);
  }, [filters]);

  return { filters, toggleFilter, clearFilters, isAnyFilterEnabled };
}
