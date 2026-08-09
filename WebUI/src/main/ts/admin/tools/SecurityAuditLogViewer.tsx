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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  AUDIT_LOG_PAGE_SIZE_OPTIONS,
  AuditLogForbiddenError,
  DEFAULT_AUDIT_LOG_PAGE_SIZE,
  datetimeLocalToIso,
  getAuditLogEntry,
  queryAuditLogEntries,
  type SystemAuditLogEntry,
} from "../../api/auditlog";
import { formatApiError, isSessionRedirectError } from "../../api/client";
import { message } from "../../i18n/message";
import { ADMIN_MSG } from "../messages";

export type AuditLogFilterDraft = {
  fromLocal: string;
  toLocal: string;
  module: string;
  eventType: string;
  outcome: string;
  actor: string;
};

const EMPTY_FILTERS: AuditLogFilterDraft = {
  fromLocal: "",
  toLocal: "",
  module: "",
  eventType: "",
  outcome: "",
  actor: "",
};

function formatEventTime(iso: string | undefined): string {
  if (!iso) {
    return "—";
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return d.toLocaleString();
}

function truncate(text: string | undefined, max = 80): string {
  if (!text) {
    return "—";
  }
  const t = text.trim();
  if (t.length <= max) {
    return t;
  }
  return `${t.slice(0, max - 1)}…`;
}

/** Apply simple {0}/{1}/… placeholders after TMX resolve (works with key fallbacks). */
export function formatAuditPageSummary(
  start: number,
  end: number,
  total: number,
): string {
  const template = message(ADMIN_MSG.AUDIT_PAGE_SUMMARY);
  return template
    .replace("{0}", String(start))
    .replace("{1}", String(end))
    .replace("{2}", String(total));
}

const filterFieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: 4,
  minWidth: 140,
  maxWidth: 220,
};

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#475569",
};

const inputStyle: React.CSSProperties = {
  padding: "6px 8px",
  border: "1px solid #cbd5e1",
  borderRadius: 4,
  font: "inherit",
};

const thStyle: React.CSSProperties = {
  padding: "10px 12px",
  textAlign: "left",
  borderBottom: "2px solid #e2e8f0",
  color: "#475569",
  fontWeight: 600,
  whiteSpace: "nowrap",
};

const tdStyle: React.CSSProperties = {
  padding: "10px 12px",
  borderBottom: "1px solid #e2e8f0",
  verticalAlign: "top",
};

/**
 * Admin Security Audit Log viewer — list with filters/pagination + detail
 * panel for userMessage / logMessage (Phase 4 / #2619).
 *
 * Backed by existing REST {@code GET /services/auditlog/entries}.
 */
