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
 * DCE — locks the permissive default for everything the client cannot
 * resolve (unknown names, missing state) — and locks the SPA shell
 * UI-mode claim (#6619) that enforces per-mode uiContexts.
 */

import { describe, expect, it } from "vitest";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  asWireArray,
  filterMenuActionsByVisibility,
  isMenuVisibleByVisibilityContexts,
  isMenuVisibleInShellMode,
  normalizeVisibilityContextName,
  SPA_SHELL_UI_MODE_ID,
  SPA_SHELL_UI_MODE_NAME,
  uiContextMatchesShellMode,
  visibilityContextValues,
  VISIBILITY_CONTEXT_ASSIGNMENT_TYPE,
  VISIBILITY_CONTEXT_CHECKOUT_STATUS,
  VISIBILITY_CONTEXT_CLIENT_CONTEXT,
  VISIBILITY_CONTEXT_COMMUNITY,
  VISIBILITY_CONTEXT_CONTENT_TYPE,
  VISIBILITY_CONTEXT_FOLDER_SECURITY,
  VISIBILITY_CONTEXT_LOCALES,
  VISIBILITY_CONTEXT_OBJECT_TYPE,
  VISIBILITY_CONTEXT_PUBLISHABLE_TYPE,
  VISIBILITY_CONTEXT_ROLES,
  VISIBILITY_CONTEXT_WORKFLOWS,
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

describe("asWireArray", () => {
  it("wraps single wire dicts and passes arrays through", () => {
    expect(asWireArray(null)).toEqual([]);
    expect(asWireArray(undefined)).toEqual([]);
    expect(asWireArray([{ name: "6" }])).toEqual([{ name: "6" }]);
    expect(asWireArray({ name: "6" } as never)).toEqual([{ name: "6" }]);
  });
});

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

  it("accepts numeric wire ids (the H2 seed serves numbers)", () => {
    expect(normalizeVisibilityContextName(6)).toBe(
      VISIBILITY_CONTEXT_CHECKOUT_STATUS,
    );
    expect(normalizeVisibilityContextName(11)).toBe(
      VISIBILITY_CONTEXT_FOLDER_SECURITY,
    );
    expect(normalizeVisibilityContextName(12)).toBeNull();
    expect(normalizeVisibilityContextName(0)).toBeNull();
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

  it("stringifies numeric values and drops blanks", () => {
    expect(
      visibilityContextValues({ name: 1, value: 2 } as never),
    ).toEqual(["2"]);
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

  it("evaluates single-dict wire shapes (H2 seed serves dicts)", () => {
    // Live shape: {"name": 6, "value": "Checked Out", ...} as one object.
    const action = menu("Edit", {
      visibilityContexts: {
        description: "Hide Edit action for Reader.",
        name: 6,
        value: "Checked Out",
      } as never,
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
  });

  it("reads dict properties for the multi-select rule", () => {
    const single = menu("Take Down", {
      properties: {
        name: "SupportsMultiSelect",
        value: "no",
      } as never,
    });
    expect(
      isMenuVisibleByVisibilityContexts(single, { multiSelected: true }),
    ).toBe(false);
    const multi = menu("Stage", {
      properties: { name: "SupportsMultiSelect", value: "YES" } as never,
    });
    expect(
      isMenuVisibleByVisibilityContexts(multi, { multiSelected: true }),
    ).toBe(true);
  });

  it("hides menus scoped exclusively to foreign modes (shell claim #6619)", () => {
    // Supersedes the #6618 permissive lock: the SPA shell claim is now
    // resolved, so foreign-scoped menus hide instead of staying visible.
    const foreign = menu("Mode", {
      uiContexts: [
        {
          modeId: "999",
          modeName: "SomeWorkbenchMode",
          contextId: "1",
          contextName: "ctx",
        },
      ],
    });
    expect(isMenuVisibleByVisibilityContexts(foreign, EMPTY_STATE)).toBe(false);
    const claimed = menu("Mode", {
      uiContexts: [
        {
          modeId: "999",
          modeName: "SomeWorkbenchMode",
          contextId: "1",
          contextName: "ctx",
        },
        {
          modeId: "4",
          modeName: "CXMAIN",
          contextId: "1",
          contextName: "ctx",
        },
      ],
    });
    expect(isMenuVisibleByVisibilityContexts(claimed, EMPTY_STATE)).toBe(true);
  });
});

describe("selection-row identity gaps (#6618)", () => {
  /**
   * Exactly what ContentExplorerShell#menuVisibilityState supplies today:
   * multi-select flag, user name, role identities, locale, row access
   * band. No checkout status, no numeric content/object/workflow/
   * community/assignment ids, no client context, no publishable type —
   * none of those facts are on PSPathItem rows.
   */
  const SHELL_STATE: ExplorerMenuVisibilityState = {
    multiSelected: false,
    userName: "Admin",
    userRoles: ["Admin"],
    locale: "en-us",
    accessLevel: "ADMIN",
  };

  function gapMenu(
    contextId: string,
    value: string | number,
  ): MenuAction {
    return menu(`gap-${contextId}`, {
      visibilityContexts: [{ name: contextId, value } as never],
    });
  }

  it("keeps every row-identity hide rule visible under the shell state", () => {
    // One hide-rule per gap context, each with a value that WOULD hide
    // the action if the shell knew the fact. All stay visible because
    // the shell state carries none of these facts — the #6517
    // permissive default, locked until a server follow-on exposes them.
    const gaps: Array<[string, string | number]> = [
      [VISIBILITY_CONTEXT_CHECKOUT_STATUS, "Checked Out"],
      [VISIBILITY_CONTEXT_CONTENT_TYPE, "123"],
      [VISIBILITY_CONTEXT_OBJECT_TYPE, "2"],
      [VISIBILITY_CONTEXT_ASSIGNMENT_TYPE, "1"],
      [VISIBILITY_CONTEXT_WORKFLOWS, "5"],
      [VISIBILITY_CONTEXT_COMMUNITY, "1001"],
      [VISIBILITY_CONTEXT_CLIENT_CONTEXT, "browse"],
      [VISIBILITY_CONTEXT_PUBLISHABLE_TYPE, "page"],
    ];
    for (const [contextId, value] of gaps) {
      expect(
        isMenuVisibleByVisibilityContexts(gapMenu(contextId, value), SHELL_STATE),
        `context ${contextId} stays permissive without row identity`,
      ).toBe(true);
    }
  });

  it("still hides evaluated contexts under the full shell state", () => {
    // Contrast: the contexts the shell CAN resolve keep hiding menus
    // under the exact same state — permissive is per-context, not global.
    const byRole = gapMenu(VISIBILITY_CONTEXT_ROLES, "Admin");
    expect(isMenuVisibleByVisibilityContexts(byRole, SHELL_STATE)).toBe(
      false,
    );
    const byLocale = gapMenu(VISIBILITY_CONTEXT_LOCALES, "en-us");
    expect(isMenuVisibleByVisibilityContexts(byLocale, SHELL_STATE)).toBe(
      false,
    );
  });

  it("evaluates assignment-type, workflow, and publishable-type ids once known", () => {
    // Evaluator readiness: when a server follow-on exposes the identity
    // fields, the hide rules work with no evaluator change.
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("1", "1"), {
        assignmentType: 1,
      }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("1", "1"), {
        assignmentType: 2,
      }),
    ).toBe(true);
    expect(
      isMenuVisibleByVisibilityContexts(
        gapMenu(VISIBILITY_CONTEXT_ASSIGNMENT_TYPE, "1"),
        EMPTY_STATE,
      ),
    ).toBe(true);

    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("9", "5"), { workflowId: 5 }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("9", "5"), { workflowId: 6 }),
    ).toBe(true);
    expect(
      isMenuVisibleByVisibilityContexts(
        gapMenu(VISIBILITY_CONTEXT_WORKFLOWS, "5"),
        EMPTY_STATE,
      ),
    ).toBe(true);

    const byPublishable = gapMenu(
      VISIBILITY_CONTEXT_PUBLISHABLE_TYPE,
      "page",
    );
    expect(
      isMenuVisibleByVisibilityContexts(byPublishable, {
        publishableType: "Page",
      }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(byPublishable, {
        publishableType: "asset",
      }),
    ).toBe(true);
    expect(isMenuVisibleByVisibilityContexts(byPublishable, EMPTY_STATE)).toBe(
      true,
    );
  });

  it("accepts string-form numeric ids for content, object, and workflow types", () => {
    // Server DTOs may ship ids as strings; the evaluator already
    // compares numerically, so a string "123" row id hides like 123.
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("3", 123), {
        contentTypeId: "123",
      }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("4", "2"), { objectType: "2" }),
    ).toBe(false);
    expect(
      isMenuVisibleByVisibilityContexts(gapMenu("9", "5"), {
        workflowId: "5",
      }),
    ).toBe(false);
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

describe("SPA shell UI-mode claim (#6619)", () => {
  it("publishes the shell claim", () => {
    expect(SPA_SHELL_UI_MODE_ID).toBe("4");
    expect(SPA_SHELL_UI_MODE_NAME).toBe("CXMAIN");
  });

  it("matches entries by shell mode name case-insensitively", () => {
    expect(
      uiContextMatchesShellMode({ modeId: "4", modeName: "CXMAIN" }),
    ).toBe(true);
    expect(
      uiContextMatchesShellMode({ modeId: "7", modeName: "cxmain" }),
    ).toBe(true);
  });

  it("matches entries by shell mode id, including numeric wire ids", () => {
    expect(uiContextMatchesShellMode({ modeId: "4", modeName: "Other" })).toBe(
      true,
    );
    expect(uiContextMatchesShellMode({ modeId: 4, modeName: "Other" })).toBe(
      true,
    );
  });

  it("rejects foreign modes, nulls, and blanks", () => {
    expect(
      uiContextMatchesShellMode({ modeId: "7", modeName: "Publishing" }),
    ).toBe(false);
    expect(uiContextMatchesShellMode(null)).toBe(false);
    expect(uiContextMatchesShellMode(undefined)).toBe(false);
    expect(uiContextMatchesShellMode({})).toBe(false);
  });

  it("keeps unscoped menus visible in shell mode", () => {
    expect(isMenuVisibleInShellMode(null)).toBe(true);
    expect(isMenuVisibleInShellMode(undefined)).toBe(true);
    expect(isMenuVisibleInShellMode(menu("Copy"))).toBe(true);
    expect(isMenuVisibleInShellMode(menu("Copy", { uiContexts: [] }))).toBe(
      true,
    );
    // Single wire dicts unwrap like single visibilityContexts (#6517).
    expect(
      isMenuVisibleInShellMode(
        menu("Copy", {
          uiContexts: { modeId: "7", modeName: "Publishing" } as never,
        }),
      ),
    ).toBe(false);
  });

  it("keeps menus scoped to the shell mode visible", () => {
    const scoped = menu("Edit", {
      uiContexts: [
        { modeId: "7", modeName: "Publishing" },
        { modeId: "4", modeName: "CXMAIN" },
      ],
    });
    expect(isMenuVisibleInShellMode(scoped)).toBe(true);
    expect(isMenuVisibleByVisibilityContexts(scoped, EMPTY_STATE)).toBe(true);
  });

  it("hides menus scoped exclusively to other modes", () => {
    const foreign = menu("Publish", {
      uiContexts: [
        { modeId: "7", modeName: "Publishing" },
        { modeId: "9", modeName: "Design" },
      ],
    });
    expect(isMenuVisibleInShellMode(foreign)).toBe(false);
    expect(isMenuVisibleByVisibilityContexts(foreign, EMPTY_STATE)).toBe(
      false,
    );
    expect(filterMenuActionsByVisibility([foreign], EMPTY_STATE)).toEqual([]);
  });

  it("stays permissive for unresolvable entries", () => {
    const unknown = menu("Legacy", {
      uiContexts: [
        { modeId: "7", modeName: "Publishing" },
        { modeId: " ", modeName: " " },
      ],
    });
    expect(isMenuVisibleInShellMode(unknown)).toBe(true);
    expect(isMenuVisibleByVisibilityContexts(unknown, EMPTY_STATE)).toBe(true);
  });

  it("drops cascade parents whose children all hide on shell mode", () => {
    const menus = [
      menu("Parent", {
        menuType: "MENU",
        children: [
          menu("ForeignChild", {
            uiContexts: [{ modeId: "7", modeName: "Publishing" }],
          }),
        ],
      }),
    ];
    expect(filterMenuActionsByVisibility(menus, EMPTY_STATE)).toEqual([]);
  });

  it("hides seeded CXNAV-only nav-pane menus under the shell claim (#6824)", () => {
    // Installer seed (cmsTableData.xml RXMODEUICONTEXTACTION) scopes the
    // Create_New cascade and Search_Within_Folder solely to CXNAV
    // (MODEID 3, navigation pane) — never main-toolbar items in DCE.
    const createNew = menu("Create_New", {
      menuType: "MENU",
      uiContexts: [{ modeId: "3", modeName: "CXNAV" }],
      children: [
        menu("Create_New_Folder", {
          uiContexts: [{ modeId: "3", modeName: "CXNAV" }],
        }),
        menu("Create_New_Item", {
          uiContexts: [{ modeId: "3", modeName: "CXNAV" }],
        }),
      ],
    });
    const searchWithinFolder = menu("Search_Within_Folder", {
      uiContexts: [{ modeId: "3", modeName: "CXNAV" }],
    });
    expect(isMenuVisibleInShellMode(createNew)).toBe(false);
    expect(isMenuVisibleInShellMode(searchWithinFolder)).toBe(false);
    expect(
      filterMenuActionsByVisibility([createNew, searchWithinFolder], EMPTY_STATE),
    ).toEqual([]);
  });

  it("keeps dual CXMAIN+CXNAV scoped menus visible (#6824)", () => {
    const dual = menu("Edit", {
      uiContexts: [
        { modeId: "3", modeName: "CXNAV" },
        { modeId: "4", modeName: "CXMAIN" },
      ],
    });
    expect(isMenuVisibleInShellMode(dual)).toBe(true);
    expect(isMenuVisibleByVisibilityContexts(dual, EMPTY_STATE)).toBe(true);
  });

  it("hides seeded IAMAIN-only menus under the shell claim (#6824)", () => {
    // Active Assembly main area (MODEID 6) mirrors the QA regression seed:
    // exclusively foreign modes hide, same as CXNAV-only ones.
    const foreign = menu("aa_table_editor", {
      uiContexts: [{ modeId: "6", modeName: "IAMAIN" }],
    });
    expect(isMenuVisibleInShellMode(foreign)).toBe(false);
    expect(isMenuVisibleByVisibilityContexts(foreign, EMPTY_STATE)).toBe(
      false,
    );
  });

  it("keeps the seeded Arrange cascade visible once CXMAIN-scoped (#6889)", () => {
    // Installer seed (cmsTableData.xml RXMODEUICONTEXTACTION) scopes the
    // Arrange family to the Site Explorer Item context (MODEID 1) and —
    // since #6886/#6889 — the modern Explorer main area (MODEID 4,
    // CXMAIN). Before the CXMAIN rows, both the toolbar dropdown and the
    // row context-menu pivot vanished under the shell claim even though
    // the unscoped catalog still listed Arrange.
    const arrangeChildren = [
      "../sys_rcSupport/updaterelateditems.html",
      "../sys_rcSupport/updaterelateditems.html",
      "../sys_rcSupport/modifyslotitem.html",
      "../sys_rcSupport/updaterelateditems.html",
    ].map((url, index) =>
      menu(["Arrange_MoveUpLeft", "Arrange_MoveDownRight", "Arrange_ChangeTemplateSlot", "Arrange_Remove"][index], {
        url,
        uiContexts: [
          { modeId: "1", modeName: "Site Centric" },
          { modeId: "4", modeName: "CXMAIN" },
        ],
      }),
    );
    const arrange = menu("Arrange", {
      menuType: "MENU",
      uiContexts: [
        { modeId: "1", modeName: "Site Centric" },
        { modeId: "4", modeName: "CXMAIN" },
      ],
      children: arrangeChildren,
    });
    expect(isMenuVisibleInShellMode(arrange)).toBe(true);
    const visible = filterMenuActionsByVisibility([arrange], EMPTY_STATE);
    expect(visible).toHaveLength(1);
    expect(visible[0].children?.map((c) => c.name)).toEqual([
      "Arrange_MoveUpLeft",
      "Arrange_MoveDownRight",
      "Arrange_ChangeTemplateSlot",
      "Arrange_Remove",
    ]);
  });
});
