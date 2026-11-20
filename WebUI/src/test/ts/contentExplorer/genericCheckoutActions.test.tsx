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

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type {
  MenuAction,
  PSPathItem,
} from "../../../main/ts/api/contentExplorer/types";
import {
  EXPLORER_CHECKIN_ACTION,
  EXPLORER_CHECKOUT_ACTION,
  EXPLORER_FORCE_CHECKIN_ACTION,
  filterContextMenuActions,
  filterToolbarActions,
  withExplorerCheckoutActions,
  withExplorerForceCheckinAction,
} from "../../../main/ts/contentExplorer/actionEnablement";
import { ActionToolbar } from "../../../main/ts/contentExplorer/ActionToolbar";
import { resolvePublishKind } from "../../../main/ts/contentExplorer/itemPublish";
import { renderA11yGate } from "./a11y";

/** Generic custom-type row: no page/asset token, outside /Sites and /Assets. */
function genericItem(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "ext",
    path: "/Other/ext",
    type: "percExternalLink",
    id: "88",
    ...overrides,
  };
}

function pageItem(): PSPathItem {
  return {
    name: "Home",
    path: "/Sites/Demo/Home",
    type: "percPage",
    id: "42",
  };
}

function folderItem(): PSPathItem {
  return { name: "Assets", path: "/Assets/", type: "folder", id: "" };
}

function names(actions: MenuAction[]): string[] {
  return actions.map((a) => a.name);
}

describe("generic checkout trio enablement (#6651)", () => {
  it("classifies the custom-type row as kind none", () => {
    expect(resolvePublishKind(genericItem())).toBe("none");
  });

  it("injects Check Out and Check In for a generic row", () => {
    const out = withExplorerCheckoutActions([], genericItem());
    expect(names(out)).toContain(EXPLORER_CHECKOUT_ACTION.name);
    expect(names(out)).toContain(EXPLORER_CHECKIN_ACTION.name);
  });

  it("injects the trio for a generic row exactly like for a page", () => {
    expect(names(withExplorerCheckoutActions([], genericItem()))).toEqual(
      names(withExplorerCheckoutActions([], pageItem())),
    );
  });

  it("does not duplicate catalog Check Out / Check In leaves", () => {
    const seeded: MenuAction[] = [
      { name: "Check_Out", label: "Check Out", sortRank: 1, menuType: "MENUITEM" },
    ];
    const out = withExplorerCheckoutActions(seeded, genericItem());
    expect(names(out).filter((n) => n === "Check_Out")).toHaveLength(1);
    expect(names(out)).toContain("Check_In");
  });

  it("injects nothing for folders or an empty selection", () => {
    expect(withExplorerCheckoutActions([], folderItem())).toEqual([]);
    expect(withExplorerCheckoutActions([], null)).toEqual([]);
    expect(withExplorerCheckoutActions([], undefined)).toEqual([]);
  });

  it("injects Force Check-in for an Admin generic selection only", () => {
    expect(
      names(withExplorerForceCheckinAction([], genericItem(), true)),
    ).toContain(EXPLORER_FORCE_CHECKIN_ACTION.name);
    expect(withExplorerForceCheckinAction([], genericItem(), false)).toEqual(
      [],
    );
    expect(withExplorerForceCheckinAction([], genericItem())).toEqual([]);
    expect(withExplorerForceCheckinAction([], folderItem(), true)).toEqual(
      [],
    );
  });

  it("keeps the trio on the toolbar for a generic row, drops it for folders", () => {
    const toolbar = names(filterToolbarActions([], undefined, genericItem(), true));
    expect(toolbar).toContain("Check_Out");
    expect(toolbar).toContain("Check_In");
    expect(toolbar).toContain("Force_Checkin");
    const folderToolbar = names(
      filterToolbarActions([], undefined, folderItem(), true),
    );
    expect(folderToolbar).not.toContain("Check_Out");
    expect(folderToolbar).not.toContain("Check_In");
    expect(folderToolbar).not.toContain("Force_Checkin");
  });

  it("keeps the trio in the context menu for a generic row", () => {
    const menu = names(
      filterContextMenuActions([], undefined, genericItem(), true),
    );
    expect(menu).toContain("Check_Out");
    expect(menu).toContain("Check_In");
    expect(menu).toContain("Force_Checkin");
  });

  it("renders the trio with accessible names and passes the a11y gate", async () => {
    const actions = filterToolbarActions([], undefined, genericItem(), true);
    const { container } = render(
      <ActionToolbar actions={actions} ariaLabel="Server actions" />,
    );
    expect(screen.getByLabelText("Check Out")).toBeTruthy();
    expect(screen.getByLabelText("Check In")).toBeTruthy();
    expect(screen.getByLabelText("Force Check-in")).toBeTruthy();
    await renderA11yGate(container);
  });
});
