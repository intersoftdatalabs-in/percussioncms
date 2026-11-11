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

/**
 * 008 US3 (T021): new-content menu listing — exactly the allowed types,
 * host action excluded, template branch entries preserved (FR-006/007).
 */
import { describe, expect, it } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  contentTypeChoicesFromActions,
  isNewItemHostActionName,
} from "../../../main/ts/contentExplorer/contentTypeChoices";

function action(name: string, label?: string): MenuAction {
  return { name, label: label ?? name, sortRank: 1, menuType: "MENUITEM" } as MenuAction;
}

describe("new-content type listing (FR-006)", () => {
  it("lists exactly the allowed types for the folder", () => {
    const choices = contentTypeChoicesFromActions([
      action("percPage", "Page"),
      action("percFileAsset", "File"),
      action("rffImage", "Image"),
    ]);
    expect(choices.map((c) => c.name)).toEqual(["percPage", "percFileAsset", "rffImage"]);
  });

  it("excludes the host create action itself", () => {
    expect(isNewItemHostActionName("create_new_item")).toBe(true);
    const choices = contentTypeChoicesFromActions([
      action("create_new_item", "New"),
      action("percPage", "Page"),
    ]);
    expect(choices.map((c) => c.name)).toEqual(["percPage"]);
  });

  it("de-dupes and falls back to the action name for blank labels", () => {
    const choices = contentTypeChoicesFromActions([
      action("percPage", "Page"),
      action("percPage", "Page"),
      action("rffTextBlock", ""),
    ]);
    expect(choices).toEqual([
      { name: "percPage", label: "Page" },
      { name: "rffTextBlock", label: "rffTextBlock" },
    ]);
  });

  it("returns empty when no usable actions exist", () => {
    expect(contentTypeChoicesFromActions([])).toEqual([]);
    expect(contentTypeChoicesFromActions(null)).toEqual([]);
  });
});
