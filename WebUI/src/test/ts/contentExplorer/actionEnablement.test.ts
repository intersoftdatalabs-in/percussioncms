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
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import {
  filterContextMenuActions,
  filterEnabledMenuActions,
  filterToolbarActions,
  isActionAllowedOnSurface,
  isToolbarPublishNowHidden,
  isClientHandledAction,
  isDesktopOnlyActionUrl,
  isWebExecutableLeaf,
} from "../../../main/ts/contentExplorer/actionEnablement";

const BASE = "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer";

function leaf(
  partial: Partial<MenuAction> & Pick<MenuAction, "name">,
): MenuAction {
  return {
    label: partial.label ?? partial.name,
    sortRank: partial.sortRank ?? 1,
    menuType: partial.menuType ?? "MENUITEM",
    ...partial,
  };
}

describe("isClientHandledAction", () => {
  it("treats missing / blank / CLIENT sentinel URLs as client-handled", () => {
    expect(isClientHandledAction(leaf({ name: "open" }))).toBe(true);
    expect(isClientHandledAction(leaf({ name: "open", url: "" }))).toBe(true);
    expect(isClientHandledAction(leaf({ name: "open", url: "  " }))).toBe(true);
    expect(isClientHandledAction(leaf({ name: "open", url: "CLIENT" }))).toBe(
      true,
    );
    expect(isClientHandledAction(leaf({ name: "open", url: "client" }))).toBe(
      true,
    );
  });

  it("treats real URLs as not client-handled", () => {
    expect(
      isClientHandledAction(
        leaf({ name: "edit", url: "/Rhythmyx/cm/app/editAsset.jsp?id=1" }),
      ),
    ).toBe(false);
  });
});

describe("isDesktopOnlyActionUrl / isWebExecutableLeaf", () => {
  it("flags javascript:, file:, and custom app protocols as desktop-only", () => {
    expect(isDesktopOnlyActionUrl("javascript:alert(1)", BASE)).toBe(true);
    expect(isDesktopOnlyActionUrl("file:///C:/secret", BASE)).toBe(true);
    expect(isDesktopOnlyActionUrl("rxapp://launch-cx", BASE)).toBe(true);
    expect(isDesktopOnlyActionUrl("applet:foo", BASE)).toBe(true);
  });

  it("allows relative CMS paths and same-origin http(s)", () => {
    expect(isDesktopOnlyActionUrl("/Rhythmyx/sys_ActionPage/panel.html", BASE)).toBe(
      false,
    );
    expect(
      isDesktopOnlyActionUrl(
        "http://localhost:9992/Rhythmyx/cm/app/editAsset.jsp",
        BASE,
      ),
    ).toBe(false);
    expect(isDesktopOnlyActionUrl("CLIENT", BASE)).toBe(false);
    expect(isDesktopOnlyActionUrl(undefined, BASE)).toBe(false);
  });

  it("marks leaves with desktop-only URLs as non-executable", () => {
    expect(
      isWebExecutableLeaf(
        leaf({ name: "bad", url: "javascript:void(0)" }),
        BASE,
      ),
    ).toBe(false);
    expect(
      isWebExecutableLeaf(
        leaf({ name: "ok", url: "/Rhythmyx/cm/app/editAsset.jsp" }),
        BASE,
      ),
    ).toBe(true);
    expect(isWebExecutableLeaf(leaf({ name: "client-only" }), BASE)).toBe(true);
  });
});

describe("isActionAllowedOnSurface", () => {
  it("hides CONTEXTMENU roots on the toolbar but keeps them for context menu", () => {
    const ctxRoot = leaf({
      name: "cx-popup",
      menuType: "CONTEXTMENU",
    });
    expect(isActionAllowedOnSurface(ctxRoot, "toolbar")).toBe(false);
    expect(isActionAllowedOnSurface(ctxRoot, "contextmenu")).toBe(true);
  });

  it("allows MENUITEM and MENU on both surfaces", () => {
    expect(
      isActionAllowedOnSurface(leaf({ name: "open", menuType: "MENUITEM" }), "toolbar"),
    ).toBe(true);
    expect(
      isActionAllowedOnSurface(
        leaf({ name: "file", menuType: "MENU", children: [] }),
        "contextmenu",
      ),
    ).toBe(true);
  });
});

