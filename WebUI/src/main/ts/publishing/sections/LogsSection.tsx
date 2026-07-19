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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useState } from "react";
import {
  fetchLogDetails,
  fetchPublishingLogs,
  purgePublishingLogs,
} from "../../api/publishing/statusApi";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  tableStyle,
  tdStyle,
  thStyle,
  toolbarStyle,
} from "../publishing.styles";
import type { PublishingLogEntry } from "../types";

/**
 * Confirm gate for purge — pure helper for tests and UI.
 */
export function canPurge(selectedIds: Array<string | number>): boolean {
  return selectedIds.length > 0;
}

export function LogsSection(): React.ReactElement {
  const [logs, setLogs] = useState<PublishingLogEntry[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [details, setDetails] = useState<unknown>(null);
  const [confirmPurge, setConfirmPurge] = useState(false);

  async function load(): Promise<void> {
    setLoading(true);
    setError(null);
    try {
      const list = await fetchPublishingLogs({ days: 7, maxcount: 100 });
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

  async function onDetails(jobId: string | number): Promise<void> {
    try {
      const data = await fetchLogDetails({ jobId });
      setDetails(data);
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    }
  }

  async function onPurgeConfirmed(): Promise<void> {
    if (!canPurge([...selected])) {
      return;
    }
    try {
      await purgePublishingLogs({ jobIds: [...selected] });
      setSelected(new Set());
      setConfirmPurge(false);
      await load();
    } catch {
      setError(message(MSG.PUBLISH_ERROR));
    }
  }

  return (
    <div data-testid="publish-section-logs">
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
                  <td style={tdStyle}>{log.status ?? "—"}</td>
                  <td style={tdStyle}>
                    {id && (
                      <button
                        type="button"
                        style={buttonStyle}
                        onClick={() => void onDetails(id)}
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
        <pre style={{ marginTop: 12, fontSize: "0.8rem", overflow: "auto" }}>
          {typeof details === "string"
            ? details
            : JSON.stringify(details, null, 2)}
        </pre>
      )}
    </div>
  );
}
