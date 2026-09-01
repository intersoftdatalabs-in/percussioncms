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
import type { ViewFieldSummary } from "../../../main/ts/api/developer/types";
import {
  addViewFieldCriterion,
  catalogViewFieldsNotInUse,
  isKnownViewFieldName,
  moveViewFieldCriterion,
  removeViewFieldCriterion,
  viewFieldsEqual,
} from "../../../main/ts/developer/viewFieldCriteria";

const contentId: ViewFieldSummary = {
  fieldName: "sys_contentid",
  operator: "equal",
  fieldValue: "1",
  fieldType: "Number",
  position: 0,
};

describe("view field catalog", () => {
  it("shares known CX fields with the display-format picker", () => {
    expect(isKnownViewFieldName("sys_title")).toBe(true);
    expect(isKnownViewFieldName("sys_contentid")).toBe(true);
    expect(isKnownViewFieldName("not_a_cx_field")).toBe(false);
  });
});

describe("view field list mutations", () => {
  it("adds a catalog field and reindexes", () => {
    const next = addViewFieldCriterion([contentId], "sys_title", "like", "News%");
    expect(next.map((f) => f.fieldName)).toEqual(["sys_contentid", "sys_title"]);
    expect(next[1].position).toBe(1);
    expect(next[1].operator).toBe("like");
    expect(next[1].fieldValue).toBe("News%");
  });

  it("does not add duplicates or unknown sources", () => {
    expect(addViewFieldCriterion([contentId], "sys_contentid")).toEqual([contentId]);
    expect(addViewFieldCriterion([contentId], "not_a_cx_field")).toEqual([contentId]);
  });

  it("removes and reorders", () => {
    const withTitle = addViewFieldCriterion([contentId], "sys_title");
    const moved = moveViewFieldCriterion(withTitle, 0, 1);
    expect(moved.map((f) => f.fieldName)).toEqual(["sys_title", "sys_contentid"]);
    const removed = removeViewFieldCriterion(moved, 0);
    expect(removed.map((f) => f.fieldName)).toEqual(["sys_contentid"]);
    expect(removed[0].position).toBe(0);
  });

  it("detects dirty order", () => {
    const withTitle = addViewFieldCriterion([contentId], "sys_title");
    expect(viewFieldsEqual(withTitle, addViewFieldCriterion([contentId], "sys_title"))).toBe(true);
    expect(viewFieldsEqual(withTitle, [contentId])).toBe(false);
  });

  it("omits in-use fields from the picker", () => {
    const available = catalogViewFieldsNotInUse([contentId]);
    expect(available.some((f) => f.source === "sys_contentid")).toBe(false);
    expect(available.some((f) => f.source === "sys_title")).toBe(true);
  });
});
