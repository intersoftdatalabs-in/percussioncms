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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PATHS } from "../../../../main/ts/api/paths";
import {
  ACTION_MENU_DESIGN_GAPS,
  ACTION_MENU_VISIBILITY_ALIASES,
  createActionMenu,
  deleteActionMenu,
  getActionMenuDetail,
  listActionMenus,
  isActionMenuWriteReady,
  isValidActionMenuName,
  normalizeActionMenuName,
  saveActionMenu,
  saveActionMenuChildren,
  unwrapActionMenu,
  unwrapActionMenuChildren,
  unwrapActionMenuList,
  visibilityContextValue,
  withoutStaleActionMenuWriteGap,
  wrapActionMenuChildrenForWire,
  wrapActionMenuForWire,
} from "../../../../main/ts/api/developer/actionMenusApi";

describe("unwrapActionMenu (#3380)", () => {
  it("unwraps Jackson ActionMenu root envelope so guid is reachable", () => {
    const unwrapped = unwrapActionMenu({
      ActionMenu: {
        name: "Edit",
        id: 42,
        guid: { stringValue: "0-107-42", type: 107, uuid: 42 },
      },
    });
    expect(unwrapped.name).toBe("Edit");
    expect(unwrapped.guid?.stringValue).toBe("0-107-42");
    expect(unwrapped.guidString).toBe("0-107-42");
  });

  it("synthesizes 0-107-{id} when Guid is omitted", () => {
    const unwrapped = unwrapActionMenu({ name: "Edit", id: 9 });
    expect(unwrapped.guid?.stringValue).toBe("0-107-9");
    expect(unwrapped.guidString).toBe("0-107-9");
  });

  it("uses guidString when nested guid is absent", () => {
    const unwrapped = unwrapActionMenu({ name: "Edit", guidString: "0-107-7" });
    expect(unwrapped.guid?.stringValue).toBe("0-107-7");
    expect(unwrapped.guidString).toBe("0-107-7");
  });

  it("normalizes visibilityContexts value from values alias", () => {
    const unwrapped = unwrapActionMenu({
      name: "Edit",
      visibilityContexts: [{ name: "community", values: "1001" }],
    });
    expect(unwrapped.visibilityContexts).toEqual([
      { name: "community", description: "", value: "1001" },
    ]);
  });

  it("preserves partialOverlay from GET detail", () => {
    const unwrapped = unwrapActionMenu({
      name: "Edit",
      partialOverlay: true,
      visibilityContexts: [],
    });
    expect(unwrapped.partialOverlay).toBe(true);
  });

  it("returns empty object for null payload", () => {
    expect(unwrapActionMenu(null)).toEqual({});
  });
});

describe("visibilityContextValue / ACTION_MENU_VISIBILITY_ALIASES", () => {
  it("returns empty string when value and values are both empty", () => {
    expect(visibilityContextValue({ name: "community" })).toBe("");
    expect(visibilityContextValue({ name: "community", value: "" })).toBe("");
    expect(visibilityContextValue(null)).toBe("");
  });

  it("includes REST aliases role, locale, workflow, and publishableType", () => {
    expect(ACTION_MENU_VISIBILITY_ALIASES).toEqual(
      expect.arrayContaining(["role", "locale", "workflow", "publishableType", "roles", "community"]),
    );
  });
});

describe("unwrapActionMenuList", () => {
  it("normalizes each list row GUID", () => {
    const list = unwrapActionMenuList({
      ActionMenu: [
        { name: "Edit", id: 2 },
        { name: "Open", guidString: "0-107-3" },
      ],
    });
    expect(list).toHaveLength(2);
    expect(list[0].guidString).toBe("0-107-2");
    expect(list[1].guidString).toBe("0-107-3");
  });
});

