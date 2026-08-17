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
import { mockFetch } from "./setup";

vi.mock("../../../main/ts/contentBrowser/ContentBrowser", () => ({
  ContentBrowser: (props: {
    onConfirm?: (selection: {
      items: Array<{ id: string; name: string; path: string }>;
    }) => void;
    onCancel?: () => void;
  }) => (
    <div data-testid="content-browser">
      <button
        type="button"
        data-testid="content-browser-confirm"
        onClick={() =>
          props.onConfirm?.({
            items: [{ id: "7", name: "Snippet", path: "/Sites/A/s" }],
          })
        }
      >
        Confirm
      </button>
      <button
        type="button"
        data-testid="content-browser-cancel"
        onClick={() => props.onCancel?.()}
      >
        Cancel
      </button>
    </div>
  ),
}));

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

const slotAddMenu = [
  {
    name: "Slot_Add",
    label: "Add to Slot",
    url: "../sys_cxSupport/variantlistwithslots.html",
    sortRank: 1,
    menuType: "MENUITEM" as const,
  },
];

describe("ContentExplorerShell slot add (#3495)", () => {
  it("folder browse without a slot stays needs-slot and does not POST", async () => {
    stubPathFetch();
    const addToSlot = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotAddMenu}
        loadWorkflowMenuActions={async () => null}
        addToSlot={addToSlot}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Add"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-server-actions-error").textContent).toMatch(
        /select a slot/i,
      );
    });
    expect(addToSlot).not.toHaveBeenCalled();
    expect(screen.queryByTestId("explorer-slot-add-dialog")).toBeNull();
  });

  it("picker cancel does not POST when an AA slot is selected", async () => {
    stubPathFetch();
    const addToSlot = vi.fn();
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotAddMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3 }}
        addToSlot={addToSlot}
        loadSlotAllowedTemplates={async () => [
          { id: 4, name: "rffSnTitle", label: "Title" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Add"));
    await waitFor(() => {
      expect(screen.getByTestId("explorer-slot-add-dialog")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("content-browser-cancel"));
    await waitFor(() => {
      expect(screen.queryByTestId("explorer-slot-add-dialog")).toBeNull();
    });
    expect(addToSlot).not.toHaveBeenCalled();
  });

  it("successful pick posts slot-relationship ids", async () => {
    stubPathFetch();
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 7,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={async () => slotAddMenu}
        loadWorkflowMenuActions={async () => null}
        slot={{ ownerId: 42, slotId: 3 }}
        addToSlot={addToSlot}
        loadSlotAllowedTemplates={async () => [
          { id: 4, name: "rffSnTitle", label: "Title" },
        ]}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("action-toolbar-item-Slot_Add")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("action-toolbar-item-Slot_Add"));
    await waitFor(() => {
      expect(screen.getByTestId("content-browser-confirm")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("content-browser-confirm"));
    await waitFor(() => {
      expect(addToSlot).toHaveBeenCalledWith({
        ownerId: 42,
        dependentId: 7,
        slotId: 3,
        templateId: 4,
        folderId: undefined,
      });
    });
  });
});
