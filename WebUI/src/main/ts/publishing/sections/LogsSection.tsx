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

import React, { useEffect, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  fetchLogDetails,
  fetchPublishingLogs,
  purgePublishingLogs,
} from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import { LogDetailsPanel } from "../components/LogDetailsPanel";
import {
  buildLogRequest,
  DEFAULT_LOG_DAYS,
  DEFAULT_LOG_MAXCOUNT,
  LOG_DAYS_OPTIONS,
  LOG_MAXCOUNT_OPTIONS,
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
import type { PublishSiteSummary, PublishingLogEntry } from "../types";

/**
 * Confirm gate for purge — pure helper for tests and UI.
 */
export function canPurge(selectedIds: Array<string | number>): boolean {
  return selectedIds.length > 0;
}

export function LogsSection(): React.ReactElement {
  const [logs, setLogs] = useState<PublishingLogEntry[]>([]);
  const [sites, setSites] = useState<PublishSiteSummary[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [details, setDetails] = useState<unknown>(null);
  const [detailsJob, setDetailsJob] = useState<PublishingLogEntry | null>(null);
  const [confirmPurge, setConfirmPurge] = useState(false);

  const [siteId, setSiteId] = useState("");
  const [pubServerId, setPubServerId] = useState("");
  const [days, setDays] = useState(DEFAULT_LOG_DAYS);
  const [maxcount, setMaxcount] = useState(DEFAULT_LOG_MAXCOUNT);

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
      });
      const list = await fetchPublishingLogs(request);
      setLogs(list);
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    } finally {
      setLoading(false);
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
      </div>

      <div style={toolbarStyle}>
        <button type="button" style={buttonStyle} onClick={() => void load()}>
          {message(MSG.PUBLISH_SECTION_LOGS)}
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
      {!loading && logs.length === 0 && (
        <p style={emptyStyle}>{message(MSG.PUBLISH_EMPTY_LOGS)}</p>
      )}

      {logs.length > 0 && (
        <table style={tableStyle}>
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
            {logs.map((log) => {
              const id = String(log.jobId ?? "");
              return (
                <tr key={id || log.siteName}>
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
