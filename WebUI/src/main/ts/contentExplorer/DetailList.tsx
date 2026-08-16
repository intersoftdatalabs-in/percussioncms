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
 * Detail list for the modern Content Explorer (US1 / T019).
 *
 * <p>Server-side pagination via {@link paginatedFolder} + client-side
 * windowed rendering for SC-005 large folders. The page size is fixed at
 * 50 to bound render cost; the user paginates forward/back via the
 * controls below the list.</p>
 *
 * <p>#3328: a dedicated type-icon column always precedes the display-format
 * columns. Folders (including {@code $System$} and user folders) show a
 * folder/open icon that activates browse. Multi-select checkboxes stay in
 * their own column and do not replace the folder affordance.</p>
 *
 * <p>FR-027 / T092b: when a `displayFormat` is supplied, the list honours
 * the column ordering + selection defined by the format. When absent
 * (default), the list falls back to Name + Type + Path. Supported column
 * ids are `name | type | path | title | category | modified | workflow` —
 * mapped to {@link PSPathItem} fields. Unknown ids are ignored. The
 * matcher is pure so a unit-test fixture exercises every supported id.
 * </p>
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { paginatedFolder } from "../api/contentExplorer/pathApi";
import type { PSPathItem, PSPagedResult } from "../api/contentExplorer/types";
import { MKD_LANG_IGNORE_ATTR } from "../i18n/mkdLangIgnore";

/**
 * Public display-format column contract. Maps to the live
 * `PSX_DISPLAYFORMATPROPERTY_VIEW` rows the modern list can render
 * today from {@link PSPathItem} (i.e. columns whose values are present on
 * the per-row item the `paginatedFolder` endpoint returns).
 *
 * The `modified` and `workflow` columns live on
 * {@link PSFolderProperties} rather than `PSPathItem`; they are noted
 * here for the future when the API exposes per-row modification dates
 * and workflow IDs. Until then, those column ids resolve to empty
 * strings (the test fixture asserts this behaviour).
 *
 * Adding a new column requires (1) adding the column id to this union,
 * (2) adding a renderer in {@link renderDisplayFormatCell}, (3) adding a
 * translation key in `EXPLORER_MSG`.
 */
export type DetailColumnId =
  | "name"
  | "type"
  | "path"
  | "title"
  | "category"
  | "modified"
  | "workflow";

/**
 * One row of the display format definition. The modern list renders
 * columns in the supplied order; if the host passes an empty array, the
 * default Name + Type + Path is used.
 */
export interface DetailDisplayFormat {
  columns: DetailColumnId[];
}

/**
 * Pure column-value resolver. Extracted as a top-level function so the
 * Vitest unit test (`DetailList.test.tsx`) can exercise every column id
 * + edge case (null fields, missing optional props) without rendering.
 */
export function resolveDisplayFormatColumns(
  columns: DetailColumnId[] | undefined | null,
): DetailColumnId[] {
  if (!columns || typeof (columns as Iterable<unknown>)[Symbol.iterator] !== "function") {
    return ["name", "type", "path"];
  }
  const allowed: DetailColumnId[] = [
    "name",
    "type",
    "path",
    "title",
    "category",
    "modified",
    "workflow",
  ];
  const dedup: DetailColumnId[] = [];
  for (const c of columns) {
    if (allowed.includes(c) && !dedup.includes(c)) {
      dedup.push(c);
    }
  }
  if (dedup.length === 0) {
    return ["name", "type", "path"];
  }
  return dedup;
}

/**
 * Pure per-cell renderer. Returns the cell text for the given column +
 * item. Pure so the unit test can drive it without rendering.
 *
 * Note on `modified` / `workflow`: PSPathItem does not carry these fields
 * yet (they live on PSFolderProperties). The renderer returns empty
 * strings for them today; when the API exposes per-row modification +
 * workflow the renderer is updated to consume them. The unit test
 * asserts the empty-string contract.
 */
