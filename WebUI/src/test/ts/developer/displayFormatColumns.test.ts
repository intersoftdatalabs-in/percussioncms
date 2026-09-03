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
import type { DisplayFormatColumn } from "../../../main/ts/api/developer/types";
import {
  addDisplayFormatColumn,
  applyColumnSortDirection,
  catalogFieldsNotInUse,
  columnsEditEqual,
  columnsOrderEqual,
  defaultSortSource,
  isColumnAscendingSort,
  isPackagedDisplayFormat,
  isValidColumnSource,
  moveDisplayFormatColumn,
  removeDisplayFormatColumn,
} from "../../../main/ts/developer/displayFormatColumns";

const title: DisplayFormatColumn = {
  source: "sys_title",
  displayName: "Title",
  position: 0,
};

describe("isPackagedDisplayFormat", () => {
  it("treats installer catalog names as packaged", () => {
    expect(isPackagedDisplayFormat("Default")).toBe(true);
    expect(isPackagedDisplayFormat("By_Author")).toBe(true);
    expect(isPackagedDisplayFormat("cm1_default")).toBe(true);
  });

  it("treats uniquely named user formats as editable", () => {
    expect(isPackagedDisplayFormat("qa4097abcd")).toBe(false);
    expect(isPackagedDisplayFormat("My_User_Format")).toBe(false);
  });
});

describe("isValidColumnSource", () => {
  it("accepts system field names", () => {
    expect(isValidColumnSource("sys_title")).toBe(true);
    expect(isValidColumnSource("sys_contentcreatedby")).toBe(true);
  });

  it("rejects blank, whitespace, wildcards, and path", () => {
    expect(isValidColumnSource("")).toBe(false);
    expect(isValidColumnSource("has space")).toBe(false);
    expect(isValidColumnSource("sys*title")).toBe(false);
    expect(isValidColumnSource("../sys_title")).toBe(false);
    expect(isValidColumnSource("sys/title")).toBe(false);
  });
});

describe("column list mutations", () => {
  it("adds a catalog field and reindexes", () => {
    const next = addDisplayFormatColumn([title], "sys_contentid");
    expect(next.map((c) => c.source)).toEqual(["sys_title", "sys_contentid"]);
    expect(next[1].position).toBe(1);
    expect(next[1].displayName).toBe("Content id");
  });

  it("does not add duplicates or invalid sources", () => {
    expect(addDisplayFormatColumn([title], "sys_title")).toEqual([title]);
    expect(addDisplayFormatColumn([title], "has space")).toEqual([title]);
  });

  it("does not remove sys_title", () => {
    const withCreated = addDisplayFormatColumn([title], "sys_contentcreatedby");
    expect(removeDisplayFormatColumn(withCreated, 0)).toEqual(withCreated);
    const removed = removeDisplayFormatColumn(withCreated, 1);
    expect(removed.map((c) => c.source)).toEqual(["sys_title"]);
  });

  it("reorders and compares signatures", () => {
    const two = addDisplayFormatColumn([title], "sys_workflow");
    const down = moveDisplayFormatColumn(two, 0, 1);
    expect(down.map((c) => c.source)).toEqual(["sys_workflow", "sys_title"]);
    expect(columnsOrderEqual(two, down)).toBe(false);
    expect(columnsOrderEqual(two, addDisplayFormatColumn([title], "sys_workflow"))).toBe(true);
  });

  it("omits in-use fields from the picker catalog", () => {
    const remaining = catalogFieldsNotInUse([title]);
    expect(remaining.some((f) => f.source === "sys_title")).toBe(false);
    expect(remaining.some((f) => f.source === "sys_contentid")).toBe(true);
  });
});

describe("default sort column and direction", () => {
  it("treats missing flags as ascending", () => {
    expect(isColumnAscendingSort(title)).toBe(true);
    expect(isColumnAscendingSort({ ...title, ascendingSort: true })).toBe(true);
    expect(isColumnAscendingSort({ ...title, ascendingSort: false })).toBe(false);
    expect(isColumnAscendingSort({ ...title, descendingSort: true })).toBe(false);
    expect(isColumnAscendingSort({ ...title, sortOrder: false })).toBe(false);
  });

  it("prefers sortedColumnNames when that source is in the list", () => {
    const two = addDisplayFormatColumn([title], "sys_contentid");
    expect(defaultSortSource(two, "sys_contentid")).toBe("sys_contentid");
    expect(defaultSortSource(two, "missing")).toBe("sys_title");
    expect(defaultSortSource([], "sys_title")).toBe("");
  });

  it("falls back to the first column when no sort is stored", () => {
    const two = addDisplayFormatColumn([title], "sys_contentid");
    expect(defaultSortSource(two, null)).toBe("sys_title");
    expect(defaultSortSource(two, "")).toBe("sys_title");
    expect(defaultSortSource(two, undefined)).toBe("sys_title");
  });

  it("applies descending on the named column only", () => {
    const two = addDisplayFormatColumn([title], "sys_contentid");
    const next = applyColumnSortDirection(two, "sys_contentid", false);
    expect(isColumnAscendingSort(next[0])).toBe(true);
    expect(isColumnAscendingSort(next[1])).toBe(false);
    expect(next[1].descendingSort).toBe(true);
    expect(next[1].sortOrder).toBe(false);
    expect(applyColumnSortDirection(two, "missing", false)).toEqual(two);
  });

  it("detects sort-direction dirty without a reorder", () => {
    const two = addDisplayFormatColumn([title], "sys_workflow");
    const desc = applyColumnSortDirection(two, "sys_title", false);
    expect(columnsOrderEqual(two, desc)).toBe(true);
    expect(columnsEditEqual(two, desc)).toBe(false);
    expect(columnsEditEqual(two, addDisplayFormatColumn([title], "sys_workflow"))).toBe(true);
  });
});
