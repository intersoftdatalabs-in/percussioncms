/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import type { ViewDef } from "../../../main/ts/api/developer/types";
import {
  groupViewsByParentCategory,
  isCustomUrlView,
  normalizeViewParentCategory,
  viewKey,
  viewLabel,
} from "../../../main/ts/contentExplorer/viewCatalog";

function v(
  partial: Partial<ViewDef> & Pick<ViewDef, "name">,
): ViewDef {
  return partial;
}

describe("viewCatalog grouping (#3116)", () => {
  it("places views into parentCategory 1–4 and sorts by label", () => {
    const grouped = groupViewsByParentCategory([
      v({ name: "Zulu", label: "Zulu", parentCategory: 1 }),
      v({ name: "Alpha", label: "Alpha", parentCategory: 1 }),
      v({ name: "Comm", parentCategory: 2 }),
      v({ name: "All", parentCategory: 3 }),
      v({ name: "Other", parentCategory: 4 }),
    ]);
    expect(grouped[1].map((d) => d.name)).toEqual(["Alpha", "Zulu"]);
    expect(grouped[2].map((d) => d.name)).toEqual(["Comm"]);
    expect(grouped[3].map((d) => d.name)).toEqual(["All"]);
    expect(grouped[4].map((d) => d.name)).toEqual(["Other"]);
  });

  it("drops rows without a usable key and buckets unknown category into Other", () => {
    const grouped = groupViewsByParentCategory([
      v({ name: "", label: "NoKey", parentCategory: 1 }),
      { label: "AlsoNoKey", parentCategory: 2 },
      v({ name: "Inbox", parentCategory: 0 }),
      v({ name: "Orphan", parentCategory: 99 }),
    ]);
    expect(grouped[1]).toHaveLength(0);
    expect(grouped[2]).toHaveLength(0);
    expect(grouped[4].map((d) => d.name)).toEqual(["Inbox", "Orphan"]);
  });

  it("returns empty groups for null / empty input", () => {
    expect(groupViewsByParentCategory(null)[1]).toEqual([]);
    expect(groupViewsByParentCategory([])[3]).toEqual([]);
  });

  it("normalizes category and view keys / labels", () => {
    expect(normalizeViewParentCategory(2)).toBe(2);
    expect(normalizeViewParentCategory(undefined)).toBe(4);
    expect(viewKey({ name: "View_All" })).toBe("View_All");
    expect(viewKey({ id: 12 })).toBe("12");
    expect(viewLabel({ name: "View_All", label: "All content" })).toBe(
      "All content",
    );
    expect(isCustomUrlView({ customView: true })).toBe(true);
    expect(isCustomUrlView({ standardView: true })).toBe(false);
  });
});
