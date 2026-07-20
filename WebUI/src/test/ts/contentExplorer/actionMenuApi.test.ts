/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { ActionMenu } from "../../../main/ts/api/contentExplorer/types";
import {
  findActions,
  findAllowedContentTypeMenus,
  findAllowedTemplateMenus,
  mapActionMenusToMenuActions,
} from "../../../main/ts/api/contentExplorer/actionMenuApi";
import { PATHS } from "../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

function makeMenu(partial: Partial<ActionMenu>): ActionMenu {
  return {
    id: 1,
    name: "open",
    sortRank: 0,
    menuType: "MENUITEM",
    ...partial,
  };
}

describe("mapActionMenusToMenuActions", () => {
  it("sorts the top-level actions by sortRank ascending", () => {
    const menus: ActionMenu[] = [
      makeMenu({ name: "rename", sortRank: 20 }),
      makeMenu({ name: "open", sortRank: 10 }),
      makeMenu({ name: "delete", sortRank: 30 }),
    ];
    const actions = mapActionMenusToMenuActions(menus);
    expect(actions.map((a) => a.name)).toEqual(["open", "rename", "delete"]);
  });

  it("flattens cascading children under each parent menu", () => {
    const menus: ActionMenu[] = [
      makeMenu({
        name: "file",
        menuType: "MENU",
        sortRank: 0,
        children: {
          ActionMenuList: [
            makeMenu({ name: "save", sortRank: 1 }),
            makeMenu({ name: "saveAs", sortRank: 2 }),
          ],
        },
      }),
    ];
    const [file] = mapActionMenusToMenuActions(menus);
    expect(file?.children?.map((c) => c.name)).toEqual(["save", "saveAs"]);
  });

  it("falls back to name when label is absent", () => {
    const menus: ActionMenu[] = [makeMenu({ name: "rename", label: undefined })];
    const [rename] = mapActionMenusToMenuActions(menus);
    expect(rename?.label).toBe("rename");
  });

  it("preserves label, url, handler, and description when present", () => {
    const menus: ActionMenu[] = [
      makeMenu({
        name: "edit",
        label: "Edit Item",
        url: "/edit",
        handler: "client",
        description: "Open the item in the editor",
      }),
    ];
    const [edit] = mapActionMenusToMenuActions(menus);
    expect(edit).toEqual({
      name: "edit",
      label: "Edit Item",
      url: "/edit",
      handler: "client",
      description: "Open the item in the editor",
      sortRank: 0,
      menuType: "MENUITEM",
      parameters: undefined,
      children: undefined,
    });
  });

  it("omits the children key when there are none", () => {
    const menus: ActionMenu[] = [makeMenu({ name: "delete" })];
    const [deleteAction] = mapActionMenusToMenuActions(menus);
    expect(deleteAction.children).toBeUndefined();
    expect("children" in deleteAction).toBe(false);
  });

  it("returns an empty array when the input list is empty", () => {
    expect(mapActionMenusToMenuActions([])).toEqual([]);
  });

  it("does not mutate the input menus array (sortRank-based sort is in place of a copy)", () => {
    const original: ActionMenu[] = [
      makeMenu({ name: "rename", sortRank: 20 }),
      makeMenu({ name: "open", sortRank: 10 }),
    ];
    const snapshot = original.slice();
    mapActionMenusToMenuActions(original);
    expect(original.map((m) => m.name)).toEqual(snapshot.map((m) => m.name));
  });
});

describe("actionMenuApi REST wrappers", () => {
  it("findActions unwraps the ActionMenu envelope", async () => {
    const fetchMock = vi
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            ActionMenu: [
              makeMenu({ name: "open", sortRank: 1 }),
              makeMenu({ name: "rename", sortRank: 2 }),
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    const result = await findActions({ name: "open" });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const url = fetchMock.mock.calls[0]?.[0];
    expect(typeof url).toBe("string");
    expect(String(url)).toBe(`${PATHS.ACTIONS_ROOT}/find?name=open`);
    expect(result.map((m) => m.name)).toEqual(["open", "rename"]);
  });

  it("findActions returns [] on envelope miss / empty body", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({}), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await findActions();
    expect(result).toEqual([]);
  });

  it("findAllowedContentTypeMenus posts the {contentIds} body and unwraps ActionMenuList", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          ActionMenuList: [
            makeMenu({ name: "trans1", sortRank: 0 }),
            makeMenu({ name: "trans2", sortRank: 1 }),
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await findAllowedContentTypeMenus([101, 102]);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0]?.[1];
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({ contentIds: [101, 102] });
    expect(result.map((m) => m.name)).toEqual(["trans1", "trans2"]);
  });

  it("findAllowedTemplateMenus passes contentId + isAA query param and unwraps ActionMenuList", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          ActionMenuList: [makeMenu({ name: "tmpl", sortRank: 0 })],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await findAllowedTemplateMenus(42, true);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const url = fetchMock.mock.calls[0]?.[0];
    expect(String(url)).toBe(`${PATHS.ACTIONS_ROOT}/find/templates/42?isAA=true`);
    expect(result.map((m) => m.name)).toEqual(["tmpl"]);
  });

  it("findAllowedTemplateMenus uses isAA=false by default", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ ActionMenuList: [] }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await findAllowedTemplateMenus(7);
    const url = fetchMock.mock.calls[0]?.[0];
    expect(String(url)).toBe(`${PATHS.ACTIONS_ROOT}/find/templates/7?isAA=false`);
  });
});
