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
import { describe, expect, it, vi } from "vitest";
import {
  ExplorerTree,
  isExplorerTreeMoveTarget,
  nextExplorerTreeRowIndex,
  treeRowMoveForKey,
} from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

/** Sibling folders under /Sites: Alpha, Beta, Frost. */
function sibling(path: string, name: string): PSPathItem {
  return {
    id: `keyboard-nav-${name}`,
    path,
    name,
    type: "folder",
    hasFolderChildren: false,
  };
}

const ALPHA = sibling("/Sites/Alpha", "Alpha");
const BETA = sibling("/Sites/Beta", "Beta");
const FROST = sibling("/Sites/Frost", "Frost");

/** Wire shape from PSPathItemList (pathApi.findChildren unwraps PathItem). */
function pathItemListResponse(items: PSPathItem[]): Response {
  return new Response(JSON.stringify({ PathItem: items }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

function mockSiblings(): void {
  mockFetch(async () => pathItemListResponse([ALPHA, BETA, FROST]));
}

async function renderSiblingTree(
  onSelectFolder: (path: string) => void = () => undefined,
  selectedPath: string | null = null,
): Promise<{ container: HTMLElement }> {
  mockSiblings();
  const rendered = render(
    <ExplorerTree
      initialPath="/Sites"
      selectedPath={selectedPath}
      onSelectFolder={onSelectFolder}
    />,
  );
  await waitFor(() =>
    expect(screen.getByTestId("tree-node-/Sites/Frost")).toBeInTheDocument(),
  );
  return rendered;
}

function focusTreeItem(path: string): HTMLElement {
  const item = screen
    .getByTestId(`tree-node-${path}`)
    .querySelector('[role="treeitem"]') as HTMLElement;
  item.focus();
  return item;
}

function treeItem(path: string): HTMLElement {
  return screen
    .getByTestId(`tree-node-${path}`)
    .querySelector('[role="treeitem"]') as HTMLElement;
}

function tabStops(): HTMLElement[] {
  return Array.from(
    document.querySelectorAll('[role="treeitem"]'),
  ).filter((el) => (el as HTMLElement).tabIndex === 0) as HTMLElement[];
}

describe("ExplorerTree keyboard-move helpers (#7035)", () => {
  it("maps ArrowUp/ArrowDown/Home/End to moves, nothing else", () => {
    expect(treeRowMoveForKey("ArrowDown")).toBe("next");
    expect(treeRowMoveForKey("ArrowUp")).toBe("prev");
    expect(treeRowMoveForKey("Home")).toBe("first");
    expect(treeRowMoveForKey("End")).toBe("last");
    expect(treeRowMoveForKey("Enter")).toBeNull();
    expect(treeRowMoveForKey(" ")).toBeNull();
    expect(treeRowMoveForKey("ArrowLeft")).toBeNull();
    expect(treeRowMoveForKey("ArrowRight")).toBeNull();
    expect(treeRowMoveForKey("ContextMenu")).toBeNull();
    expect(treeRowMoveForKey("a")).toBeNull();
  });

  it("wraps ArrowUp/ArrowDown at the ends, jumps Home/End", () => {
    expect(nextExplorerTreeRowIndex(3, 0, "next")).toBe(1);
    expect(nextExplorerTreeRowIndex(3, 2, "next")).toBe(0);
    expect(nextExplorerTreeRowIndex(3, 0, "prev")).toBe(2);
    expect(nextExplorerTreeRowIndex(3, 2, "prev")).toBe(1);
    expect(nextExplorerTreeRowIndex(3, 1, "first")).toBe(0);
    expect(nextExplorerTreeRowIndex(3, 1, "last")).toBe(2);
    expect(nextExplorerTreeRowIndex(0, 0, "next")).toBe(0);
    expect(nextExplorerTreeRowIndex(0, 5, "last")).toBe(0);
  });

  it("keeps text-entry targets on native key behavior", () => {
    expect(isExplorerTreeMoveTarget(document.createElement("div"))).toBe(true);
    expect(isExplorerTreeMoveTarget(document.createElement("input"))).toBe(
      false,
    );
    expect(isExplorerTreeMoveTarget(document.createElement("textarea"))).toBe(
      false,
    );
    expect(isExplorerTreeMoveTarget(document.createElement("select"))).toBe(
      false,
    );
    const editable = document.createElement("div");
    editable.setAttribute("contenteditable", "true");
    expect(isExplorerTreeMoveTarget(editable)).toBe(false);
    expect(isExplorerTreeMoveTarget(null)).toBe(false);
  });
});

describe("ExplorerTree arrow-key navigation (#7035)", () => {
  it("ArrowDown moves focus and selection to the next visible row", async () => {
    const onSelectFolder = vi.fn();
    const { container } = await renderSiblingTree(onSelectFolder);
    focusTreeItem("/Sites/Alpha");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Beta",
      expect.objectContaining({ path: "/Sites/Beta" }),
    );
    expect(document.activeElement).toBe(treeItem("/Sites/Beta"));
    await renderA11yGate(container);
  });

  it("ArrowUp/ArrowDown wrap around the ends", async () => {
    const onSelectFolder = vi.fn();
    await renderSiblingTree(onSelectFolder);
    // ArrowDown on the last row wraps to the first.
    focusTreeItem("/Sites/Frost");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Alpha",
      expect.objectContaining({ path: "/Sites/Alpha" }),
    );
    expect(document.activeElement).toBe(treeItem("/Sites/Alpha"));
    // ArrowUp on the first row wraps to the last.
    fireEvent.keyDown(document.activeElement!, { key: "ArrowUp" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Frost",
      expect.objectContaining({ path: "/Sites/Frost" }),
    );
    expect(document.activeElement).toBe(treeItem("/Sites/Frost"));
  });

  it("Home/End jump to the first/last visible row", async () => {
    const onSelectFolder = vi.fn();
    await renderSiblingTree(onSelectFolder);
    focusTreeItem("/Sites/Beta");
    fireEvent.keyDown(document.activeElement!, { key: "Home" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Alpha",
      expect.objectContaining({ path: "/Sites/Alpha" }),
    );
    fireEvent.keyDown(document.activeElement!, { key: "End" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Frost",
      expect.objectContaining({ path: "/Sites/Frost" }),
    );
    expect(document.activeElement).toBe(treeItem("/Sites/Frost"));
  });

  it("keeps exactly one tab stop on the focused row (roving tabindex)", async () => {
    const onSelectFolder = vi.fn();
    await renderSiblingTree(onSelectFolder, "/Sites/Beta");
    // Pre-interaction the shell selection is the single tab stop.
    expect(tabStops()).toHaveLength(1);
    expect(tabStops()[0]).toBe(treeItem("/Sites/Beta"));
    // After an arrow move the tab stop follows focus.
    focusTreeItem("/Sites/Beta");
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Frost",
      expect.objectContaining({ path: "/Sites/Frost" }),
    );
    expect(tabStops()).toHaveLength(1);
    expect(tabStops()[0]).toBe(treeItem("/Sites/Frost"));
  });

  it("labels rows with tree levels and carries the focus-ring class", async () => {
    await renderSiblingTree();
    for (const path of ["/Sites/Alpha", "/Sites/Beta", "/Sites/Frost"]) {
      const row = treeItem(path);
      expect(row.getAttribute("aria-level")).toBe("1");
      expect(row.classList.contains("perc-explorer-tree-row")).toBe(true);
    }
    // The :focus-visible ring style is rendered with the tree.
    const styles = Array.from(document.querySelectorAll("style")).map(
      (el) => el.textContent ?? "",
    );
    expect(
      styles.some((css) =>
        css.includes(".perc-explorer-tree-row:focus-visible"),
      ),
    ).toBe(true);
  });
});
