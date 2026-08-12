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

/**
 * Accessible navigation tree for Architecture (#3095 / #3096).
 *
 * <p>Uses ARIA tree / treeitem + keyboard expand/collapse and focus movement
 * (peer: ExplorerTree). Structure mutations are driven from the shell toolbar
 * against the selected node.</p>
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import type { NavTreeNode } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import { isNavBranch, sectionTypeLabel } from "./treeModel";
import { ARCH_MSG } from "./messages";

export interface NavTreeProps {
  root: NavTreeNode | null;
  /** When true, show loading chrome instead of tree. */
  loading?: boolean;
  /** Error message to surface (role=alert). */
  error?: string | null;
  /** Optional controlled selection (section id). */
  selectedId?: string | null;
  onSelect?: (node: NavTreeNode) => void;
}

const treeContainerStyle: React.CSSProperties = {
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: 8,
  background: "#fafafa",
  padding: "8px 0",
  minHeight: 120,
  maxHeight: "min(60vh, 520px)",
  overflow: "auto",
};

const emptyStyle: React.CSSProperties = {
  padding: "1rem 1.25rem",
  color: catalogColors.empty,
  fontSize: "0.95rem",
};

const errorStyle: React.CSSProperties = {
  ...emptyStyle,
  color: catalogColors.error,
};

function nodeRowStyle(
  selected: boolean,
  depth: number,
): React.CSSProperties {
  return {
    display: "flex",
    alignItems: "center",
    gap: 6,
    padding: "4px 10px 4px",
    paddingLeft: 10 + depth * 16,
    cursor: "pointer",
    background: selected ? "#e6f4f8" : "transparent",
    borderLeft: selected
      ? `3px solid ${catalogColors.accent}`
      : "3px solid transparent",
    fontSize: "0.95rem",
    lineHeight: 1.35,
    outline: "none",
  };
}

const toggleStyle: React.CSSProperties = {
  display: "inline-flex",
  width: 16,
  justifyContent: "center",
  color: catalogColors.muted,
  userSelect: "none",
  flexShrink: 0,
};

const badgeStyle: React.CSSProperties = {
  marginLeft: 6,
  fontSize: "0.7rem",
  fontWeight: 600,
  textTransform: "uppercase" as const,
  letterSpacing: "0.03em",
  color: catalogColors.muted,
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: 3,
  padding: "0 4px",
  background: "#fff",
};

/** Flatten visible nodes in document order for arrow up/down. */
function collectVisible(
  node: NavTreeNode,
  expanded: Record<string, boolean>,
  out: NavTreeNode[] = [],
): NavTreeNode[] {
  out.push(node);
  if (isNavBranch(node) && (expanded[node.id] ?? false)) {
    for (const child of node.children) {
      collectVisible(child, expanded, out);
    }
  }
  return out;
}

/** Parent id map for ArrowLeft focus-to-parent. */
function buildParentMap(
  node: NavTreeNode,
  parentId: string | null = null,
  map: Map<string, string | null> = new Map(),
): Map<string, string | null> {
  map.set(node.id, parentId);
  if (node.children?.length) {
    for (const child of node.children) {
      buildParentMap(child, node.id, map);
    }
  }
  return map;
}

/**
 * Site navigation tree (navons / sections) with selection for structure actions.
 */
