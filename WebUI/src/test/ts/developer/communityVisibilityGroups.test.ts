/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import type { CommunityVisibleObject } from "../../../main/ts/api/developer/types";
import {
  groupVisibleObjectsByType,
  normalizeVisibilityTypeKey,
  UNKNOWN_VISIBILITY_TYPE_KEY,
  visibilityTypeGroupLabel,
  visibleObjectRowKey,
  VISIBILITY_TYPE_GROUP_ORDER,
} from "../../../main/ts/developer/communityVisibilityGroups";

const sample: CommunityVisibleObject[] = [
  { name: "site1", label: "Corporate", type: "SITE", id: 3 },
  { name: "percPage", label: "Page", type: "NODEDEF", id: 1 },
  { name: "rffSnTitle", label: "Title Snippet", type: "TEMPLATE", id: 2 },
  { name: "wf1", label: "Simple", type: "workflow", id: 4 },
  { name: "orphan", label: "No type", id: 5 },
];

describe("communityVisibilityGroups", () => {
  it("normalizeVisibilityTypeKey uppercases and maps empty to unknown", () => {
    expect(normalizeVisibilityTypeKey("nodedef")).toBe("NODEDEF");
    expect(normalizeVisibilityTypeKey("  TEMPLATE  ")).toBe("TEMPLATE");
    expect(normalizeVisibilityTypeKey("")).toBe(UNKNOWN_VISIBILITY_TYPE_KEY);
    expect(normalizeVisibilityTypeKey(null)).toBe(UNKNOWN_VISIBILITY_TYPE_KEY);
    expect(normalizeVisibilityTypeKey(undefined)).toBe(UNKNOWN_VISIBILITY_TYPE_KEY);
  });

  it("visibilityTypeGroupLabel uses curated labels and fallbacks", () => {
    expect(visibilityTypeGroupLabel("NODEDEF")).toMatch(/Content type/i);
    expect(visibilityTypeGroupLabel("TEMPLATE")).toMatch(/Template/i);
    expect(visibilityTypeGroupLabel("CUSTOM_X")).toBe("CUSTOM_X");
    expect(visibilityTypeGroupLabel(UNKNOWN_VISIBILITY_TYPE_KEY)).toBe("Unknown type");
  });

  it("groupVisibleObjectsByType returns empty for empty input", () => {
    expect(groupVisibleObjectsByType([])).toEqual([]);
  });

  it("groups by type, sorts curated order, and sorts objects by label/name", () => {
    const groups = groupVisibleObjectsByType(sample);
    const keys = groups.map((g) => g.typeKey);
    expect(keys).toEqual([
      "NODEDEF",
      "TEMPLATE",
      "SITE",
      "WORKFLOW",
      UNKNOWN_VISIBILITY_TYPE_KEY,
    ]);
    // Curated order positions
    expect(VISIBILITY_TYPE_GROUP_ORDER.indexOf("NODEDEF")).toBeLessThan(
      VISIBILITY_TYPE_GROUP_ORDER.indexOf("TEMPLATE"),
    );
    expect(groups[0].objects.map((o) => o.name)).toEqual(["percPage"]);
    expect(groups[1].label).toMatch(/Template/i);
    const unknown = groups.find((g) => g.typeKey === UNKNOWN_VISIBILITY_TYPE_KEY);
    expect(unknown?.objects.map((o) => o.name)).toEqual(["orphan"]);
  });

  it("sorts objects within a group case-insensitively by label then name", () => {
    const objs: CommunityVisibleObject[] = [
      { name: "b", label: "Zebra", type: "SITE" },
      { name: "a", label: "apple", type: "SITE" },
      { name: "c", label: "Mango", type: "SITE" },
    ];
    const groups = groupVisibleObjectsByType(objs);
    expect(groups).toHaveLength(1);
    expect(groups[0].objects.map((o) => o.label)).toEqual(["apple", "Mango", "Zebra"]);
  });

  it("visibleObjectRowKey prefers guid, then id, then name", () => {
    expect(
      visibleObjectRowKey({ guid: { stringValue: "0-2-1" }, name: "x", id: 9 }),
    ).toBe("0-2-1");
    expect(visibleObjectRowKey({ id: 9, name: "x" })).toBe("id:9");
    expect(visibleObjectRowKey({ name: "x" })).toBe("name:x");
    expect(visibleObjectRowKey({}, 3)).toBe("obj-idx:3");
  });
});