describe("filterEnabledMenuActions", () => {
  it("drops desktop-only leaves and empty cascade parents", () => {
    const actions: MenuAction[] = [
      leaf({ name: "open", sortRank: 1 }),
      leaf({ name: "evil", url: "javascript:alert(1)", sortRank: 2 }),
      {
        name: "file",
        label: "File",
        sortRank: 3,
        menuType: "MENU",
        children: [
          leaf({ name: "desktop-only", url: "rxapp://cx" }),
          leaf({ name: "nested-ok", url: "/Rhythmyx/ok" }),
        ],
      },
      {
        name: "empty-after-filter",
        label: "Empty",
        sortRank: 4,
        menuType: "MENU",
        children: [leaf({ name: "gone", url: "file:///tmp/x" })],
      },
    ];

    const filtered = filterToolbarActions(actions, BASE);
    expect(filtered.map((a) => a.name)).toEqual(["open", "file"]);
    const file = filtered.find((a) => a.name === "file");
    expect(file?.children?.map((c) => c.name)).toEqual(["nested-ok"]);
  });

  it("keeps CONTEXTMENU roots only on the context-menu surface", () => {
    const actions: MenuAction[] = [
      leaf({ name: "open", menuType: "MENUITEM" }),
      {
        name: "cx-root",
        label: "CX",
        sortRank: 2,
        menuType: "CONTEXTMENU",
        children: [leaf({ name: "cx-child" })],
      },
    ];

    expect(filterToolbarActions(actions, BASE).map((a) => a.name)).toEqual([
      "open",
    ]);
    expect(filterContextMenuActions(actions, BASE).map((a) => a.name)).toEqual([
      "open",
      "cx-root",
    ]);
  });

  it("returns empty for null/empty input without throwing", () => {
    expect(filterEnabledMenuActions(null, { surface: "toolbar", baseHref: BASE })).toEqual(
      [],
    );
    expect(filterEnabledMenuActions([], { surface: "toolbar", baseHref: BASE })).toEqual(
      [],
    );
  });

  it("keeps MENU children nested rather than hoisting them to the toolbar (#3379)", () => {
    const actions: MenuAction[] = [
      {
        name: "file",
        label: "File",
        sortRank: 1,
        menuType: "MENU",
        children: [
          leaf({ name: "open", url: "/Rhythmyx/ok" }),
          leaf({ name: "saveAs", url: "/Rhythmyx/saveas" }),
        ],
      },
    ];
    const filtered = filterToolbarActions(actions, BASE);
    expect(filtered.map((a) => a.name)).toEqual(["file"]);
    expect(filtered[0]?.children?.map((c) => c.name)).toEqual(["open", "saveAs"]);
  });

  it("does not mutate the input tree", () => {
    const child = leaf({ name: "child", url: "javascript:x" });
    const parent: MenuAction = {
      name: "parent",
      label: "Parent",
      sortRank: 1,
      menuType: "MENU",
      children: [child, leaf({ name: "keep" })],
    };
    const input = [parent];
    const filtered = filterToolbarActions(input, BASE);
    expect(input[0].children?.length).toBe(2);
    expect(filtered[0].children?.map((c) => c.name)).toEqual(["keep"]);
    expect(filtered[0]).not.toBe(input[0]);
  });

  it("hides toolbar Publish Now until a page or asset is selected (#3467)", () => {
    const actions: MenuAction[] = [
      leaf({ name: "open" }),
      leaf({ name: "Publish_Now", label: "Publish Now" }),
    ];
    expect(filterToolbarActions(actions, BASE, null).map((a) => a.name)).toEqual(
      ["open"],
    );
    expect(
      filterToolbarActions(actions, BASE, {
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
      }).map((a) => a.name),
    ).toEqual(["open"]);
    expect(
      filterToolbarActions(actions, BASE, {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        leaf: true,
      }).map((a) => a.name),
    ).toEqual(["open", "Publish_Now"]);
    expect(
      isToolbarPublishNowHidden(leaf({ name: "Publish_Now" }), null),
    ).toBe(true);
  });
});
