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

/**
 * Vitest contract tests for client-side Workbench visibility evaluation
 * (#6517 / parent #4530). Locks the documented mapping against
 * {@code PSActionVisibilityChecker} hide-on-match semantics so
 * Workbench-configured rules shape the React menus the way they shaped
 * DCE — and locks the permissive default for everything the client
 * cannot resolve (unknown names, missing state, uiContexts).
 */

import { describe, expect, it } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  filterMenuActionsByVisibility,
  isMenuVisibleByVisibilityContexts,
  normalizeVisibilityContextName,
  visibilityContextValues,
  VISIBILITY_CONTEXT_CHECKOUT_STATUS,
  VISIBILITY_CONTEXT_CLIENT_CONTEXT,
  VISIBILITY_CONTEXT_COMMUNITY,
  VISIBILITY_CONTEXT_CONTENT_TYPE,
  VISIBILITY_CONTEXT_FOLDER_SECURITY,
  VISIBILITY_CONTEXT_LOCALES,
  VISIBILITY_CONTEXT_OBJECT_TYPE,
  VISIBILITY_CONTEXT_ROLES,
  type ExplorerMenuVisibilityState,
} from "../../../main/ts/contentExplorer/menuVisibility";

function menu(
  name: string,
  overrides: Partial<MenuAction> = {},
): MenuAction {
  return {
    name,
    label: name,
    sortRank: 1,
    menuType: "MENUITEM",
    ...overrides,
  };
}

const EMPTY_STATE: ExplorerMenuVisibilityState = {};

describe("normalizeVisibilityContextName", () => {
  it("accepts numeric ids 1-11", () => {
    expect(normalizeVisibilityContextName("6")).toBe(
      VISIBILITY_CONTEXT_CHECKOUT_STATUS,
    );
    expect(normalizeVisibilityContextName("11")).toBe(
      VISIBILITY_CONTEXT_FOLDER_SECURITY,
    );
  });

  it("accepts Workbench alias spellings case-insensitively", () => {
    expect(normalizeVisibilityContextName("checkoutStatus")).toBe(
      VISIBILITY_CONTEXT_CHECKOUT_STATUS,
    );
    expect(normalizeVisibilityContextName("ROLES")).toBe(
      VISIBILITY_CONTEXT_ROLES,
    );
    expect(normalizeVisibilityContextName("  locales  ")).toBe(
      VISIBILITY_CONTEXT_LOCALES,
    );
  });

  it("returns null for blank, unknown, and non-string names", () => {
    expect(normalizeVisibilityContextName("")).toBeNull();
    expect(normalizeVisibilityContextName("nonsense")).toBeNull();
    expect(normalizeVisibilityContextName("12")).toBeNull();
    expect(normalizeVisibilityContextName(null)).toBeNull();
  });
});

describe("visibilityContextValues", () => {
  it("reads the singular wire value", () => {
    expect(
      visibilityContextValues({ name: "6", value: "Checked Out" } as never),
    ).toEqual(["Checked Out"]);
  });

  it("drops blanks and stringifies numbers", () => {
    expect(
      visibilityContextValues({ name: "3", value: "  " } as never),
    ).toEqual([]);
    expect(visibilityContextValues(null)).toEqual([]);
  });
});

