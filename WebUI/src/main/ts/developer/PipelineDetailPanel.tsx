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

import React, { useEffect, useRef, useState } from "react";
import { useSpaBootstrap } from "../app/bootstrap/BootstrapContext";
import { isApiError } from "../api/client";
import {
  executeResource,
  getApplicationDetail,
  getApplicationValidation,
  getPipelineIr,
  putHttpBackendTank,
  startApplication,
  stopApplication,
} from "../api/developer/pipelinesApi";
import type {
  ApplicationDetail,
  ApplicationValidationProblem,
  ApplicationValidationResult,
  PipelineExecuteRequest,
  PipelineExecuteResult,
  PipelineIrDocument,
  PipelineIrResource,
  PipelineIrStages,
} from "../api/developer/types";
import {
  catalogColors,
  backButton,
  errorAlert,
  metaGrid,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const DEFAULT_INVOKE_BODY = '{\n  "params": {}\n}\n';

const toolbarStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "8px",
  alignItems: "center",
  marginBottom: "12px",
};

const primaryButton: React.CSSProperties = {
  background: catalogColors.accent,
  color: "#fff",
  border: "none",
  borderRadius: "4px",
  padding: "8px 14px",
  cursor: "pointer",
  fontSize: "0.9rem",
};

const secondaryButton: React.CSSProperties = {
  background: "#fff",
  color: catalogColors.text,
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "8px 14px",
  cursor: "pointer",
  fontSize: "0.9rem",
};

const disabledPrimary: React.CSSProperties = {
  ...primaryButton,
  background: catalogColors.disabled,
  cursor: "not-allowed",
};

const disabledSecondary: React.CSSProperties = {
  ...secondaryButton,
  color: catalogColors.disabled,
  cursor: "not-allowed",
};

const successNotice: React.CSSProperties = {
  color: catalogColors.accent,
  marginBottom: "12px",
  fontSize: "0.9rem",
};

const fieldLabel: React.CSSProperties = {
  display: "block",
  fontSize: "0.85rem",
  marginBottom: "4px",
  color: catalogColors.muted,
};

const textInput: React.CSSProperties = {
  width: "100%",
  maxWidth: "480px",
  boxSizing: "border-box",
  fontFamily: "monospace",
  fontSize: "0.9rem",
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
};

const jsonTextarea: React.CSSProperties = {
  width: "100%",
  maxWidth: "720px",
  minHeight: "120px",
  boxSizing: "border-box",
  fontFamily: "monospace",
  fontSize: "0.85rem",
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  resize: "vertical",
};

const resultPre: React.CSSProperties = {
  margin: 0,
  padding: "12px",
  maxWidth: "720px",
  overflowX: "auto",
  background: "#f7fafc",
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: "4px",
  fontFamily: "monospace",
  fontSize: "0.8rem",
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
};

function canStart(detail: ApplicationDetail): boolean {
  return detail.hidden !== true && detail.enabled !== false && detail.active !== true;
}

function canStop(detail: ApplicationDetail): boolean {
  return detail.hidden !== true && detail.active === true;
}

function lifecycleErrMsg(err: unknown, fallback: string): string {
  if (isApiError(err) && err.status === 403) {
    return DEV_MSG.PIPE_FORBIDDEN;
  }
  return panelErrMsg(err, fallback);
}

function invokeErrMsg(err: unknown): string {
  if (isApiError(err) && err.status === 403) {
    return DEV_MSG.PIPE_FORBIDDEN;
  }
  return panelErrMsg(err, DEV_MSG.PIPE_INVOKE_ERROR);
}

function parseExecuteBody(raw: string):
  | { ok: true; body: PipelineExecuteRequest }
  | { ok: false; message: string } {
  const trimmed = raw.trim();
  if (!trimmed) {
    return { ok: true, body: {} };
  }
  try {
    const parsed: unknown = JSON.parse(trimmed);
    if (parsed == null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return { ok: false, message: DEV_MSG.PIPE_INVOKE_BODY_INVALID };
    }
    return { ok: true, body: parsed as PipelineExecuteRequest };
  } catch {
    return { ok: false, message: DEV_MSG.PIPE_INVOKE_BODY_INVALID };
  }
}

