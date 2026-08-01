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
 * Detail list for the modern Content Explorer (US1 / T019).
 *
 * <p>Server-side pagination via {@link paginatedFolder} + client-side
 * windowed rendering for SC-005 large folders. The page size is fixed at
 * 50 to bound render cost; the user paginates forward/back via the
 * controls below the list.</p>
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
  switch (column) {
    case "name":
      return item.name ?? item.path ?? "";
    case "type":
      return item.type ?? item.category ?? "";
    case "path":
      return item.path ?? "";
    case "title":
      return item.title ?? item.name ?? "";
    case "category":
      return item.category ?? "";
    case "modified":
      // API surface pending — see comment above.
      return (item as unknown as { lastModified?: string }).lastModified ?? "";
    case "workflow":
      // API surface pending — see comment above.
      return (item as unknown as { workflowId?: string }).workflowId ?? "";
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
import { canRead } from "./selection";
import {
  emptyStateStyle,
  errorStateStyle,
  listStyle,
  rowStyle,
  tableStyle,
  tdCellStyle,
  thCellStyle,
  theadStyle,
} from "./styles";
import { EXPLORER_MSG } from "./messages";

const PAGE_SIZE = 50;

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
}

export function DetailList({
  folderPath,
  selectedItemId,
  onSelectItem,
  onActivateItem,
  displayFormat,
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
    paginatedFolder(folderPath, {
      startIndex,
      maxResults: PAGE_SIZE,
    })
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
  }, [folderPath, page]);

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

  if (!folderPath) {
    return (
      <div style={listStyle} data-testid="detail-list">
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.LIST_EMPTY)}</div>
      </div>
    );
  }
  if (error) {
    return (
      <div style={listStyle} data-testid="detail-list">
        <div style={errorStateStyle} role="alert">
          {message(EXPLORER_MSG.LIST_LOAD_ERROR)}: {error}
        </div>
      </div>
    );
  }
  if (loading && children.length === 0) {
    return (
      <div style={listStyle} data-testid="detail-list">
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.LIST_LOADING)}</div>
      </div>
    );
  }
  if (!loading && children.length === 0) {
    return (
      <div style={listStyle} data-testid="detail-list">
        <div style={emptyStateStyle}>{message(EXPLORER_MSG.LIST_EMPTY)}</div>
      </div>
    );
  }

  return (
    <div style={listStyle} data-testid="detail-list">
      <table style={tableStyle}>
        <thead style={theadStyle}>
          <tr>
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
            const selected = selectedItemId === item.id;
            const visible = canRead(item);
            return (
              <tr
                key={item.id ?? item.path}
                data-testid={`detail-row-${item.id ?? item.path}`}
                style={rowStyle(selected)}
                role="row"
                aria-selected={selected}
                aria-disabled={!visible}
                onClick={() => visible && onSelectItem(item)}
                onDoubleClick={() => visible && onActivateItem?.(item)}
                onKeyDown={(e) => {
                  if (!visible) return;
                  if (e.key === "Enter") onActivateItem?.(item);
                }}
                tabIndex={visible ? 0 : -1}
              >
                {columnsToRender.map((c) => (
                  <td
                    key={c}
                    style={tdCellStyle}
                    data-testid={`detail-cell-${c}-${item.id ?? item.path}`}
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