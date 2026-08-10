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
 * ContentBrowser — embeddable navigate / search / select dialog
 * (feature 992-react-content-explorer US2 + #2793 SearchPanel host).
 *
 * <p>Hosts call {@code window.PercModernUI.mount(el, "ContentBrowser", props)}
 * to render a dialog with a folder tree on the left, a list of items
 * (with pagination) on the right, and a {@link ReducedActions} action bar
 * for create-folder / rename / move / copy / delete when the host is in
 * select mode. On confirm, the component calls
 * {@code onConfirm(selectionResult)}; on cancel, {@code onCancel()}.</p>
 *
 * <p>When {@code enableSearch} is true, the shared {@link SearchPanel} is
 * mounted (same catalog + free-text + saved-search execute clients as the
 * product Explorer shell — no new REST). Open selects the hit for confirm;
 * Reveal navigates the tree/list to the result's parent folder.</p>
 *
 * <p>The component reuses {@code ExplorerTree} + {@code DetailList} +
 * {@code ReducedActions} + {@code SearchPanel} from the
 * {@code contentExplorer/} module. The selection filter is applied
 * client-side (defense in depth; the server is authoritative on AuthZ).</p>
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ExplorerTree } from "../contentExplorer/ExplorerTree";
import { DetailList } from "../contentExplorer/DetailList";
import { ReducedActions } from "../contentExplorer/ReducedActions";
import {
  defaultReducedActionHandlers,
  type ReducedActionHandlers,
} from "../contentExplorer/ReducedActions";
import { SearchPanel } from "../contentExplorer/SearchPanel";
import { message } from "../i18n/message";
import { canRead } from "../contentExplorer/selection";
import type {
  PSItemProperties,
  PSPathItem,
  SelectionItem,
  SelectionResult,
} from "../api/contentExplorer/types";
import type { ContentBrowserProps } from "./types";
import {
  appendUniqueById,
  selectionItemFromSearchResult,
} from "./selectionHelpers";
import {
  errorStateStyle,
  shellStyle,
  actionButtonStyle,
} from "../contentExplorer/styles";

/** CSS-only styling for the dialog chrome (re-uses the explorer shell layout). */
const dialogStyleBase: React.CSSProperties = {
  ...shellStyle,
  border: "1px solid #999",
  minWidth: 720,
  minHeight: 360,
  background: "#fff",
  position: "relative",
  padding: 0,
};

const searchPanelHostStyle: React.CSSProperties = {
  gridArea: "search",
  borderTop: "1px solid #ddd",
  padding: 8,
  background: "#fcfcfc",
  maxHeight: "36vh",
  overflow: "auto",
};

const headerStyle: React.CSSProperties = {
  gridArea: "header",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  gap: 12,
  padding: "10px 14px",
  borderBottom: "1px solid #ddd",
  background: "#f7f7f7",
};

const footerStyle: React.CSSProperties = {
  gridArea: "footer",
  display: "flex",
  alignItems: "center",
  justifyContent: "flex-end",
  gap: 8,
  padding: "10px 14px",
  borderTop: "1px solid #ddd",
  background: "#fafafa",
};

const titleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: "1rem",
  fontWeight: 600,
};

/** US2 (T042) TMX keys — fallback to the key when I18N is unavailable. */
const BROWSER_MSG = {
  TITLE: "perc.ui.contentBrowser@Content Browser",
  CONFIRM: "perc.ui.contentBrowser@Confirm",
  CANCEL: "perc.ui.contentBrowser@Cancel",
  EMPTY: "perc.ui.contentBrowser@No items selected",
  TYPE_MISMATCH: "perc.ui.contentBrowser@Selected item type is not allowed",
  CATEGORY_MISMATCH: "perc.ui.contentBrowser@Selected item category is not allowed",
  SEARCH_REGION: "perc.ui.contentBrowser@Search",
};

function toSelectionItem(item: PSPathItem): SelectionItem {
  return {
    id: item.id ?? item.path,
    path: item.path,
    name: item.name ?? item.path,
    type: item.type,
    category: item.category,
    contentTypeIds: undefined,
  };
}

function passesFilters(
  item: PSPathItem,
  allowedTypes: ReadonlyArray<string> | null,
  allowedCategories: ReadonlyArray<string> | null,
): boolean {
  if (allowedTypes && allowedTypes.length > 0) {
    const t = (item.type ?? item.category ?? "").toLowerCase();
    if (!allowedTypes.some((x) => x.toLowerCase() === t)) {
      return false;
    }
  }
  if (allowedCategories && allowedCategories.length > 0) {
    const c = (item.category ?? item.type ?? "").toLowerCase();
    if (!allowedCategories.some((x) => x.toLowerCase() === c)) {
      return false;
    }
  }
  return true;
}

