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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import {
  displayFormatOptionKey,
  isNumericDisplayFormatId,
  mapDisplayFormatToDetailColumns,
  resolvePathItemProperty,
  resolvePathmanagementDisplayFormatId,
  toDetailDisplayFormat,
} from "../../../main/ts/contentExplorer/displayFormatMap";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

describe("displayFormatMap", () => {
  it("maps known CX sources to DetailColumnId values", () => {
    const cols = mapDisplayFormatToDetailColumns([
      { source: "sys_title" },
      { source: "sys_contenttypename" },
      { source: "sys_contentlastmodifieddate" },
      { source: "sys_workflow" },
      { source: "unknown_field" },
    ]);
    expect(cols).toEqual(["title", "type", "modified", "workflow"]);
  });

  it("returns empty when no columns map", () => {
    expect(mapDisplayFormatToDetailColumns([{ source: "nope" }])).toEqual([]);
    expect(toDetailDisplayFormat([{ source: "nope" }])).toBeUndefined();
  });

  it("skips null column entries without throwing", () => {
    const cols = mapDisplayFormatToDetailColumns([
      null as unknown as { source: string },
      { source: "sys_title" },
      undefined as unknown as { source: string },
      { source: "path" },
    ]);
    expect(cols).toEqual(["title", "path"]);
  });

  it("toDetailDisplayFormat wraps mapped columns", () => {
    expect(
      toDetailDisplayFormat([{ source: "name" }, { source: "path" }]),
    ).toEqual({ columns: ["name", "path"] });
  });

  it("resolvePathItemProperty reads displayProperties case-insensitively", () => {
    const item: PSPathItem = {
      name: "n",
      path: "/Sites/x",
      displayProperties: {
        Sys_Title: "Hello",
        sys_workflow: "Simple Workflow",
      },
    };
    expect(resolvePathItemProperty(item, "sys_title")).toBe("Hello");
    expect(resolvePathItemProperty(item, "sys_workflow")).toBe(
      "Simple Workflow",
    );
    expect(resolvePathItemProperty(item, "missing")).toBe("");
  });

  it("displayFormatOptionKey prefers displayId", () => {
    expect(displayFormatOptionKey({ displayId: 7, name: "x" })).toBe("7");
    expect(displayFormatOptionKey({ name: "Default" })).toBe("Default");
  });

  it("resolvePathmanagementDisplayFormatId ignores displayId 0 and names", () => {
    expect(resolvePathmanagementDisplayFormatId({ displayId: 0, name: "FolderList" })).toBe(
      "",
    );
    expect(
      resolvePathmanagementDisplayFormatId({
        displayId: 0,
        guid: { uuid: 12, stringValue: "0-31-12" },
      }),
    ).toBe("12");
    expect(
      resolvePathmanagementDisplayFormatId({
        guidString: "0-31-9",
      }),
    ).toBe("9");
    expect(resolvePathmanagementDisplayFormatId({ displayId: 3 })).toBe("3");
  });

  it("displayFormatOptionKey does not use displayId 0 as the option value", () => {
    expect(
      displayFormatOptionKey({ displayId: 0, name: "FolderList" }),
    ).toBe("FolderList");
    expect(
      displayFormatOptionKey({
        displayId: 0,
        name: "FolderList",
        guidString: "0-31-4",
      }),
    ).toBe("4");
  });

  it("isNumericDisplayFormatId accepts positive integers only", () => {
    expect(isNumericDisplayFormatId("3")).toBe(true);
    expect(isNumericDisplayFormatId(" 12 ")).toBe(true);
    expect(isNumericDisplayFormatId("0")).toBe(false);
    expect(isNumericDisplayFormatId("FolderList")).toBe(false);
    expect(isNumericDisplayFormatId("")).toBe(false);
    expect(isNumericDisplayFormatId(null)).toBe(false);
  });
});
