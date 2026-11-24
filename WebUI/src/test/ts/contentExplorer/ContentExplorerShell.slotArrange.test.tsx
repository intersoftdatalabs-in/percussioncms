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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";
import { ContentExplorerShell } from "../../../main/ts/contentExplorer/ContentExplorerShell";
import { renderA11yGate } from "./a11y";
import { mockFetch } from "./setup";

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const SLOT_ARRANGE_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const SLOT_ARRANGE_TEST_TIMEOUT = 30_000;

const adminBootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "explorer",
  isAdmin: true,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderShell(ui: ReactElement) {
  return render(
    <BootstrapProvider value={adminBootstrap}>{ui}</BootstrapProvider>,
  );
}

function stubPathFetch() {
  mockFetch(async (input) => {
    const url = typeof input === "string" ? input : (input as Request).url;
    if (url.includes("paginatedFolder") || url.includes("/folder/")) {
      return new Response(
        JSON.stringify({
          PagedItemList: {
            childrenInPage: [],
            childrenCount: 0,
            startIndex: 0,
          },
          PathItem: [],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    }
    return new Response("{}", {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  });
}

const arrangeMenu = [
  {
    name: "Arrange_Remove",
    label: "Remove from Slot",
    url: "../sys_cxSupport/itemassembly.html",
    sortRank: 1,
    menuType: "MENUITEM" as const,
  },
  {
    name: "Arrange_MoveUpLeft",
    label: "Move Up",
    url: "../sys_cxSupport/itemassembly.html",
    sortRank: 2,
    menuType: "MENUITEM" as const,
  },
  {
    name: "Arrange_ChangeTemplateSlot",
    label: "Change Template",
    url: "../sys_cxItemAssembly/variantlistwithslots.html",
    sortRank: 3,
    menuType: "MENUITEM" as const,
  },
];

describe("ContentExplorerShell slot arrange (#3496)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("folder browse without a slot disables arrange children with a slot tooltip (#6915)", { timeout: SLOT_ARRANGE_TEST_TIMEOUT }, async () => {
    stubPathFetch();
    const removeSlotRel = vi.fn();
    const moveSlotRel = vi.fn();
    const changeSlotTemplate = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => arrangeMenu}
        loadWorkflowMenuActions={async () => null}
        removeSlotRel={removeSlotRel}
        moveSlotRel={moveSlotRel}
        changeSlotTemplate={changeSlotTemplate}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Arrange_Remove")).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    // DCE parity (#6915): aria-disabled + tooltip name the missing slot
    // context instead of dispatching a needs-slot status hint.
    const remove = screen.getByTestId("action-toolbar-item-Arrange_Remove");
    expect(remove.getAttribute("aria-disabled")).toBe("true");
    expect(remove.getAttribute("title")).toMatch(/select a slot/i);
    fireEvent.click(remove);
    fireEvent.keyDown(remove, { key: "Enter" });
    expect(removeSlotRel).not.toHaveBeenCalled();
    expect(moveSlotRel).not.toHaveBeenCalled();
    expect(changeSlotTemplate).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("explorer-server-actions-error")?.textContent ?? "",
    ).not.toMatch(/select a slot/i);
  });

  it("slot without a snippet disables arrange children naming the missing item (#6915)", { timeout: SLOT_ARRANGE_TEST_TIMEOUT }, async () => {
    stubPathFetch();
    const removeSlotRel = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => arrangeMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3 }}
        removeSlotRel={removeSlotRel}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Arrange_Remove")).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    const remove = screen.getByTestId("action-toolbar-item-Arrange_Remove");
    expect(remove.getAttribute("aria-disabled")).toBe("true");
    expect(remove.getAttribute("title")).toMatch(/item in the slot/i);
    fireEvent.click(remove);
    fireEvent.keyDown(remove, { key: "Enter" });
    expect(removeSlotRel).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("explorer-server-actions-error")?.textContent ?? "",
    ).not.toMatch(/item in the slot/i);
  });

  it("move and remove call relationship REST when a snippet is selected", { timeout: SLOT_ARRANGE_TEST_TIMEOUT }, async () => {
    stubPathFetch();
    const removeSlotRel = vi.fn().mockResolvedValue(undefined);
    const moveSlotRel = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => arrangeMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3, relationshipId: 88 }}
        removeSlotRel={removeSlotRel}
        moveSlotRel={moveSlotRel}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft")).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft"));
    await waitFor(() => {
      expect(moveSlotRel).toHaveBeenCalledWith(88, "UP");
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_Remove"));
    await waitFor(() => {
      expect(removeSlotRel).toHaveBeenCalledWith(88);
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
  });

  it("change template picker apply POSTs template-slot REST", { timeout: SLOT_ARRANGE_TEST_TIMEOUT }, async () => {
    stubPathFetch();
    const changeSlotTemplate = vi.fn().mockResolvedValue({
      relationshipId: 88,
      ownerId: 42,
      dependentId: 7,
      slotId: 5,
      templateId: 6,
      sortRank: 0,
    });
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => arrangeMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{
          ownerId: 42,
          slotId: 3,
          relationshipId: 88,
          snippetTemplateId: 4,
          ownerTemplateId: 7,
        }}
        changeSlotTemplate={changeSlotTemplate}
        loadSlotCanvas={async () => ({
          ownerId: 42,
          templateId: 7,
          slots: [
            { slotId: 3, name: "sidebar", label: "Sidebar", items: [] },
            { slotId: 5, name: "list", label: "List", items: [] },
          ],
        })}
        loadSlotAllowedTemplates={async (slotId) =>
          slotId === 5
            ? [{ id: 6, name: "rffSnCallout", label: "Callout" }]
            : [{ id: 4, name: "rffSnTitle", label: "Title" }]
        }
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
      ).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(
      screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-change-dialog")).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    await renderA11yGate(container);
    fireEvent.change(screen.getByTestId("explorer-slot-change-slot"), {
      target: { value: "5" },
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId("explorer-slot-change-template") as HTMLSelectElement)
          .value,
      ).toBe("6");
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("explorer-slot-change-apply"));
    await waitFor(() => {
      expect(changeSlotTemplate).toHaveBeenCalledWith(88, 5, 6);
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("explorer-slot-change-dialog")).toBeNull();
  });

  it("change template picker cancel does not POST", { timeout: SLOT_ARRANGE_TEST_TIMEOUT }, async () => {
    stubPathFetch();
    const changeSlotTemplate = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => arrangeMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3, relationshipId: 88, snippetTemplateId: 4 }}
        changeSlotTemplate={changeSlotTemplate}
        loadSlotAllowedTemplates={async () => [
          { id: 4, name: "rffSnTitle", label: "Title" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
      ).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(
      screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-change-dialog")).toBeTruthy();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    fireEvent.click(screen.getByTestId("explorer-slot-change-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-slot-change-dialog")).toBeNull();
    },
      { timeout: SLOT_ARRANGE_ROUND_TRIP_TIMEOUT },
    );
    expect(changeSlotTemplate).not.toHaveBeenCalled();
  });
});
