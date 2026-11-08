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
import {
  ExplorerTree,
  applyCollapseAllExplorerTree,
  applyExpandAllExplorerTree,
  collectLoadedExplorerTreeKeys,
} from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

/** Three-level hierarchy: /Sites → /Sites/Foo → /Sites/Foo/Bar. */
const FOO_FOLDER: PSPathItem = {
  id: "foo-1",
  path: "/Sites/Foo",
  name: "Foo",
  type: "folder",
  hasFolderChildren: true,
};

const LEAF_FOLDER: PSPathItem = {
  id: "leaf-1",
  path: "/Sites/Leaf",
  name: "Leaf",
  type: "folder",
  hasFolderChildren: false,
};

const BAR_FOLDER: PSPathItem = {
  id: "bar-1",
  path: "/Sites/Foo/Bar",
  name: "Bar",
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

/** fooCalls counts lazy loads of /Sites/Foo children (Bar). */
function mockThreeLevelTree(onFooLoad?: () => void): void {
  mockFetch(async (input) => {
    const url = typeof input === "string" ? input : (input as Request).url;
    if (url.endsWith("/pathmanagement/path/folder/Sites")) {
      return pathItemListResponse([FOO_FOLDER, LEAF_FOLDER]);
    }
    if (url.endsWith("/pathmanagement/path/folder/Sites/Foo")) {
      onFooLoad?.();
      return pathItemListResponse([BAR_FOLDER]);
    }
    return pathItemListResponse([]);
  });
}

describe("expand-all / collapse-all state reducers (#5760)", () => {
  it("collectLoadedExplorerTreeKeys canonicalizes keys and children", () => {
    const keys = collectLoadedExplorerTreeKeys({
      "/Sites/": {
        children: [
          { path: "/Sites/Foo/", folderPath: "//Sites/Foo" },
          { path: "/Sites/Leaf" },
        ],
      },
      "/Sites/Foo": { children: [{ path: "/Sites/Foo/Bar" }] },
    });
    expect(keys).toContain("/Sites");
    expect(keys).toContain("/Sites/Foo");
    expect(keys).toContain("/Sites/Leaf");
    expect(keys).toContain("/Sites/Foo/Bar");
    expect(keys).not.toContain("/Sites/");
    expect(keys).not.toContain("/Sites/Foo/");
  });

  it("collectLoadedExplorerTreeKeys skips blank keys and paths", () => {
    expect(
      collectLoadedExplorerTreeKeys({
        "": { children: [{ path: "   " }] },
      }),
    ).toEqual([]);
  });

  it("applyExpandAllExplorerTree marks every loaded key expanded", () => {
    const next = applyExpandAllExplorerTree(
      { "/Sites": true },
      {
        "/Sites": { children: [{ path: "/Sites/Foo" }] },
        "/Sites/Foo": { children: [{ path: "/Sites/Foo/Bar" }] },
      },
    );
    expect(next["/Sites"]).toBe(true);
    expect(next["/Sites/Foo"]).toBe(true);
    expect(next["/Sites/Foo/Bar"]).toBe(true);
  });

  it("applyExpandAllExplorerTree keeps prior extras", () => {
    const next = applyExpandAllExplorerTree({ "/Other": true }, {});
    expect(next["/Other"]).toBe(true);
  });

  it("applyCollapseAllExplorerTree keeps only the product root expanded", () => {
    expect(applyCollapseAllExplorerTree("/Sites/")).toEqual({
      "/Sites": true,
    });
    expect(
      applyCollapseAllExplorerTree("/"),
    ).toEqual({ "/": true });
  });
});

describe("ExplorerTree expand-all / collapse-all (#5760)", () => {
  it("renders keyboard-accessible expand-all and collapse-all buttons", async () => {
    mockThreeLevelTree();
    const { container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    const expandAll = screen.getByTestId("tree-expand-all");
    const collapseAll = screen.getByTestId("tree-collapse-all");
    expect(expandAll.tagName).toBe("BUTTON");
    expect(collapseAll.tagName).toBe("BUTTON");
    expect(expandAll).toHaveAccessibleName(/expand all/i);
    expect(collapseAll).toHaveAccessibleName(/collapse all/i);
    expect(
      screen.getByTestId("explorer-tree-expand-toolbar"),
    ).toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("expand-all reveals loaded descendants without refetching them", async () => {
    let fooCalls = 0;
    mockThreeLevelTree(() => {
      fooCalls += 1;
    });
    const { container } = render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    // Load Bar via the single-folder toggle first.
    fireEvent.click(screen.getByTestId("tree-toggle-/Sites/Foo"));
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    expect(fooCalls).toBe(1);
    // Collapse hides the loaded descendant but keeps the roots.
    fireEvent.click(screen.getByTestId("tree-collapse-all"));
    await waitFor(() =>
      expect(screen.queryByTestId("tree-node-/Sites/Foo/Bar")).toBeNull(),
    );
    expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument();
    expect(screen.getByTestId("tree-node-/Sites/Leaf")).toBeInTheDocument();
    // Expand-all reveals the loaded descendant with no refetch.
    fireEvent.click(screen.getByTestId("tree-expand-all"));
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    expect(fooCalls).toBe(1);
    await renderA11yGate(container);
  });

  it("collapse-all collapses to roots", async () => {
    mockThreeLevelTree();
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath={null}
        onSelectFolder={() => undefined}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-toggle-/Sites/Foo"));
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByTestId("tree-collapse-all"));
    await waitFor(() =>
      expect(screen.queryByTestId("tree-node-/Sites/Foo/Bar")).toBeNull(),
    );
    // Roots stay visible: top-level folders are not hidden.
    expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument();
    expect(screen.getByTestId("tree-node-/Sites/Leaf")).toBeInTheDocument();
  });

  it("expand-all and collapse-all preserve selection and never select", async () => {
    mockThreeLevelTree();
    const onSelectFolder = vi.fn();
    render(
      <ExplorerTree
        initialPath="/Sites"
        selectedPath="/Sites/Foo"
        onSelectFolder={onSelectFolder}
      />,
    );
    await waitFor(() =>
      expect(screen.getByTestId("tree-node-/Sites/Foo")).toBeInTheDocument(),
    );
    const selectedRow = screen
      .getByTestId("tree-node-/Sites/Foo")
      .querySelector('[role="treeitem"]')!;
    expect(selectedRow).toHaveAttribute("aria-selected", "true");

    fireEvent.click(screen.getByTestId("tree-expand-all"));
    await waitFor(() =>
      expect(
        screen.getByTestId("tree-node-/Sites/Foo/Bar"),
      ).toBeInTheDocument(),
    );
    expect(onSelectFolder).not.toHaveBeenCalled();
    expect(selectedRow).toHaveAttribute("aria-selected", "true");

    fireEvent.click(screen.getByTestId("tree-collapse-all"));
    await waitFor(() =>
      expect(screen.queryByTestId("tree-node-/Sites/Foo/Bar")).toBeNull(),
    );
    expect(onSelectFolder).not.toHaveBeenCalled();
    expect(
      screen
        .getByTestId("tree-node-/Sites/Foo")
        .querySelector('[role="treeitem"]'),
    ).toHaveAttribute("aria-selected", "true");
  });
});
