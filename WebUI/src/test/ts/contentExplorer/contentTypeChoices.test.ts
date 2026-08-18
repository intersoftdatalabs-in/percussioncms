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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  contentTypeChoicesFromActions,
  isNewItemHostActionName,
  loadAllowedContentTypes,
} from "../../../main/ts/contentExplorer/contentTypeChoices";

afterEach(() => {
  vi.restoreAllMocks();
});

function action(
  partial: Partial<MenuAction> & Pick<MenuAction, "name">,
): MenuAction {
  return {
    label: partial.label ?? partial.name,
    sortRank: partial.sortRank ?? 0,
    menuType: partial.menuType ?? "MENUITEM",
    ...partial,
  };
}

describe("contentTypeChoices", () => {
  it("treats New / Create_New_Item as host names", () => {
    expect(isNewItemHostActionName("New")).toBe(true);
    expect(isNewItemHostActionName("Create_New_Item")).toBe(true);
    expect(isNewItemHostActionName("rffEvent")).toBe(false);
  });

  it("maps action-menu leaves and skips New Item hosts", () => {
    const choices = contentTypeChoicesFromActions([
      action({ name: "New", label: "New Item" }),
      action({ name: "rffEvent", label: "Event" }),
      action({ name: "rffEvent", label: "Event dup" }),
      action({ name: "percFile", label: "File" }),
    ]);
    expect(choices).toEqual([
      { name: "rffEvent", label: "Event" },
      { name: "percFile", label: "File" },
    ]);
  });

  it("prefers find/types menus over the content-types catalog", async () => {
    vi.spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            ActionMenuList: [
              { name: "percPage", label: "Page", sortRank: 0, menuType: "MENUITEM" },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ ContentType: [{ name: "ignored" }] }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    const types = await loadAllowedContentTypes();
    expect(types).toEqual([{ name: "percPage", label: "Page" }]);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  it("falls back to GET /contenttypes when find/types is empty", async () => {
    vi.spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ ActionMenuList: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            ContentType: [{ name: "percFile", label: "File" }],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    const types = await loadAllowedContentTypes();
    expect(types).toEqual([{ name: "percFile", label: "File" }]);
  });
});
