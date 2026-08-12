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
 * Product Explorer Views catalog tree (#3116).
 *
 * <p>System category <strong>Views</strong> with My / Community / All /
 * Other groups from {@code GET /services/views} grouped by
 * {@code parentCategory} 1–4. Selecting a group expands children only.
 * Selecting a leaf reports the {@link ViewDef} to the host (shell runs
 * V1 execute for standard views).</p>
 */

import React, { useCallback, useEffect, useState } from "react";
import { formatApiError } from "../api/client";
import { listViews as listViewsApi } from "../api/contentExplorer/viewsApi";
import type { ViewDef } from "../api/developer/types";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import {
  emptyStateStyle,
  errorStateStyle,
  nodeLabelStyle,
  nodeRowStyle,
  toggleStyle,
  viewsTreeStyle,
} from "./styles";
import {
  VIEW_CATEGORY_MSG,
  VIEW_PARENT_CATEGORIES,
  groupViewsByParentCategory,
  type ViewParentCategory,
  viewKey,
  viewLabel,
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
    setOpenGroups((prev) => ({ ...prev, [cat]: !prev[cat] }));
    onSelectGroup?.(cat);
  }, [onSelectGroup]);

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

  const groups = groupViewsByParentCategory(state.views);

  return (
    <div
      style={viewsTreeStyle}
      role="tree"
      data-testid="explorer-views-tree"
      aria-label={message(EXPLORER_MSG.VIEWS_TREE_REGION)}
    >
      <div data-testid="explorer-views-root">
        <div
          role="treeitem"
          aria-expanded={rootOpen}
          aria-selected={false}
          tabIndex={0}
          style={nodeRowStyle(false, 0)}
          data-testid="explorer-views-root-row"
          onClick={() => setRootOpen((v) => !v)}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
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
                  tabIndex={0}
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
                    const label = viewLabel(def);
                    return (
                      <div
                        key={key}
                        role="treeitem"
                        aria-selected={selected}
                        tabIndex={0}
                        style={nodeRowStyle(selected, 2)}
                        data-testid={`explorer-views-leaf-${key}`}
                        onClick={() => onSelectView?.(def)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            onSelectView?.(def);
                          }
                        }}
                      >
                        <span style={toggleStyle} aria-hidden="true">
                          {" "}
                        </span>
                        <span
                          style={nodeLabelStyle}
                          title={label}
                          {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}
                        >
                          {label}
                        </span>
                      </div>
                    );
                  })}
              </div>
            );
          })}
      </div>
    </div>
  );
}
