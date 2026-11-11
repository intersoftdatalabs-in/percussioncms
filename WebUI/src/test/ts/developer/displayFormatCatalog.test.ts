/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import type { DisplayFormat } from "../../../main/ts/api/developer/types";
import {
  displayFormatOpenKey,
  displayFormatRowKey,
  displayFormatSelection,
  filterDisplayFormatCatalog,
  sortDisplayFormatCatalog,
} from "../../../main/ts/developer/displayFormatCatalog";

function format(overrides: Partial<DisplayFormat> = {}): DisplayFormat {
  return { name: "Default", label: "Default View", ...overrides };
}

describe("displayFormatCatalog", () => {
  it("prefers name, then internal name, then guid for the open key", () => {
    expect(displayFormatOpenKey(format())).toBe("Default");
    expect(displayFormatOpenKey(format({ name: undefined, internalName: "sys_Default" }))).toBe(
      "sys_Default",
    );
    expect(
      displayFormatOpenKey(format({ name: undefined, internalName: undefined, guidString: "9-1-1" })),
    ).toBe("9-1-1");
    expect(displayFormatOpenKey({})).toBeNull();
  });

  it("builds stable row keys with a positional fallback", () => {
    expect(displayFormatRowKey(format({ guidString: "9-1-1" }), 3)).toBe("9-1-1");
    expect(displayFormatRowKey(format({ guidString: undefined }), 3)).toBe("Default");
    expect(displayFormatRowKey({}, 3)).toBe("df-3");
  });

  it("sorts by name with display-name fallback and tolerates null", () => {
    expect(sortDisplayFormatCatalog(null)).toEqual([]);
    const sorted = sortDisplayFormatCatalog([
      format({ name: "Zulu", displayName: undefined }),
      format({ name: "alpha", label: "Beta" }),
      format({ name: "Aaron", label: "Aaron" }),
    ]);
    expect(sorted.map((f) => f.name)).toEqual(["Aaron", "alpha", "Zulu"]);
    // Source array untouched.
    expect(sorted[1].label).toBe("Beta");
  });

  it("filters across name, label, internal name, display name, and description", () => {
    const items = [
      format(),
      format({
        name: "NoirSummary",
        label: "Noir Summary",
        internalName: "sys_Noir",
        description: "Second format",
      }),
    ];
    expect(filterDisplayFormatCatalog(items, "noir").map((f) => f.name)).toEqual(["NoirSummary"]);
    expect(filterDisplayFormatCatalog(items, "sys_noir").map((f) => f.name)).toEqual([
      "NoirSummary",
    ]);
    expect(filterDisplayFormatCatalog(items, "second").map((f) => f.name)).toEqual(["NoirSummary"]);
    expect(filterDisplayFormatCatalog(items, "zzz")).toEqual([]);
    expect(filterDisplayFormatCatalog(items, "")).toHaveLength(2);
  });

  it("builds a detail selection with the guid fallback, or null without a key", () => {
    expect(displayFormatSelection(format({ guidString: "9-1-1" }))).toEqual({
      idOrName: "Default",
      catalogGuid: "9-1-1",
    });
    expect(displayFormatSelection(format({ guidString: undefined, name: "Plain" }))).toEqual({
      idOrName: "Plain",
      catalogGuid: undefined,
    });
    expect(displayFormatSelection({})).toBeNull();
  });
});
