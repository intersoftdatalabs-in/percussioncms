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

  it("folder browse without a slot stays needs-slot and does not invent arrange", async () => {
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
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_Remove"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
        /select a slot/i,
      );
    });
    expect(removeSlotRel).not.toHaveBeenCalled();
    expect(moveSlotRel).not.toHaveBeenCalled();
    expect(changeSlotTemplate).not.toHaveBeenCalled();
  });

  it("slot without a snippet stays needs-snippet and does not call REST", async () => {
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
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_Remove"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
        /item in the slot/i,
      );
    });
    expect(removeSlotRel).not.toHaveBeenCalled();
  });

  it("move and remove call relationship REST when a snippet is selected", async () => {
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
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_MoveUpLeft"));
    await waitFor(() => {
      expect(moveSlotRel).toHaveBeenCalledWith(88, "UP");
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Arrange_Remove"));
    await waitFor(() => {
      expect(removeSlotRel).toHaveBeenCalledWith(88);
    });
  });

  it("change template picker apply POSTs template-slot REST", async () => {
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
    });
    fireEvent.click(
      screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-change-dialog")).toBeTruthy();
    });
    await renderA11yGate(container);
    fireEvent.change(screen.getByTestId("explorer-slot-change-slot"), {
      target: { value: "5" },
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId("explorer-slot-change-template") as HTMLSelectElement)
          .value,
      ).toBe("6");
    });
    fireEvent.click(screen.getByTestId("explorer-slot-change-apply"));
    await waitFor(() => {
      expect(changeSlotTemplate).toHaveBeenCalledWith(88, 5, 6);
    });
    expect(screen.queryByTestId("explorer-slot-change-dialog")).toBeNull();
  });

  it("change template picker cancel does not POST", async () => {
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
    });
    fireEvent.click(
      screen.getByTestId("action-toolbar-item-Arrange_ChangeTemplateSlot"),
    );
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-change-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-slot-change-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-slot-change-dialog")).toBeNull();
    });
    expect(changeSlotTemplate).not.toHaveBeenCalled();
  });
});
