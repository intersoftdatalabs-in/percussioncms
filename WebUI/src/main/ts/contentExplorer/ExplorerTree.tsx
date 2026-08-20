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
 * Canonical tree node key. CMS paths are URL-style {@code /} (not OS
 * separators). Collapse repository {@code //} prefixes and trailing slashes
 * so {@code //Assets} and {@code /Assets/} match {@code /Assets}.
 */
export function normalizeExplorerTreePathKey(
  path: string | null | undefined,
): string {
  const raw = String(path ?? "").trim().replace(/\\/g, "/");
  if (!raw || raw === "/") {
    return "/";
  }
  let p = raw.replace(/^[A-Za-z]:/, "");
  while (p.startsWith("//")) {
    p = p.slice(1);
  }
  if (!p.startsWith("/")) {
    p = `/${p}`;
  }
  p = p.replace(/\/{2,}/g, "/");
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

/** Immediate parent of a canonical tree path ({@code /} for roots). */
export function parentExplorerTreePathKey(
  path: string | null | undefined,
): string {
  return parentExplorerTreePath(path) ?? "/";
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

function lookupNodeKey(
  record: Record<string, unknown>,
  path: string | null | undefined,
): string | undefined {
  const target = normalizeExplorerTreePathKey(path);
  return Object.keys(record).find(
    (key) => normalizeExplorerTreePathKey(key) === target,
  );
}

function pathItemMatchesSelected(
  item: Pick<PSPathItem, "path" | "folderPath"> | null | undefined,
  selectedPath: string | null | undefined,
): boolean {
  if (item == null || selectedPath == null || selectedPath === "") {
    return false;
  }
  const target = normalizeExplorerTreePathKey(selectedPath);
  return (
    normalizeExplorerTreePathKey(item.path) === target ||
    normalizeExplorerTreePathKey(item.folderPath) === target ||
    normalizeExplorerTreePathKey(resolveExplorerListPath(item, item.path)) ===
      target
  );
}

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
  nodes: Record<
    string,
    { children: ReadonlyArray<Pick<PSPathItem, "path" | "folderPath">> }
  >,
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
  /** Path actually sent to {@link findChildren} (folderPath vs finder id). */
  listPath?: string;
}

/**
 * Node keys to refetch after a folder mutation. Matches the selected list
 * path against tree keys <em>and</em> stored {@code listPath} values so a
 * finder id ({@code /Assets}) is not left stale when the shell selected
 * {@code //Folders/$System$/Assets} (#3652 / #3653).
 */
function collectEpochReloadKeys(
  nodes: Record<string, NodeState>,
  selectedPath: string | null,
  initialPath: string,
): string[] {
  const keys = new Set<string>();
  const addExisting = (path: string | null | undefined) => {
    const hit = lookupNodeKey(nodes, path);
    if (hit != null) {
      keys.add(hit);
    }
  };
  addExisting(initialPath);
  addExisting(selectedPath);
  addExisting(parentExplorerTreePathKey(selectedPath ?? initialPath));
  const selectedNorm = normalizeExplorerTreePathKey(
    selectedPath ?? initialPath,
  );
  for (const [key, state] of Object.entries(nodes)) {
    if (
      state.listPath != null &&
      normalizeExplorerTreePathKey(state.listPath) === selectedNorm
    ) {
      keys.add(key);
    }
    if (
      state.children.some((child) =>
        pathItemMatchesSelected(child, selectedPath),
      )
    ) {
      keys.add(key);
    }
  }
  if (keys.size === 0) {
    keys.add(normalizeExplorerTreePathKey(initialPath));
  }
  return [...keys];
}

function applyLoadedChildren(
  prev: Record<string, NodeState>,
  path: string,
  children: PSPathItem[],
  listPath: string,
): Record<string, NodeState> {
  const key =
    lookupNodeKey(prev, path) ?? normalizeExplorerTreePathKey(path);
  const oldChildren = prev[key]?.children ?? [];
  const next: Record<string, NodeState> = { ...prev };
  for (const alias of Object.keys(next)) {
    if (
      alias !== key &&
      normalizeExplorerTreePathKey(alias) === normalizeExplorerTreePathKey(key)
    ) {
      delete next[alias];
    }
  }
  next[key] = {
    loaded: true,
    loading: false,
    error: null,
    children,
    listPath,
  };
  const newPathKeys = new Set(
    children.map((child) => normalizeExplorerTreePathKey(child.path)),
  );
  for (const child of oldChildren) {
    const oldNorm = normalizeExplorerTreePathKey(child.path);
    if (newPathKeys.has(oldNorm)) {
      continue;
    }
    const stale = lookupNodeKey(next, child.path);
    if (
      stale != null &&
      normalizeExplorerTreePathKey(stale) !== normalizeExplorerTreePathKey(key)
    ) {
      delete next[stale];
    }
  }
  return next;
}

const EMPTY_STATE: NodeState = {
  loaded: false,
  loading: false,
  error: null,
  children: [],
};

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
    [normalizeExplorerTreePathKey(initialPath)]: true,
  }));
  const [nodes, setNodes] = useState<Record<string, NodeState>>(() => ({
    [normalizeExplorerTreePathKey(initialPath)]: EMPTY_STATE,
  }));
  const nodesRef = useRef(nodes);
  nodesRef.current = nodes;
  const selectedPathRef = useRef(selectedPath);
  selectedPathRef.current = selectedPath;

  const ensureLoaded = useCallback(
    async (path: string, folder?: PSPathItem | null, force?: boolean) => {
      const key =
        lookupNodeKey(nodesRef.current, path) ??
        normalizeExplorerTreePathKey(path);
      setNodes((prev) => {
        const curKey = lookupNodeKey(prev, key) ?? key;
        const cur = prev[curKey] ?? EMPTY_STATE;
        if (!force && (cur.loaded || cur.loading)) return prev;
        return { ...prev, [curKey]: { ...cur, loading: true, error: null } };
      });
      try {
        const existing =
          nodesRef.current[key] ??
          nodesRef.current[lookupNodeKey(nodesRef.current, path) ?? ""];
        const listPath =
          resolveExplorerListPath(folder, path) ?? existing?.listPath ?? path;
        const children = await findChildren(listPath);
        setNodes((prev) => applyLoadedChildren(prev, key, children, listPath));
      } catch (err) {
        const msg = formatApiError(err, message(EXPLORER_MSG.TREE_LOAD_ERROR));
        const rootKey = normalizeExplorerTreePathKey(initialPath);
        const isRoot = normalizeExplorerTreePathKey(key) === rootKey;
        setNodes((prev) => {
          if (!isRoot && force) {
            const next = { ...prev };
            const hit = lookupNodeKey(next, key);
            if (hit != null && normalizeExplorerTreePathKey(hit) !== rootKey) {
              delete next[hit];
            }
            return next;
          }
          const curKey = lookupNodeKey(prev, key) ?? key;
          return {
            ...prev,
            [curKey]: {
              loaded: true,
              loading: false,
              error: msg,
              children: [],
            },
          };
        });
      }
    },
    [initialPath],
  );

  useEffect(() => {
    void ensureLoaded(initialPath);
  }, [ensureLoaded, initialPath]);

  useEffect(() => {
    if (!childrenEpoch) {
      return;
    }
    const snapshot = nodesRef.current;
    const toReload = [
      ...new Set([
        ...collectChildrenEpochReloadPaths(
          snapshot,
          selectedPathRef.current,
          initialPath,
        ),
        ...collectEpochReloadKeys(
          snapshot,
          selectedPathRef.current,
          initialPath,
        ),
      ]),
    ];
    setExpanded((prev) => {
      const next = { ...prev };
      for (const reloadPath of toReload) {
        next[normalizeExplorerTreePathKey(reloadPath)] = true;
      }
      return next;
    });
    for (const reloadPath of toReload) {
      // Pass the PathItem when we have it so force-reload uses
      // folderPath (Assets → //Folders/$System$/Assets) not the finder key.
      void ensureLoaded(
        reloadPath,
        findFolderItemForPath(snapshot, reloadPath),
        true,
      );
    }
    // Reload selected + its listing parent + root. Keep the parent expanded
    // so rename/delete is visible without View → Refresh. selectedPath is
    // read from a ref so selection clicks after an epoch bump do not re-run
    // this effect (#3640). Do not walk every loaded node (#3645 review).
  }, [childrenEpoch, ensureLoaded, initialPath]);

  const toggle = useCallback(
    (path: string, folder: PSPathItem) => {
      const key = normalizeExplorerTreePathKey(path);
      setExpanded((prev) => ({
        ...prev,
        [key]: !(prev[key] ?? prev[path] ?? false),
      }));
      const node = nodes[key] ?? nodes[lookupNodeKey(nodes, path) ?? ""];
      if (!node?.loaded) {
        void ensureLoaded(key, folder);
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
    const pathKey = normalizeExplorerTreePathKey(path);
    const isOpen = expanded[pathKey] ?? expanded[path] ?? false;
    const state =
      nodes[pathKey] ??
      nodes[lookupNodeKey(nodes, path) ?? ""] ??
      EMPTY_STATE;
    const listPath = resolveExplorerListPath(folder, path);
    const selectedNorm = normalizeExplorerTreePathKey(selectedPath);
    const selected =
      selectedNorm === pathKey ||
      (listPath != null &&
        selectedNorm === normalizeExplorerTreePathKey(listPath));
    const folderish = isFolder(folder);

    return (
      <div key={pathKey} data-testid={`tree-node-${path}`}>
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

  const rootKey =
    lookupNodeKey(nodes, initialPath) ??
    normalizeExplorerTreePathKey(initialPath);
  const rootState = nodes[rootKey] ?? nodes[initialPath] ?? EMPTY_STATE;
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