describe("isMenuVisibleByVisibilityContexts", () => {
  it("keeps menus with no contexts", () => {
    expect(isMenuVisibleByVisibilityContexts(menu("Open"), EMPTY_STATE)).toBe(
      true,
    );
  });

  it("keeps menus whose context names are unknown", () => {
    const action = menu("Open", {
      visibilityContexts: [{ name: "future-context", value: "x" } as never],
    });
    expect(isMenuVisibleByVisibilityContexts(action, EMPTY_STATE)).toBe(true);
  });

  it("hides on checkout-status match, including legacy aliases", () => {
    const action = menu("Edit", {
      visibilityContexts: [{ name: "6", value: "Checked Out" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(action, {
        checkoutStatus: "Checked Out",
      }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(action, {
        checkoutStatus: "Checked Out By Me",
      }),
    ).toBe(true);
    const legacy = menu("Edit", {
      visibilityContexts: [{ name: "checkoutstatus", value: "MySelf" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(legacy, {
        checkoutStatus: "Checked Out By Me",
      }),
    ).toBe(false);
  });

  it("stays permissive when the checkout status is unknown", () => {
    const action = menu("Edit", {
      visibilityContexts: [{ name: "6", value: "Checked Out" } as never],
    });
    expect(isMenuVisibleByVisibilityContexts(action, EMPTY_STATE)).toBe(true);
  });

  it("hides on community / locale / client-context match", () => {
    const byCommunity = menu("A", {
      visibilityContexts: [{ name: "2", value: "1001" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(byCommunity, { communityId: 1001 }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(byCommunity, { communityId: 1002 }),
    ).toBe(true);

    const byLocale = menu("B", {
      visibilityContexts: [{ name: "locales", value: "fr-fr" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(byLocale, { locale: "FR-FR" }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(byLocale, { locale: "en-us" }),
    ).toBe(true);

    const byClient = menu("C", {
      visibilityContexts: [{ name: "5", value: "multiselect" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(byClient, {
        clientContext: "MultiSelect",
      }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(byClient, {
        clientContext: "browse",
      }),
    ).toBe(true);
  });

  it("hides on numeric content/object-type match", () => {
    const byType = menu("T", {
      visibilityContexts: [{ name: "3", value: "123" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(byType, { contentTypeId: 123 }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(byType, { contentTypeId: 124 }),
    ).toBe(true);

    const byObject = menu("O", {
      visibilityContexts: [{ name: "objecttype", value: "2" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(byObject, { objectType: 2 }),
    ).toBe(false);
  });

  it("hides on roles only when every user role is listed", () => {
    const action = menu("R", {
      visibilityContexts: [
        { name: "7", value: "Admin" } as never,
        { name: "7", value: "Editor" } as never,
      ],
    });
    expect(
      isMenuVisibleByVisibilityContexts(action, { userRoles: ["Admin"] }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(action, {
        userRoles: ["Admin", "Designer"],
      }),
    ).toBe(true);
    // No known roles: cannot evaluate, stays visible.
    expect(isMenuVisibleByVisibilityContexts(action, EMPTY_STATE)).toBe(true);
  });

  it("hides non-multi actions while several rows are checked", () => {
    const single = menu("Take Down");
    expect(
      isMenuVisibleByVisibilityContexts(single, { multiSelected: true }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(single, { multiSelected: false }),
    ).toBe(true);
    const multi = menu("Stage", {
      properties: [{ name: "SupportsMultiSelect", value: "yes" }],
    });
    expect(
      isMenuVisibleByVisibilityContexts(multi, { multiSelected: true }),
    ).toBe(true);
  });

  it("hides below-band rows on folder-security values", () => {
    const action = menu("Secure", {
      visibilityContexts: [{ name: "11", value: "write" } as never],
    });
    expect(
      isMenuVisibleByVisibilityContexts(action, { accessLevel: "READ" }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(action, { accessLevel: "ADMIN" }),
    ).toBe(true);
    // Unknown band: permissive.
    expect(isMenuVisibleByVisibilityContexts(action, EMPTY_STATE)).toBe(true);
  });

  it("never hides on uiContexts alone (server gap stays permissive)", () => {
    const action = menu("Mode", {
      uiContexts: [
        {
          modeId: "999",
          modeName: "SomeWorkbenchMode",
          contextId: "1",
          contextName: "ctx",
        },
      ],
    });
    expect(isMenuVisibleByVisibilityContexts(action, EMPTY_STATE)).toBe(true);
  });
});

describe("filterMenuActionsByVisibility", () => {
  it("drops hidden roots but keeps visible siblings", () => {
    const menus = [
      menu("Keep"),
      menu("Hide", {
        visibilityContexts: [
          { name: VISIBILITY_CONTEXT_COMMUNITY, value: "7" } as never,
        ],
      }),
    ];
    const out = filterMenuActionsByVisibility(menus, { communityId: "7" });
    expect(out.map((entry) => entry.name)).toEqual(["Keep"]);
  });

  it("drops a cascade parent when every child is hidden", () => {
    const menus = [
      menu("Parent", {
        children: [
          menu("Child", {
            visibilityContexts: [
              { name: VISIBILITY_CONTEXT_CLIENT_CONTEXT, value: "x" } as never,
            ],
          }),
        ],
      }),
    ];
    expect(
      filterMenuActionsByVisibility(menus, { clientContext: "x" }),
    ).toEqual([]);
    expect(
      filterMenuActionsByVisibility(menus, { clientContext: "y" }),
    ).toHaveLength(1);
  });

  it("keeps visible children under a visible parent", () => {
    const menus = [
      menu("Parent", {
        menuType: "MENU",
        children: [
          menu("VisibleChild"),
          menu("HiddenChild", {
            visibilityContexts: [
              {
                name: VISIBILITY_CONTEXT_OBJECT_TYPE,
                value: "9",
              } as never,
            ],
          }),
        ],
      }),
    ];
    const out = filterMenuActionsByVisibility(menus, { objectType: 9 });
    expect(out).toHaveLength(1);
    expect(out[0].children?.map((child) => child.name)).toEqual([
      "VisibleChild",
    ]);
  });

  it("passes legacy envelope children through untouched", () => {
    const menus = [
      menu("View", {
        menuType: "MENU",
        children: {
          ActionMenuList: [
            {
              name: "View_Properties",
              label: "View Properties",
              sortRank: 1,
              menuType: "MENUITEM",
            },
          ],
        } as unknown as MenuAction[],
      }),
    ];
    const out = filterMenuActionsByVisibility(menus, EMPTY_STATE);
    expect(out).toHaveLength(1);
    expect(out[0].children).toEqual(menus[0].children);
  });
});