describe("action menu name validation", () => {
  it("trims names", () => {
    expect(normalizeActionMenuName("  MyMenu  ")).toBe("MyMenu");
    expect(normalizeActionMenuName("")).toBe("");
    expect(normalizeActionMenuName(null)).toBe("");
  });

  it("accepts REST-safe create names and rejects junk", () => {
    expect(isValidActionMenuName("MyMenu")).toBe(true);
    expect(isValidActionMenuName("qa4112")).toBe(true);
    expect(isValidActionMenuName("  UserOne  ")).toBe(true);
    expect(isValidActionMenuName("")).toBe(false);
    expect(isValidActionMenuName("has space")).toBe(false);
    expect(isValidActionMenuName("wild*card")).toBe(false);
    expect(isValidActionMenuName("pct%name")).toBe(false);
    expect(isValidActionMenuName("../x")).toBe(false);
    expect(isValidActionMenuName("a/b")).toBe(false);
    expect(isValidActionMenuName("a\\b")).toBe(false);
    expect(isValidActionMenuName("foo\u00A0bar")).toBe(false);
    expect(isValidActionMenuName("foo\u3000bar")).toBe(false);
    expect(isValidActionMenuName("foo\u200Bbar")).toBe(false);
  });

  it("disables write until the menu name is valid on create", () => {
    expect(isActionMenuWriteReady({ isNew: true, name: "" })).toBe(false);
    expect(isActionMenuWriteReady({ isNew: true, name: "has space" })).toBe(false);
    expect(isActionMenuWriteReady({ isNew: true, name: "MyMenu" })).toBe(true);
    expect(isActionMenuWriteReady({ isNew: false, name: "MyMenu" })).toBe(true);
    expect(isActionMenuWriteReady({ isNew: false, name: "" })).toBe(false);
  });
});

describe("action menu wire wrap", () => {
  it("wraps POST/PUT under ActionMenu root", () => {
    expect(
      wrapActionMenuForWire({
        name: "MyMenu",
        label: "My Menu",
        description: "Created via REST",
        menuType: "MENUITEM",
      }),
    ).toEqual({
      ActionMenu: {
        name: "MyMenu",
        label: "My Menu",
        description: "Created via REST",
        menuType: "MENUITEM",
      },
    });
  });

  it("drops the create/update/delete and cascading-children gaps from ACTION_MENU_DESIGN_GAPS", () => {
    expect(ACTION_MENU_DESIGN_GAPS.some((g) => /create/i.test(g))).toBe(false);
    expect(ACTION_MENU_DESIGN_GAPS.some((g) => /usage/i.test(g))).toBe(false);
    expect(ACTION_MENU_DESIGN_GAPS.some((g) => /cascading/i.test(g))).toBe(false);
    expect(ACTION_MENU_DESIGN_GAPS.some((g) => /visibility/i.test(g))).toBe(false);
  });

  it("filters stale REST write and children gaps on GET detail", () => {
    expect(
      withoutStaleActionMenuWriteGap([
        "Action menu create / update / delete not supported via this API",
        "Usage / command / visibility tab completeness is a later slice.",
        "Visibility context editing not supported via this API",
        "Cascading child menu composition not supported via this API",
        "Visibility context editing not supported via this API",
      ]),
    ).toEqual([]);
  });

  it("wraps children PUT under ActionMenuList", () => {
    expect(wrapActionMenuChildrenForWire([{ name: "ChildA" }, { name: "ChildB" }])).toEqual({
      ActionMenuList: [{ name: "ChildA" }, { name: "ChildB" }],
    });
  });
});

describe("unwrapActionMenuChildren", () => {
  it("unwraps nested array and ActionMenuList envelopes", () => {
    expect(unwrapActionMenuChildren([{ name: "Open", id: 8 }]).map((c) => c.name)).toEqual(["Open"]);
    expect(
      unwrapActionMenuChildren({ ActionMenuList: [{ name: "Copy" }] }).map((c) => c.name),
    ).toEqual(["Copy"]);
  });

  it("attaches unwrapped children on GET detail", () => {
    const unwrapped = unwrapActionMenu({
      ActionMenu: {
        name: "Parent",
        children: { ActionMenu: [{ name: "Open", id: 3 }] },
      },
    });
    expect(unwrapped.children?.map((c) => c.name)).toEqual(["Open"]);
    expect(unwrapped.children?.[0].guidString).toBe("0-107-3");
  });

  it("drops empty-value visibility rows from the wire wrap", () => {
    expect(
      wrapActionMenuForWire({
        name: "MyMenu",
        visibilityContexts: [
          { name: "community", value: "1001" },
          { name: "contentType", value: "" },
        ],
      }),
    ).toEqual({
      ActionMenu: {
        name: "MyMenu",
        visibilityContexts: [{ name: "community", value: "1001" }],
      },
    });
  });

  it("wraps PUT usage/command/visibility under ActionMenu root", () => {
    expect(
      wrapActionMenuForWire({
        name: "MyMenu",
        label: "My Menu",
        handler: "SERVER",
        parameters: [{ name: "sys_test", value: "1" }],
        visibilityContexts: [{ name: "community", value: "1001" }],
      }),
    ).toEqual({
      ActionMenu: {
        name: "MyMenu",
        label: "My Menu",
        handler: "SERVER",
        parameters: [{ name: "sys_test", value: "1" }],
        visibilityContexts: [{ name: "community", value: "1001" }],
      },
    });
  });
});

