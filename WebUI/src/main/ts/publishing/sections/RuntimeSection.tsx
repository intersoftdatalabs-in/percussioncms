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

import React, { useCallback, useEffect, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  clearSiteItems,
  demandPublish,
  listRuntimeEditions,
  purgeRuntimeJobLog,
  startEditionJob,
  stopRuntimeJob,
  type RuntimeEditionStatus,
  type RuntimeJobResponse,
} from "../../api/publishing/runtimeApi";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  formRowStyle,
  listItemStyle,
  listStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";

/** Pure helper for tests: whether stop is available for a row. */
export function canStopEdition(row: RuntimeEditionStatus): boolean {
  return (row.runningJobId ?? 0) > 0;
}

/** Pure helper: parse demand content ids from a comma/space-separated string. */
export function parseContentIds(raw: string): string[] {
  return raw
    .split(/[\s,;]+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

/**
 * Runtime / Editions: start & stop edition jobs, demand publish, site clear,
 * advanced log purge by job id.
 */
export function RuntimeSection(): React.ReactElement {
  const [sites, setSites] = useState<Array<{ name: string; id: string }>>([]);
  const [siteId, setSiteId] = useState("");
  const [editions, setEditions] = useState<RuntimeEditionStatus[]>([]);
  const [selectedEdition, setSelectedEdition] = useState("");
  const [demandIds, setDemandIds] = useState("");
  const [purgeJobId, setPurgeJobId] = useState("");
  const [lastResult, setLastResult] = useState<RuntimeJobResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    fetchSites()
      .then((list) => {
        const mapped = list.map((s) => ({
          name: s.name,
          id: String(s.siteId ?? s.id ?? s.name),
        }));
        setSites(mapped);
        if (mapped.length > 0) {
          setSiteId(mapped[0].id);
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)));
  }, []);

  const reload = useCallback(() => {
    if (!siteId) {
      return;
    }
    setLoading(true);
    setError(null);
    listRuntimeEditions(siteId)
      .then((list) => {
        setEditions(list);
        if (list.length > 0 && !selectedEdition) {
          setSelectedEdition(String(list[0].editionId ?? ""));
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, [siteId, selectedEdition]);

  useEffect(() => {
    reload();
  }, [siteId]);

  async function onStart(editionId: string): Promise<void> {
    setBusy(true);
    setError(null);
    try {
      const res = await startEditionJob(editionId);
      setLastResult(res);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function onStop(jobId: number): Promise<void> {
    setBusy(true);
    setError(null);
    try {
      const res = await stopRuntimeJob(jobId);
      setLastResult(res);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function onDemand(): Promise<void> {
    const ids = parseContentIds(demandIds);
    if (!selectedEdition || ids.length === 0) {
      setError("Select an edition and enter at least one content id");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await demandPublish(selectedEdition, { contentIds: ids });
      setLastResult(res);
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function onClearSite(): Promise<void> {
    if (!siteId) {
      return;
    }
    if (
      !window.confirm(
        "Clear published site record for this site? This cannot be undone.",
      )
    ) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await clearSiteItems(siteId);
      setLastResult({ status: "site cleared" });
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function onPurgeLog(): Promise<void> {
    if (!purgeJobId.trim()) {
      setError("Enter a job id to purge");
      return;
    }
    if (!window.confirm(`Purge log for job ${purgeJobId}?`)) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await purgeRuntimeJobLog(purgeJobId.trim());
      setLastResult({ jobId: Number(purgeJobId), status: "log purged" });
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="publish-section-runtime">
      <div style={toolbarStyle}>
        <label>
          Site{" "}
          <select
            value={siteId}
            onChange={(e) => setSiteId(e.target.value)}
            aria-label="Runtime site"
          >
            {sites.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <button type="button" style={buttonStyle} onClick={reload} disabled={busy}>
          Refresh
        </button>
      </div>

      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}

      <h3 style={{ fontSize: "1rem" }}>{message(MSG.PUBLISH_SECTION_RUNTIME)}</h3>
      {!loading && editions.length === 0 && (
        <p style={emptyStyle}>No editions for this site.</p>
      )}
      <ul style={listStyle}>
        {editions.map((ed) => {
          const id = String(ed.editionId ?? "");
          const selected = id === selectedEdition;
          return (
            <li key={id} style={listItemStyle}>
              <button
                type="button"
                style={{
                  ...buttonStyle,
                  fontWeight: selected ? 600 : 400,
                  borderColor: selected ? "#0b6" : "#ccc",
                }}
                onClick={() => setSelectedEdition(id)}
              >
                {ed.name}
              </button>
              <span style={{ color: "#666", fontSize: "0.85rem" }}>
                {canStopEdition(ed)
                  ? `Job ${ed.runningJobId}${ed.jobStatus ? ` · ${ed.jobStatus}` : ""}`
                  : "Idle"}
              </span>
              <button
                type="button"
                style={primaryButtonStyle}
                disabled={busy || !id}
                onClick={() => void onStart(id)}
              >
                Start
              </button>
              {canStopEdition(ed) && (
                <button
                  type="button"
                  style={buttonStyle}
                  disabled={busy}
                  onClick={() => void onStop(ed.runningJobId!)}
                >
                  {message(MSG.PUBLISH_STOP)}
                </button>
              )}
            </li>
          );
        })}
      </ul>

      <div style={{ marginTop: 20, borderTop: "1px solid #eee", paddingTop: 12 }}>
        <h3 style={{ fontSize: "1rem" }}>Demand publish</h3>
        <p style={{ fontSize: "0.85rem", color: "#666" }}>
          Selected edition: {selectedEdition || "none"}. Enter content ids
          (comma-separated). Folder parent is resolved on the server when
          possible.
        </p>
        <div style={formRowStyle}>
          <label htmlFor="demand-ids">Content ids</label>
          <input
            id="demand-ids"
            value={demandIds}
            onChange={(e) => setDemandIds(e.target.value)}
            placeholder="e.g. 101, 102"
          />
        </div>
        <button
          type="button"
          style={primaryButtonStyle}
          disabled={busy}
          onClick={() => void onDemand()}
        >
          Queue demand
        </button>
      </div>

      <div style={{ marginTop: 20, borderTop: "1px solid #eee", paddingTop: 12 }}>
        <h3 style={{ fontSize: "1rem" }}>Advanced cleanup</h3>
        <div style={toolbarStyle}>
          <button
            type="button"
            style={buttonStyle}
            disabled={busy || !siteId}
            onClick={() => void onClearSite()}
          >
            Clear site record
          </button>
        </div>
        <div style={formRowStyle}>
          <label htmlFor="purge-job">Purge job log by id</label>
          <input
            id="purge-job"
            value={purgeJobId}
            onChange={(e) => setPurgeJobId(e.target.value)}
          />
        </div>
        <button
          type="button"
          style={buttonStyle}
          disabled={busy}
          onClick={() => void onPurgeLog()}
        >
          Purge log
        </button>
      </div>

      {lastResult && (
        <p style={{ marginTop: 16 }} role="status">
          Last result: {lastResult.status}
          {lastResult.jobId != null ? ` · job ${lastResult.jobId}` : ""}
          {lastResult.requestId != null
            ? ` · request ${lastResult.requestId}`
            : ""}
          {lastResult.delivered != null
            ? ` · delivered ${lastResult.delivered}`
            : ""}
        </p>
      )}
    </div>
  );
}
