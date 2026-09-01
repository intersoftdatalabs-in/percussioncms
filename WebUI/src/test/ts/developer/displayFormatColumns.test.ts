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
  catalogFieldsNotInUse,
  columnsOrderEqual,
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