describe("actionMenusApi write paths", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs create body to /services/actions", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "MyMenu", label: "My Menu", menuType: "MENUITEM" }),
    );
    const saved = await createActionMenu({
      name: "MyMenu",
      label: "My Menu",
      menuType: "MENUITEM",
    });
    expect(saved.name).toBe("MyMenu");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.ACTION_MENUS_ROOT);
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("/catalog");
    expect(JSON.parse(String(init.body))).toEqual({
      ActionMenu: { name: "MyMenu", label: "My Menu", menuType: "MENUITEM" },
    });
  });

  it("PUTs save body to /services/actions/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "MyMenu", label: "Updated" }));
    const saved = await saveActionMenu("MyMenu", {
      name: "MyMenu",
      label: "Updated",
      handler: "SERVER",
    });
    expect(saved.label).toBe("Updated");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.ACTION_MENUS_ROOT}/MyMenu`);
    expect(JSON.parse(String(init.body))).toEqual({
      ActionMenu: { name: "MyMenu", label: "Updated", handler: "SERVER" },
    });
  });

  it("PUTs children to /services/actions/{idOrName}/children", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        name: "ParentMenu",
        menuType: "MENU",
        children: [{ name: "ChildA" }, { name: "ChildB" }],
      }),
    );
    const saved = await saveActionMenuChildren("ParentMenu", [
      { name: "ChildA" },
      { name: "ChildB" },
    ]);
    expect(saved.children?.map((c) => c.name)).toEqual(["ChildA", "ChildB"]);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.ACTION_MENUS_ROOT}/ParentMenu/children`,
    );
    expect(JSON.parse(String(init.body))).toEqual({
      ActionMenuList: [{ name: "ChildA" }, { name: "ChildB" }],
    });
  });

  it("DELETEs /services/actions/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteActionMenu("MyMenu");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.ACTION_MENUS_ROOT}/MyMenu`);
  });

  it("applies design-gap fallback to catalog list rows", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ActionMenu: [{ name: "Edit", id: 2 }] }),
    );
    const list = await listActionMenus();
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe("Edit");
    expect(list[0].designGaps).toEqual(ACTION_MENU_DESIGN_GAPS);
  });

  it("unwraps GET /services/actions/catalog/{idOrName} Jackson root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ActionMenu: { name: "MyMenu", label: "My Menu", parameters: [] } }),
    );
    const detail = await getActionMenuDetail("MyMenu");
    expect(detail.name).toBe("MyMenu");
    expect(detail.label).toBe("My Menu");
  });

  it("surfaces 400 invalid name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "name cannot contain whitespace" }, 400),
    );
    await expect(createActionMenu({ name: "bad name" })).rejects.toMatchObject({
      status: 400,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Action menu already exists: MyMenu" }, 409),
    );
    await expect(createActionMenu({ name: "MyMenu" })).rejects.toMatchObject({
      status: 409,
    });
  });

  it("surfaces 404 missing action menu", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Action menu not found" }, 404));
    await expect(getActionMenuDetail("missing")).rejects.toMatchObject({ status: 404 });
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Action menu not found" }, 404));
    await expect(deleteActionMenu("missing")).rejects.toMatchObject({ status: 404 });
  });

  it("surfaces 403 non-Admin create", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(createActionMenu({ name: "MyMenu" })).rejects.toMatchObject({
      status: 403,
    });
  });
});
