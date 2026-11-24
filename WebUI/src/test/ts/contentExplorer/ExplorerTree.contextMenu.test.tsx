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
import { describe, expect, it, vi } from "vitest";
import { ExplorerTree } from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

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
const CONTEXT_MENU_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const CONTEXT_MENU_TEST_TIMEOUT = 30_000;

/** Child of /Sites — path is the data-testid suffix (`tree-node-${path}`). */
const ROOT_FOLDER: PSPathItem = {
  id: "root-1",
  path: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: false,
};

/** Wire shape from PSPathItemList (pathApi.findChildren unwraps PathItem). */
function pathItemListResponse(items: PSPathItem[]): Response {
  return new Response(JSON.stringify({ PathItem: items }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
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

describe("ExplorerTree folder context menu (#6826)", () => {
  it("right-click selects the row and opens the folder-scoped menu", { timeout: CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    const onSelectFolder = vi.fn();
    const onFolderContextMenu = vi.fn();
    const { container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={onSelectFolder}
        onFolderContextMenu={onFolderContextMenu}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument(), { timeout: CONTEXT_MENU_ROUND_TRIP_TIMEOUT });

    fireEvent.contextMenu(treeRow(), { clientX: 100, clientY: 200 });

    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Foo",
      expect.objectContaining({ path: "/Sites/Foo" }),
    );
    expect(onFolderContextMenu).toHaveBeenCalledWith(
      expect.objectContaining({ path: "/Sites/Foo" }),
      100,
      200,
    );
    await renderA11yGate(container);
  });

  it("context-menu key opens the menu anchored at the row", { timeout: CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    const onSelectFolder = vi.fn();
    const onFolderContextMenu = vi.fn();
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={onSelectFolder}
        onFolderContextMenu={onFolderContextMenu}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument(), { timeout: CONTEXT_MENU_ROUND_TRIP_TIMEOUT });

    fireEvent.keyDown(treeRow(), { key: "ContextMenu" });

    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Foo",
      expect.objectContaining({ path: "/Sites/Foo" }),
    );
    expect(onFolderContextMenu).toHaveBeenCalledTimes(1);
    const [folder, x, y] = onFolderContextMenu.mock.calls[0] as [
      PSPathItem,
      number,
      number,
    ];
    expect(folder.path).toBe("/Sites/Foo");
    expect(Number.isFinite(x)).toBe(true);
    expect(Number.isFinite(y)).toBe(true);
  });

  it("Shift+F10 opens the menu; plain F10 does not", { timeout: CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    const onFolderContextMenu = vi.fn();
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
        onFolderContextMenu={onFolderContextMenu}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument(), { timeout: CONTEXT_MENU_ROUND_TRIP_TIMEOUT });

    fireEvent.keyDown(treeRow(), { key: "F10", shiftKey: true });
    expect(onFolderContextMenu).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(treeRow(), { key: "F10" });
    expect(onFolderContextMenu).toHaveBeenCalledTimes(1);
  });

  it("right-click without a host handler still selects (plain click parity)", { timeout: CONTEXT_MENU_TEST_TIMEOUT }, async () => {
    mockFetch(async () => pathItemListResponse([ROOT_FOLDER]));
    const onSelectFolder = vi.fn();
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={onSelectFolder}
      />,
    );
    await waitFor(() => expect(treeRow()).toBeInTheDocument(), { timeout: CONTEXT_MENU_ROUND_TRIP_TIMEOUT });

    fireEvent.contextMenu(treeRow(), { clientX: 5, clientY: 5 });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Foo",
      expect.objectContaining({ path: "/Sites/Foo" }),
    );
  });
});
