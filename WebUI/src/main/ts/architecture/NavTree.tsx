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
 * Accessible navigation tree for Architecture (#3095 / #3096 / #3354).
 *
 * <p>Uses ARIA tree / treeitem + APG keyboard (Tab in, arrows, Home/End,
 * no trap). Structure mutations are driven from the shell toolbar against
 * the selected node.</p>
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import type { NavTreeNode } from "../api/architecture/types";
import { catalogColors } from "../developer/catalogStyles";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import type { SectionType } from "../api/architecture/types";
import { isNavBranch } from "./treeModel";
import { ARCH_MSG, ARCH_MSG_KEYS } from "./messages";
import { collectVisibleNavNodes, resolveNavTreeKey } from "./navTreeKeyboard";

/** i18n type badge for nav tree rows (#3098 / #3351 blog). */
function typeBadgeMeta(
  sectionType: SectionType | string,
): { label: string; i18nKey: string } | null {
  switch (String(sectionType).toLowerCase()) {
    case "section":
      return null;
    case "sectionlink":
      return {
        label: ARCH_MSG.TYPE_SECTION_LINK,
        i18nKey: ARCH_MSG_KEYS.TYPE_SECTION_LINK,
      };
    case "externallink":
      return {
        label: ARCH_MSG.TYPE_EXTERNAL_LINK,
        i18nKey: ARCH_MSG_KEYS.TYPE_EXTERNAL_LINK,
      };
    case "blog":
      return {
        label: ARCH_MSG.TYPE_BLOG,
        i18nKey: ARCH_MSG_KEYS.TYPE_BLOG,
      };
    default:
      // Do not surface raw CMS type names for unforeseen section types.
      return null;
  }
}

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
  };
}

const TREE_ITEM_FOCUS_CSS = `
.perc-architecture-nav-tree-item:focus {
  outline: none;
}
.perc-architecture-nav-tree-item:focus-visible {
  outline: 2px solid ${catalogColors.accent};
  outline-offset: -2px;
}
`;

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
  const [focusedId, setFocusedId] = useState<string | null>(
    selectedId ?? root?.id ?? null,
  );
  const treeRef = useRef<HTMLDivElement | null>(null);

  const rootId = root?.id ?? null;
  useEffect(() => {
    if (rootId) {
      setExpanded((prev) =>
        prev[rootId] ? prev : { ...prev, [rootId]: true },
      );
    }
  }, [rootId]);

  useEffect(() => {
    setFocusedId(selectedId ?? rootId);
  }, [rootId, selectedId]);

  useEffect(() => {
    if (!root || !focusedId) return;
    const visible = collectVisibleNavNodes(root, expanded);
    if (visible.some((n) => n.id === focusedId)) {
      return;
    }
    const selectedVisible =
      selectedId != null && visible.some((n) => n.id === selectedId);
    setFocusedId(selectedVisible ? selectedId : (visible[0]?.id ?? root.id));
  }, [root, expanded, focusedId, selectedId]);

  const toggle = useCallback((id: string) => {
    setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));
  }, []);

  const setExpandedOpen = useCallback((id: string, open: boolean) => {
    setExpanded((prev) =>
      prev[id] === open ? prev : { ...prev, [id]: open },
    );
  }, []);

  const focusItem = useCallback((id: string) => {
    setFocusedId(id);
    const el = treeRef.current?.querySelector<HTMLElement>(
      `[data-testid="nav-tree-item-${id}"]`,
    );
    el?.focus();
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent, node: NavTreeNode) => {
      if (!root) return;
      const result = resolveNavTreeKey(e.key, node, root, expanded);
      if (result.action === "none") {
        return;
      }
      e.preventDefault();
      if (result.action === "prevent") {
        return;
      }
      if (result.action === "select") {
        onSelect?.(node);
        setFocusedId(node.id);
        if (result.toggleExpand) toggle(node.id);
        return;
      }
      if (result.action === "expand") {
        setExpandedOpen(result.id, true);
        setFocusedId(result.id);
        return;
      }
      if (result.action === "collapse") {
        setExpandedOpen(result.id, false);
        setFocusedId(result.id);
        return;
      }
      if (result.action === "focus") {
        focusItem(result.id);
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
    const typeBadge = typeBadgeMeta(node.sectionType);
    // Roving tabindex follows last focused item (Tab lands there; others -1).
    const isTabStop =
      focusedId != null
        ? node.id === focusedId
        : selected ||
          (selectedId == null && root != null && node.id === root.id);

    return (
      <div key={node.id} data-testid={`nav-tree-node-${node.id}`}>
        <div
          role="treeitem"
          aria-level={depth + 1}
          aria-expanded={branch ? open : undefined}
          aria-selected={selected}
          tabIndex={isTabStop ? 0 : -1}
          className="perc-architecture-nav-tree-item"
          data-testid={`nav-tree-item-${node.id}`}
          data-section-type={node.sectionType}
          style={nodeRowStyle(selected, depth)}
          onClick={() => {
            setFocusedId(node.id);
            onSelect?.(node);
          }}
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
                data-i18n-key={typeBadge.i18nKey}
              >
                {typeBadge.label}
              </span>
            ) : null}
            {node.requiresLogin ? (
              <span
                style={badgeStyle}
                title={ARCH_MSG.SECURE_TITLE}
                aria-label={ARCH_MSG.SECURE_TITLE}
                data-i18n-key={ARCH_MSG_KEYS.SECURE_TITLE}
                data-i18n-badge-key={ARCH_MSG_KEYS.SECURE_BADGE}
                data-testid={`nav-tree-secure-${node.id}`}
              >
                {ARCH_MSG.SECURE_BADGE}
              </span>
            ) : null}
          </span>
        </div>
        {branch && open ? (
          <div role="group" data-testid={`nav-tree-group-${node.id}`}>
            {node.children.map((child) => renderNode(child, depth + 1))}
          </div>
        ) : null}
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
      <div
        style={emptyStyle}
        data-testid="architecture-nav-tree-empty"
        role="status"
      >
        <p
          style={{
            margin: "0 0 0.35rem",
            fontWeight: 600,
            color: "#1a202c",
          }}
          data-testid="architecture-nav-tree-empty-title"
        >
          {ARCH_MSG.TREE_EMPTY_TITLE}
        </p>
        <p
          style={{ margin: "0 0 0.5rem", color: catalogColors.muted }}
          data-testid="architecture-nav-tree-empty-body"
        >
          {ARCH_MSG.TREE_EMPTY}
        </p>
        <p
          style={{ margin: 0, fontSize: "0.9rem" }}
          data-testid="architecture-nav-tree-empty-hint"
        >
          {ARCH_MSG.TREE_EMPTY_HINT}
        </p>
      </div>
    );
  } else {
    body = (
      <div
        ref={treeRef}
        role="tree"
        aria-label={ARCH_MSG.TREE_PANEL_TITLE}
      >
        <style>{TREE_ITEM_FOCUS_CSS}</style>
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
