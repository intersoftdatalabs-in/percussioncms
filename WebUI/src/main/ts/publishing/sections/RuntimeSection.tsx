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

import React, { useCallback, useEffect, useState } from "react";
import { formatApiError } from "../../api/client";
import { fetchSites } from "../../api/home/homeApi";
import { listServers } from "../../api/publishing/serversApi";
import type { PublishServer } from "../../api/publishing/types";
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

const RT = MSG.PUBLISH.SECTIONS.RUNTIME;

/** Catalog text with a single {0} replacement (works with or without I18N args). */
export function runtimeMessage(key: string, arg?: string): string {
  const text = message(key);
  return arg == null ? text : text.split("{0}").join(arg);
}
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

/**
 * Job id to open on Status, or null when the edition is idle.
 * Idle rows must not offer a link (runningJobId missing or not greater than 0).
 */
export function openableRunningJobId(row: RuntimeEditionStatus): string | null {
  const id = row.runningJobId ?? 0;
  if (!(typeof id === "number" && id > 0)) {
    return null;
  }
  return String(id);
}

export interface RuntimeSectionProps {
  /** Switch to Status job detail for a running edition job. */
  onOpenRunningJob?: (jobId: string) => void;
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
export function RuntimeSection({
  onOpenRunningJob,
}: RuntimeSectionProps = {}): React.ReactElement {
  const [sites, setSites] = useState<Array<{ name: string; id: string }>>([]);
  const [siteId, setSiteId] = useState("");
  const [servers, setServers] = useState<PublishServer[]>([]);
  const [pubServerId, setPubServerId] = useState("");
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
    listRuntimeEditions(siteId, pubServerId || undefined)
      .then((list) => {
        setEditions(list);
        if (list.length > 0 && !selectedEdition) {
          setSelectedEdition(String(list[0].editionId ?? ""));
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, [siteId, pubServerId, selectedEdition]);

  useEffect(() => {
    if (!siteId) {
      setServers([]);
      setPubServerId("");
      return;
    }
    listServers(siteId)
      .then((list) => {
        setServers(list);
        if (list.length > 0) {
          const firstId = String(list[0].serverId ?? list[0].id ?? "");
          setPubServerId((prev) => prev || firstId);
        } else {
          setPubServerId("");
        }
      })
      .catch(() => setServers([]));
  }, [siteId]);

  useEffect(() => {
    reload();
  }, [siteId, pubServerId]);

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
      setError(message(RT.DEMAND_NEED_IDS));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const res = await demandPublish(selectedEdition, { contentIds: ids });
      setLastResult(res);
    } catch (e) {
      setLastResult(null);
      setError(formatApiError(e, message(MSG.PUBLISH_ERROR)));
    } finally {
      setBusy(false);
    }
  }

  async function onClearSite(): Promise<void> {
    if (!siteId) {
      return;
    }
    if (
      !window.confirm(message(RT.CONFIRM_CLEAR))
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
      setError(message(RT.NEED_JOB_ID));
      return;
    }
    if (!window.confirm(runtimeMessage(RT.CONFIRM_PURGE, purgeJobId))) {
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
          {message(RT.SITE)}{" "}
          <select
            value={siteId}
            onChange={(e) => setSiteId(e.target.value)}
            aria-label={message(RT.SITE_PICKER_ARIA)}
          >
            {sites.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          {message(RT.PUBLISH_SERVER)}{" "}
          <select
            data-testid="runtime-pub-server"
            value={pubServerId}
            onChange={(e) => setPubServerId(e.target.value)}
            aria-label={message(RT.SERVER_PICKER_ARIA)}
          >
            {servers.map((s) => {
              const id = String(s.serverId ?? s.id ?? "");
              return (
                <option key={id} value={id}>
                  {s.serverName ?? s.name ?? id}
                </option>
              );
            })}
          </select>
        </label>
        <button type="button" style={buttonStyle} onClick={reload} disabled={busy}>
          {message(RT.REFRESH)}
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
        <p style={emptyStyle}>{message(RT.EDITIONS_EMPTY)}</p>
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
                  ? `${runtimeMessage(RT.JOB_RUNNING, String(ed.runningJobId))}${ed.jobStatus ? ` · ${ed.jobStatus}` : ""}`
                  : message(RT.IDLE)}
              </span>
              <button
                type="button"
                style={primaryButtonStyle}
                data-testid={`runtime-start-${id}`}
                disabled={busy || !id}
                onClick={() => void onStart(id)}
              >
                {message(RT.START)}
              </button>
              {canStopEdition(ed) && (
                <button
                  type="button"
                  style={buttonStyle}
                  data-testid={`runtime-stop-${id}`}
                  disabled={busy}
                  onClick={() => void onStop(ed.runningJobId!)}
                >
                  {message(MSG.PUBLISH_STOP)}
                </button>
              )}
              {openableRunningJobId(ed) != null && (
                <button
                  type="button"
                  style={buttonStyle}
                  data-testid={`runtime-open-job-${id}`}
                  disabled={busy || onOpenRunningJob == null}
                  onClick={() => {
                    const jobId = openableRunningJobId(ed);
                    if (jobId != null) {
                      onOpenRunningJob?.(jobId);
                    }
                  }}
                >
                  {message(RT.OPEN_JOB)}
                </button>
              )}
            </li>
          );
        })}
      </ul>

      <div style={{ marginTop: 20, borderTop: "1px solid #eee", paddingTop: 12 }}>
        <h3 style={{ fontSize: "1rem" }} data-testid="runtime-demand-heading">
          {message(RT.DEMAND_HEADING)}
        </h3>
        <p style={{ fontSize: "0.85rem", color: "#666" }}>
          {runtimeMessage(
            RT.DEMAND_HELP,
            selectedEdition || message(RT.DEMAND_NONE),
          )}
        </p>
        <div style={formRowStyle}>
          <label htmlFor="demand-ids">{message(RT.CONTENT_IDS)}</label>
          <input
            id="demand-ids"
            data-testid="runtime-demand-ids"
            value={demandIds}
            onChange={(e) => setDemandIds(e.target.value)}
            placeholder={message(RT.CONTENT_IDS_PLACEHOLDER)}
          />
        </div>
        <button
          type="button"
          style={primaryButtonStyle}
          data-testid="runtime-demand-submit"
          disabled={busy}
          onClick={() => void onDemand()}
        >
          {message(RT.QUEUE_DEMAND)}
        </button>
      </div>

      <div style={{ marginTop: 20, borderTop: "1px solid #eee", paddingTop: 12 }}>
        <h3 style={{ fontSize: "1rem" }}>{message(RT.ADVANCED_CLEANUP_HEADING)}</h3>
        <div style={toolbarStyle}>
          <button
            type="button"
            style={buttonStyle}
            disabled={busy || !siteId}
            onClick={() => void onClearSite()}
          >
            {message(RT.CLEAR_SITE)}
          </button>
        </div>
        <div style={formRowStyle}>
          <label htmlFor="purge-job">{message(RT.PURGE_JOB_LOG)}</label>
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
          {message(RT.PURGE_LOG)}
        </button>
      </div>

      {lastResult && (
        <p
          style={{ marginTop: 16 }}
          role="status"
          data-testid="runtime-job-status"
        >
          {runtimeMessage(RT.LAST_RESULT, lastResult.status ?? "")}
          {lastResult.jobId != null
            ? ` · ${runtimeMessage(RT.LAST_JOB, String(lastResult.jobId))}`
            : ""}
          {lastResult.requestId != null
            ? ` · ${runtimeMessage(RT.LAST_REQUEST, String(lastResult.requestId))}`
            : ""}
          {lastResult.delivered != null
            ? ` · ${runtimeMessage(RT.LAST_DELIVERED, String(lastResult.delivered))}`
            : ""}
        </p>
      )}
    </div>
  );
}
