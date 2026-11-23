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
 * Product Explorer Views catalog tree (#3116 / #3325, keyboard #7043).
 *
 * <p>System category <strong>Views</strong> with My / Community / All /
 * Other groups from {@code GET /services/views} grouped by
 * {@code parentCategory} 1–4. Selecting a group expands children only.
 * Selecting a leaf reports the {@link ViewDef} to the host (shell runs
 * V1 execute for standard views and Inbox C1 execute).</p>
 *
 * <p>Keyboard contract (same as the folder tree, #7035): the catalog is
 * a single Tab stop (roving tabindex); ArrowUp/ArrowDown move focus and
 * the selection together across the visible rows (clamped at the ends
 * per the WAI-ARIA APG tree pattern, skipping collapsed subtrees),
 * Home/End jump to the first/last row, and the focused row carries a
 * {@code :focus-visible} ring. Moving onto a leaf runs the host view
 * callback like activating it; moving onto a group or the Views root
 * moves focus only. Rows carry their tree level.</p>
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { formatApiError } from "../api/client";
import { listViews as listViewsApi } from "../api/contentExplorer/viewsApi";
import type { ViewDef } from "../api/developer/types";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import { message } from "../i18n/message";
import {
  isExplorerTreeMoveTarget,
  nextExplorerTreeRowIndex,
  treeRowMoveForKey,
} from "./ExplorerTree";
import { EXPLORER_MSG } from "./messages";
import {
  emptyStateStyle,
  errorStateStyle,
  inboxIconStyle,
  nodeLabelStyle,
  nodeRowStyle,
  toggleStyle,
  viewsTreeStyle,
} from "./styles";
import {
  PATH_MY_CONTENT_INBOX,
  VIEW_CATEGORY_MSG,
  VIEW_PARENT_CATEGORIES,
  groupViewsByParentCategory,
  isInboxView,
  type ViewCatalogGroups,
  type ViewParentCategory,
  viewKey,
  viewTreeLabel,
} from "./viewCatalog";

export interface ViewsCatalogTreeProps {
  /** Override catalog load (default {@link listViewsApi}). */
  listViews?: () => Promise<ViewDef[]>;
  /** Currently selected view key (name / id). */
  selectedViewKey?: string | null;
  /** Fires when the operator activates a view leaf. */
  onSelectView?: (view: ViewDef) => void;
  /** Fires when a category group row is activated (expand only in host). */
  onSelectGroup?: (category: ViewParentCategory) => void;
}

/**
 * Stable identity for one rendered catalog row (#7043, parent #4530).
 * Leaf keys carry the parent category so the same view key in two
 * groups (or a renamed group) never collides as a roving-tabindex
 * anchor or a focus/selection target.
 */
export const VIEWS_CATALOG_ROOT_KEY = "views:root";

export function viewsCatalogGroupKey(cat: ViewParentCategory): string {
  return `views:group:${cat}`;
}

export function viewsCatalogLeafKey(
  cat: ViewParentCategory,
  key: string,
): string {
  return `views:leaf:${cat}:${key}`;
}

/**
 * Normalized keys of the rows the catalog actually renders, in display
 * order: the Views root, then each category group row, then that group's
 * leaves while it is open (mirrors the render below, including the
 * synthesized Inbox leaf from {@code groupViewsByParentCategory}).
 * Collapsed subtrees contribute their group row only, so arrow moves
 * skip them. Pure so Vitest can drive it without rendering.
 */
export function collectVisibleViewsCatalogKeys(
  rootOpen: boolean,
  openGroups: Record<ViewParentCategory, boolean>,
  groups: ViewCatalogGroups,
): string[] {
  const out: string[] = [VIEWS_CATALOG_ROOT_KEY];
  if (!rootOpen) {
    return out;
  }
  for (const cat of VIEW_PARENT_CATEGORIES) {
    out.push(viewsCatalogGroupKey(cat));
    if (!openGroups[cat]) {
      continue;
    }
    for (const def of groups[cat] ?? []) {
      const key = viewKey(def);
      if (key) {
        out.push(viewsCatalogLeafKey(cat, key));
      }
    }
  }
  return out;
}

/**
 * Keyboard focus ring for catalog rows (#7043). Rows are focusable via
 * roving tabindex but inline styles cannot express
 * {@code :focus-visible}, so this class carries the ring. Plain
 * {@code :focus} is suppressed so mouse clicks do not leave a sticky
 * outline; keyboard operators still get the visible ring (same pattern
 * as the folder-tree rows, #7035).
 */
const VIEWS_CATALOG_ROW_FOCUS_CSS = `
.perc-explorer-views-catalog-row:focus {
  outline: none;
}
.perc-explorer-views-catalog-row:focus-visible {
  outline: 2px solid #1a73e8;
  outline-offset: -2px;
}
`;

type CatalogState =
  | { kind: "loading" }
  | { kind: "ready"; views: ViewDef[] }
  | { kind: "error"; message: string };

export function ViewsCatalogTree({
  listViews = listViewsApi,
  selectedViewKey = null,
  onSelectView,
  onSelectGroup,
}: ViewsCatalogTreeProps): React.ReactElement {
  const [state, setState] = useState<CatalogState>({ kind: "loading" });
  const [epoch, setEpoch] = useState(0);
  const [rootOpen, setRootOpen] = useState(true);
  const [openGroups, setOpenGroups] = useState<Record<ViewParentCategory, boolean>>(
    () => ({ 1: true, 2: false, 3: false, 4: false }),
  );
  const catalogRef = useRef<HTMLDivElement | null>(null);

  /**
   * Roving-tabindex anchor (#7043): the key of the last-interacted row.
   * Every in-catalog interaction (click, Enter/Space, arrow keys) funnels
   * through {@code setFocusKey} so the single tab stop follows the
   * operator; the host-provided selection (or the first visible row)
   * covers the pre-interaction render.
   */
  const [focusKey, setFocusKey] = useState<string | null>(null);

  /**
   * External-selection reset (#7043, same pattern as the #7045 folder-tree
   * review): {@code selectedViewKey} can change without catalog
   * interaction (host view runs, list double-click flows). Clearing the
   * anchor lets the tab stop resolve against the new selection instead
   * of staying on a stale row.
   */
  useEffect(() => {
    setFocusKey(null);
  }, [selectedViewKey]);

  useEffect(() => {
    let cancelled = false;
    setState({ kind: "loading" });
    listViews()
      .then((views) => {
        if (cancelled) return;
        setState({ kind: "ready", views: Array.isArray(views) ? views : [] });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setState({
          kind: "error",
          message: formatApiError(err, message(EXPLORER_MSG.VIEWS_LOAD_ERROR)),
        });
      });
    return () => {
      cancelled = true;
    };
  }, [listViews, epoch]);

  const toggleGroup = useCallback((cat: ViewParentCategory) => {
    setFocusKey(viewsCatalogGroupKey(cat));
    setOpenGroups((prev) => ({ ...prev, [cat]: !prev[cat] }));
    onSelectGroup?.(cat);
  }, [onSelectGroup]);

  /**
   * Catalog rows for the row-key indexes below. Empty until the load
   * resolves so every hook below runs unconditionally (hooks cannot sit
   * behind the loading/error early returns).
   */
  const readyViews = useMemo(
    () => (state.kind === "ready" ? state.views : []),
    [state],
  );
  const groups = useMemo(
    () => groupViewsByParentCategory(readyViews),
    [readyViews],
  );

  /**
   * Row-key indexes for this render (#7043): the visible order, the leaf
   * row key for each rendered definition, and the first rendered row key
   * per view key (a view key resolves to at most one visible leaf for the
   * selection branch of the tab stop below).
   */
  const { visibleKeys, leafDefByKey, leafKeyByViewKey } = useMemo(() => {
    const keys = collectVisibleViewsCatalogKeys(rootOpen, openGroups, groups);
    const defByKey = new Map<string, ViewDef>();
    const keyByViewKey = new Map<string, string>();
    for (const cat of VIEW_PARENT_CATEGORIES) {
      if (!rootOpen || !openGroups[cat]) {
        continue;
      }
      for (const def of groups[cat] ?? []) {
        const key = viewKey(def);
        if (!key) {
          continue;
        }
        const rowKey = viewsCatalogLeafKey(cat, key);
        defByKey.set(rowKey, def);
        if (!keyByViewKey.has(key)) {
          keyByViewKey.set(key, rowKey);
        }
      }
    }
    return { visibleKeys: keys, leafDefByKey: defByKey, leafKeyByViewKey: keyByViewKey };
  }, [groups, rootOpen, openGroups]);

  /**
   * Effective roving tab stop (#7043, same contract as #7035): resolved
   * against the rendered rows so the catalog always keeps exactly one
   * tab stop. The last-interacted row wins while it is still visible;
   * otherwise the host selection wins while visible; otherwise the first
   * visible row. A focusKey naming a collapsed group child no longer
   * strands every row at tabIndex=-1.
   */
  const tabStopKey = useMemo(() => {
    if (visibleKeys.length === 0) {
      return null;
    }
    if (focusKey && visibleKeys.includes(focusKey)) {
      return focusKey;
    }
    if (selectedViewKey) {
      const hit = leafKeyByViewKey.get(selectedViewKey);
      if (hit && visibleKeys.includes(hit)) {
        return hit;
      }
    }
    return visibleKeys[0] ?? null;
  }, [focusKey, selectedViewKey, leafKeyByViewKey, visibleKeys]);

  /**
   * Selection funnel for view leaves (#7043): keeps the roving tab stop
   * on the activated row and reports the selection to the host.
   */
  const handleSelectView = useCallback(
    (cat: ViewParentCategory, def: ViewDef) => {
      setFocusKey(viewsCatalogLeafKey(cat, viewKey(def)));
      onSelectView?.(def);
    },
    [onSelectView],
  );

  /**
   * Arrow-key row move at the catalog container (#7043, parent #4530):
   * DOM focus and selection move together across the visible rows
   * (clamped at the ends per the WAI-ARIA APG tree pattern, skipping
   * collapsed subtrees); Home/End jump to the first/last row. Moving
   * onto a leaf runs the host view callback, exactly like activating
   * that leaf; moving onto a group or the Views root moves focus only
   * (group expand/collapse stays on click, Enter/Space, ArrowLeft/
   * ArrowRight). Text-entry targets keep native behavior via
   * {@code isExplorerTreeMoveTarget}. Rows let these keys bubble here so
   * focus traversal is owned in exactly one place.
   */
  const onCatalogKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      const move = treeRowMoveForKey(e.key);
      if (!move || !isExplorerTreeMoveTarget(e.target)) {
        return;
      }
      const container = catalogRef.current;
      if (!container) {
        return;
      }
      const items = Array.from(container.querySelectorAll('[role="treeitem"]'));
      if (items.length === 0) {
        return;
      }
      const active = document.activeElement as HTMLElement | null;
      const activeItem = active?.closest?.('[role="treeitem"]');
      // No catalog row focused yet: ArrowDown/Home land on the first
      // row, ArrowUp/End on the last row.
      const current =
        activeItem && container.contains(activeItem)
          ? items.indexOf(activeItem)
          : move === "next" || move === "first"
            ? -1
            : items.length;
      const next = nextExplorerTreeRowIndex(items.length, current, move);
      const targetKey = visibleKeys[next];
      if (!targetKey) {
        return;
      }
      e.preventDefault();
      (items[next] as HTMLElement).focus();
      setFocusKey(targetKey);
      const def = leafDefByKey.get(targetKey);
      if (def) {
        onSelectView?.(def);
      }
    },
    [leafDefByKey, onSelectView, visibleKeys],
  );

  if (state.kind === "loading") {
    return (
      <div style={viewsTreeStyle} data-testid="explorer-views-tree">
        <div
          style={emptyStateStyle}
          role="status"
          aria-live="polite"
          data-testid="explorer-views-loading"
        >
          {message(EXPLORER_MSG.VIEWS_LOADING)}
        </div>
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div style={viewsTreeStyle} data-testid="explorer-views-tree">
        <div
          style={errorStateStyle}
          role="alert"
          data-testid="explorer-views-error"
        >
          {message(EXPLORER_MSG.VIEWS_LOAD_ERROR)}: {state.message}
        </div>
        <button
          type="button"
          data-testid="explorer-views-retry"
          onClick={() => setEpoch((n) => n + 1)}
        >
          {message(EXPLORER_MSG.RETRY)}
        </button>
      </div>
    );
  }

  return (
    <div
      style={viewsTreeStyle}
      role="tree"
      data-testid="explorer-views-tree"
      aria-label={message(EXPLORER_MSG.VIEWS_TREE_REGION)}
      ref={catalogRef}
      onKeyDown={onCatalogKeyDown}
    >
      <style>{VIEWS_CATALOG_ROW_FOCUS_CSS}</style>
      <div data-testid="explorer-views-root">
        <div
          role="treeitem"
          aria-expanded={rootOpen}
          aria-selected={false}
          aria-level={1}
          className="perc-explorer-views-catalog-row"
          tabIndex={tabStopKey === VIEWS_CATALOG_ROOT_KEY ? 0 : -1}
          style={nodeRowStyle(false, 0)}
          data-testid="explorer-views-root-row"
          onClick={() => {
            setFocusKey(VIEWS_CATALOG_ROOT_KEY);
            setRootOpen((v) => !v);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              setFocusKey(VIEWS_CATALOG_ROOT_KEY);
              setRootOpen((v) => !v);
            } else if (e.key === "ArrowRight") {
              e.preventDefault();
              setRootOpen(true);
            } else if (e.key === "ArrowLeft") {
              e.preventDefault();
              setRootOpen(false);
            }
          }}
        >
          <span style={toggleStyle} aria-hidden="true">
            {rootOpen ? "▾" : "▸"}
          </span>
          <span style={nodeLabelStyle}>{message(EXPLORER_MSG.VIEWS_CATEGORY)}</span>
        </div>
        {rootOpen &&
          VIEW_PARENT_CATEGORIES.map((cat) => {
            const open = openGroups[cat];
            const children = groups[cat];
            return (
              <div key={cat} data-testid={`explorer-views-group-${cat}`}>
                <div
                  role="treeitem"
                  aria-expanded={open}
                  aria-selected={false}
                  aria-level={2}
                  className="perc-explorer-views-catalog-row"
                  tabIndex={tabStopKey === viewsCatalogGroupKey(cat) ? 0 : -1}
                  style={nodeRowStyle(false, 1)}
                  data-testid={`explorer-views-group-${cat}-row`}
                  onClick={() => toggleGroup(cat)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      toggleGroup(cat);
                    } else if (e.key === "ArrowRight") {
                      e.preventDefault();
                      if (!open) toggleGroup(cat);
                    } else if (e.key === "ArrowLeft") {
                      e.preventDefault();
                      if (open) toggleGroup(cat);
                    }
                  }}
                >
                  <span style={toggleStyle} aria-hidden="true">
                    {open ? "▾" : "▸"}
                  </span>
                  <span style={nodeLabelStyle}>
                    {message(VIEW_CATEGORY_MSG[cat])}
                  </span>
                </div>
                {open &&
                  children.map((def) => {
                    const key = viewKey(def);
                    const selected = selectedViewKey === key;
                    const inbox = isInboxView(def);
                    const label = inbox
                      ? message(EXPLORER_MSG.VIEWS_INBOX)
                      : viewTreeLabel(def, children);
                    return (
                      <div
                        key={key}
                        role="treeitem"
                        aria-selected={selected}
                        aria-level={3}
                        className="perc-explorer-views-catalog-row"
                        tabIndex={tabStopKey === viewsCatalogLeafKey(cat, key) ? 0 : -1}
                        style={nodeRowStyle(selected, 2)}
                        data-testid={
                          inbox
                            ? "explorer-views-leaf-Inbox"
                            : `explorer-views-leaf-${key}`
                        }
                        data-inbox={inbox ? "true" : undefined}
                        data-cx-path={inbox ? PATH_MY_CONTENT_INBOX : undefined}
                        data-icon={inbox ? "Inbox" : undefined}
                        onClick={() => handleSelectView(cat, def)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            handleSelectView(cat, def);
                          }
                        }}
                      >
                        <span style={toggleStyle} aria-hidden="true">
                          {" "}
                        </span>
                        {inbox ? (
                          <span
                            style={inboxIconStyle}
                            data-testid="explorer-views-inbox-icon"
                            data-icon="Inbox"
                            aria-hidden="true"
                            title={message(EXPLORER_MSG.VIEWS_INBOX_ICON)}
                          >
                            {"✉"}
                          </span>
                        ) : null}
                        {inbox ? (
                          <span data-testid="explorer-views-inbox" hidden />
                        ) : null}
                        <span
                          style={nodeLabelStyle}
                          title={label}
                          {...(inbox
                            ? {}
                            : { [MKD_LANG_IGNORE_ATTR]: "1" as const })}
                        >
                          {label}
                        </span>
                      </div>
                    );
                  })}
                {open && children.length === 0 && (
                  <div
                    role="status"
                    data-testid={`explorer-views-group-${cat}-empty`}
                    style={nodeRowStyle(false, 2)}
                  >
                    <span
                      style={{ ...nodeLabelStyle, color: "#555", fontStyle: "italic" }}
                    >
                      {message(EXPLORER_MSG.VIEWS_GROUP_EMPTY)}
                    </span>
                  </div>
                )}
              </div>
            );
          })}
      </div>
    </div>
  );
}
