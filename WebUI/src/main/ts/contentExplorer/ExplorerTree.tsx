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

/**
 * Folder tree for the modern Content Explorer (US1 / T018).
 *
 * <p>Lazy children load — each node fetches its children on first expand.
 * Selection is reported back via {@link ExplorerTreeProps.onSelectFolder}.
 * Empty/error states use the shared TMX keys from {@link EXPLORER_MSG}.</p>
 */

import React, { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { formatApiError } from "../api/client";
import { findChildren } from "../api/contentExplorer/pathApi";
import type { PSPathItem } from "../api/contentExplorer/types";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import { message } from "../i18n/message";
import { isSafeExplorerTreeChild } from "./folderPath";
import { resolveExplorerListPath } from "./sitePath";
import { isFolder } from "./selection";
import {
  emptyStateStyle,
  errorStateStyle,
  nodeLabelStyle,
  nodeRowStyle,
  treeStyle,
  toggleStyle,
} from "./styles";
import { EXPLORER_MSG } from "./messages";

export interface ExplorerTreeProps {
  /** Roots to display (defaults to product root, fetched on first expand). */
  initialPath?: string;
  /** Currently selected folder path (controlled). */
  selectedPath: string | null;
  /** Fires when the user activates a folder row. */
  onSelectFolder: (path: string, folder: PSPathItem | null) => void;
  /**
   * Fires when the user toggles a folder. Parent uses this to refresh the
   * detail list when a new folder is selected.
   */
  onActivate?: (path: string, folder: PSPathItem) => void;
  /**
   * When this value changes (and is non-zero), reload children of the
   * selected folder, its tree parent, and the tree root even if they were
   * already loaded. Used after Create Folder (#3640), Rename (#3645 / #3652),
   * and Delete (#3646 / #3653) so the tree shows the new name (or drops the
   * deleted name) without a manual Refresh. Hosts should pass a dedicated
   * folder-mutation epoch, not the detail-list epoch.
   */
  childrenEpoch?: number;
}

/**
 * Compare tree node keys with list paths that may differ by a trailing
 * slash or a repository {@code //} prefix ({@code /Assets} vs
 * {@code /Assets/} vs {@code //Assets}).
 */
export function normalizeExplorerTreePathKey(
  path: string | null | undefined,
): string {
  let p = String(path ?? "").trim().replace(/\\/g, "/");
  if (!p || p === "/") {
    return "/";
  }
  p = p.replace(/^[A-Za-z]:/, "");
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  p = p.replace(/\/{2,}/g, "/");
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  return p.replace(/\/+$/, "") || "/";
}

/**
 * Parent of a CMS tree path ({@code /Sites/Foo} → {@code /Sites}).
 * Root {@code /} has no parent.
 */
export function parentExplorerTreePath(
  path: string | null | undefined,
): string | null {
  const n = normalizeExplorerTreePathKey(path);
  if (n === "/") {
    return null;
  }
  const i = n.lastIndexOf("/");
  if (i <= 0) {
    return "/";
  }
  return n.slice(0, i) || "/";
}

function pathKeyMatches(
  candidate: string | null | undefined,
  wanted: ReadonlySet<string>,
): boolean {
  if (candidate == null || String(candidate).trim().length === 0) {
    return false;
  }
  return wanted.has(normalizeExplorerTreePathKey(candidate));
}

/**
 * Node keys to force-reload on a folder-mutation epoch.
 *
 * <p>Product Explorer seeds {@code initialPath="/"} while the selected list
 * path may be a repository {@code folderPath} ({@code /Folders/$System$/Assets})
 * and the visible tree node is keyed by finder {@code path} ({@code /Assets}).
 * Reloading only {@code selectedPath} + root leaves the expanded parent's
 * children stale after Delete/Rename (#3653 / #3652). Match loaded children
 * by path <em>or</em> folderPath, and always include the tree parent of
 * {@code selectedPath} so a tree-selected deleted folder drops from its
 * parent without View → Refresh.</p>
 *
 * <p>Does not walk every loaded node — expanded siblings stay cached
 * (#3645 review).</p>
 */
export function collectChildrenEpochReloadPaths(
  nodes: Record<string, { children: ReadonlyArray<Pick<PSPathItem, "path" | "folderPath">> }>,
  selectedPath: string | null,
  initialPath: string,
): string[] {
  const out = new Set<string>();
  const hasNode = (key: string | null | undefined): boolean =>
    key != null && Object.prototype.hasOwnProperty.call(nodes, key);
  if (initialPath) {
    out.add(initialPath);
  }
  const selectedKey = resolveLoadedNodePath(nodes, selectedPath, initialPath);
  if (hasNode(selectedKey)) {
    out.add(selectedKey);
  }
  const wanted = new Set<string>();
  const addWanted = (p: string | null | undefined): void => {
    const n = normalizeExplorerTreePathKey(p);
    if (n !== "/") {
      wanted.add(n);
    }
  };
  addWanted(selectedPath);
  addWanted(selectedKey);
  addWanted(parentExplorerTreePath(selectedPath));
  addWanted(parentExplorerTreePath(selectedKey));

  for (const [key, state] of Object.entries(nodes)) {
    if (pathKeyMatches(key, wanted)) {
      out.add(key);
    }
    for (const child of state.children ?? []) {
      if (
        pathKeyMatches(child.path, wanted) ||
        pathKeyMatches(child.folderPath, wanted)
      ) {
        out.add(key);
        if (hasNode(child.path)) {
          out.add(child.path);
        }
      }
    }
  }
  return [...out];
}

interface NodeState {
  loaded: boolean;
  loading: boolean;
  error: string | null;
  children: PSPathItem[];
}

const EMPTY_STATE: NodeState = {
  loaded: false,
  loading: false,
  error: null,
  children: [],
};

function resolveLoadedNodePath(
  nodes: Record<string, unknown>,
  selectedPath: string | null,
  fallback: string,
): string {
  const target = normalizeExplorerTreePathKey(selectedPath ?? fallback);
  const hit = Object.keys(nodes).find(
    (key) => normalizeExplorerTreePathKey(key) === target,
  );
  return hit ?? selectedPath ?? fallback;
}

function findFolderItemForPath(
  nodes: Record<string, NodeState>,
  path: string,
): PSPathItem | null {
  const target = normalizeExplorerTreePathKey(path);
  for (const state of Object.values(nodes)) {
    for (const child of state.children) {
      if (normalizeExplorerTreePathKey(child.path) === target) {
        return child;
      }
      if (
        child.folderPath &&
        normalizeExplorerTreePathKey(child.folderPath) === target
      ) {
        return child;
      }
    }
  }
  return null;
}

export function ExplorerTree({
  initialPath = "/",
  selectedPath,
  onSelectFolder,
  onActivate,
  childrenEpoch = 0,
}: ExplorerTreeProps): React.ReactElement {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => ({
    [initialPath]: true,
  }));
  const [nodes, setNodes] = useState<Record<string, NodeState>>(() => ({
    [initialPath]: EMPTY_STATE,
  }));
  const nodesRef = useRef(nodes);
  nodesRef.current = nodes;
  const selectedPathRef = useRef(selectedPath);
  selectedPathRef.current = selectedPath;

  const ensureLoaded = useCallback(
    async (path: string, folder?: PSPathItem | null, force?: boolean) => {
      setNodes((prev) => {
        const cur = prev[path] ?? EMPTY_STATE;
        if (!force && (cur.loaded || cur.loading)) return prev;
        return { ...prev, [path]: { ...cur, loading: true, error: null } };
      });
      try {
        const listPath = resolveExplorerListPath(folder, path) ?? path;
        const children = await findChildren(listPath);
        setNodes((prev) => ({
          ...prev,
          [path]: { loaded: true, loading: false, error: null, children },
        }));
      } catch (err) {
        const msg = formatApiError(err, message(EXPLORER_MSG.TREE_LOAD_ERROR));
        setNodes((prev) => ({
          ...prev,
          [path]: { loaded: true, loading: false, error: msg, children: [] },
        }));
      }
    },
    [],
  );

  useEffect(() => {
    void ensureLoaded(initialPath);
  }, [ensureLoaded, initialPath]);

  useEffect(() => {
    if (!childrenEpoch) {
      return;
    }
    const snapshot = nodesRef.current;
    const toReload = collectChildrenEpochReloadPaths(
      snapshot,
      selectedPathRef.current,
      initialPath,
    );
    for (const path of toReload) {
      // Pass the PathItem when we have it so force-reload uses
      // folderPath (Assets → //Folders/$System$/Assets) not the finder key.
      void ensureLoaded(path, findFolderItemForPath(snapshot, path), true);
    }
    // Reload selected + its tree parent + root. selectedPath is read from
    // a ref so selection clicks after an epoch bump do not re-run this
    // effect (#3640). Do not walk every loaded node (#3645 review).
  }, [childrenEpoch, ensureLoaded, initialPath]);

  const toggle = useCallback(
    (path: string, folder: PSPathItem) => {
      setExpanded((prev) => ({ ...prev, [path]: !prev[path] }));
      if (!nodes[path]?.loaded) {
        void ensureLoaded(path, folder);
      }
      onActivate?.(path, folder);
    },
    [ensureLoaded, nodes, onActivate],
  );

  const renderNode = (
    folder: PSPathItem,
    depth: number,
  ): React.ReactElement => {
    const path = folder.path;
    const isOpen = expanded[path] ?? false;
    const state = nodes[path] ?? EMPTY_STATE;
    const listPath = resolveExplorerListPath(folder, path);
    const selected =
      selectedPath === path ||
      (listPath != null && selectedPath === listPath);
    const folderish = isFolder(folder);

    return (
      <div key={path} data-testid={`tree-node-${path}`}>
        <div
          role="treeitem"
          aria-expanded={folderish ? isOpen : undefined}
          aria-selected={selected}
          tabIndex={0}
          style={nodeRowStyle(selected, depth)}
          onClick={() => onSelectFolder(path, folder)}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              onSelectFolder(path, folder);
              if (folderish) toggle(path, folder);
              return;
            }
            // ARIA tree keyboard navigation (WCAG 2.1):
            if (e.key === "ArrowRight") {
              e.preventDefault();
              if (folderish && !isOpen) toggle(path, folder);
            } else if (e.key === "ArrowLeft") {
              e.preventDefault();
              if (folderish && isOpen) toggle(path, folder);
            } else if (e.key === "ArrowDown" || e.key === "ArrowUp") {
              // Let the browser handle focus traversal between treeitems;
              // native tab order through `[tabIndex={0}]` rows is correct.
              return;
            }
          }}
        >
          <span
            style={toggleStyle}
            onClick={(e) => {
              e.stopPropagation();
              if (folderish) toggle(path, folder);
            }}
            aria-hidden="true"
          >
            {folderish ? (isOpen ? "▾" : "▸") : " "}
          </span>
          <span
            style={nodeLabelStyle}
            title={folder.path}
            {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}
          >
            {folder.name || folder.path}
          </span>
        </div>
        {folderish &&
          isOpen &&
          state.children
            // Guard against self-path or ancestor cycles from a bad API payload
            // (would recurse forever in render). Accept name vs FOLDER_ROOT
            // prefix mismatch on sample sites (#3001 / #3326).
            .filter((child) =>
              isSafeExplorerTreeChild(path, folder.folderPath, child.path),
            )
            .map((child) => renderNode(child, depth + 1))}
      </div>
    );
  };

  const rootState = nodes[initialPath] ?? EMPTY_STATE;
  const error = useMemo(() => {
    if (rootState.error) return rootState.error;
    const firstError = Object.values(nodes).find((n) => n.error)?.error;
    return firstError ?? null;
  }, [nodes, rootState.error]);

  if (error) {
    // Not role=tree: axe aria-required-children fails when there are no treeitems.
    return (
      <div style={treeStyle} data-testid="explorer-tree">
        <div style={errorStateStyle} role="alert" data-testid="explorer-tree-error">
          {error}
        </div>
      </div>
    );
  }

  if (rootState.loaded && rootState.children.length === 0) {
    return (
      <div style={treeStyle} data-testid="explorer-tree">
        <div style={emptyStateStyle} data-testid="explorer-tree-empty">
          {message(EXPLORER_MSG.TREE_EMPTY)}
        </div>
      </div>
    );
  }

  return (
    <div style={treeStyle} role="tree" data-testid="explorer-tree">
      {rootState.loading && rootState.children.length === 0 && (
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.TREE_LOADING)}</div>
      )}
      {rootState.children.map((child) => renderNode(child, 0))}
    </div>
  );
}