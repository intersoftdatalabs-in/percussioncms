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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  isNonWorkflowedItem,
  isWorkflowEligibleItem,
} from "../../../main/ts/contentExplorer/workflowEligibility";

function item(partial: Partial<PSPathItem> & { name: string; path: string }): PSPathItem {
  return partial;
}

describe("workflowEligibility (#3330)", () => {
  it("rejects server Folder / FSFolder types even when they have a content id", () => {
    const folder: PSPathItem = {
      id: "16777215-101-703",
      name: "New-Folder",
      path: "/Folders/New-Folder/",
      type: "Folder",
      leaf: true,
    };
    expect(isNonWorkflowedItem(folder)).toBe(true);
    expect(isWorkflowEligibleItem(folder)).toBe(false);

    const fs: PSPathItem = {
      id: "9",
      name: "Design",
      path: "/Design/",
      type: "FSFolder",
    };
    expect(isWorkflowEligibleItem(fs)).toBe(false);
  });

  it("rejects workflowId -1 / 0 sentinels on otherwise item-shaped rows", () => {
    expect(
      isWorkflowEligibleItem({
        id: "42",
        name: "ghost",
        path: "/Folders/ghost",
        type: "percPage",
        displayProperties: { sys_workflowid: -1 },
      }),
    ).toBe(false);
    expect(
      isNonWorkflowedItem({
        id: "42",
        name: "ghost",
        path: "/Folders/ghost",
        type: "percPage",
        displayProperties: { workflowId: "0" },
      }),
    ).toBe(true);
  });

  it("accepts a page with a real id and no invalid workflow sentinel", () => {
    const page = item({
      id: "33554432-101-1",
      name: "Home",
      path: "/Sites/Demo/Home",
      type: "page",
      leaf: true,
    });
    expect(isWorkflowEligibleItem(page)).toBe(true);
    expect(isNonWorkflowedItem(page)).toBe(false);
  });
});
