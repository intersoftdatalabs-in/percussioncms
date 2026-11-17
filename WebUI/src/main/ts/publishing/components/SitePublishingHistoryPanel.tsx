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
import { isSessionRedirectError } from "../../api/client";
import { fetchPublishingLogs } from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import {
  DEFAULT_LOG_DAYS,
  DEFAULT_LOG_MAXCOUNT,
  filterLogEntries,
  LOG_DAYS_OPTIONS,
  LOG_MAXCOUNT_OPTIONS,
  buildLogRequest,
  type LogStatusFilter,
} from "../logsFilter";
import {
  buildPublishingLogsCsv,
  downloadPublishingLogsCsv,
} from "../logsExport";
import {
  describeSiteHistoryCount,
  siteHistoryExportFilename,
} from "../siteHistory";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import { EmptyState } from "./EmptyState";
import type { PublishingLogEntry } from "../types";

export interface SitePublishingHistoryPanelProps {
  /** Workspace site id — the audit scope is fixed to this site. */
  siteId: string | number;
  siteName?: string;
}

/**
 * Per-site publishing history audit view (#6473, parent #4531).
 * Loads `POST …/pubstatus/logs` scoped to the workspace site, narrows the
 * loaded rows with the same status/search controls as the Logs section,
 * and exports the visible rows to a per-site CSV file. Read-only: no
 * purge, no job actions — those stay on Logs / Status.
 */
