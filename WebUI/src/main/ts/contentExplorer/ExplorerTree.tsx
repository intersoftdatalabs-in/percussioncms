/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React, { useEffect, useState, useCallback, useMemo } from "react";
import { findChildren } from "../api/contentExplorer/pathApi";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
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

export function ExplorerTree({
  initialPath = "/",
  selectedPath,
  onSelectFolder,
  onActivate,
}: ExplorerTreeProps): React.ReactElement {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => ({
    [initialPath]: true,
  }));
  const [nodes, setNodes] = useState<Record<string, NodeState>>(() => ({
    [initialPath]: EMPTY_STATE,
  }));

  const ensureLoaded = useCallback(async (path: string) => {
    setNodes((prev) => {
      const cur = prev[path] ?? EMPTY_STATE;
      if (cur.loaded || cur.loading) return prev;
      return { ...prev, [path]: { ...cur, loading: true, error: null } };
    });
    try {
      const children = await findChildren(path);
      setNodes((prev) => ({
        ...prev,
        [path]: { loaded: true, loading: false, error: null, children },
      }));
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setNodes((prev) => ({
        ...prev,
        [path]: { loaded: true, loading: false, error: msg, children: [] },
      }));
    }
  }, []);

  useEffect(() => {
    void ensureLoaded(initialPath);
  }, [ensureLoaded, initialPath]);

  const toggle = useCallback(
    (path: string, folder: PSPathItem) => {
      setExpanded((prev) => ({ ...prev, [path]: !prev[path] }));
      if (!nodes[path]?.loaded) {
        void ensureLoaded(path);
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
    const selected = selectedPath === path;
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
          <span style={nodeLabelStyle} title={folder.path}>
            {folder.name || folder.path}
          </span>
        </div>
        {folderish && isOpen && state.children.map((child) =>
          renderNode(child, depth + 1),
        )}
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
    return (
      <div style={treeStyle} role="tree" data-testid="explorer-tree">
        <div style={errorStateStyle} role="alert">
          {message(EXPLORER_MSG.TREE_LOAD_ERROR)}: {error}
        </div>
      </div>
    );
  }

  if (rootState.loaded && rootState.children.length === 0) {
    return (
      <div style={treeStyle} role="tree" data-testid="explorer-tree">
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.TREE_EMPTY)}</div>
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