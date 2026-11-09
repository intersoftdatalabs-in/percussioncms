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
  EXPLORER_TREE_TYPE_AHEAD_TIMEOUT_MS,
  ExplorerTree,
  appendExplorerTreeTypeAhead,
  isExplorerTreeTypeAheadKey,
  matchExplorerTreeTypeAhead,
} from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { mockFetch } from "./setup";
import { renderA11yGate } from "./a11y";

/** Sibling folders under /Sites: Alpha, Beta, Frost. */
function sibling(path: string, name: string): PSPathItem {
  return {
    id: `typeahead-${name}`,
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

async function renderSiblingTree(onSelectFolder: (path: string) => void = () => undefined): Promise<{ container: HTMLElement }> {
  mockSiblings();
  const rendered = render(
    <ExplorerTree
      initialPath="/Sites"
      selectedPath={null}
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

describe("ExplorerTree type-ahead helpers (#5841)", () => {
  it("accepts single printable keys and rejects navigation/modifiers", () => {
    expect(isExplorerTreeTypeAheadKey({ key: "b" })).toBe(true);
    expect(isExplorerTreeTypeAheadKey({ key: " " })).toBe(true);
    expect(isExplorerTreeTypeAheadKey({ key: "ArrowDown" })).toBe(false);
    expect(isExplorerTreeTypeAheadKey({ key: "Enter" })).toBe(false);
    expect(isExplorerTreeTypeAheadKey({ key: "Home" })).toBe(false);
    expect(isExplorerTreeTypeAheadKey({ key: "b", ctrlKey: true })).toBe(false);
    expect(isExplorerTreeTypeAheadKey({ key: "b", metaKey: true })).toBe(false);
    expect(isExplorerTreeTypeAheadKey({ key: "b", altKey: true })).toBe(false);
  });

  it("folds buffer case for prefix matching", () => {
    expect(appendExplorerTreeTypeAhead("", "B")).toBe("b");
    expect(appendExplorerTreeTypeAhead("f", "R")).toBe("fr");
  });

  it("matches the next prefix hit with wrap-around, -1 on no match", () => {
    const rows = [{ label: "Alpha" }, { label: "Beta" }, { label: "Frost" }];
    expect(matchExplorerTreeTypeAhead(rows, "b", -1)).toBe(1);
    expect(matchExplorerTreeTypeAhead(rows, "F", -1)).toBe(2);
    // Search is exclusive of the anchor: from Beta, "b" wraps past Frost/Alpha back to Beta.
    expect(matchExplorerTreeTypeAhead(rows, "b", 1)).toBe(1);
    expect(matchExplorerTreeTypeAhead(rows, "z", 0)).toBe(-1);
    expect(matchExplorerTreeTypeAhead(rows, "", 0)).toBe(-1);
    expect(matchExplorerTreeTypeAhead([], "b", -1)).toBe(-1);
    expect(matchExplorerTreeTypeAhead(rows, "fr", -1)).toBe(2);
  });

  it("keeps the 1s accumulation window constant sane", () => {
    expect(EXPLORER_TREE_TYPE_AHEAD_TIMEOUT_MS).toBe(1000);
  });
});

describe("ExplorerTree type-ahead (#5841)", () => {
  it("typing a visible prefix moves focus and selection", async () => {
    const onSelectFolder = vi.fn();
    const { container } = await renderSiblingTree(onSelectFolder);
    focusTreeItem("/Sites/Alpha");
    fireEvent.keyDown(document.activeElement!, { key: "f" });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Frost",
      expect.objectContaining({ path: "/Sites/Frost" }),
    );
    expect(document.activeElement).toBe(
      screen
        .getByTestId("tree-node-/Sites/Frost")
        .querySelector('[role="treeitem"]'),
    );
    await renderA11yGate(container);
  });

  it("accumulates rapid keys into one prefix and wraps around", async () => {
    const onSelectFolder = vi.fn();
    await renderSiblingTree(onSelectFolder);
    // Anchor on Frost; "b" wraps around to Beta.
    focusTreeItem("/Sites/Frost");
    fireEvent.keyDown(document.activeElement!, { key: "b" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Beta",
      expect.objectContaining({ path: "/Sites/Beta" }),
    );
    // Rapid second key narrows the buffer: "be" still matches Beta.
    fireEvent.keyDown(document.activeElement!, { key: "e" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Beta",
      expect.objectContaining({ path: "/Sites/Beta" }),
    );
  });

  it("resets the buffer after the timeout so the next key starts fresh", async () => {
    const onSelectFolder = vi.fn();
    await renderSiblingTree(onSelectFolder);
    focusTreeItem("/Sites/Alpha");
    fireEvent.keyDown(document.activeElement!, { key: "f" });
    expect(onSelectFolder).toHaveBeenLastCalledWith(
      "/Sites/Frost",
      expect.objectContaining({ path: "/Sites/Frost" }),
    );
    await new Promise((resolve) =>
      setTimeout(resolve, EXPLORER_TREE_TYPE_AHEAD_TIMEOUT_MS + 250),
    );
    // Fresh buffer "a" matches Alpha from the Frost anchor (wrap).
    onSelectFolder.mockClear();
    fireEvent.keyDown(document.activeElement!, { key: "a" });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Alpha",
      expect.objectContaining({ path: "/Sites/Alpha" }),
    );
  });

  it("ignores non-printable keys and keeps selection on no match", async () => {
    const onSelectFolder = vi.fn();
    const { container } = await renderSiblingTree(onSelectFolder);
    const alphaItem = focusTreeItem("/Sites/Alpha");
    // Navigation keys fall through to native traversal: no type-ahead jump.
    fireEvent.keyDown(document.activeElement!, { key: "ArrowDown" });
    expect(onSelectFolder).not.toHaveBeenCalled();
    // No-match prefix keeps selection and focus.
    fireEvent.keyDown(document.activeElement!, { key: "z" });
    expect(onSelectFolder).not.toHaveBeenCalled();
    expect(document.activeElement).toBe(alphaItem);
    // Enter still activates via the row handler (pre-existing behavior);
    // the row preventDefaults so type-ahead skips the already-handled key.
    onSelectFolder.mockClear();
    fireEvent.keyDown(document.activeElement!, { key: "Enter" });
    expect(onSelectFolder).toHaveBeenCalledWith(
      "/Sites/Alpha",
      expect.objectContaining({ path: "/Sites/Alpha" }),
    );
    await renderA11yGate(container);
  });
});
