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

import { describe, expect, it, vi } from "vitest";
import {
  applyExplorerTreeLoadError,
  collectExpandAllPendingLoads,
  handleExplorerTreeRowKeyDown,
  isExplorerTreeNodeSelected,
  resolveExplorerTreeError,
  resolveExplorerTreeRoot,
} from "../../../main/ts/contentExplorer/ExplorerTree";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";

function folder(path: string, folderPath?: string): PSPathItem {
  return {
    id: path,
    path,
    name: path.split("/").pop() ?? path,
    type: "folder",
    hasFolderChildren: true,
    ...(folderPath ? { folderPath } : {}),
  };
}

describe("ExplorerTree complexity-extraction helpers (#5793)", () => {
  it("applies a root load error in place", () => {
    const next = applyExplorerTreeLoadError({}, "/Sites", "/", "boom", {
      isRoot: true,
    });
    expect(next["/Sites"]?.error).toBe("boom");
    expect(next["/Sites"]?.loaded).toBe(true);
  });

  it("drops a stale non-root node on forced reload failure", () => {
    const prev = {
      "/": { loaded: true, loading: false, error: null, children: [] },
      "/Sites/Foo": {
        loaded: true,
        loading: false,
        error: null,
        children: [],
      },
    };
    const next = applyExplorerTreeLoadError(prev, "/Sites/Foo", "/", "boom", {
      isRoot: false,
      force: true,
    });
    expect(next["/Sites/Foo"]).toBeUndefined();
    expect(next["/"]).toBeDefined();
  });

  it("resolves root state and prefers the root error", () => {
    const nodes = {
      "/": { loaded: true, loading: false, error: "root-boom", children: [] },
      "/Sites": {
        loaded: true,
        loading: false,
        error: "child-boom",
        children: [],
      },
    };
    const { rootKey, rootState } = resolveExplorerTreeRoot(nodes, "/");
    expect(rootKey).toBe("/");
    expect(rootState.error).toBe("root-boom");
    expect(resolveExplorerTreeError(nodes, rootState.error)).toBe("root-boom");
  });

  it("falls back to the first node error", () => {
    const nodes = {
      "/": { loaded: true, loading: false, error: null, children: [] },
      "/Sites": {
        loaded: true,
        loading: false,
        error: "child-boom",
        children: [],
      },
    };
    expect(resolveExplorerTreeError(nodes, null)).toBe("child-boom");
    expect(
      resolveExplorerTreeError(
        { "/": { loaded: true, loading: false, error: null, children: [] } },
        null,
      ),
    ).toBeNull();
  });

  it("matches selection by finder path or folderPath list path", () => {
    const item = folder("/Assets", "//Folders/$System$/Assets");
    expect(isExplorerTreeNodeSelected(item, "/Assets", "/Assets")).toBe(true);
    expect(
      isExplorerTreeNodeSelected(item, "/Assets", "//Folders/$System$/Assets"),
    ).toBe(true);
    expect(isExplorerTreeNodeSelected(item, "/Assets", "/Sites")).toBe(false);
  });

  it("collects expand-all pending loads without duplicating known nodes", () => {
    const snapshot = {
      "/": {
        loaded: true,
        loading: false,
        error: null,
        children: [folder("/Sites")],
        listPath: "/",
      },
      "/Stale": {
        loaded: false,
        loading: false,
        error: null,
        children: [],
      },
    };
    const pending = collectExpandAllPendingLoads(snapshot);
    const keys = pending.map((p) => p.key).sort();
    expect(keys).toEqual(["/Sites", "/Stale"].sort());
  });

  it("routes Enter/Space/arrows through the row-key handler", () => {
    const onSelect = vi.fn();
    const onToggle = vi.fn();
    const onExpand = vi.fn();
    const ctx = {
      folderish: true,
      isOpen: false,
      isSiteRoot: false,
      onSelect,
      onToggle,
      onExpandIfCollapsed: onExpand,
    };
    handleExplorerTreeRowKeyDown(
      { key: "Enter", preventDefault: vi.fn() } as unknown as React.KeyboardEvent,
      ctx,
    );
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(onToggle).toHaveBeenCalledTimes(1);

    onToggle.mockClear();
    handleExplorerTreeRowKeyDown(
      { key: "ArrowLeft", preventDefault: vi.fn() } as unknown as React.KeyboardEvent,
      { ...ctx, isOpen: true },
    );
    expect(onToggle).toHaveBeenCalledTimes(1);

    const noopToggle = vi.fn();
    handleExplorerTreeRowKeyDown(
      { key: "ArrowDown", preventDefault: vi.fn() } as unknown as React.KeyboardEvent,
      { ...ctx, onToggle: noopToggle },
    );
    expect(noopToggle).not.toHaveBeenCalled();
  });
});
