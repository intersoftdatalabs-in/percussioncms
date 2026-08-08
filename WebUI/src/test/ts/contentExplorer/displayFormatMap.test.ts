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
  mapDisplayFormatToDetailColumns,
  resolvePathItemProperty,
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
});
