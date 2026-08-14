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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  loadExplorerMenuCatalog,
  mergeContentTypeMenusIntoCatalog,
  NEW_ITEM_HOST_PREFERRED_KEYS,
  parseExplorerContentId,
} from "../../../main/ts/contentExplorer/menuCatalogLoad";

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

describe("parseExplorerContentId", () => {
  it("parses finite numeric ids and rejects junk", () => {
    expect(parseExplorerContentId("42")).toBe(42);
    expect(parseExplorerContentId(7)).toBe(7);
    expect(parseExplorerContentId(undefined)).toBeNull();
    expect(parseExplorerContentId("")).toBeNull();
    expect(parseExplorerContentId("nope")).toBeNull();
  });
});

describe("mergeContentTypeMenusIntoCatalog", () => {
  it("does not replace the cascade tree with a flat type-menu dump (#3379)", () => {
    const tree: MenuAction[] = [
      action({
        name: "file",
        label: "File",
        menuType: "MENU",
        children: [action({ name: "open" }), action({ name: "saveAs" })],
      }),
      action({ name: "preview" }),
    ];
    const typeMenus: MenuAction[] = [
      action({ name: "new-page", label: "Page" }),
      action({ name: "new-file", label: "File item" }),
    ];
    const merged = mergeContentTypeMenusIntoCatalog(tree, typeMenus);
    expect(merged.map((a) => a.name)).toEqual(["file", "preview"]);
    expect(merged[0]?.children?.map((c) => c.name)).toEqual([
      "open",
      "saveAs",
      "new-page",
      "new-file",
    ]);
    expect(merged.some((a) => a.name === "new-page")).toBe(false);
  });

  it("does not mutate the input tree", () => {
    const tree: MenuAction[] = [
      action({
        name: "new",
        menuType: "MENU",
        children: [action({ name: "new-folder" })],
      }),
    ];
    const snapshot = tree[0].children?.length;
    mergeContentTypeMenusIntoCatalog(tree, [action({ name: "new-page" })]);
    expect(tree[0].children?.length).toBe(snapshot);
  });

  it("drops leftover type leaves when there is no MENU host", () => {
    const tree: MenuAction[] = [action({ name: "preview" })];
    const merged = mergeContentTypeMenusIntoCatalog(tree, [
      action({ name: "new-page" }),
    ]);
    expect(merged.map((a) => a.name)).toEqual(["preview"]);
  });

  it("returns a copy when type menus are empty", () => {
    const tree: MenuAction[] = [action({ name: "open" })];
    const merged = mergeContentTypeMenusIntoCatalog(tree, []);
    expect(merged).toEqual(tree);
    expect(merged).not.toBe(tree);
  });

  it("nests leftover type leaves under a Create MENU host", () => {
    expect(NEW_ITEM_HOST_PREFERRED_KEYS).toContain("create");
    const tree: MenuAction[] = [
      action({
        name: "create",
        label: "Create",
        menuType: "MENU",
        children: [action({ name: "folder" })],
      }),
    ];
    const merged = mergeContentTypeMenusIntoCatalog(tree, [
      action({ name: "new-page" }),
    ]);
    expect(merged.map((a) => a.name)).toEqual(["create"]);
    expect(merged[0]?.children?.map((c) => c.name)).toEqual([
      "folder",
      "new-page",
    ]);
  });

  it("treats a Create label as a New-item host", () => {
    const tree: MenuAction[] = [
      action({
        name: "items",
        label: "Create items",
        menuType: "MENU",
        children: [action({ name: "folder" })],
      }),
    ];
    const merged = mergeContentTypeMenusIntoCatalog(tree, [
      action({ name: "new-page" }),
    ]);
    expect(merged[0]?.children?.map((c) => c.name)).toContain("new-page");
  });
});

describe("loadExplorerMenuCatalog", () => {
  it("always loads find() and keeps MENU children when types are also returned", async () => {
    const findPayload = {
      ActionMenu: [
        {
          id: 8,
          name: "file",
          label: "File",
          sortRank: 0,
          menuType: "MENU",
          children: [
            { id: 2, name: "open", sortRank: 1, menuType: "MENUITEM" },
          ],
        },
      ],
    };
    const typesPayload = {
      ActionMenuList: [
        { id: 90, name: "new-page", sortRank: 0, menuType: "MENUITEM" },
      ],
    };
    vi.spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(JSON.stringify(findPayload), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify(typesPayload), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );

    const actions = await loadExplorerMenuCatalog({
      id: "101",
      name: "Home",
      type: "percPage",
      path: "/Sites/Demo/Home",
      folderPath: "/Sites/Demo",
      displayProperties: { workflowId: "5" },
    } as never);

    expect(actions.map((a) => a.name)).toEqual(["file"]);
    expect(actions[0]?.children?.map((c) => c.name)).toContain("open");
    expect(actions[0]?.children?.map((c) => c.name)).toContain("new-page");
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });

  it("uses only find() for folder-only selection", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          ActionMenu: [
            { id: 1, name: "new-folder", sortRank: 0, menuType: "MENUITEM" },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const actions = await loadExplorerMenuCatalog({
      id: "1",
      name: "Sites",
      type: "Folder",
      path: "/Sites",
      folderPath: "/Sites",
    } as never);
    expect(actions.map((a) => a.name)).toEqual(["new-folder"]);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});
