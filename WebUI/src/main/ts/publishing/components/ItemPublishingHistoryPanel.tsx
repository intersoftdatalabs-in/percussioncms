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

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { fetchItemPublishingHistory } from "../../api/publishing/itemHistoryApi";
import { formatApiError, isSessionRedirectError } from "../../api/client";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import {
  formatPublishedDate,
  itemHistoryShellHref,
  sortHistoryNewestFirst,
  type ItemPublishingHistory,
} from "../itemHistory";
import {
  emptyStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import { EmptyState } from "./EmptyState";

export interface ItemPublishingHistoryPanelProps {
  itemId?: string;
  currentSection: "status" | "logs";
  onItemIdChange?: (itemId: string) => void;
  onOpenSection?: (section: "status" | "logs") => void;
}

export function ItemPublishingHistoryPanel({
  itemId,
  currentSection,
  onItemIdChange,
  onOpenSection,
}: ItemPublishingHistoryPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [loadedId, setLoadedId] = useState("");
  const [rows, setRows] = useState<ItemPublishingHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lookedUp, setLookedUp] = useState(false);

  useEffect(() => {
    setInputId(itemId ?? "");
  }, [itemId]);

  const load = useCallback(async (rawId: string) => {
    const safe = mapIdParam(rawId);
    if (!safe) {
      setLookedUp(false);
      setRows([]);
      setLoadedId("");
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_ITEM_HISTORY_INVALID_ID)
          : message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID),
      );
      return;
    }
    setLoading(true);
    setError(null);
    setLookedUp(true);
    setLoadedId(safe);
    try {
      const list = await fetchItemPublishingHistory(safe);
      setRows(list);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setRows([]);
      setError(
        formatApiError(err, message(MSG.PUBLISH_ITEM_HISTORY_ERROR)),
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const initial = mapIdParam(itemId);
    if (initial) {
      void load(initial);
    }
  }, [itemId, load]);

  const sorted = useMemo(() => sortHistoryNewestFirst(rows), [rows]);

  function onSubmit(e: React.FormEvent): void {
    e.preventDefault();
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void load(next);
  }

  const otherSection = currentSection === "status" ? "logs" : "status";
  const otherHref = itemHistoryShellHref({
    section: otherSection,
    itemId: loadedId || mapIdParam(inputId),
  });
  const otherLabel =
    otherSection === "logs"
      ? message(MSG.PUBLISH_ITEM_HISTORY_OPEN_LOGS)
      : message(MSG.PUBLISH_ITEM_HISTORY_OPEN_STATUS);

  return (
    <section
      data-testid="item-publishing-history"
      aria-labelledby="item-publishing-history-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-publishing-history-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_ITEM_HISTORY)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_ITEM_HISTORY_HINT)}
      </p>
      <form onSubmit={onSubmit} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-history-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-history-id"
            name="itemId"
            value={inputId}
            onChange={(e) => setInputId(e.target.value)}
            data-testid="item-history-id"
            autoComplete="off"
          />
        </div>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-history-lookup"
        >
          {message(MSG.PUBLISH_ITEM_HISTORY_VIEW)}
        </button>
        <a
          href={otherHref}
          data-testid={`item-history-open-${otherSection}`}
          style={{ fontSize: "0.9rem" }}
          onClick={(e) => {
            if (onOpenSection) {
              e.preventDefault();
              const next = mapIdParam(inputId);
              if (next) {
                onItemIdChange?.(next);
              }
              onOpenSection(otherSection);
            }
          }}
        >
          {otherLabel}
        </a>
      </form>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="item-history-error"
        >
          {error}
        </p>
      )}
      {!loading && lookedUp && !error && sorted.length === 0 && (
        <EmptyState
          testId="item-history-empty"
          title={message(MSG.PUBLISH_ITEM_HISTORY_EMPTY)}
          nextAction={message(MSG.PUBLISH_ITEM_HISTORY_EMPTY_HINT)}
        />
      )}
      {!loading && !lookedUp && !error && (
        <p style={emptyStyle} data-testid="item-history-idle">
          {message(MSG.PUBLISH_ITEM_HISTORY_NEED_ID)}
        </p>
      )}
      {sorted.length > 0 && (
        <table
          style={tableStyle}
          data-testid="item-history-table"
        >
          <thead>
            <tr>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_SERVER)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_LOCATION)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_REVISION)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_DATE)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_OPERATION)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_ITEM_HISTORY_STATUS)}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((row, idx) => {
              const failed = String(row.status ?? "").toUpperCase() === "FAILURE";
              return (
                <tr
                  key={`${row.contentId ?? ""}-${row.revisionId ?? ""}-${idx}`}
                  data-testid="item-history-row"
                >
                  <td style={tdStyle}>{row.server ?? "—"}</td>
                  <td style={tdStyle}>{row.location ?? "—"}</td>
                  <td style={tdStyle}>{row.revisionId ?? "—"}</td>
                  <td style={tdStyle}>{formatPublishedDate(row.publishedDate)}</td>
                  <td style={tdStyle}>{row.operation ?? "—"}</td>
                  <td
                    style={tdStyle}
                    title={failed ? row.errorMessage : undefined}
                  >
                    {row.status ?? "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}