export function renderDisplayFormatCell(
  column: DetailColumnId,
  item: PSPathItem,
): string {
  // Prefer pathmanagement displayProperties (columnData) when a display
  // format id was requested — keys are CX system field names.
  const fromProps = (keys: string[]): string => {
    const props = item.displayProperties;
    if (!props || typeof props !== "object") return "";
    for (const key of keys) {
      const v = props[key];
      if (v != null && String(v).length > 0) return String(v);
    }
    const lowerKeys = keys.map((k) => k.toLowerCase());
    for (const [k, v] of Object.entries(props)) {
      if (lowerKeys.includes(k.toLowerCase()) && v != null && String(v).length > 0) {
        return String(v);
      }
    }
    return "";
  };

  switch (column) {
    case "name":
      return (
        fromProps(["sys_title", "name", "Name"]) ||
        item.name ||
        item.path ||
        ""
      );
    case "type":
      return (
        fromProps(["sys_contenttypename", "sys_contenttype", "type", "Type"]) ||
        item.type ||
        item.category ||
        ""
      );
    case "path":
      return item.path ?? "";
    case "title":
      return (
        fromProps(["sys_title", "title", "Title"]) ||
        item.title ||
        item.name ||
        ""
      );
    case "category":
      return fromProps(["category", "Category"]) || item.category || "";
    case "modified":
      return (
        fromProps([
          "sys_contentlastmodifieddate",
          "sys_contentmodifieddate",
          "lastModified",
          "modified",
        ]) ||
        (item as unknown as { lastModified?: string }).lastModified ||
        ""
      );
    case "workflow":
      return (
        fromProps(["sys_workflow", "sys_workflowid", "workflow", "workflowId"]) ||
        (item as unknown as { workflowId?: string }).workflowId ||
        ""
      );
    default:
      return "";
  }
}

/** Translate a column id to the human-readable header text. */
export function columnHeaderLabel(
  column: DetailColumnId,
  messages: typeof EXPLORER_MSG,
): string {
  switch (column) {
    case "name":
      return message(messages.COL_NAME);
    case "type":
      return message(messages.COL_TYPE);
    case "path":
      return message(messages.COL_PATH);
    case "title":
      return message(messages.COL_TITLE);
    case "category":
      return message(messages.COL_CATEGORY);
    case "modified":
      return message(messages.COL_MODIFIED);
    case "workflow":
      return message(messages.COL_WORKFLOW);
    default:
      return column;
  }
}
import { message } from "../i18n/message";
import { isPreviewableItem } from "./previewItem";
import { canRead, isFolder, sameExplorerItemId } from "./selection";
import {
  emptyStateStyle,
  errorStateStyle,
  folderIconButtonStyle,
  iconTdCellStyle,
  iconThCellStyle,
  itemIconStyle,
  listStyle,
  rowStyle,
  tableStyle,
  tdCellStyle,
  thCellStyle,
  theadStyle,
} from "./styles";
import { EXPLORER_MSG } from "./messages";

const PAGE_SIZE = 50;

function FolderClosedGlyph(): React.ReactElement {
  return (
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" focusable="false">
      <path fill="currentColor" d="M1 4a1 1 0 0 1 1-1h4l1 1.5h7a1 1 0 0 1 1 1V13a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1z" />
    </svg>
  );
}

function FolderOpenGlyph(): React.ReactElement {
  return (
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" focusable="false">
      <path fill="currentColor" opacity="0.7" d="M1 4a1 1 0 0 1 1-1h4l1 1.5h5.5a1 1 0 0 1 1 1V7H2.2z" />
      <path fill="currentColor" d="M1 7h10.2a1 1 0 0 1 .95.7L14 14H2.5A1.5 1.5 0 0 1 1 12.5z" />
    </svg>
  );
}

function ItemGlyph(): React.ReactElement {
  return (
    <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" focusable="false">
      <path fill="currentColor" d="M4 1.5h5.2L13 5.3V14.5H4z" />
      <path fill="#fff" d="M9.2 1.7v3.6H12.8" />
    </svg>
  );
}