export function SitePublishingHistoryPanel({
  siteId,
  siteName,
}: SitePublishingHistoryPanelProps): React.ReactElement {
  const [logs, setLogs] = useState<PublishingLogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exportError, setExportError] = useState<string | null>(null);
  const [days, setDays] = useState(DEFAULT_LOG_DAYS);
  const [maxcount, setMaxcount] = useState(DEFAULT_LOG_MAXCOUNT);
  const [showOnlyFailures, setShowOnlyFailures] = useState(false);
  const [statusFilter, setStatusFilter] = useState<LogStatusFilter>("all");
  const [query, setQuery] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await fetchPublishingLogs(
        buildLogRequest({
          siteId,
          days,
          maxcount,
          showOnlyFailures,
        }),
      );
      setLogs(Array.isArray(list) ? list : []);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setLogs([]);
      setError(message(MSG.PUBLISH_SITE_HISTORY_ERROR));
    } finally {
      setLoading(false);
    }
  }, [siteId, days, maxcount, showOnlyFailures]);

  useEffect(() => {
    void load();
  }, [load]);

  const visibleLogs = useMemo(
    () => filterLogEntries(logs, { query, status: statusFilter }),
    [logs, query, statusFilter],
  );

  function onExport(): void {
    setExportError(null);
    if (visibleLogs.length === 0) {
      setExportError(message(MSG.PUBLISH_SITE_HISTORY_EXPORT_EMPTY));
      return;
    }
    try {
      const csv = buildPublishingLogsCsv(visibleLogs);
      downloadPublishingLogsCsv(csv, siteHistoryExportFilename(siteName));
    } catch {
      setExportError(message(MSG.PUBLISH_SITE_HISTORY_EXPORT_FAILED));
    }
  }

  return (
    <div style={{ marginTop: 20 }} data-testid="publish-site-history">
      <h3 style={{ fontSize: "1rem" }}>
        {message(MSG.PUBLISH_SITE_HISTORY)}
      </h3>
      <p style={{ fontSize: "0.85rem", color: "#555" }}>
        {message(MSG.PUBLISH_SITE_HISTORY_HINT)}
      </p>
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 12,
          alignItems: "flex-end",
        }}
        data-testid="publish-site-history-filters"
      >
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 120 }}>
          <label htmlFor="site-history-days">Days</label>
          <select
            id="site-history-days"
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            data-testid="publish-site-history-days"
          >
            {LOG_DAYS_OPTIONS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 120 }}>
          <label htmlFor="site-history-max">Show</label>
          <select
            id="site-history-max"
            value={maxcount}
            onChange={(e) => setMaxcount(Number(e.target.value))}
            data-testid="publish-site-history-maxcount"
          >
            {LOG_MAXCOUNT_OPTIONS.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 140 }}>
          <label htmlFor="site-history-status">Status</label>
          <select
            id="site-history-status"
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as LogStatusFilter)
            }
            data-testid="publish-site-history-status"
          >
            <option value="all">All</option>
            <option value="failed">Failed</option>
            <option value="success">Success</option>
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 220 }}>
          <label htmlFor="site-history-query">Search</label>
          <input
            id="site-history-query"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Server, job, status"
            data-testid="publish-site-history-query"
          />
        </div>
        <label
          htmlFor="site-history-failures-only"
          style={{ ...formRowStyle, marginBottom: 0, maxWidth: 180 }}
        >
          <input
            id="site-history-failures-only"
            type="checkbox"
            checked={showOnlyFailures}
            onChange={(e) => setShowOnlyFailures(e.target.checked)}
            data-testid="publish-site-history-failures"
          />{" "}
          Failures only (server)
        </label>
      </div>

      <div style={toolbarStyle}>
        <button
          type="button"
          style={buttonStyle}
          onClick={() => void load()}
          data-testid="publish-site-history-refresh"
        >
          {message(MSG.PUBLISH_SITE_HISTORY_REFRESH)}
        </button>
        <button
          type="button"
          style={buttonStyle}
          onClick={onExport}
          data-testid="publish-site-history-export"
        >
          {message(MSG.PUBLISH_SITE_HISTORY_EXPORT)}
        </button>
      </div>

      <p
        style={{ fontSize: "0.85rem", color: "#555" }}
        data-testid="publish-site-history-count"
      >
        {describeSiteHistoryCount(visibleLogs.length, logs.length)}
      </p>

      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="publish-site-history-error">
          {error}
        </p>
      )}
      {exportError && (
        <p
          style={errorStyle}
          role="alert"
          data-testid="publish-site-history-export-error"
        >
          {exportError}
        </p>
      )}
      {!loading && !error && visibleLogs.length === 0 && logs.length === 0 && (
        <EmptyState
          testId="publish-site-history-empty"
          title={message(MSG.PUBLISH_SITE_HISTORY_EMPTY)}
          nextAction={message(MSG.PUBLISH_SITE_HISTORY_EMPTY_HINT)}
          actionLabel={message(MSG.PUBLISH_SITE_HISTORY_REFRESH)}
          onAction={() => void load()}
          actionTestId="publish-site-history-empty-reload"
        />
      )}
      {!loading && !error && visibleLogs.length === 0 && logs.length > 0 && (
        <EmptyState
          testId="publish-site-history-empty-filtered"
          title={message(MSG.PUBLISH_SITE_HISTORY_EMPTY_FILTERED)}
          nextAction={message(MSG.PUBLISH_SITE_HISTORY_EMPTY_FILTERED_HINT)}
        />
      )}

      {visibleLogs.length > 0 && (
        <table style={tableStyle} data-testid="publish-site-history-table">
          <thead>
            <tr>
              <th style={thStyle}>Job</th>
              <th style={thStyle}>{message(MSG.PUBLISH_SELECT_SERVER)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_SECTION_STATUS)}</th>
            </tr>
          </thead>
          <tbody>
            {visibleLogs.map((log, idx) => {
              const id = String(log.jobId ?? "");
              return (
                <tr
                  key={id ? `job-${id}` : `row-${idx}`}
                  data-testid="publish-site-history-row"
                >
                  <td style={tdStyle}>{id || "—"}</td>
                  <td style={tdStyle}>
                    {String(log.serverName ?? log.pubServerName ?? "—")}
                  </td>
                  <td style={tdStyle}>{log.status ?? "—"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
