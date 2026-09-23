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

import React, { useEffect, useMemo, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  fetchLogDetails,
  fetchPublishingLogs,
  purgePublishingLogs,
} from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { LogDetailsPanel } from "../components/LogDetailsPanel";
import {
  buildPublishingLogsCsv,
  downloadPublishingLogsCsv,
} from "../logsExport";
import {
  buildLogRequest,
  DEFAULT_LOG_DAYS,
  DEFAULT_LOG_MAXCOUNT,
  filterLogEntries,
  LOG_DAYS_OPTIONS,
  LOG_MAXCOUNT_OPTIONS,
  type LogStatusFilter,
} from "../logsFilter";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  formRowStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import { ItemPublishingHistoryPanel } from "../components/ItemPublishingHistoryPanel";
import type {
  PublishSection,
  PublishSiteSummary,
  PublishingLogEntry,
} from "../types";

/**
 * Confirm gate for purge — pure helper for tests and UI.
 */
export function canPurge(selectedIds: Array<string | number>): boolean {
  return selectedIds.length > 0;
}

export interface LogsSectionProps {
  itemId?: string;
  onItemIdChange?: (itemId: string) => void;
  onOpenSection?: (section: PublishSection) => void;
}

export function LogsSection({
  itemId,
  onItemIdChange,
  onOpenSection,
}: LogsSectionProps = {}): React.ReactElement {
  const [logs, setLogs] = useState<PublishingLogEntry[]>([]);
  const [sites, setSites] = useState<PublishSiteSummary[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [exportError, setExportError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [details, setDetails] = useState<unknown>(null);
  const [detailsJob, setDetailsJob] = useState<PublishingLogEntry | null>(null);
  const [confirmPurge, setConfirmPurge] = useState(false);

  const [siteId, setSiteId] = useState("");
  const [pubServerId, setPubServerId] = useState("");
  const [days, setDays] = useState(DEFAULT_LOG_DAYS);
  const [maxcount, setMaxcount] = useState(DEFAULT_LOG_MAXCOUNT);
  const [showOnlyFailures, setShowOnlyFailures] = useState(false);
  const [statusFilter, setStatusFilter] = useState<LogStatusFilter>("all");
  const [query, setQuery] = useState("");

  useEffect(() => {
    fetchSites()
      .then((list) =>
        setSites(
          list.map((s) => ({
            name: s.name,
            id: s.id,
            siteId: s.siteId ?? s.id,
          })),
        ),
      )
      .catch(() => setSites([]));
  }, []);

  async function load(): Promise<void> {
    setLoading(true);
    setError(null);
    setDetails(null);
    setDetailsJob(null);
    try {
      const request = buildLogRequest({
        siteId: siteId || undefined,
        pubServerId: pubServerId || undefined,
        days,
        maxcount,
        showOnlyFailures,
      });
      const list = await fetchPublishingLogs(request);
      setLogs(list);
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    } finally {
      setLoading(false);
    }
  }

  const visibleLogs = useMemo(
    () => filterLogEntries(logs, { query, status: statusFilter }),
    [logs, query, statusFilter],
  );

  function onExportFiltered(): void {
    setExportError(null);
    try {
      downloadPublishingLogsCsv(buildPublishingLogsCsv(visibleLogs));
    } catch {
      setExportError("Could not export the filtered publish logs.");
    }
  }

  function toggle(id: string): void {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  async function onDetails(log: PublishingLogEntry): Promise<void> {
    const jobId = log.jobId;
    if (jobId == null || jobId === "") {
      return;
    }
    try {
      const data = await fetchLogDetails(jobId);
      setDetails(data);
      setDetailsJob(log);
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    }
  }

  async function onPurgeConfirmed(): Promise<void> {
    if (!canPurge([...selected])) {
      return;
    }
    try {
      await purgePublishingLogs([...selected]);
      setSelected(new Set());
      setConfirmPurge(false);
      await load();
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    }
  }

  return (
    <div data-testid="publish-section-logs">
      <ItemPublishingHistoryPanel
        itemId={itemId}
        currentSection="logs"
        onItemIdChange={onItemIdChange}
        onOpenSection={onOpenSection}
      />
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: 12,
          marginBottom: 12,
          alignItems: "flex-end",
        }}
        data-testid="publish-logs-filters"
      >
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 200 }}>
          <label htmlFor="logs-site">Site id</label>
          <select
            id="logs-site"
            value={siteId}
            onChange={(e) => setSiteId(e.target.value)}
            data-testid="logs-filter-site"
          >
            <option value="">All</option>
            {sites.map((s) => {
              const id = String(s.siteId ?? s.id ?? s.name);
              return (
                <option key={id} value={id}>
                  {s.name}
                </option>
              );
            })}
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 160 }}>
          <label htmlFor="logs-server">Server id</label>
          <input
            id="logs-server"
            value={pubServerId}
            onChange={(e) => setPubServerId(e.target.value)}
            placeholder="All"
            data-testid="logs-filter-server"
          />
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 120 }}>
          <label htmlFor="logs-days">Days</label>
          <select
            id="logs-days"
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            data-testid="logs-filter-days"
          >
            {LOG_DAYS_OPTIONS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 120 }}>
          <label htmlFor="logs-max">Show</label>
          <select
            id="logs-max"
            value={maxcount}
            onChange={(e) => setMaxcount(Number(e.target.value))}
            data-testid="logs-filter-maxcount"
          >
            {LOG_MAXCOUNT_OPTIONS.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 140 }}>
          <label htmlFor="logs-status">Status</label>
          <select
            id="logs-status"
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as LogStatusFilter)
            }
            data-testid="logs-filter-status"
          >
            <option value="all">All</option>
            <option value="failed">Failed</option>
            <option value="success">Success</option>
          </select>
        </div>
        <div style={{ ...formRowStyle, marginBottom: 0, maxWidth: 220 }}>
          <label htmlFor="logs-query">Search</label>
          <input
            id="logs-query"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Site, server, job, status"
            data-testid="logs-filter-query"
          />
        </div>
        <label
          htmlFor="logs-failures-only"
          style={{ ...formRowStyle, marginBottom: 0, maxWidth: 180 }}
        >
          <input
            id="logs-failures-only"
            type="checkbox"
            checked={showOnlyFailures}
            onChange={(e) => setShowOnlyFailures(e.target.checked)}
            data-testid="logs-filter-failures"
          />{" "}
          Failures only (server)
        </label>
      </div>

      <div style={toolbarStyle}>
        <button
          type="button"
          style={buttonStyle}
          onClick={() => void load()}
          data-testid="logs-filter-apply"
        >
          {message(MSG.PUBLISH_SECTION_LOGS)}
        </button>
        <button
          type="button"
          style={buttonStyle}
          onClick={onExportFiltered}
          data-testid="logs-export-filtered"
        >
          Export
        </button>
        <button
          type="button"
          style={buttonStyle}
          disabled={!canPurge([...selected])}
          onClick={() => setConfirmPurge(true)}
        >
          {message("perc.ui.publish.title@Delete")}
        </button>
      </div>

      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {exportError && (
        <p style={errorStyle} role="alert" data-testid="publish-logs-export-error">
          {exportError}
        </p>
      )}
      {!loading && visibleLogs.length === 0 && (
        <p style={emptyStyle} data-testid="publish-logs-empty">
          {message(MSG.PUBLISH_EMPTY_LOGS)}
        </p>
      )}

      {visibleLogs.length > 0 && (
        <table style={tableStyle} data-testid="publish-logs-table">
          <thead>
            <tr>
              <th style={thStyle} />
              <th style={thStyle}>{message(MSG.PUBLISH_SECTION_SITES)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_SELECT_SERVER)}</th>
              <th style={thStyle}>{message(MSG.PUBLISH_SECTION_STATUS)}</th>
              <th style={thStyle} />
            </tr>
          </thead>
          <tbody>
            {visibleLogs.map((log) => {
              const id = String(log.jobId ?? "");
              return (
                <tr
                  key={id || log.siteName}
                  data-testid={id ? `publish-log-row-${id}` : "publish-log-row"}
                >
                  <td style={tdStyle}>
                    <input
                      type="checkbox"
                      checked={selected.has(id)}
                      onChange={() => toggle(id)}
                      aria-label={`select log ${id}`}
                    />
                  </td>
                  <td style={tdStyle}>{log.siteName ?? "—"}</td>
                  <td style={tdStyle}>
                    {String(log.serverName ?? log.pubServerName ?? "—")}
                  </td>
                  <td style={tdStyle}>{log.status ?? "—"}</td>
                  <td style={tdStyle}>
                    {id && (
                      <button
                        type="button"
                        style={buttonStyle}
                        onClick={() => void onDetails(log)}
                      >
                        details
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      {confirmPurge && (
        <div role="dialog" aria-modal="true" style={{ marginTop: 12 }}>
          <p>Confirm purge of {selected.size} log(s)?</p>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => void onPurgeConfirmed()}
          >
            Confirm
          </button>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => setConfirmPurge(false)}
          >
            Cancel
          </button>
        </div>
      )}

      {details != null && (
        <LogDetailsPanel
          details={details}
          jobSummary={{
            jobId: detailsJob?.jobId,
            siteName: detailsJob?.siteName,
            serverName: String(
              detailsJob?.serverName ?? detailsJob?.pubServerName ?? "",
            ),
            status: detailsJob?.status,
          }}
          onClose={() => {
            setDetails(null);
            setDetailsJob(null);
          }}
        />
      )}
    </div>
  );
}