function formatExecuteResult(result: PipelineExecuteResult): string {
  try {
    return JSON.stringify(result, null, 2);
  } catch {
    return String(result);
  }
}

function defaultResourceName(detail: ApplicationDetail | null): string {
  const first = detail?.dataSets?.find((ds) => typeof ds.name === "string" && ds.name.trim());
  return first?.name?.trim() ?? "";
}

const STAGE_KEYS: Array<keyof PipelineIrStages> = [
  "pageTank",
  "backendTank",
  "mapper",
  "selector",
  "pager",
  "updater",
];

/** Ordered inventory of present stage keys for catalog chrome. */
export function presentStageLabels(stages: PipelineIrStages | undefined): string[] {
  if (!stages) return [];
  const out: string[] = [];
  for (const key of STAGE_KEYS) {
    const stage = stages[key];
    if (stage && stage.present) out.push(key);
  }
  return out;
}

function yesNo(value: boolean | null | undefined): string {
  if (value == null) return "—";
  return value ? DEV_MSG.YES : DEV_MSG.NO;
}

function IrResourceCard({
  resource,
  index,
}: {
  resource: PipelineIrResource;
  index: number;
}): React.ReactElement {
  const stages = resource.stages;
  const present = presentStageLabels(stages);
  const tables = stages?.backendTank?.present ? stages.backendTank.tables || [] : [];
  const mappings = stages?.mapper?.present ? stages.mapper.mappings || [] : [];
  const pageTank = stages?.pageTank?.present ? stages.pageTank : null;
  const selector = stages?.selector?.present ? stages.selector : null;
  const updater = stages?.updater?.present ? stages.updater : null;
  const joinCount = stages?.backendTank?.joinCount;
  const adapterType = stages?.backendTank?.adapterType;
  const httpUrl = stages?.backendTank?.url;

  return (
    <article
      data-testid={`developer-pipe-ir-resource-${index}`}
      style={{
        marginBottom: "16px",
        padding: "12px",
        border: `1px solid ${catalogColors.softBorder}`,
        borderRadius: "6px",
        background: catalogColors.surface,
      }}
    >
      <header style={{ marginBottom: "8px" }}>
        <h4 style={{ margin: "0 0 4px", fontFamily: "monospace" }}>
          {resource.name || `resource-${index}`}
        </h4>
        <div style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {[resource.kind, resource.requestPage, resource.pipeName]
            .filter(Boolean)
            .join(" · ") || "—"}
        </div>
        {resource.description ? (
          <p style={{ margin: "6px 0 0", color: catalogColors.muted, fontSize: "0.9rem" }}>
            {resource.description}
          </p>
        ) : null}
      </header>

      <dl style={{ ...metaGrid, marginBottom: "8px" }}>
        <dt>{DEV_MSG.PIPE_IR_COL_STAGES}</dt>
        <dd style={{ margin: 0, ...monoCell }}>
          {present.length > 0 ? present.join(", ") : DEV_MSG.PIPE_NONE}
        </dd>
        <dt>{DEV_MSG.PIPE_IR_COL_TX}</dt>
        <dd style={{ margin: 0, ...monoCell }}>{resource.transactionMode || "—"}</dd>
        {pageTank ? (
          <>
            <dt>{DEV_MSG.PIPE_IR_PAGE_TANK}</dt>
            <dd style={{ margin: 0, ...monoCell }}>
              {DEV_MSG.PIPE_IR_SCHEMA}: {pageTank.schemaSource || "—"}
            </dd>
          </>
        ) : null}
        {selector ? (
          <>
            <dt>{DEV_MSG.PIPE_IR_SELECTOR}</dt>
            <dd style={{ margin: 0 }}>
              {selector.method || "—"}
              {selector.whereClauseCount != null
                ? ` · where ${selector.whereClauseCount}`
                : ""}
              {selector.unique ? " · unique" : ""}
            </dd>
          </>
        ) : null}
        {updater ? (
          <>
            <dt>{DEV_MSG.PIPE_IR_UPDATER}</dt>
            <dd style={{ margin: 0 }}>
              insert {yesNo(updater.allowInsert)} · update {yesNo(updater.allowUpdate)} ·
              delete {yesNo(updater.allowDelete)}
              {updater.updateColumnCount != null
                ? ` · cols ${updater.updateColumnCount}`
                : ""}
            </dd>
          </>
        ) : null}
        {joinCount != null && joinCount > 0 ? (
          <>
            <dt>{DEV_MSG.PIPE_IR_JOINS}</dt>
            <dd style={{ margin: 0 }}>{joinCount}</dd>
          </>
        ) : null}
        {adapterType ? (
          <>
            <dt>{DEV_MSG.PIPE_HTTP_ADAPTER}</dt>
            <dd style={{ margin: 0, ...monoCell }} data-testid={`developer-pipe-ir-adapter-${index}`}>
              {adapterType}
              {httpUrl ? ` · ${httpUrl}` : ""}
            </dd>
          </>
        ) : null}
      </dl>

      {tables.length > 0 ? (
        <section
          style={{ marginBottom: "12px" }}
          data-testid={`developer-pipe-ir-tanks-${index}`}
        >
          <h5 style={{ margin: "0 0 6px", fontSize: "0.95rem" }}>{DEV_MSG.PIPE_IR_TANKS}</h5>
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "0.85rem",
              }}
            >
              <thead>
                <tr style={tableHeaderRow}>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_ALIAS}</th>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_TABLE}</th>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_DS}</th>
                </tr>
              </thead>
              <tbody>
                {tables.map((t, ti) => (
                  <tr key={`${t.alias || t.table || "t"}-${ti}`} style={tableRow}>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>
                      {t.alias || "—"}
                    </td>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>
                      {t.table || "—"}
                    </td>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>
                      {t.datasource || "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {mappings.length > 0 ? (
        <section data-testid={`developer-pipe-ir-mapper-${index}`}>
          <h5 style={{ margin: "0 0 6px", fontSize: "0.95rem" }}>
            {DEV_MSG.PIPE_IR_MAPPER} ({mappings.length})
          </h5>
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "0.85rem",
              }}
            >
              <thead>
                <tr style={tableHeaderRow}>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_DOC}</th>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_BACKEND}</th>
                  <th style={{ padding: "6px 8px" }}>{DEV_MSG.PIPE_IR_COL_BACKEND_KIND}</th>
                </tr>
              </thead>
              <tbody>
                {mappings.map((m, mi) => (
                  <tr key={`${m.documentField || "m"}-${mi}`} style={tableRow}>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>
                      {m.documentField || "—"}
                    </td>
                    <td style={{ padding: "6px 8px", fontFamily: "monospace" }}>
                      {m.backend || "—"}
                    </td>
                    <td style={{ padding: "6px 8px" }}>{m.backendKind || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </article>
  );
}


type ProblemsState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "unavailable" }
  | { status: "forbidden" }
  | { status: "error"; message: string }
  | { status: "ready"; result: ApplicationValidationResult };

export function PipelineDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const { isAdmin } = useSpaBootstrap();
  const [detail, setDetail] = useState<ApplicationDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [lifecycleAction, setLifecycleAction] = useState<"start" | "stop" | null>(null);
  const [resourceName, setResourceName] = useState("");
  const [invokeBody, setInvokeBody] = useState(DEFAULT_INVOKE_BODY);
  const [invokeBusy, setInvokeBusy] = useState(false);
  const [invokeError, setInvokeError] = useState<string | null>(null);
  const [invokeResult, setInvokeResult] = useState<string | null>(null);
  const [problems, setProblems] = useState<ProblemsState>({ status: "idle" });
  const [ir, setIr] = useState<PipelineIrDocument | null>(null);
  const [irError, setIrError] = useState<string | null>(null);
  const [irLoading, setIrLoading] = useState(true);
  const [httpAdapter, setHttpAdapter] = useState("HTTP");
  const [httpUrl, setHttpUrl] = useState("http://127.0.0.1/pipeline-http-fixture");
  const [httpBusy, setHttpBusy] = useState(false);
  const [httpError, setHttpError] = useState<string | null>(null);
  const [httpNotice, setHttpNotice] = useState<string | null>(null);
  const inflight = useRef(false);
  const invokeInflight = useRef(false);
  const httpInflight = useRef(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    let cancelled = false;
    mountedRef.current = true;
    setDetail(null);
    setError(null);
    setActionError(null);
    setNotice(null);
    setBusy(false);
    setLifecycleAction(null);
    setResourceName("");
    setInvokeBody(DEFAULT_INVOKE_BODY);
    setInvokeBusy(false);
    setInvokeError(null);
    setInvokeResult(null);
    setProblems({ status: "idle" });
    setIr(null);
    setIrError(null);
    setIrLoading(true);
    setHttpAdapter("HTTP");
    setHttpUrl("http://127.0.0.1/pipeline-http-fixture");
    setHttpBusy(false);
    setHttpError(null);
    setHttpNotice(null);
    inflight.current = false;
    invokeInflight.current = false;
    httpInflight.current = false;
    getApplicationDetail(idOrName)
      .then((d) => {
        if (!cancelled) {
          setDetail(d);
          setResourceName(defaultResourceName(d));
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.PIPE_DETAIL_ERROR));
      });

    getPipelineIr(idOrName)
      .then((doc) => {
        if (!cancelled) {
          setIr(doc);
          setIrLoading(false);
          const firstHttp = (doc.resources || []).find((r) =>
            (r.stages?.backendTank?.adapterType || "").toUpperCase().includes("HTTP"),
          );
          const tank = firstHttp?.stages?.backendTank;
          if (tank?.url) {
            setHttpUrl(tank.url);
            if (tank.adapterType) setHttpAdapter(tank.adapterType);
          }
          const named = firstHttp?.name?.trim();
          if (named) {
            setResourceName((current) => current || named);
          }
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setIrError(panelErrMsg(err, DEV_MSG.PIPE_IR_ERROR));
          setIrLoading(false);
        }
      });

    return () => {
      cancelled = true;
      mountedRef.current = false;
    };
  }, [idOrName]);

  useEffect(() => {
    if (!isAdmin || detail == null) {
      setProblems({ status: "idle" });
      return;
    }
    let cancelled = false;
    setProblems({ status: "loading" });
    getApplicationValidation(idOrName)
      .then((result) => {
        if (!cancelled) setProblems({ status: "ready", result });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (isApiError(err) && err.status === 404) {
          setProblems({ status: "unavailable" });
          return;
        }
        if (isApiError(err) && err.status === 403) {
          setProblems({ status: "forbidden" });
          return;
        }
        setProblems({
          status: "error",
          message: panelErrMsg(err, DEV_MSG.PIPE_PROBLEMS_ERROR),
        });
      });
    return () => {
      cancelled = true;
    };
  }, [isAdmin, idOrName, detail]);

  async function onStart(): Promise<void> {
    if (!detail || inflight.current || !canStart(detail)) return;
    inflight.current = true;
    setBusy(true);
    setLifecycleAction("start");
    setActionError(null);
    setNotice(null);
    try {
      const next = await startApplication(idOrName);
      if (!mountedRef.current) return;
      setDetail(next);
      setNotice(DEV_MSG.PIPE_STARTED);
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setActionError(lifecycleErrMsg(err, DEV_MSG.PIPE_START_ERROR));
    } finally {
      inflight.current = false;
      if (mountedRef.current) {
        setBusy(false);
        setLifecycleAction(null);
      }
    }
  }

  async function onStop(): Promise<void> {
    if (!detail || inflight.current || !canStop(detail)) return;
    inflight.current = true;
    setBusy(true);
    setLifecycleAction("stop");
    setActionError(null);
    setNotice(null);
    try {
      const next = await stopApplication(idOrName);
      if (!mountedRef.current) return;
      setDetail(next);
      setNotice(DEV_MSG.PIPE_STOPPED);
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setActionError(lifecycleErrMsg(err, DEV_MSG.PIPE_STOP_ERROR));
    } finally {
      inflight.current = false;
      if (mountedRef.current) {
        setBusy(false);
        setLifecycleAction(null);
      }
    }
  }

  async function onSaveHttpTank(): Promise<void> {
    if (!detail || httpInflight.current) return;
    const resource = resourceName.trim();
    if (!resource) {
      setHttpError(DEV_MSG.PIPE_HTTP_RESOURCE_REQUIRED);
      setHttpNotice(null);
      return;
    }
    const url = httpUrl.trim();
    if (!url) {
      setHttpError(DEV_MSG.PIPE_HTTP_URL_REQUIRED);
      setHttpNotice(null);
      return;
    }
    const adapter = httpAdapter.trim().toUpperCase();
    if (adapter !== "HTTP" && adapter !== "REST") {
      setHttpError(DEV_MSG.PIPE_HTTP_SAVE_ERROR);
      setHttpNotice(null);
      return;
    }
    httpInflight.current = true;
    setHttpBusy(true);
    setHttpError(null);
    setHttpNotice(null);
    try {
      const saved = await putHttpBackendTank(idOrName, resource, {
        adapterType: httpAdapter.trim() || "HTTP",
        url,
        httpMethod: "GET",
      });
      if (!mountedRef.current) return;
      if (saved.url) setHttpUrl(saved.url);
      if (saved.adapterType) setHttpAdapter(saved.adapterType);
      setHttpNotice(DEV_MSG.PIPE_HTTP_SAVED);
      try {
        const nextIr = await getPipelineIr(idOrName);
        if (mountedRef.current) setIr(nextIr);
      } catch {
        // persist succeeded; IR refresh is best-effort
      }
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setHttpError(lifecycleErrMsg(err, DEV_MSG.PIPE_HTTP_SAVE_ERROR));
    } finally {
      httpInflight.current = false;
      if (mountedRef.current) setHttpBusy(false);
    }
  }

  async function onInvoke(): Promise<void> {
    if (!detail || invokeInflight.current) return;
    const resource = resourceName.trim();
    if (!resource) {
      setInvokeError(DEV_MSG.PIPE_INVOKE_RESOURCE_REQUIRED);
      setInvokeResult(null);
      return;
    }
    const parsed = parseExecuteBody(invokeBody);
    if (!parsed.ok) {
      setInvokeError(parsed.message);
      setInvokeResult(null);
      return;
    }
    invokeInflight.current = true;
    setInvokeBusy(true);
    setInvokeError(null);
    setInvokeResult(null);
    try {
      const result = await executeResource(idOrName, resource, parsed.body);
      if (!mountedRef.current) return;
      setInvokeResult(formatExecuteResult(result));
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setInvokeError(invokeErrMsg(err));
    } finally {
      invokeInflight.current = false;
      if (mountedRef.current) setInvokeBusy(false);
    }
  }

  const startEnabled = detail != null && !busy && canStart(detail);
  const stopEnabled = detail != null && !busy && canStop(detail);
  // Keep Invoke clickable when resource is blank so client validation can surface.
  const invokeEnabled = detail != null && !invokeBusy;
  const dataSetNames = (detail?.dataSets || [])
    .map((ds) => ds.name?.trim())
    .filter((n): n is string => Boolean(n));
  const problemRows: ApplicationValidationProblem[] =
    problems.status === "ready" ? problems.result.problems || [] : [];
  const irResources = ir?.resources || [];

  return (
    <div data-testid="developer-pipe-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-pipe-back"
        aria-label={DEV_MSG.PIPE_BACK}
        style={backButton}
      >
        ← {DEV_MSG.PIPE_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-pipe-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-pipe-detail-loading">{DEV_MSG.PIPE_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-pipe-detail-title">
              {detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
              {detail.id != null ? `id ${detail.id}` : ""}
              {detail.appRoot ? ` · ${detail.appRoot}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.PIPE_COL_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.appType || "—"}</dd>
              <dt>{DEV_MSG.PIPE_COL_ENABLED}</dt>
              <dd style={{ margin: 0 }} data-testid="developer-pipe-meta-enabled">
                {detail.enabled == null
                  ? "—"
                  : detail.enabled
                    ? DEV_MSG.YES
                    : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.PIPE_META_RUNNING}</dt>
              <dd style={{ margin: 0 }} data-testid="developer-pipe-meta-running">
                {detail.active == null
                  ? "—"
                  : detail.active
                    ? DEV_MSG.YES
                    : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.PIPE_META_HIDDEN}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hidden == null ? "—" : detail.hidden ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.PIPE_META_VERSION}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.version || "—"}</dd>
              <dt>{DEV_MSG.PIPE_COL_ROOT}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.appRoot || "—"}</dd>
            </dl>
          </header>

          {isAdmin ? (
            <div
              role="toolbar"
              aria-label="Pipeline application lifecycle"
              data-testid="developer-pipe-lifecycle"
              style={toolbarStyle}
            >
              <button
                type="button"
                data-testid="developer-pipe-start"
                aria-label={DEV_MSG.PIPE_START}
                title={
                  startEnabled ? DEV_MSG.PIPE_START : DEV_MSG.PIPE_START_DISABLED_HINT
                }
                disabled={!startEnabled}
                onClick={() => void onStart()}
                style={startEnabled ? primaryButton : disabledPrimary}
              >
                {lifecycleAction === "start" ? DEV_MSG.PIPE_STARTING : DEV_MSG.PIPE_START}
              </button>
              <button
                type="button"
                data-testid="developer-pipe-stop"
                aria-label={DEV_MSG.PIPE_STOP}
                title={stopEnabled ? DEV_MSG.PIPE_STOP : DEV_MSG.PIPE_STOP_DISABLED_HINT}
                disabled={!stopEnabled}
                onClick={() => void onStop()}
                style={stopEnabled ? secondaryButton : disabledSecondary}
              >
                {lifecycleAction === "stop" ? DEV_MSG.PIPE_STOPPING : DEV_MSG.PIPE_STOP}
              </button>
            </div>
          ) : null}

          {notice ? (
            <div role="status" data-testid="developer-pipe-lifecycle-notice" style={successNotice}>
              {notice}
            </div>
          ) : null}

          {actionError ? (
            <div
              role="alert"
              data-testid="developer-pipe-lifecycle-error"
              style={{ ...errorAlert, marginBottom: "12px" }}
            >
              {actionError}
            </div>
          ) : null}

          <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-datasets">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_DATASETS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.PIPE_DATASETS_HINT}</p>
            {(detail.dataSets || []).length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-pipe-datasets-empty">
                {DEV_MSG.PIPE_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-pipe-datasets-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_KIND}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_REQUEST}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DESCRIPTION}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(detail.dataSets || []).map((ds, i) => (
                      <tr
                        key={ds.name || `ds-${i}`}
                        data-testid={`developer-pipe-ds-row-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {ds.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{ds.kind || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {ds.requestPage || "—"}
                        </td>
                        <td style={{ padding: "8px", color: catalogColors.muted }}>
                          {ds.description || ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-ir">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_IR}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.PIPE_IR_HINT}</p>

            {irLoading ? (
              <div data-testid="developer-pipe-ir-loading">{DEV_MSG.PIPE_IR_LOADING}</div>
            ) : null}

            {irError ? (
              <div role="alert" data-testid="developer-pipe-ir-error" style={errorAlert}>
                {irError}
              </div>
            ) : null}

            {!irLoading && !irError && ir ? (
              <>
                <dl style={{ ...metaGrid, marginBottom: "12px" }} data-testid="developer-pipe-ir-meta">
                  <dt>{DEV_MSG.PIPE_IR_SOURCE}</dt>
                  <dd style={{ margin: 0, ...monoCell }}>{ir.source || "—"}</dd>
                  <dt>{DEV_MSG.PIPE_IR_VERSION}</dt>
                  <dd style={{ margin: 0, ...monoCell }}>{ir.irVersion || "—"}</dd>
                </dl>

                <h4 style={{ fontSize: "0.95rem", margin: "0 0 8px" }}>
                  {DEV_MSG.PIPE_IR_RESOURCES}
                </h4>
                {irResources.length === 0 ? (
                  <p
                    style={{ color: catalogColors.empty }}
                    data-testid="developer-pipe-ir-empty"
                  >
                    {DEV_MSG.PIPE_IR_EMPTY}
                  </p>
                ) : (
                  <div data-testid="developer-pipe-ir-resources">
                    {irResources.map((resource, index) => (
                      <IrResourceCard
                        key={resource.name || `ir-res-${index}`}
                        resource={resource}
                        index={index}
                      />
                    ))}
                  </div>
                )}
              </>
            ) : null}
          </section>

          {isAdmin ? (
            <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-http">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_HTTP}</h3>
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {DEV_MSG.PIPE_HTTP_HINT}
              </p>
              <div style={{ marginBottom: "12px" }}>
                <label htmlFor="developer-pipe-http-adapter" style={fieldLabel}>
                  {DEV_MSG.PIPE_HTTP_ADAPTER}
                </label>
                <select
                  id="developer-pipe-http-adapter"
                  data-testid="developer-pipe-http-adapter"
                  value={httpAdapter}
                  onChange={(e) => setHttpAdapter(e.target.value)}
                  disabled={httpBusy}
                  style={{ ...textInput, maxWidth: "240px" }}
                >
                  <option value="HTTP">{DEV_MSG.PIPE_HTTP_ADAPTER_HTTP}</option>
                  <option value="SQL">{DEV_MSG.PIPE_HTTP_ADAPTER_SQL}</option>
                </select>
              </div>
              <div style={{ marginBottom: "12px" }}>
                <label htmlFor="developer-pipe-http-url" style={fieldLabel}>
                  {DEV_MSG.PIPE_HTTP_URL}
                </label>
                <input
                  id="developer-pipe-http-url"
                  data-testid="developer-pipe-http-url"
                  value={httpUrl}
                  onChange={(e) => setHttpUrl(e.target.value)}
                  disabled={httpBusy}
                  style={textInput}
                  placeholder={DEV_MSG.PIPE_HTTP_URL_PLACEHOLDER}
                  autoComplete="off"
                />
              </div>
              <button
                type="button"
                data-testid="developer-pipe-http-save"
                aria-label={DEV_MSG.PIPE_HTTP_SAVE}
                disabled={httpBusy}
                onClick={() => void onSaveHttpTank()}
                style={httpBusy ? disabledPrimary : primaryButton}
              >
                {httpBusy ? DEV_MSG.PIPE_HTTP_SAVING : DEV_MSG.PIPE_HTTP_SAVE}
              </button>
              {httpNotice ? (
                <div
                  role="status"
                  data-testid="developer-pipe-http-notice"
                  style={{ ...successNotice, marginTop: "12px" }}
                >
                  {httpNotice}
                </div>
              ) : null}
              {httpError ? (
                <div
                  role="alert"
                  data-testid="developer-pipe-http-error"
                  style={{ ...errorAlert, marginTop: "12px" }}
                >
                  {httpError}
                </div>
              ) : null}
            </section>
          ) : null}

          {isAdmin ? (
            <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-invoke">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_INVOKE}</h3>
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {DEV_MSG.PIPE_INVOKE_HINT}
              </p>
              <div style={{ marginBottom: "12px" }}>
                <label htmlFor="developer-pipe-invoke-resource" style={fieldLabel}>
                  {DEV_MSG.PIPE_INVOKE_RESOURCE}
                </label>
                <input
                  id="developer-pipe-invoke-resource"
                  data-testid="developer-pipe-invoke-resource"
                  list="developer-pipe-invoke-resource-list"
                  value={resourceName}
                  onChange={(e) => setResourceName(e.target.value)}
                  disabled={invokeBusy}
                  style={textInput}
                  autoComplete="off"
                />
                {dataSetNames.length > 0 ? (
                  <datalist id="developer-pipe-invoke-resource-list">
                    {dataSetNames.map((name) => (
                      <option key={name} value={name} />
                    ))}
                  </datalist>
                ) : null}
              </div>
              <div style={{ marginBottom: "12px" }}>
                <label htmlFor="developer-pipe-invoke-body" style={fieldLabel}>
                  {DEV_MSG.PIPE_INVOKE_BODY}
                </label>
                <textarea
                  id="developer-pipe-invoke-body"
                  data-testid="developer-pipe-invoke-body"
                  value={invokeBody}
                  onChange={(e) => setInvokeBody(e.target.value)}
                  disabled={invokeBusy}
                  style={jsonTextarea}
                  spellCheck={false}
                />
              </div>
              <button
                type="button"
                data-testid="developer-pipe-invoke-run"
                aria-label={DEV_MSG.PIPE_INVOKE_RUN}
                disabled={!invokeEnabled}
                onClick={() => void onInvoke()}
                style={invokeEnabled ? primaryButton : disabledPrimary}
              >
                {invokeBusy ? DEV_MSG.PIPE_INVOKE_RUNNING : DEV_MSG.PIPE_INVOKE_RUN}
              </button>
              {invokeError ? (
                <div
                  role="alert"
                  data-testid="developer-pipe-invoke-error"
                  style={{ ...errorAlert, marginTop: "12px" }}
                >
                  {invokeError}
                </div>
              ) : null}
              {invokeResult ? (
                <div style={{ marginTop: "12px" }} data-testid="developer-pipe-invoke-result">
                  <h4 style={{ fontSize: "0.95rem", margin: "0 0 8px" }}>
                    {DEV_MSG.PIPE_INVOKE_RESULT}
                  </h4>
                  <pre style={resultPre}>{invokeResult}</pre>
                </div>
              ) : null}
            </section>
          ) : null}

          {isAdmin ? (
            <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-problems">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_PROBLEMS}</h3>
              <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {DEV_MSG.PIPE_PROBLEMS_HINT}
              </p>
              {problems.status === "loading" || problems.status === "idle" ? (
                <p
                  style={{ color: catalogColors.empty }}
                  data-testid="developer-pipe-problems-loading"
                >
                  {DEV_MSG.PIPE_PROBLEMS_LOADING}
                </p>
              ) : null}
              {problems.status === "unavailable" ? (
                <p
                  style={{ color: catalogColors.empty }}
                  data-testid="developer-pipe-problems-unavailable"
                >
                  {DEV_MSG.PIPE_PROBLEMS_UNAVAILABLE}
                </p>
              ) : null}
              {problems.status === "forbidden" ? (
                <div
                  role="alert"
                  data-testid="developer-pipe-problems-forbidden"
                  style={errorAlert}
                >
                  {DEV_MSG.PIPE_PROBLEMS_FORBIDDEN}
                </div>
              ) : null}
              {problems.status === "error" ? (
                <div
                  role="alert"
                  data-testid="developer-pipe-problems-error"
                  style={errorAlert}
                >
                  {problems.message}
                </div>
              ) : null}
              {problems.status === "ready" && problemRows.length === 0 ? (
                <p
                  style={{ color: catalogColors.empty }}
                  data-testid="developer-pipe-problems-empty"
                >
                  {DEV_MSG.PIPE_PROBLEMS_EMPTY}
                </p>
              ) : null}
              {problems.status === "ready" && problemRows.length > 0 ? (
                <div style={{ overflowX: "auto" }}>
                  <table
                    data-testid="developer-pipe-problems-table"
                    style={{
                      width: "100%",
                      borderCollapse: "collapse",
                      fontSize: "0.9rem",
                    }}
                  >
                    <thead>
                      <tr style={tableHeaderRow}>
                        <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_PROB_SEVERITY}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_PROB_CODE}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_PROB_MESSAGE}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_PROB_RESOURCE}</th>
                        <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_PROB_PATH}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {problemRows.map((p, i) => (
                        <tr
                          key={`${p.code || "p"}-${i}`}
                          data-testid={`developer-pipe-problem-row-${i}`}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px" }}>{p.severity || "—"}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {p.code || "—"}
                          </td>
                          <td style={{ padding: "8px" }}>{p.message || "—"}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {p.resource || "—"}
                          </td>
                          <td style={{ padding: "8px", fontFamily: "monospace", color: catalogColors.muted }}>
                            {p.path || "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>
          ) : null}

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-pipe-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {detail.designGaps.map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
