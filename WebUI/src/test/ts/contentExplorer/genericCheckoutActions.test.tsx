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
import { afterEach, describe, expect, it, vi } from "vitest";
import type {
  MenuAction,
  PSPathItem,
} from "../../../main/ts/api/contentExplorer/types";
import * as itemWorkflowApi from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import * as itemFieldsApi from "../../../main/ts/editor/itemFieldsApi";
import {
  EXPLORER_CHECKIN_ACTION,
  EXPLORER_CHECKOUT_ACTION,
  EXPLORER_FORCE_CHECKIN_ACTION,
  filterContextMenuActions,
  filterToolbarActions,
  isToolbarCheckoutHidden,
  isToolbarForceCheckinHidden,
  withExplorerCheckoutActions,
  withExplorerForceCheckinAction,
} from "../../../main/ts/contentExplorer/actionEnablement";
import { dispatchAction } from "../../../main/ts/contentExplorer/actionDispatch";
import { ActionToolbar } from "../../../main/ts/contentExplorer/ActionToolbar";
import { CheckoutOwnerPanel } from "../../../main/ts/contentExplorer/CheckoutOwnerPanel";
import { resolvePublishKind } from "../../../main/ts/contentExplorer/itemPublish";
import { isPageOrAssetContentType } from "../../../main/ts/contentExplorer/selection";
import { renderA11yGate } from "./a11y";

/**
 * Checkout trio for generic custom-type rows (#6651 / parent #4530).
 *
 * <p>{@link resolvePublishKind} returns {@code "none"} for rows outside
 * {@code /Sites} and {@code /Assets} with no page/asset token, but the
 * workflow endpoints are id-based
 * ({@code GET …/workflow/checkOut|checkIn|forceCheckIn/{id}} and the
 * editor check-in the single Check In uses). Enablement, dispatch
 * (single + multi), and the checkout-owner panel therefore treat any
 * non-folder row with an id as eligible. Dispatch and owner-panel cases
 * adapted from #6847 (mayank-intsof). The H2 live proof is
 * {@code modules/perc-qa-automation/frontend/tests/explorer-generic-checkout.spec.js}.</p>
 */

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

function toolbarAction(name: string): MenuAction {
  return { name, label: name, sortRank: 1, menuType: "MENUITEM" };
}

describe("generic checkout trio enablement (#6651)", () => {
  it("classifies the custom-type row as kind none", () => {
    expect(resolvePublishKind(genericItem())).toBe("none");
  });

  it("still treats a typed generic row as an item for the owner panel", () => {
    expect(isPageOrAssetContentType(genericItem())).toBe(true);
    expect(isPageOrAssetContentType(genericItem({ type: "" }))).toBe(false);
  });

  it("does not hide the trio for a generic selection", () => {
    for (const name of ["Check_Out", "Check_In"]) {
      expect(isToolbarCheckoutHidden(toolbarAction(name), genericItem())).toBe(false);
      expect(isToolbarCheckoutHidden(toolbarAction(name), undefined)).toBe(true);
    }
    expect(
      isToolbarForceCheckinHidden(toolbarAction("Force_Checkin"), genericItem()),
    ).toBe(false);
    expect(
      isToolbarForceCheckinHidden(toolbarAction("Force_Checkin"), undefined),
    ).toBe(true);
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

describe("generic checkout trio dispatch and owner panel (#6651)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("single Check In prompts then posts the editor check-in for the generic id", async () => {
    const workflowCheckIn = vi
      .spyOn(itemWorkflowApi, "checkInItem")
      .mockResolvedValue(undefined);
    const editorCheckin = vi
      .spyOn(itemFieldsApi, "checkinEditorItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(toolbarAction("Check_In"), {
      item: genericItem(),
      promptCheckinComment: async () => "  generic note  ",
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(editorCheckin).toHaveBeenCalledWith("88", "generic note");
    // Single Check In rides the editor endpoint, not the workflow one.
    expect(workflowCheckIn).not.toHaveBeenCalled();
  });

  it("single Force Check-in submits the generic id on the default REST path", async () => {
    const force = vi
      .spyOn(itemWorkflowApi, "forceCheckInItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(toolbarAction("Force_Checkin"), {
      item: genericItem(),
      confirm: () => true,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(force).toHaveBeenCalledWith("88");
  });

  it("multi-select Check In and Force Check-in include generic rows and skip folders", async () => {
    const checkIn = vi
      .spyOn(itemWorkflowApi, "checkInItem")
      .mockResolvedValue(undefined);
    const force = vi
      .spyOn(itemWorkflowApi, "forceCheckInItem")
      .mockResolvedValue(undefined);
    const folder: PSPathItem = {
      id: "7",
      name: "News",
      path: "/Other/News",
      type: "folder",
      leaf: false,
    } as PSPathItem;
    const confirm = vi.fn().mockReturnValue(true);

    const checkedIn = await dispatchAction(toolbarAction("Check_In"), {
      item: genericItem(),
      selectedItems: [genericItem(), folder],
      confirm,
      promptCheckinComment: vi.fn(),
    });
    expect(checkIn).toHaveBeenCalledTimes(1);
    expect(checkIn).toHaveBeenCalledWith("88");
    expect(checkedIn.refresh).toBe(true);
    expect(checkedIn.messageText).toMatch(/Folders are not checked in: News/i);

    const forced = await dispatchAction(toolbarAction("Force_Checkin"), {
      item: genericItem(),
      selectedItems: [genericItem(), folder],
      confirm,
    });
    expect(force).toHaveBeenCalledTimes(1);
    expect(force).toHaveBeenCalledWith("88");
    expect(forced.refresh).toBe(true);
    expect(forced.messageText).toMatch(/Folders are not force checked in: News/i);
  });

  it("checkout-owner panel names the holder for a generic row (a11y gate)", async () => {
    const { container } = render(
      <CheckoutOwnerPanel
        itemId="88"
        load={vi.fn().mockResolvedValue({
          checkOutUser: "editor",
          currentUser: "Admin",
          itemName: "ext",
        })}
      />,
    );
    expect(
      await screen.findByTestId("explorer-checkout-owner-user"),
    ).toHaveTextContent("editor");
    await renderA11yGate(container);
  });
});