export const SecurityAuditLogViewer: React.FC = () => {
  const [draft, setDraft] = useState<AuditLogFilterDraft>(EMPTY_FILTERS);
  const [applied, setApplied] = useState<AuditLogFilterDraft>(EMPTY_FILTERS);
  const [offset, setOffset] = useState(0);
  const [limit, setLimit] = useState(DEFAULT_AUDIT_LOG_PAGE_SIZE);
  const [entries, setEntries] = useState<SystemAuditLogEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<SystemAuditLogEntry | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const mountedRef = useRef(true);

  const loadPage = useCallback(async () => {
    setLoading(true);
    setError(null);
    setForbidden(false);
    try {
      const page = await queryAuditLogEntries({
        from: datetimeLocalToIso(applied.fromLocal),
        to: datetimeLocalToIso(applied.toLocal),
        module: applied.module,
        eventType: applied.eventType,
        outcome: applied.outcome,
        actor: applied.actor,
        offset,
        limit,
      });
      if (!mountedRef.current) {
        return;
      }
      setEntries(page.entries ?? []);
      setTotal(page.total ?? 0);
    } catch (err) {
      if (!mountedRef.current || isSessionRedirectError(err)) {
        return;
      }
      if (err instanceof AuditLogForbiddenError) {
        setForbidden(true);
        setEntries([]);
        setTotal(0);
        setError(message(ADMIN_MSG.AUDIT_FORBIDDEN));
      } else {
        setError(formatApiError(err, message(ADMIN_MSG.ERROR_GENERIC)));
      }
    } finally {
      if (mountedRef.current) {
        setLoading(false);
      }
    }
  }, [applied, offset, limit]);

  useEffect(() => {
    mountedRef.current = true;
    void loadPage();
    return () => {
      mountedRef.current = false;
    };
  }, [loadPage]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      setDetailError(null);
      return;
    }
    let cancelled = false;
    setDetailLoading(true);
    setDetailError(null);
    void getAuditLogEntry(selectedId)
      .then((entry) => {
        if (!cancelled) {
          setDetail(entry);
        }
      })
      .catch((err) => {
        if (cancelled || isSessionRedirectError(err)) {
          return;
        }
        if (err instanceof AuditLogForbiddenError) {
          setDetailError(message(ADMIN_MSG.AUDIT_FORBIDDEN));
        } else {
          setDetailError(
            formatApiError(err, message(ADMIN_MSG.ERROR_GENERIC)),
          );
        }
        setDetail(null);
      })
      .finally(() => {
        if (!cancelled) {
          setDetailLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  const onApplyFilters = (): void => {
    setApplied({ ...draft });
    setOffset(0);
    setSelectedId(null);
  };

  const onResetFilters = (): void => {
    setDraft(EMPTY_FILTERS);
    setApplied(EMPTY_FILTERS);
    setOffset(0);
    setSelectedId(null);
  };

  const pageStart = total === 0 ? 0 : offset + 1;
  const pageEnd = Math.min(offset + limit, total);
  const canPrev = offset > 0;
  const canNext = offset + limit < total;

  return (
    <div
      className="perc-security-audit-log"
      data-testid="perc-security-audit-log"
    >
      <header style={{ marginBottom: 16 }}>
        <h2 style={{ margin: "0 0 4px" }}>
          {message(ADMIN_MSG.AUDIT_TITLE)}
        </h2>
        <p style={{ margin: 0, color: "#64748b", fontSize: 14 }}>
          {message(ADMIN_MSG.AUDIT_DESCRIPTION)}
        </p>
      </header>

      <form
        data-testid="audit-log-filters"
        onSubmit={(e) => {
          e.preventDefault();
          onApplyFilters();
        }}
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: 12,
          alignItems: "flex-end",
          marginBottom: 16,
          padding: 12,
          background: "#f8fafc",
          borderRadius: 6,
          border: "1px solid #e2e8f0",
        }}
      >
        <div style={filterFieldStyle}>
          <label htmlFor="audit-from" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_FROM)}
          </label>
          <input
            id="audit-from"
            type="datetime-local"
            value={draft.fromLocal}
            onChange={(e) =>
              setDraft((d) => ({ ...d, fromLocal: e.target.value }))
            }
            style={inputStyle}
            data-testid="audit-filter-from"
          />
        </div>
        <div style={filterFieldStyle}>
          <label htmlFor="audit-to" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_TO)}
          </label>
          <input
            id="audit-to"
            type="datetime-local"
            value={draft.toLocal}
            onChange={(e) =>
              setDraft((d) => ({ ...d, toLocal: e.target.value }))
            }
            style={inputStyle}
            data-testid="audit-filter-to"
          />
        </div>
        <div style={filterFieldStyle}>
          <label htmlFor="audit-module" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_MODULE)}
          </label>
          <input
            id="audit-module"
            type="text"
            value={draft.module}
            onChange={(e) =>
              setDraft((d) => ({ ...d, module: e.target.value }))
            }
            placeholder="AUTH"
            style={inputStyle}
            data-testid="audit-filter-module"
          />
        </div>
        <div style={filterFieldStyle}>
          <label htmlFor="audit-event-type" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_EVENT_TYPE)}
          </label>
          <input
            id="audit-event-type"
            type="text"
            value={draft.eventType}
            onChange={(e) =>
              setDraft((d) => ({ ...d, eventType: e.target.value }))
            }
            style={inputStyle}
            data-testid="audit-filter-event-type"
          />
        </div>
        <div style={filterFieldStyle}>
          <label htmlFor="audit-outcome" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_OUTCOME)}
          </label>
          <select
            id="audit-outcome"
            value={draft.outcome}
            onChange={(e) =>
              setDraft((d) => ({ ...d, outcome: e.target.value }))
            }
            style={inputStyle}
            data-testid="audit-filter-outcome"
          >
            <option value="">{message(ADMIN_MSG.AUDIT_FILTER_ALL)}</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILURE">FAILURE</option>
          </select>
        </div>
        <div style={filterFieldStyle}>
          <label htmlFor="audit-actor" style={labelStyle}>
            {message(ADMIN_MSG.AUDIT_FILTER_ACTOR)}
          </label>
          <input
            id="audit-actor"
            type="text"
            value={draft.actor}
            onChange={(e) =>
              setDraft((d) => ({ ...d, actor: e.target.value }))
            }
            style={inputStyle}
            data-testid="audit-filter-actor"
          />
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button
            type="submit"
            className="perc-button-primary"
            data-testid="audit-filter-apply"
            style={{
              padding: "8px 14px",
              background: "#0284c7",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              fontWeight: 600,
            }}
          >
            {message(ADMIN_MSG.AUDIT_APPLY_FILTERS)}
          </button>
          <button
            type="button"
            onClick={onResetFilters}
            data-testid="audit-filter-reset"
            style={{
              padding: "8px 14px",
              background: "#fff",
              color: "#334155",
              border: "1px solid #cbd5e1",
              borderRadius: 4,
              cursor: "pointer",
            }}
          >
            {message(ADMIN_MSG.AUDIT_RESET_FILTERS)}
          </button>
        </div>
      </form>

      {error && (
        <div
          role="alert"
          style={{
            color: forbidden ? "#b45309" : "#b91c1c",
            background: forbidden ? "#fffbeb" : "#fef2f2",
            border: `1px solid ${forbidden ? "#fcd34d" : "#fecaca"}`,
            padding: "10px 12px",
            borderRadius: 4,
            marginBottom: 12,
          }}
          data-testid="audit-log-error"
        >
          {error}
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: selectedId ? "minmax(0, 1fr) minmax(280px, 360px)" : "1fr",
          gap: 16,
          alignItems: "start",
        }}
      >
        <div style={{ minWidth: 0 }}>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              justifyContent: "space-between",
              alignItems: "center",
              gap: 8,
              marginBottom: 8,
            }}
          >
            <div
              data-testid="audit-log-page-summary"
              style={{ fontSize: 13, color: "#475569" }}
            >
              {loading
                ? message(ADMIN_MSG.LOADING)
                : formatAuditPageSummary(pageStart, pageEnd, total)}
            </div>
            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <label htmlFor="audit-page-size" style={labelStyle}>
                {message(ADMIN_MSG.AUDIT_PAGE_SIZE)}
              </label>
              <select
                id="audit-page-size"
                value={limit}
                onChange={(e) => {
                  setLimit(Number(e.target.value));
                  setOffset(0);
                }}
                style={inputStyle}
                data-testid="audit-page-size"
              >
                {AUDIT_LOG_PAGE_SIZE_OPTIONS.map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={!canPrev || loading}
                onClick={() => setOffset((o) => Math.max(0, o - limit))}
                data-testid="audit-page-prev"
                style={{
                  padding: "6px 10px",
                  cursor: canPrev && !loading ? "pointer" : "not-allowed",
                }}
              >
                {message(ADMIN_MSG.AUDIT_PREV)}
              </button>
              <button
                type="button"
                disabled={!canNext || loading}
                onClick={() => setOffset((o) => o + limit)}
                data-testid="audit-page-next"
                style={{
                  padding: "6px 10px",
                  cursor: canNext && !loading ? "pointer" : "not-allowed",
                }}
              >
                {message(ADMIN_MSG.AUDIT_NEXT)}
              </button>
            </div>
          </div>

          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                minWidth: 720,
              }}
              data-testid="audit-log-table"
            >
              <thead>
                <tr>
                  <th style={thStyle}>{message(ADMIN_MSG.AUDIT_COL_TIME)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUDIT_COL_MODULE)}</th>
                  <th style={thStyle}>
                    {message(ADMIN_MSG.AUDIT_COL_EVENT_TYPE)}
                  </th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUDIT_COL_OUTCOME)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUDIT_COL_ACTOR)}</th>
                  <th style={thStyle}>{message(ADMIN_MSG.AUDIT_COL_TARGET)}</th>
                  <th style={thStyle}>
                    {message(ADMIN_MSG.AUDIT_COL_USER_MESSAGE)}
                  </th>
                </tr>
              </thead>
              <tbody>
                {!loading && entries.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      style={{
                        ...tdStyle,
                        textAlign: "center",
                        color: "#94a3b8",
                      }}
                      data-testid="audit-log-empty"
                    >
                      {message(ADMIN_MSG.AUDIT_EMPTY)}
                    </td>
                  </tr>
                ) : (
                  entries.map((row) => {
                    const id = row.auditId ?? "";
                    const selected = id !== "" && id === selectedId;
                    return (
                      <tr
                        key={id || `${row.eventTime}-${row.actor}`}
                        data-testid={
                          id ? `audit-log-row-${id}` : "audit-log-row"
                        }
                        data-audit-id={id || undefined}
                        onClick={() => id && setSelectedId(id)}
                        onKeyDown={(e) => {
                          if ((e.key === "Enter" || e.key === " ") && id) {
                            e.preventDefault();
                            setSelectedId(id);
                          }
                        }}
                        tabIndex={id ? 0 : undefined}
                        role="button"
                        aria-selected={selected}
                        style={{
                          cursor: id ? "pointer" : "default",
                          background: selected ? "#e0f2fe" : undefined,
                        }}
                      >
                        <td style={tdStyle}>
                          {formatEventTime(row.eventTime)}
                        </td>
                        <td style={tdStyle}>{row.moduleCode || "—"}</td>
                        <td style={tdStyle}>{row.eventType || "—"}</td>
                        <td style={tdStyle}>{row.outcome || "—"}</td>
                        <td style={tdStyle}>{row.actor || "—"}</td>
                        <td style={tdStyle}>{truncate(row.target, 40)}</td>
                        <td style={tdStyle}>
                          {truncate(row.userMessage, 60)}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {selectedId && (
          <aside
            data-testid="audit-log-detail"
            aria-label={message(ADMIN_MSG.AUDIT_DETAIL_TITLE)}
            style={{
              border: "1px solid #e2e8f0",
              borderRadius: 6,
              padding: 12,
              background: "#fff",
              position: "sticky",
              top: 8,
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: 12,
              }}
            >
              <h3 style={{ margin: 0, fontSize: 16 }}>
                {message(ADMIN_MSG.AUDIT_DETAIL_TITLE)}
              </h3>
              <button
                type="button"
                onClick={() => setSelectedId(null)}
                data-testid="audit-detail-close"
                aria-label={message(ADMIN_MSG.CANCEL)}
                style={{
                  border: "none",
                  background: "transparent",
                  cursor: "pointer",
                  fontSize: 18,
                  lineHeight: 1,
                }}
              >
                ×
              </button>
            </div>
            {detailLoading && (
              <div data-testid="audit-detail-loading">
                {message(ADMIN_MSG.LOADING)}
              </div>
            )}
            {detailError && (
              <div role="alert" data-testid="audit-detail-error" style={{ color: "#b91c1c" }}>
                {detailError}
              </div>
            )}
            {!detailLoading && detail && (
              <dl
                style={{
                  margin: 0,
                  display: "grid",
                  gridTemplateColumns: "auto 1fr",
                  gap: "6px 10px",
                  fontSize: 13,
                }}
                data-testid="audit-detail-fields"
              >
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_ID)}
                  value={detail.auditId}
                  testId="audit-detail-id"
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_TIME)}
                  value={formatEventTime(detail.eventTime)}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_MODULE)}
                  value={detail.moduleCode}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_EVENT_TYPE)}
                  value={detail.eventType}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_OUTCOME)}
                  value={detail.outcome}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_ACTOR)}
                  value={detail.actor}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_TARGET)}
                  value={detail.target}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_SOURCE_IP)}
                  value={detail.sourceIp}
                />
                <DetailRow
                  label={message(ADMIN_MSG.AUDIT_COL_SERVER)}
                  value={detail.serverNode}
                />
                <dt style={{ fontWeight: 600, gridColumn: "1 / -1", marginTop: 8 }}>
                  {message(ADMIN_MSG.AUDIT_COL_USER_MESSAGE)}
                </dt>
                <dd
                  style={{
                    gridColumn: "1 / -1",
                    margin: 0,
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                    background: "#f8fafc",
                    padding: 8,
                    borderRadius: 4,
                  }}
                  data-testid="audit-detail-user-message"
                >
                  {detail.userMessage || "—"}
                </dd>
                <dt style={{ fontWeight: 600, gridColumn: "1 / -1", marginTop: 8 }}>
                  {message(ADMIN_MSG.AUDIT_COL_LOG_MESSAGE)}
                </dt>
                <dd
                  style={{
                    gridColumn: "1 / -1",
                    margin: 0,
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                    fontFamily: "ui-monospace, monospace",
                    fontSize: 12,
                    background: "#0f172a",
                    color: "#e2e8f0",
                    padding: 8,
                    borderRadius: 4,
                  }}
                  data-testid="audit-detail-log-message"
                >
                  {detail.logMessage || "—"}
                </dd>
              </dl>
            )}
          </aside>
        )}
      </div>
    </div>
  );
};

function DetailRow({
  label,
  value,
  testId,
}: {
  label: string;
  value?: string | null;
  testId?: string;
}): React.ReactElement {
  return (
    <>
      <dt style={{ fontWeight: 600, color: "#64748b" }}>{label}</dt>
      <dd style={{ margin: 0 }} data-testid={testId}>
        {value && String(value).trim() ? value : "—"}
      </dd>
    </>
  );
}
