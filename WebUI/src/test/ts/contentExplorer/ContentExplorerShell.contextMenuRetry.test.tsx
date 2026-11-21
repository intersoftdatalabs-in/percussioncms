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

const PROBE_ACTION = {
  name: "tree-probe-action",
  label: "Tree probe action",
  sortRank: 1,
  menuType: "MENUITEM" as const,
};

describe("ContentExplorerShell context-menu catalog Retry (#6874)", () => {
  it("failed catalog load shows the error with Retry, not 'No actions'", async () => {
    stubTreeAndListFetch();
    const loadMenuActions = vi.fn(async () => {
      throw new Error("catalog down");
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

    // Loading answers first; the failure swaps in the error state.
    expect(screen.getByTestId("context-menu-loading")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByTestId("context-menu-error")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("context-menu-error").textContent).toMatch(
      /could not load/i,
    );
    expect(screen.getByTestId("context-menu-retry")).toBeTruthy();
    // A failure is not an empty catalog.
    expect(screen.queryByTestId("context-menu-empty")).toBeNull();
    expect(screen.queryByTestId("context-menu-item-tree-probe-action")).toBeNull();

    await renderA11yGate(container);
  });

  it("Retry re-runs the catalog load for the same row and swaps in the actions", async () => {
    stubTreeAndListFetch();
    // The toolbar shares loadMenuActions, so gate on a flag instead of
    // mockRejectedValueOnce: every catalog load fails until Retry flips it.
    let catalogUp = false;
    const loadMenuActions = vi.fn(async () => {
      if (!catalogUp) {
        throw new Error("catalog down");
      }
      return [PROBE_ACTION];
    });

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
    const retry = await screen.findByTestId("context-menu-retry");
    expect(loadMenuActions).toHaveBeenCalled();

    catalogUp = true;
    fireEvent.click(retry);
    await waitFor(() =>
      expect(
        screen.getByTestId("context-menu-item-tree-probe-action"),
      ).toBeInTheDocument(),
    );
    expect(loadMenuActions).toHaveBeenLastCalledWith(
      expect.objectContaining({ path: "/Sites/Foo" }),
    );
    expect(screen.queryByTestId("context-menu-error")).toBeNull();
    expect(screen.queryByTestId("context-menu-empty")).toBeNull();
  });

  it("keeps the error and returns focus to Retry when the retry fails", async () => {
    stubTreeAndListFetch();
    const loadMenuActions = vi.fn(async () => {
      throw new Error("still down");
    });

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
    const retry = await screen.findByTestId("context-menu-retry");
    retry.focus();
    expect(retry).toHaveFocus();
    fireEvent.click(retry);

    await waitFor(
      () => {
        expect(loadMenuActions.mock.calls.length).toBeGreaterThanOrEqual(2);
      },
      { timeout: 10_000 },
    );
    const retryAgain = await screen.findByTestId(
      "context-menu-retry",
      undefined,
      { timeout: 10_000 },
    );
    expect(screen.getByTestId("context-menu-error")).toBeTruthy();
    await waitFor(
      () => {
        expect(retryAgain).toHaveFocus();
      },
      { timeout: 10_000 },
    );
  });
});
