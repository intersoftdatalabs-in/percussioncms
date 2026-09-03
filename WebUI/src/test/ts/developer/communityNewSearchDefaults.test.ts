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
import {
  collectSearchRefKeys,
  mergeSearchPickerRows,
  searchRefIsSelected,
  searchRefKeys,
  searchRefPrimaryKey,
  selectedPickerPrimaryKeys,
  sameSearchKeySet,
  toNewSearchWriteRefs,
  toggleSearchRefSelection,
} from "../../../main/ts/developer/communityNewSearchDefaults";

describe("communityNewSearchDefaults helpers (UI-09)", () => {
  it("prefers name, then guid, then non-zero id", () => {
    expect(searchRefKeys({ name: "SimpleSearch", id: 42 })).toEqual([
      "name:simplesearch",
      "id:42",
    ]);
    expect(searchRefPrimaryKey({ guid: { stringValue: "0-301-42" }, id: 42 })).toBe(
      "guid:0-301-42",
    );
    expect(searchRefKeys({ id: 0, name: "  " })).toEqual([]);
  });

  it("matches catalog name+id rows to id-only assigned refs", () => {
    const assigned = collectSearchRefKeys([{ id: 42 }]);
    expect(searchRefIsSelected({ name: "SimpleSearch", id: 42 }, assigned)).toBe(true);
    expect(searchRefIsSelected({ name: "Other", id: 43 }, assigned)).toBe(false);
  });

  it("merges catalog with assigned extras and skips duplicates", () => {
    const rows = mergeSearchPickerRows(
      [
        { name: "SimpleSearch", id: 42, label: "Simple" },
        { name: "Other", id: 43 },
      ],
      [
        { name: "SimpleSearch", id: 42 },
        { name: "OrphanSearch", id: 99 },
      ],
    );
    expect(rows.map((r) => r.name)).toEqual(["SimpleSearch", "Other", "OrphanSearch"]);
  });

  it("builds PUT refs for selected rows and empty-clear", () => {
    const rows = mergeSearchPickerRows(
      [
        { name: "SimpleSearch", id: 42 },
        { name: "Other", id: 43 },
      ],
      [],
    );
    const selected = collectSearchRefKeys([{ name: "SimpleSearch" }]);
    expect(toNewSearchWriteRefs(rows, selected)).toEqual([
      { name: "SimpleSearch", id: 42 },
    ]);
    expect(toNewSearchWriteRefs(rows, new Set())).toEqual([]);
  });

  it("toggles all identity keys together", () => {
    const row = { name: "SimpleSearch", id: 42 };
    const on = toggleSearchRefSelection(row, new Set());
    expect(searchRefIsSelected(row, on)).toBe(true);
    const off = toggleSearchRefSelection(row, on);
    expect(searchRefIsSelected(row, off)).toBe(false);
  });

  it("detects dirty picker selection vs initial primary keys", () => {
    const rows = mergeSearchPickerRows([{ name: "A", id: 1 }, { name: "B", id: 2 }], []);
    const initial = selectedPickerPrimaryKeys(rows, collectSearchRefKeys([{ name: "A" }]));
    const same = selectedPickerPrimaryKeys(rows, collectSearchRefKeys([{ name: "A", id: 1 }]));
    const changed = selectedPickerPrimaryKeys(rows, collectSearchRefKeys([{ name: "B" }]));
    expect(sameSearchKeySet(same, initial)).toBe(true);
    expect(sameSearchKeySet(changed, initial)).toBe(false);
  });
});