export function NavTree({
  root,
  loading = false,
  error = null,
  selectedId = null,
  onSelect,
}: NavTreeProps): React.ReactElement {
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const treeRef = useRef<HTMLDivElement | null>(null);

  const rootId = root?.id ?? null;
  useEffect(() => {
    if (rootId) {
      setExpanded((prev) =>
        prev[rootId] ? prev : { ...prev, [rootId]: true },
      );
    }
  }, [rootId]);

  const toggle = useCallback((id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  }, []);

  const setExpandedOpen = useCallback((id: string, open: boolean) => {
    setExpanded((prev) =>
      prev[id] === open ? prev : { ...prev, [id]: open },
    );
  }, []);

  const focusItem = useCallback((id: string) => {
    const el = treeRef.current?.querySelector<HTMLElement>(
      `[data-testid="nav-tree-item-${id}"]`,
    );
    el?.focus();
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent, node: NavTreeNode) => {
      if (!root) return;
      const branch = isNavBranch(node);
      const open = expanded[node.id] ?? false;
      const visible = collectVisible(root, expanded);
      const parentMap = buildParentMap(root);
      const idx = visible.findIndex((n) => n.id === node.id);

      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        onSelect?.(node);
        if (branch) toggle(node.id);
        return;
      }

      if (e.key === "ArrowRight") {
        e.preventDefault();
        if (branch && !open) {
          setExpandedOpen(node.id, true);
          return;
        }
        if (branch && open && node.children.length > 0) {
          focusItem(node.children[0].id);
        }
        return;
      }

      if (e.key === "ArrowLeft") {
        e.preventDefault();
        if (branch && open) {
          setExpandedOpen(node.id, false);
          return;
        }
        const parentId = parentMap.get(node.id);
        if (parentId) {
          focusItem(parentId);
        }
        return;
      }

      if (e.key === "ArrowDown") {
        e.preventDefault();
        if (idx >= 0 && idx < visible.length - 1) {
          focusItem(visible[idx + 1].id);
        }
        return;
      }

      if (e.key === "ArrowUp") {
        e.preventDefault();
        if (idx > 0) {
          focusItem(visible[idx - 1].id);
        }
        return;
      }

      if (e.key === "Home") {
        e.preventDefault();
        if (visible.length > 0) focusItem(visible[0].id);
        return;
      }

      if (e.key === "End") {
        e.preventDefault();
        if (visible.length > 0) focusItem(visible[visible.length - 1].id);
      }
    },
    [root, expanded, onSelect, toggle, setExpandedOpen, focusItem],
  );

  const renderNode = (
    node: NavTreeNode,
    depth: number,
  ): React.ReactElement => {
    const branch = isNavBranch(node);
    const open = expanded[node.id] ?? false;
    const selected = selectedId === node.id;
    const typeBadge = sectionTypeLabel(node.sectionType);
    // Roving tabindex: selected item, else root when nothing selected
    const isTabStop =
      selected ||
      (selectedId == null && root != null && node.id === root.id);

    return (
      <div key={node.id} data-testid={`nav-tree-node-${node.id}`}>
        <div
          role="treeitem"
          aria-level={depth + 1}
          aria-expanded={branch ? open : undefined}
          aria-selected={selected}
          tabIndex={isTabStop ? 0 : -1}
          data-testid={`nav-tree-item-${node.id}`}
          data-section-type={node.sectionType}
          style={nodeRowStyle(selected, depth)}
          onClick={() => onSelect?.(node)}
          onKeyDown={(e) => handleKeyDown(e, node)}
        >
          <span
            style={toggleStyle}
            onClick={(e) => {
              e.stopPropagation();
              if (branch) toggle(node.id);
            }}
            aria-hidden="true"
            data-testid={branch ? `nav-tree-toggle-${node.id}` : undefined}
          >
            {branch ? (open ? "▾" : "▸") : "·"}
          </span>
          <span
            style={{ flex: 1, minWidth: 0 }}
            title={node.folderPath ?? node.title}
            {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}
          >
            {node.title}
            {typeBadge ? (
              <span
                style={badgeStyle}
                data-testid={`nav-tree-badge-${node.id}`}
              >
                {typeBadge}
              </span>
            ) : null}
            {node.requiresLogin ? (
              <span
                style={badgeStyle}
                title="Requires login"
                data-testid={`nav-tree-secure-${node.id}`}
              >
                Secure
              </span>
            ) : null}
          </span>
        </div>
        {branch && open
          ? node.children.map((child) => renderNode(child, depth + 1))
          : null}
      </div>
    );
  };

  let body: React.ReactNode;
  if (loading) {
    body = (
      <div
        style={emptyStyle}
        data-testid="architecture-nav-tree-loading"
        aria-live="polite"
      >
        {ARCH_MSG.TREE_LOADING}
      </div>
    );
  } else if (error) {
    // Not role=tree: axe aria-required-children fails when there are no treeitems.
    body = (
      <div
        style={errorStyle}
        role="alert"
        data-testid="architecture-nav-tree-error"
      >
        {error}
      </div>
    );
  } else if (!root) {
    body = (
      <div style={emptyStyle} data-testid="architecture-nav-tree-empty">
        {ARCH_MSG.TREE_EMPTY}
      </div>
    );
  } else {
    body = (
      <div
        ref={treeRef}
        role="tree"
        aria-label={ARCH_MSG.TREE_PANEL_TITLE}
      >
        {renderNode(root, 0)}
      </div>
    );
  }

  return (
    <div
      className="perc-architecture-nav-tree"
      data-testid="architecture-nav-tree"
      style={treeContainerStyle}
    >
      {body}
    </div>
  );
}

export default NavTree;
