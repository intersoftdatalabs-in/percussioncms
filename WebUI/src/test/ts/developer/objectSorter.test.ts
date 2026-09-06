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

import { beforeEach, describe, expect, it } from "vitest";
import {
  applyObjectSorter,
  defaultObjectSorterPreference,
  loadObjectSorterPreference,
  moveObjectSorterId,
  OBJECT_SORTER_STORAGE_KEY,
  parseObjectSorterPreference,
  saveObjectSorterPreference,
  type ObjectSorterRow,
} from "../../../main/ts/developer/objectSorter";

const rows: ObjectSorterRow[] = [
  { id: "zeta", name: "zetaType", label: "Alpha" },
  { id: "alpha", name: "alphaType", label: "Zulu" },
  { id: "mid", name: "midType", label: "Mike" },
];

describe("objectSorter", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("parses unknown JSON as default label-asc", () => {
    expect(parseObjectSorterPreference(null).mode).toBe("label-asc");
    expect(parseObjectSorterPreference({ mode: "nope" }).mode).toBe("label-asc");
  });

  it("sorts by name descending", () => {
    const sorted = applyObjectSorter(rows, (r) => r, {
      version: 1,
      mode: "name-desc",
      customOrder: [],
    });
    expect(sorted.map((r) => r.id)).toEqual(["zeta", "mid", "alpha"]);
  });

  it("sorts by label ascending (default catalog order)", () => {
    const sorted = applyObjectSorter(rows, (r) => r, defaultObjectSorterPreference());
    expect(sorted.map((r) => r.id)).toEqual(["zeta", "mid", "alpha"]);
  });

  it("applies custom order and appends unknown ids last", () => {
    const sorted = applyObjectSorter(rows, (r) => r, {
      version: 1,
      mode: "custom",
      customOrder: ["mid", "alpha"],
    });
    expect(sorted.map((r) => r.id)).toEqual(["mid", "alpha", "zeta"]);
  });

  it("moves a custom id up and down without wrapping", () => {
    const order = ["a", "b", "c"];
    expect(moveObjectSorterId(order, "b", "up")).toEqual(["b", "a", "c"]);
    expect(moveObjectSorterId(order, "a", "up")).toEqual(["a", "b", "c"]);
    expect(moveObjectSorterId(order, "c", "down")).toEqual(["a", "b", "c"]);
    expect(moveObjectSorterId(order, "missing", "down")).toEqual(["a", "b", "c"]);
  });

  it("round-trips sessionStorage preference", () => {
    saveObjectSorterPreference({
      version: 1,
      mode: "name-desc",
      customOrder: ["zeta"],
    });
    expect(sessionStorage.getItem(OBJECT_SORTER_STORAGE_KEY)).toContain("name-desc");
    const loaded = loadObjectSorterPreference();
    expect(loaded.mode).toBe("name-desc");
    expect(loaded.customOrder).toEqual(["zeta"]);
  });

  it("load falls back when storage JSON is invalid", () => {
    sessionStorage.setItem(OBJECT_SORTER_STORAGE_KEY, "{not-json");
    expect(loadObjectSorterPreference().mode).toBe("label-asc");
  });
});
