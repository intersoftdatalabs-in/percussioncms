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

import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type {
  MenuAction,
  PSPathItem,
} from "../../../main/ts/api/contentExplorer/types";

import * as itemWorkflowApi from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import * as itemFieldsApi from "../../../main/ts/editor/itemFieldsApi";
import {
  filterContextMenuActions,
  filterToolbarActions,
  isToolbarCheckoutHidden,
  isToolbarForceCheckinHidden,
  withExplorerCheckoutActions,
  withExplorerForceCheckinAction,
} from "../../../main/ts/contentExplorer/actionEnablement";
import { dispatchAction } from "../../../main/ts/contentExplorer/actionDispatch";
import { resolvePublishKind } from "../../../main/ts/contentExplorer/itemPublish";
import { isPageOrAssetContentType } from "../../../main/ts/contentExplorer/selection";
import { CheckoutOwnerPanel } from "../../../main/ts/contentExplorer/CheckoutOwnerPanel";
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
 * non-folder row with an id as eligible — the same submit-all posture
 * the publish family took in #6580. Unit lock for the #6651 acceptance;
 * the H2 live proof is
 * {@code modules/perc-qa-automation/frontend/tests/explorer-checkout-generic.spec.js}.</p>
 */

function generic(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "g-9",
    path: "/ContentLib/PromoSnippet",
    name: "PromoSnippet",
    title: "Promo snippet",
    type: "MyCustomArticle",
    leaf: true,
    accessLevel: "WRITE",
    ...overrides,
  } as PSPathItem;
}

function toolbarAction(name: string): MenuAction {
  return { name, label: name, sortRank: 1, menuType: "MENUITEM" };
}

const CHECKOUT_TRIO = ["Check_Out", "Check_In", "Force_Checkin"];

describe("checkout trio for generic custom-type rows (#6651)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("generic rows keep publish-kind none yet stay page-or-asset rows for the owner panel", () => {
    // Premise: no endpoint affinity — the trio must not gate on this.
    expect(resolvePublishKind(generic())).toBe("none");
    // …but the panel gate still passes: a typed non-folder row is an item.
    expect(isPageOrAssetContentType(generic())).toBe(true);
    expect(isPageOrAssetContentType(generic({ type: "" }))).toBe(false);
  });

  it("enablement keeps the trio visible for a generic selection", () => {
    for (const name of ["Check_Out", "Check_In"]) {
      expect(isToolbarCheckoutHidden(toolbarAction(name), generic())).toBe(false);
      expect(isToolbarCheckoutHidden(toolbarAction(name), undefined)).toBe(true);
    }
    expect(isToolbarForceCheckinHidden(toolbarAction("Force_Checkin"), generic())).toBe(
      false,
    );
    expect(
      isToolbarForceCheckinHidden(toolbarAction("Force_Checkin"), undefined),
    ).toBe(true);

    const visible = filterToolbarActions(
      CHECKOUT_TRIO.map(toolbarAction),
      undefined,
      generic(),
      true,
    );
    // The publish family is injected for any item too (#6580); the trio
    // must be present, not exclusive.
    for (const name of CHECKOUT_TRIO) {
      expect(visible.map((row) => row.name)).toContain(name);
    }

    const contextVisible = filterContextMenuActions(
      CHECKOUT_TRIO.map(toolbarAction),
      undefined,
      generic(),
      true,
    );
    for (const name of CHECKOUT_TRIO) {
      expect(contextVisible.map((row) => row.name)).toContain(name);
    }
  });

  it("injection adds checkout leaves for a generic row without duplicating", () => {
    const withCheckout = withExplorerCheckoutActions([], generic());
    expect(withCheckout.map((row) => row.name).sort()).toEqual(
      ["Check_In", "Check_Out"].sort(),
    );
    const withForce = withExplorerForceCheckinAction([], generic(), true);
    expect(withForce.map((row) => row.name)).toEqual(["Force_Checkin"]);
    // Already present: no duplicate injection.
    expect(
      withExplorerCheckoutActions(withCheckout, generic()).length,
    ).toBe(withCheckout.length);
  });

  it("single Check In prompts then posts the editor check-in for the generic id", async () => {
    const workflowCheckIn = vi
      .spyOn(itemWorkflowApi, "checkInItem")
      .mockResolvedValue(undefined);
    const editorCheckin = vi
      .spyOn(itemFieldsApi, "checkinEditorItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(toolbarAction("Check_In"), {
      item: generic(),
      promptCheckinComment: async () => "  generic note  ",
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(editorCheckin).toHaveBeenCalledWith("g-9", "generic note");
    // Single Check In rides the editor endpoint, not the workflow one.
    expect(workflowCheckIn).not.toHaveBeenCalled();
  });

  it("single Force Check-in submits the generic id on the default REST path", async () => {
    const force = vi
      .spyOn(itemWorkflowApi, "forceCheckInItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(toolbarAction("Force_Checkin"), {
      item: generic(),
      confirm: () => true,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(force).toHaveBeenCalledWith("g-9");
  });

  it("multi-select Check In and Force Check-in include generic rows and skip folders", async () => {
    const checkIn = vi
      .spyOn(itemWorkflowApi, "checkInItem")
      .mockResolvedValue(undefined);
    const force = vi
      .spyOn(itemWorkflowApi, "forceCheckInItem")
      .mockResolvedValue(undefined);
    const folder = generic({
      id: "7",
      name: "News",
      path: "/ContentLib/News",
      type: "folder",
      leaf: false,
    });
    const confirm = vi.fn().mockReturnValue(true);

    const checkedIn = await dispatchAction(toolbarAction("Check_In"), {
      item: generic(),
      selectedItems: [generic(), folder],
      confirm,
      promptCheckinComment: vi.fn(),
    });
    expect(checkIn).toHaveBeenCalledTimes(1);
    expect(checkIn).toHaveBeenCalledWith("g-9");
    expect(checkedIn.refresh).toBe(true);
    expect(checkedIn.messageText).toMatch(/Folders are not checked in: News/i);

    const forced = await dispatchAction(toolbarAction("Force_Checkin"), {
      item: generic(),
      selectedItems: [generic(), folder],
      confirm,
    });
    expect(force).toHaveBeenCalledTimes(1);
    expect(force).toHaveBeenCalledWith("g-9");
    expect(forced.refresh).toBe(true);
    expect(forced.messageText).toMatch(/Folders are not force checked in: News/i);
  });

  it("checkout-owner panel names the holder for a generic row (a11y gate)", async () => {
    const { container } = render(
      <CheckoutOwnerPanel
        itemId="g-9"
        load={vi.fn().mockResolvedValue({
          checkOutUser: "editor",
          currentUser: "Admin",
          itemName: "PromoSnippet",
        })}
      />,
    );
    expect(
      await screen.findByTestId("explorer-checkout-owner-user"),
    ).toHaveTextContent("editor");
    await renderA11yGate(container);
  });
});
