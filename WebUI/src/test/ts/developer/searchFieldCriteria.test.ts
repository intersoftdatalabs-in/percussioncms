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
import type { SearchFieldSummary } from "../../../main/ts/api/developer/types";
import {
  addSearchFieldCriterion,
  catalogFieldsNotInUse,
  fieldCriteriaEqual,
  isPackagedSearch,
  isValidSearchFieldName,
  moveSearchFieldCriterion,
  removeSearchFieldCriterion,
} from "../../../main/ts/developer/searchFieldCriteria";

const title: SearchFieldSummary = {
  fieldName: "sys_title",
  displayName: "Content Title",
  operator: "like",
  fieldValue: "",
  position: 0,
};

describe("isPackagedSearch", () => {
  it("treats installer catalog names as packaged", () => {
    expect(isPackagedSearch("Default_Search")).toBe(true);
    expect(isPackagedSearch("RC_Search")).toBe(true);
    expect(isPackagedSearch("default_search")).toBe(true);
  });

  it("treats uniquely named user searches as editable", () => {
    expect(isPackagedSearch("qa4110abcd")).toBe(false);
    expect(isPackagedSearch("My_User_Search")).toBe(false);
  });
});

describe("isValidSearchFieldName", () => {
  it("accepts system field names", () => {
    expect(isValidSearchFieldName("sys_title")).toBe(true);
    expect(isValidSearchFieldName("sys_contentcreatedby")).toBe(true);
  });

  it("rejects blank, whitespace, wildcards, and path", () => {
    expect(isValidSearchFieldName("")).toBe(false);
    expect(isValidSearchFieldName("has space")).toBe(false);
    expect(isValidSearchFieldName("sys*title")).toBe(false);
    expect(isValidSearchFieldName("../sys_title")).toBe(false);
    expect(isValidSearchFieldName("sys/title")).toBe(false);
  });
});

describe("search field list mutations", () => {
  it("adds a catalog field and reindexes", () => {
    const next = addSearchFieldCriterion([title], "sys_contentid");
    expect(next.map((f) => f.fieldName)).toEqual(["sys_title", "sys_contentid"]);
    expect(next[1].position).toBe(1);
    expect(next[1].displayName).toBe("Content id");
  });

  it("does not add duplicates or invalid names", () => {
    expect(addSearchFieldCriterion([title], "sys_title")).toEqual([title]);
    expect(addSearchFieldCriterion([title], "has space")).toEqual([title]);
  });

  it("removes a criterion and reindexes", () => {
    const withCreated = addSearchFieldCriterion([title], "sys_contentcreatedby");
    const removed = removeSearchFieldCriterion(withCreated, 0);
    expect(removed.map((f) => f.fieldName)).toEqual(["sys_contentcreatedby"]);
    expect(removed[0].position).toBe(0);
  });

  it("reorders criteria", () => {
    const two = addSearchFieldCriterion([title], "sys_contentid");
    const moved = moveSearchFieldCriterion(two, 0, 1);
    expect(moved.map((f) => f.fieldName)).toEqual(["sys_contentid", "sys_title"]);
    expect(moved[0].position).toBe(0);
    expect(moved[1].position).toBe(1);
  });

  it("catalogFieldsNotInUse omits already selected fields", () => {
    expect(catalogFieldsNotInUse([title]).some((f) => f.source === "sys_title")).toBe(false);
    expect(catalogFieldsNotInUse([title]).some((f) => f.source === "sys_contentid")).toBe(true);
  });

  it("fieldCriteriaEqual is order-sensitive", () => {
    const two = addSearchFieldCriterion([title], "sys_contentid");
    expect(fieldCriteriaEqual(two, two)).toBe(true);
    expect(fieldCriteriaEqual(two, moveSearchFieldCriterion(two, 0, 1))).toBe(false);
  });
});
