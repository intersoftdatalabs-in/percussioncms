/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import type { CommunityVisibleObject } from "../../../main/ts/api/developer/types";
import {
  COMMUNITY_VISIBILITY_TYPE_OPTIONS,
  filterVisibleObjects,
  normalizeVisibilityQuery,
  visibilityEmptyKind,
  visibilitySummaryCounts,
} from "../../../main/ts/developer/communityVisibilityFilters";

const sample: CommunityVisibleObject[] = [
  { name: "percPage", label: "Page", type: "NODEDEF", id: 1 },
  { name: "rffSnTitle", label: "Title Snippet", type: "TEMPLATE", id: 2 },
  { name: "site1", label: "Corporate", type: "SITE", id: 3 },
];

describe("communityVisibilityFilters", () => {
  it("includes All types as first empty-value option and curated ObjectTypeEnum names", () => {
    expect(COMMUNITY_VISIBILITY_TYPE_OPTIONS[0]).toEqual({ value: "", label: "All types" });
    const values = COMMUNITY_VISIBILITY_TYPE_OPTIONS.map((o) => o.value);
    expect(values).toContain("NODEDEF");
    expect(values).toContain("TEMPLATE");
    expect(values).toContain("WORKFLOW");
  });

  it("normalizeVisibilityQuery trims and lowercases", () => {
    expect(normalizeVisibilityQuery("  Page  ")).toBe("page");
    expect(normalizeVisibilityQuery(null)).toBe("");
    expect(normalizeVisibilityQuery(undefined)).toBe("");
  });

  it("filterVisibleObjects matches name, label, or type", () => {
    expect(filterVisibleObjects(sample, "")).toHaveLength(3);
    expect(filterVisibleObjects(sample, "   ")).toHaveLength(3);
    expect(filterVisibleObjects(sample, "title").map((o) => o.name)).toEqual(["rffSnTitle"]);
    expect(filterVisibleObjects(sample, "NODEDEF").map((o) => o.name)).toEqual(["percPage"]);
    expect(filterVisibleObjects(sample, "corporate").map((o) => o.name)).toEqual(["site1"]);
    expect(filterVisibleObjects(sample, "zzz")).toEqual([]);
  });

  it("visibilityEmptyKind distinguishes none / type / name empty reasons", () => {
    expect(visibilityEmptyKind(0, 0, "", "")).toBe("none");
    expect(visibilityEmptyKind(0, 0, "TEMPLATE", "")).toBe("type-filter");
    expect(visibilityEmptyKind(3, 0, "", "zzz")).toBe("name-filter");
    expect(visibilityEmptyKind(3, 1, "", "page")).toBeNull();
    expect(visibilityEmptyKind(2, 0, "NODEDEF", "nope")).toBe("name-filter");
  });

  it("visibilitySummaryCounts marks filtered when shown differs from total", () => {
    expect(visibilitySummaryCounts(5, 5)).toEqual({ total: 5, shown: 5, filtered: false });
    expect(visibilitySummaryCounts(5, 2)).toEqual({ total: 5, shown: 2, filtered: true });
  });
});
