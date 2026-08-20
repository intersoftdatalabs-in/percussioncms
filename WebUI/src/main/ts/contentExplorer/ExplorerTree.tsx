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
   * selected folder (and the loaded parent that lists it) even if they were
   * already loaded. Used after Create Folder (#3640), Rename (#3645 / #3652)
   * and Delete (#3646 / #3653) so the tree shows the new name / drops the
   * old path key without a manual Refresh. Hosts should pass a dedicated
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

/** Immediate parent of a canonical tree path ({@code /} for roots). */
export function parentExplorerTreePathKey(
  path: string | null | undefined,
): string {
  const n = normalizeExplorerTreePathKey(path);
  if (n === "/") {
    return "/";
  }
  const slash = n.lastIndexOf("/");
  return slash <= 0 ? "/" : n.slice(0, slash);
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

interface NodeState {
  loaded: boolean;
  loading: boolean;
  error: string | null;
  children: PSPathItem[];
  /** Path actually sent to {@link findChildren} (folderPath vs finder id). */
  listPath?: string;
}

const EMPTY_STATE: NodeState = {
  loaded: false,
  loading: false,
  error: null,
  children: [],
};

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
    const toReload = collectEpochReloadKeys(
      snapshot,
      selectedPathRef.current,
      initialPath,
    );
    setExpanded((prev) => {
      const next = { ...prev };
      for (const reloadPath of toReload) {
        next[normalizeExplorerTreePathKey(reloadPath)] = true;
      }
      return next;
    });
    for (const reloadPath of toReload) {
      void ensureLoaded(reloadPath, null, true);
    }
    // Reload the selected folder, its listing parent, and the tree root —
    // not every expanded sibling (shotgun refetch, #3645 review).
    // selectedPath is read from a ref so selection clicks after an epoch
    // bump do not re-run this effect (#3640).
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