export interface DetailListProps {
  /** Folder path whose children to list. null disables the list. */
  folderPath: string | null;
  /** Currently selected item id (controlled). */
  selectedItemId: string | null;
  /** Fires when the user activates a row. */
  onSelectItem: (item: PSPathItem) => void;
  /**
   * Fires when the user double-clicks (or hits Enter on) a row — used by
   * {@link ContentExplorerShell} to drive open/preview in the editor.
   */
  onActivateItem?: (item: PSPathItem) => void;
  /**
   * Optional display-format definition. When supplied, the list renders
   * columns in the supplied order. When absent (default), the list falls
   * back to Name + Type + Path. The resolution + cell-render logic lives
   * in pure helpers so it can be unit-tested without rendering.
   */
  displayFormat?: DetailDisplayFormat;
  /**
   * Optional CX display format id passed to {@link paginatedFolder} so the
   * server fills {@link PSPathItem.displayProperties} (columnData).
   */
  displayFormatId?: string | null;
  /** Optional multi-select / context-menu affordance on row right-click. */
  onItemContextMenu?: (item: PSPathItem, clientX: number, clientY: number) => void;
  /**
   * Optional multi-select state (#2400 / #2408). When supplied, a leading
   * checkbox column is rendered and toggle events flow through
   * {@link onToggleSelectItem}. The primary single-click `onSelectItem`
   * is independent: a row can be both the focused row and a checked
   * row. Undefined (default) preserves the legacy single-select
   * rendering with no checkbox column.
   */
  selectedItemIds?: ReadonlySet<string>;
  /** Fires when the user toggles a row's checkbox. */
  onToggleSelectItem?: (item: PSPathItem, next: boolean) => void;
}

