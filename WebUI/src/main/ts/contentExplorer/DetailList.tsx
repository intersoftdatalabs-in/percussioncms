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
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { paginatedFolder } from "../api/contentExplorer/pathApi";
import type { PSPathItem, PSPagedResult } from "../api/contentExplorer/types";
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
}

export function DetailList({
  folderPath,
  selectedItemId,
  onSelectItem,
  onActivateItem,
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
            <th style={thCellStyle}>{message(EXPLORER_MSG.COL_NAME)}</th>
            <th style={thCellStyle}>{message(EXPLORER_MSG.COL_TYPE)}</th>
            <th style={thCellStyle}>{message(EXPLORER_MSG.COL_PATH)}</th>
          </tr>
        </thead>
        <tbody>
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
                <td style={tdCellStyle} title={item.name ?? item.path}>
                  {item.name ?? item.path}
                </td>
                <td style={tdCellStyle}>{item.type ?? item.category ?? ""}</td>
                <td style={tdCellStyle} title={item.path}>
                  {item.path}
                </td>
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