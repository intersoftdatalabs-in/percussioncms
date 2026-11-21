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

function renderShell(
  ui: ReactElement,
  bootstrap: SpaBootstrap = adminBootstrap,
) {
  return render(
    <BootstrapProvider value={bootstrap}>{ui}</BootstrapProvider>,
  );
}

const TREE_FOLDER = {
  id: "folder-1",
  path: "/Sites/Foo",
  folderPath: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: false,
  accessLevel: "WRITE",
};

function stubTreeAndListFetch() {
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
          PathItem: [TREE_FOLDER],
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

function treeRow(): HTMLElement {
  const host = screen.getByTestId("tree-node-/Sites/Foo");
  const row = host.querySelector('[role="treeitem"]');
  if (!row) {
    throw new Error("tree row [role=treeitem] missing under tree-node-/Sites/Foo");
  }
  return row as HTMLElement;
}

describe("ContentExplorerShell tree context menu (#6826)", () => {
  it("tree right-click mounts the loading popup then the folder-scoped catalog", async () => {
    stubTreeAndListFetch();
    let releaseCatalog: (() => void) | undefined;
    const catalogGate = new Promise<void>((resolve) => {
      releaseCatalog = resolve;
    });
    const loadMenuActions = vi.fn(async (item: { path?: string } | null) => {
      await catalogGate;
      expect(item?.path).toBe("/Sites/Foo");
      return [
        {
          name: "tree-probe-action",
          label: "Tree probe action",
          sortRank: 1,
          menuType: "MENUITEM" as const,
        },
      ];
    });

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument());

    fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });

    // Popup answers immediately with the loading placeholder (#6826).
    expect(screen.getByTestId("context-menu")).toBeInTheDocument();
    expect(screen.getByTestId("context-menu-loading")).toBeInTheDocument();
    // Accessible name carries the folder label.
    expect(
      screen.getByTestId("context-menu").getAttribute("aria-label"),
    ).toBe("Context menu for Foo");

    releaseCatalog?.();
    await waitFor(() =>
      expect(
        screen.getByTestId("context-menu-item-tree-probe-action"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.queryByTestId("context-menu-loading"),
    ).not.toBeInTheDocument();
    expect(loadMenuActions).toHaveBeenCalledWith(
      expect.objectContaining({ path: "/Sites/Foo" }),
    );

    await renderA11yGate(container);
  });

  it("Escape closes the tree-opened menu", async () => {
    stubTreeAndListFetch();
    const loadMenuActions = vi.fn(async () => [
      {
        name: "tree-probe-action",
        label: "Tree probe action",
        sortRank: 1,
        menuType: "MENUITEM" as const,
      },
    ]);

    renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument());

    fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });
    await waitFor(() =>
      expect(
        screen.getByTestId("context-menu-item-tree-probe-action"),
      ).toBeInTheDocument(),
    );

    // Focus moved into the popup on open (#6826) so Escape reaches it.
    expect(screen.getByTestId("context-menu")).toHaveFocus();
    fireEvent.keyDown(screen.getByTestId("context-menu"), { key: "Escape" });
    expect(screen.queryByTestId("context-menu")).not.toBeInTheDocument();
  });

  it("Escape returns focus to the invoking tree row (#6864)", async () => {
    stubTreeAndListFetch();
    const loadMenuActions = vi.fn(async () => [
      {
        name: "tree-probe-action",
        label: "Tree probe action",
        sortRank: 1,
        menuType: "MENUITEM" as const,
      },
    ]);

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument());

    // Keyboard operators open the menu from the focused row; the shell
    // records document.activeElement as the invoker (#6864).
    treeRow().focus();
    expect(treeRow()).toHaveFocus();
    fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });
    await waitFor(() =>
      expect(
        screen.getByTestId("context-menu-item-tree-probe-action"),
      ).toBeInTheDocument(),
    );
    expect(screen.getByTestId("context-menu")).toHaveFocus();

    fireEvent.keyDown(screen.getByTestId("context-menu"), { key: "Escape" });
    expect(screen.queryByTestId("context-menu")).not.toBeInTheDocument();
    // Focus is back on the invoking row, not stranded on <body>.
    expect(treeRow()).toHaveFocus();

    await renderA11yGate(container);
  });

  it("Escape returns focus to the invoking detail row (#6864)", async () => {
    const ROW = {
      id: "n-6864",
      path: "/Sites/Foo",
      folderPath: "/Sites/Foo",
      name: "Foo",
      type: "file",
      accessLevel: "WRITE",
    };
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      if (url.includes("paginatedFolder") || url.includes("/folder/")) {
        return new Response(
          JSON.stringify({
            PagedItemList: {
              childrenInPage: [ROW],
              childrenCount: 1,
              startIndex: 0,
            },
            PathItem: [TREE_FOLDER, ROW],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    const loadMenuActions = vi.fn(async () => [
      {
        name: "detail-probe-action",
        label: "Detail probe action",
        sortRank: 1,
        menuType: "MENUITEM" as const,
      },
    ]);

    const { container } = renderShell(
      <ContentExplorerShell
        initialPath="/Sites"
        loadDisplayFormats={async () => []}
        loadMenuActions={loadMenuActions}
        loadWorkflowMenuActions={async () => null}
      />,
    );
    const detailRow = await screen.findByTestId("detail-row-n-6864");

    detailRow.focus();
    expect(detailRow).toHaveFocus();
    fireEvent.contextMenu(detailRow, { clientX: 120, clientY: 220 });
    await waitFor(() =>
      expect(
        screen.getByTestId("context-menu-item-detail-probe-action"),
      ).toBeInTheDocument(),
    );

    // Invoke close shares the same restore helper as Escape: activating a
    // non-dialog action returns focus to the invoking row too (#6864).
    fireEvent.keyDown(screen.getByTestId("context-menu"), { key: "Escape" });
    expect(screen.queryByTestId("context-menu")).not.toBeInTheDocument();
    expect(detailRow).toHaveFocus();

    await renderA11yGate(container);
  });
});