export function DetailList({
  folderPath,
  selectedItemId,
  onSelectItem,
  onActivateItem,
  displayFormat,
  displayFormatId,
  onItemContextMenu,
  selectedItemIds,
  onToggleSelectItem,
}: DetailListProps): React.ReactElement {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PSPagedResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const lastLoadedPath = useRef<string | null>(null);

  // Track the page-to-request in a ref so a folder change can reset
  // `page` to 0 *without* causing the effect to re-run with a stale
  // closure. (If `page` were in the deps list, the queued `setPage(0)`
  // on a folder switch would trigger a second effect run with a stale
  // captured `page`, doubling the API call.)
  const pageRef = useRef(page);
  pageRef.current = page;

  useEffect(() => {
    if (!folderPath) {
      setData(null);
      setPage(0);
      setError(null);
      lastLoadedPath.current = null;
      return;
    }
    const isNewFolder = folderPath !== lastLoadedPath.current;
    let effectivePage = page;
    if (isNewFolder) {
      // Schedule the visible page reset; do not depend on it in this run.
      setPage(0);
      lastLoadedPath.current = folderPath;
      effectivePage = 0;
    }
    const startIndex = effectivePage * PAGE_SIZE;
    let cancelled = false;
    setLoading(true);
    setError(null);
    const params: Parameters<typeof paginatedFolder>[1] = {
      startIndex,
      maxResults: PAGE_SIZE,
    };
    if (displayFormatId) {
      params.displayFormatId = displayFormatId;
    }
    paginatedFolder(folderPath, params)
      .then((res) => {
        if (cancelled) return;
        setData(res as PSPagedResult);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const msg = err instanceof Error ? err.message : String(err);
        setError(msg);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [folderPath, page, displayFormatId]);

  const children = useMemo<PSPathItem[]>(() => {
    if (!data) return [];
    return data.children;
  }, [data]);

  const totalCount = data?.totalCount;
  const totalPages = totalCount
    ? Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
    : null;

  const goPrev = useCallback(() => {
    setPage((p) => Math.max(0, p - 1));
  }, []);
  const goNext = useCallback(() => {
    setPage((p) => (totalPages == null ? p + 1 : Math.min(totalPages - 1, p + 1)));
  }, [totalPages]);

  const columnsToRender = useMemo<DetailColumnId[]>(
    () => resolveDisplayFormatColumns(displayFormat?.columns),
    [displayFormat?.columns],
  );

  const multiSelectEnabled = onToggleSelectItem != null;
  const multiSelected: ReadonlySet<string> = selectedItemIds ?? new Set();

  const visibleCheckedCount = useMemo(() => {
    if (!multiSelectEnabled) return 0;
    let n = 0;
    for (const child of children) {
      const id = child.id ?? child.path;
      if (id != null && multiSelected.has(id)) n += 1;
    }
    return n;
  }, [children, multiSelectEnabled, multiSelected]);
  const allVisibleChecked =
    multiSelectEnabled && children.length > 0 && visibleCheckedCount === children.length;
  const someVisibleChecked =
    multiSelectEnabled && visibleCheckedCount > 0 && !allVisibleChecked;

  if (!folderPath) {
    return (
      <div
        style={listStyle}
        data-testid="detail-list"
        data-folder-path={folderPath ?? ""}
      >
        <div style={emptyStateStyle} data-testid="detail-list-empty">
          {message(EXPLORER_MSG.LIST_EMPTY)}
        </div>
      </div>
    );
  }
  if (error) {
    return (
      <div
        style={listStyle}
        data-testid="detail-list"
        data-folder-path={folderPath ?? ""}
      >
        <div style={errorStateStyle} role="alert">
          {message(EXPLORER_MSG.LIST_LOAD_ERROR)}: {error}
        </div>
      </div>
    );
  }
  if (loading && children.length === 0) {
    return (
      <div
        style={listStyle}
        data-testid="detail-list"
        data-folder-path={folderPath ?? ""}
      >
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.LIST_LOADING)}</div>
      </div>
    );
  }
  if (!loading && children.length === 0) {
    return (
      <div
        style={listStyle}
        data-testid="detail-list"
        data-folder-path={folderPath ?? ""}
      >
        <div style={emptyStateStyle} data-testid="detail-list-empty">
          {message(EXPLORER_MSG.LIST_EMPTY)}
        </div>
      </div>
    );
  }

  return (
    <div
      style={listStyle}
      data-testid="detail-list"
      data-folder-path={folderPath ?? ""}
    >
      <table style={tableStyle}>
        <thead style={theadStyle}>
          <tr>
            {multiSelectEnabled ? (
              <th
                style={thCellStyle}
                data-testid="detail-col-header-select"
                aria-label={message(
                  allVisibleChecked
                    ? EXPLORER_MSG.SELECT_ALL_CLEAR_LABEL
                    : EXPLORER_MSG.SELECT_ALL_LABEL,
                )}
              >
                <input
                  type="checkbox"
                  data-testid="detail-select-all"
                  aria-label={message(
                    allVisibleChecked
                      ? EXPLORER_MSG.SELECT_ALL_CLEAR_LABEL
                      : EXPLORER_MSG.SELECT_ALL_LABEL,
                  )}
                  checked={allVisibleChecked}
                  ref={(el) => {
                    if (el) el.indeterminate = someVisibleChecked;
                  }}
                  onChange={(e) => {
                    const next = e.currentTarget.checked;
                    for (const child of children) {
                      const id = child.id ?? child.path;
                      if (id == null) continue;
                      const isOn = multiSelected.has(id);
                      if (isOn !== next) {
                        onToggleSelectItem!(child, next);
                      }
                    }
                  }}
                />
              </th>
            ) : null}
            <th
              style={iconThCellStyle}
              data-testid="detail-col-header-icon"
              aria-label={message(EXPLORER_MSG.ICON_COLUMN_LABEL)}
            />
            {columnsToRender.map((c) => (
              <th
                key={c}
                style={thCellStyle}
                data-testid={`detail-col-header-${c}`}
              >
                {columnHeaderLabel(c, EXPLORER_MSG)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody {...{ [MKD_LANG_IGNORE_ATTR]: "1" as const }}>
          {children.map((item) => {
            const selected = sameExplorerItemId(selectedItemId, item.id);
            const idKey = item.id ?? item.path;
            const isChecked = multiSelectEnabled && multiSelected.has(idKey);
            const visible = canRead(item);
            const folderish = isFolder(item);
            const folderOpen = folderish && selected;
            return (
              <tr
                key={idKey}
                data-testid={`detail-row-${idKey}`}
                data-row-kind={folderish ? "folder" : "item"}
                data-item-type={item.type ?? ""}
                data-previewable={isPreviewableItem(item) ? "true" : "false"}
                data-selected={selected || isChecked ? "true" : undefined}
                style={rowStyle(selected)}
                role="row"
                aria-selected={selected}
                aria-disabled={!visible}
                onClick={() => onSelectItem(item)}
                onDoubleClick={() => visible && onActivateItem?.(item)}
                onContextMenu={(e) => {
                  if (!visible || !onItemContextMenu) return;
                  e.preventDefault();
                  onSelectItem(item);
                  onItemContextMenu(item, e.clientX, e.clientY);
                }}
                onKeyDown={(e) => {
                  if (!visible) return;
                  if (e.key === "Enter") onActivateItem?.(item);
                }}
                tabIndex={visible ? 0 : -1}
              >
                {multiSelectEnabled ? (
                  <td style={tdCellStyle}>
                    <input
                      type="checkbox"
                      data-testid={`detail-select-${idKey}`}
                      aria-label={message(EXPLORER_MSG.SELECT_ROW_LABEL)}
                      checked={Boolean(isChecked)}
                      onChange={(e) => {
                        e.stopPropagation();
                        onToggleSelectItem!(item, e.currentTarget.checked);
                      }}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </td>
                ) : null}
                <td style={iconTdCellStyle} data-testid={`detail-cell-icon-${idKey}`}>
                  {folderish ? (
                    <button
                      type="button"
                      style={folderIconButtonStyle}
                      data-testid={`detail-folder-icon-${idKey}`}
                      data-kind="folder"
                      data-folder-state={folderOpen ? "open" : "closed"}
                      aria-label={message(EXPLORER_MSG.OPEN_FOLDER_LABEL)}
                      title={message(EXPLORER_MSG.OPEN_FOLDER_LABEL)}
                      disabled={!visible}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (visible) onActivateItem?.(item);
                      }}
                    >
                      {folderOpen ? <FolderOpenGlyph /> : <FolderClosedGlyph />}
                    </button>
                  ) : (
                    <span
                      style={itemIconStyle}
                      data-testid={`detail-item-icon-${idKey}`}
                      data-kind="item"
                      aria-hidden="true"
                      title={message(EXPLORER_MSG.ITEM_ICON_LABEL)}
                    >
                      <ItemGlyph />
                    </span>
                  )}
                </td>
                {columnsToRender.map((c) => (
                  <td
                    key={c}
                    style={tdCellStyle}
                    data-testid={`detail-cell-${c}-${idKey}`}
                    title={renderDisplayFormatCell(c, item)}
                  >
                    {renderDisplayFormatCell(c, item)}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
      <div
        style={{
          display: "flex",
          gap: 8,
          alignItems: "center",
          padding: "6px 10px",
          borderTop: "1px solid #eee",
          background: "#fafafa",
          fontSize: "0.8rem",
          color: "#555",
        }}
        data-testid="detail-pagination"
      >
        <button type="button" disabled={page === 0} onClick={goPrev}>
          ‹ Prev
        </button>
        <span>
          Page {page + 1}
          {totalPages != null ? ` of ${totalPages}` : ""}
        </span>
        <button
          type="button"
          disabled={totalPages != null && page + 1 >= totalPages}
          onClick={goNext}
        >
          Next ›
        </button>
        {totalCount != null && (
          <span style={{ marginLeft: "auto" }}>
            {totalCount} {totalCount === 1 ? "item" : "items"}
          </span>
        )}
      </div>
    </div>
  );
}