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
import { describe, expect, it, vi } from "vitest";
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

const slotCreateMenu = [
  {
    name: "Slot_Create",
    label: "Create in Slot",
    url: "../sys_cxSupport/itemassembly.html",
    sortRank: 1,
    menuType: "MENUITEM" as const,
  },
];

describe("ContentExplorerShell slot create (#3497)", () => {
  it("folder browse without a slot stays needs-slot and does not create", async () => {
    stubPathFetch();
    const addToSlot = vi.fn();
    const createItem = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotCreateMenu}
        loadWorkflowMenuActions={async () => null}
        addToSlot={addToSlot}
        createItem={createItem}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Create"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
        /select a slot/i,
      );
    });
    expect(createItem).not.toHaveBeenCalled();
    expect(addToSlot).not.toHaveBeenCalled();
    expect(screen.queryByTestId("explorer-slot-create-dialog")).toBeNull();
  });

  it("picker cancel does not create or POST when an AA slot is selected", async () => {
    stubPathFetch();
    const addToSlot = vi.fn();
    const createItem = vi.fn();
    const openWindow = vi.fn();
    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotCreateMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3, folderPath: "/Sites" }}
        addToSlot={addToSlot}
        createItem={createItem}
        openWindow={openWindow}
        loadSlotAllowedTypes={async () => [
          { id: 1, name: "percRichText", label: "Rich Text" },
        ]}
        loadSlotAllowedTemplates={async () => [
          { id: 4, name: "rffSnTitle", label: "Title" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Create"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-create-dialog")).toBeTruthy();
      expect(
        (screen.getByTestId("explorer-slot-create-type") as HTMLSelectElement)
          .value,
      ).toBe("percRichText");
    });
    await renderA11yGate(container);
    fireEvent.click(screen.getByTestId("explorer-slot-create-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-slot-create-dialog")).toBeNull();
    });
    expect(createItem).not.toHaveBeenCalled();
    expect(addToSlot).not.toHaveBeenCalled();
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("successful pick creates then adds and opens the React editor", async () => {
    stubPathFetch();
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    const createItem = vi.fn().mockResolvedValue({
      itemId: "99",
      folderPath: "/Sites",
      name: "n",
      contentType: "percRichText",
    });
    const openWindow = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotCreateMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3, folderPath: "/Sites" }}
        addToSlot={addToSlot}
        createItem={createItem}
        openWindow={openWindow}
        loadSlotAllowedTypes={async () => [
          { id: 1, name: "percRichText", label: "Rich Text" },
        ]}
        loadSlotAllowedTemplates={async () => [
          { id: 4, name: "rffSnTitle", label: "Title" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Create")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Create"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-create-apply")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("explorer-slot-create-apply"));
    await waitFor(() => {
      expect(createItem).toHaveBeenCalledWith({
        contentType: "percRichText",
        folderPath: "/Sites",
        templateId: undefined,
      });
    });
    expect(addToSlot).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
    });
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("entry=editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).not.toMatch(
      /editAsset\.jsp|itemassembly\.html/i,
    );
  });
});