export const ContentBrowser: React.FC<ContentBrowserProps> = (props) => {
  const {
    mode = "select",
    multiSelect = false,
    allowFolderSelect = true,
    allowItemSelect = true,
    allowedTypes = null,
    allowedCategories = null,
    initialPath = null,
    roots: _roots = "all",
    enableSearch = false,
    search: searchTransport,
    listSavedSearches,
    executeSavedSearch,
    enablePreview = true,
    previewTemplate: _previewTemplate = null,
    title = null,
    onConfirm,
    onPreviewChange: _onPreviewChange,
    onCancel,
    onError,
  } = props;

  const [folderPath, setFolderPath] = useState<string | null>(initialPath);
  const [selected, setSelected] = useState<SelectionItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const titleText = title ?? message(BROWSER_MSG.TITLE);

  const dialogStyle = useMemo<React.CSSProperties>(
    () => ({
      ...dialogStyleBase,
      gridTemplateRows: enableSearch
        ? "auto 1fr auto auto"
        : "auto 1fr auto",
      gridTemplateAreas: enableSearch
        ? '"header header" "tree list" "search search" "footer footer"'
        : '"header header" "tree list" "footer footer"',
    }),
    [enableSearch],
  );

  // Select mode helpers — single vs multi. Multi is a controlled list.
  const isSelected = useCallback(
    (id: string | undefined) => {
      if (!id) return false;
      return selected.some((s) => s.id === id);
    },
    [selected],
  );

  const toggleSelect = useCallback(
    (item: PSPathItem) => {
      if (mode !== "select") return;
      // Apply filters client-side; reject the toggle if it doesn't match.
      if (!passesFilters(item, allowedTypes, allowedCategories)) {
        setError(message(BROWSER_MSG.TYPE_MISMATCH));
        return;
      }
      if (!allowFolderSelect && item.type === "folder") return;
      if (!allowItemSelect && item.type !== "folder") return;
      const sel = toSelectionItem(item);
      setSelected((prev) => {
        if (multiSelect) {
          return isSelected(sel.id)
            ? prev.filter((s) => s.id !== sel.id)
            : [...prev, sel];
        }
        return [sel];
      });
    },
    [
      mode,
      allowedTypes,
      allowedCategories,
      allowFolderSelect,
      allowItemSelect,
      multiSelect,
      isSelected,
    ],
  );

  const handleConfirm = useCallback(async () => {
    if (mode !== "select" || !onConfirm) return;
    // Confirm is disabled when selection is empty (button disabled via
    // `selected.length === 0`), so this guard is a defense-in-depth that
    // also applies to programmatic triggers (e.g. Enter key on a focused
    // confirm button) — never invoke onConfirm with an empty items
    // array, regardless of multiSelect.
    if (selected.length === 0) return;
    setBusy(true);
    try {
      const result: SelectionResult = { items: selected };
      await onConfirm(result);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setError(msg);
      onError?.(msg);
    } finally {
      setBusy(false);
    }
  }, [mode, multiSelect, onConfirm, onError, selected]);

  const handleCancel = useCallback(() => {
    onCancel?.();
  }, [onCancel]);

  // Reusable reduced-action handlers with a per-shell error surface.
  const actionHandlers = useMemo<ReducedActionHandlers>(
    () => ({
      ...defaultReducedActionHandlers(),
      onOpen: (item) => {
        if (item.type === "folder" || (item.leaf === false && item.id == null)) {
          setFolderPath(item.path);
        }
      },
      onPreview: () => undefined,
    }),
    [],
  );

  // Sync folder selection from the ExplorerTree. The tree fires onSelectFolder
  // when the user clicks a treeitem; we mirror that to the list via folderPath.
  const handleTreeSelect = useCallback(
    (path: string, _folder: PSPathItem | null) => {
      setFolderPath(path);
      setError(null);
    },
    [],
  );

  // Custom list row-click → toggle selection (vs the explorer's onSelectItem which just highlights).
  const handleListSelect = useCallback(
    (item: PSPathItem) => {
      // Even in browse mode, allow clicking to navigate into a folder.
      if (item.type === "folder" || item.leaf === false) {
        setFolderPath(item.path);
        return;
      }
      // Single-click in select mode toggles selection.
      if (mode === "select") {
        toggleSelect(item);
      }
    },
    [mode, toggleSelect],
  );

  const handleListActivate = useCallback(
    (item: PSPathItem) => {
      if (mode !== "select") return;
      if (!passesFilters(item, allowedTypes, allowedCategories)) {
        setError(message(BROWSER_MSG.TYPE_MISMATCH));
        return;
      }
      const sel = toSelectionItem(item);
      if (multiSelect) {
        // Add to selection but skip if already present (no duplicates from
        // repeated double-click / Enter on the same item).
        setSelected((prev) => appendUniqueById(prev, sel));
      } else {
        // Single-select: replace selection. Activate does NOT auto-confirm;
        // the host calls onConfirm explicitly via the Confirm button (or
        // its own double-click handler). Activate in single-select mode is
        // equivalent to a high-visibility selection change.
        setSelected([sel]);
      }
    },
    [mode, multiSelect, allowedTypes, allowedCategories],
  );

  // Selection error feedback (auto-clear after a tick so the message is visible briefly).
  useEffect(() => {
    if (!error) return;
    const t = window.setTimeout(() => setError(null), 4000);
    return () => window.clearTimeout(t);
  }, [error]);

  /**
   * SearchPanel "Open": map the hit into selection (select mode) so Confirm
   * can hand the host a SelectionResult. Filters still apply.
   */
  const handleSearchOpen = useCallback(
    (result: PSItemProperties) => {
      const sel = selectionItemFromSearchResult(result);
      // Synthesize a minimal PSPathItem for the same client-side filters.
      const probe: PSPathItem = {
        id: sel.id,
        path: sel.path,
        name: sel.name ?? sel.path,
        type: sel.type,
        category: sel.category,
      };
      if (!passesFilters(probe, allowedTypes, allowedCategories)) {
        setError(message(BROWSER_MSG.TYPE_MISMATCH));
        return;
      }
      if (!allowFolderSelect && (sel.type === "folder" || sel.category === "folder")) {
        return;
      }
      if (!allowItemSelect && sel.type !== "folder" && sel.category !== "folder") {
        return;
      }
      if (mode !== "select") {
        // Browse hosts: navigate to parent when known.
        if (result.folderPath) {
          setFolderPath(result.folderPath);
        }
        return;
      }
      setSelected((prev) => {
        if (multiSelect) {
          return appendUniqueById(prev, sel);
        }
        return [sel];
      });
      setError(null);
    },
    [
      mode,
      multiSelect,
      allowedTypes,
      allowedCategories,
      allowFolderSelect,
      allowItemSelect,
    ],
  );

  /** SearchPanel "Reveal in folder": drive tree/list to the parent path. */
  const handleSearchReveal = useCallback((result: PSItemProperties) => {
    // Always clear selection/filter error on Reveal (matches handleTreeSelect),
    // even when the hit has no navigable folderPath — avoids a stale alert.
    setError(null);
    const folder = result.folderPath?.trim();
    if (folder) {
      setFolderPath(folder);
    }
  }, []);

  // Render the row-level checkbox column when multi-select + select mode.
  const isSelectMode = mode === "select";

  return (
    <div
      style={dialogStyle}
      role="dialog"
      aria-label={titleText}
      data-component="ContentBrowser"
      data-feature="992-react-content-explorer"
      data-mode={mode}
      data-enable-search={enableSearch ? "true" : "false"}
      data-testid="content-browser"
    >
      <header style={headerStyle}>
        <h2 style={titleStyle}>{titleText}</h2>
        {isSelectMode && (
          <ReducedActions
            item={selected.length === 1 ? (selected[0] as unknown as PSPathItem) : null}
            folder={
              folderPath
                ? ({
                    id: undefined,
                    path: folderPath,
                    name: folderPath,
                    type: "folder",
                    accessLevel: "WRITE",
                  } as PSPathItem)
                : null
            }
            handlers={actionHandlers}
            hasPreviewHandler={enablePreview}
            onError={(msg) => {
              setError(msg);
              onError?.(msg);
            }}
          />
        )}
      </header>

      {error && (
        <div
          style={{ ...errorStateStyle, gridColumn: "1 / -1" }}
          role="alert"
          data-testid="content-browser-error"
        >
          {error}
        </div>
      )}

      <ExplorerTree
        initialPath={initialPath ?? "/"}
        selectedPath={folderPath}
        onSelectFolder={handleTreeSelect}
        onActivate={(path) => setFolderPath(path)}
      />

      <DetailList
        folderPath={folderPath}
        selectedItemId={selected[0]?.id ?? null}
        onSelectItem={handleListSelect}
        onActivateItem={handleListActivate}
      />

      {enableSearch && (
        <section
          style={searchPanelHostStyle}
          data-testid="content-browser-search-panel"
          aria-label={message(BROWSER_MSG.SEARCH_REGION)}
        >
          <SearchPanel
            search={searchTransport}
            listSavedSearches={listSavedSearches}
            executeSavedSearch={executeSavedSearch}
            onOpen={handleSearchOpen}
            onReveal={handleSearchReveal}
            initialCriteria={
              folderPath ? { folderPath } : undefined
            }
          />
        </section>
      )}

      <footer style={footerStyle} data-testid="content-browser-footer">
        {isSelectMode && (
          <span
            style={{ marginRight: "auto", fontSize: "0.85rem", color: "#555" }}
            data-testid="content-browser-selection-summary"
          >
            {selected.length === 0
              ? message(BROWSER_MSG.EMPTY)
              : `${selected.length} selected`}
          </span>
        )}
        <button
          type="button"
          style={actionButtonStyle(false)}
          onClick={handleCancel}
          data-testid="content-browser-cancel"
        >
          {message(BROWSER_MSG.CANCEL)}
        </button>
        {isSelectMode && (
          <button
            type="button"
            style={actionButtonStyle(selected.length === 0 || busy)}
            disabled={selected.length === 0 || busy}
            onClick={handleConfirm}
            data-testid="content-browser-confirm"
          >
            {message(BROWSER_MSG.CONFIRM)}
          </button>
        )}
      </footer>
    </div>
  );
};

// Re-export the canRead helper for tests that want to assert permission filtering.
export { canRead };

export default ContentBrowser;
