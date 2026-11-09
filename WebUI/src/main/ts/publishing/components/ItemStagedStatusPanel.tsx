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
import { isSessionRedirectError } from "../../api/client";
import { message, MSG } from "../../i18n/message";
import { mapIdParam } from "../deepLinkMap";
import { formatPublishedDate } from "../itemHistory";
import {
  filterStagedStatusLines,
  stagedStatusErrorMessage,
  summarizeStagedStatus,
  type StagedItemLogLine,
  type StagedStatusSeverityFilter,
} from "../itemStagedStatus";
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

export interface ItemStagedStatusPanelProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
}

/**
 * Read-only publish-status/log readback for a staged item (#5838).
 * Only issues the read-only pubhistory GET — no stage, unstage, publish,
 * or edit call exists on any path of this panel. A failed fetch keeps the
 * previous view and shows the mapped error instead of clearing the table.
 */
export function ItemStagedStatusPanel({
  itemId,
  onItemIdChange,
}: ItemStagedStatusPanelProps): React.ReactElement {
  const [inputId, setInputId] = useState(itemId ?? "");
  const [loadedId, setLoadedId] = useState("");
  const [lines, setLines] = useState<StagedItemLogLine[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lookedUp, setLookedUp] = useState(false);
  const [severity, setSeverity] =
    useState<StagedStatusSeverityFilter>("all");

  useEffect(() => {
    setInputId(itemId ?? "");
  }, [itemId]);

  const load = useCallback(async (rawId: string) => {
    const safe = mapIdParam(rawId);
    if (!safe) {
      setLookedUp(false);
      setLines([]);
      setLoadedId("");
      setError(
        rawId.trim()
          ? message(MSG.PUBLISH_STAGED_STATUS_INVALID_ID)
          : message(MSG.PUBLISH_STAGED_STATUS_NEED_ID),
      );
      return;
    }
    setLoading(true);
    setError(null);
    setLookedUp(true);
    setLoadedId(safe);
    try {
      const list = await fetchItemPublishingHistory(safe);
      setLines(list);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      // Keep the previous view: do not clear lines on a failed fetch.
      setError(stagedStatusErrorMessage(err));
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

  const summary = useMemo(() => summarizeStagedStatus(lines), [lines]);
  // Client-side severity filter over the already-loaded lines (#5887):
  // narrowing never refetches and never writes.
  const visibleLines = useMemo(
    () => filterStagedStatusLines(lines, severity),
    [lines, severity],
  );

  function onSubmit(e: React.FormEvent): void {
    e.preventDefault();
    const next = mapIdParam(inputId) || inputId.trim();
    onItemIdChange?.(mapIdParam(next) || next);
    void load(next);
  }

  return (
    <section
      data-testid="item-staged-status"
      aria-labelledby="item-staged-status-title"
      style={{
        marginBottom: 20,
        padding: 12,
        border: "1px solid #ddd",
        borderRadius: 6,
        background: "#fafafa",
      }}
    >
      <h2
        id="item-staged-status-title"
        style={{ margin: "0 0 8px", fontSize: "1.05rem" }}
      >
        {message(MSG.PUBLISH_STAGED_STATUS)}
      </h2>
      <p style={{ margin: "0 0 12px", fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_STAGED_STATUS_HINT)}
      </p>
      <form onSubmit={onSubmit} style={toolbarStyle}>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 280 }}>
          <label htmlFor="item-staged-status-id">
            {message(MSG.PUBLISH_ITEM_HISTORY_ITEM_ID)}
          </label>
          <input
            id="item-staged-status-id"
            name="itemId"
            value={inputId}
            onChange={(e) => setInputId(e.target.value)}
            data-testid="item-staged-status-id"
            autoComplete="off"
          />
        </div>
        <button
          type="submit"
          style={primaryButtonStyle}
          data-testid="item-staged-status-lookup"
        >
          {message(MSG.PUBLISH_STAGED_STATUS_VIEW)}
        </button>
      </form>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="item-staged-status-error"
        >
          {error}
        </p>
      )}
      {!loading && lookedUp && !error && lines.length === 0 && (
        <EmptyState
          testId="item-staged-status-empty"
          title={message(MSG.PUBLISH_STAGED_STATUS_EMPTY)}
          nextAction={message(MSG.PUBLISH_STAGED_STATUS_EMPTY_HINT)}
        />
      )}
      {!loading && !lookedUp && !error && (
        <p style={emptyStyle} data-testid="item-staged-status-idle">
          {message(MSG.PUBLISH_STAGED_STATUS_NEED_ID)}
        </p>
      )}
      {lines.length > 0 && (
        <>
          <p
            style={{ margin: "12px 0 8px", fontSize: "0.85rem" }}
            data-testid="item-staged-status-summary"
          >
            {message(MSG.PUBLISH_STAGED_STATUS_SUMMARY)}:{" "}
            {summary.latestStatus ?? "—"}
            {summary.latestOperation ? ` (${summary.latestOperation})` : ""}
            {summary.latestServer ? ` · ${summary.latestServer}` : ""}
            {summary.latestLocation ? ` · ${summary.latestLocation}` : ""} ·{" "}
            {summary.totalCount} {message(MSG.PUBLISH_STAGED_STATUS_LOG_LINES)},{" "}
            {summary.failureCount}{" "}
            {message(MSG.PUBLISH_STAGED_STATUS_FAILURES)}
          </p>
          <h3 style={{ margin: "8px 0", fontSize: "0.95rem" }}>
            {message(MSG.PUBLISH_STAGED_STATUS_LOG_LINES)}
            {loadedId ? ` — ${loadedId}` : ""}
          </h3>
          <div style={{ ...formRowStyle, maxWidth: 240 }}>
            <label htmlFor="item-staged-status-severity">
              {message(MSG.PUBLISH_STAGED_STATUS_SEVERITY)}
            </label>
            <select
              id="item-staged-status-severity"
              value={severity}
              onChange={(e) =>
                setSeverity(e.target.value as StagedStatusSeverityFilter)
              }
              data-testid="item-staged-status-severity"
            >
              <option value="all">
                {message(MSG.PUBLISH_STAGED_STATUS_SEVERITY_ALL)}
              </option>
              <option value="failure">
                {message(MSG.PUBLISH_STAGED_STATUS_SEVERITY_FAILURE)}
              </option>
              <option value="success">
                {message(MSG.PUBLISH_STAGED_STATUS_SEVERITY_SUCCESS)}
              </option>
            </select>
          </div>
          {visibleLines.length === 0 && (
            <EmptyState
              testId="item-staged-status-filter-empty"
              title={message(MSG.PUBLISH_STAGED_STATUS_FILTER_EMPTY)}
              nextAction={message(MSG.PUBLISH_STAGED_STATUS_FILTER_EMPTY_HINT)}
            />
          )}
          {visibleLines.length > 0 && (
          <table
            style={tableStyle}
            data-testid="item-staged-status-table"
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
              {visibleLines.map((row, idx) => {
                const failed =
                  String(row.status ?? "").toUpperCase() === "FAILURE";
                return (
                  <tr
                    key={`${row.contentId ?? ""}-${row.revisionId ?? ""}-${idx}`}
                    data-testid="item-staged-status-row"
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
        </>
      )}
    </section>
  );
}
