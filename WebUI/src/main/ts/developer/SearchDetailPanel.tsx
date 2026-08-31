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

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../api/client";
import {
  SEARCH_TYPE_STANDARD,
  createSearch,
  deleteSearch,
  getSearchDetail,
  isSearchWriteReady,
  normalizeSearchName,
  saveSearch,
  type SearchWriteBody,
} from "../api/developer/searchesApi";
import type { SearchDef } from "../api/developer/types";
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
import { ObjectAclSection } from "./ObjectAclSection";

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
  marginBottom: "12px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

const TYPE_OPTIONS: { value: string; kind: "standard" | "custom" | "user" }[] = [
  { value: SEARCH_TYPE_STANDARD, kind: "standard" },
  { value: "CustomSearch", kind: "custom" },
  { value: "Search", kind: "user" },
];

function typeLabel(kind: "standard" | "custom" | "user"): string {
  if (kind === "custom") return DEV_MSG.SR_KIND_CUSTOM;
  if (kind === "user") return DEV_MSG.SR_KIND_USER;
  return DEV_MSG.SR_KIND_STANDARD;
}

function typeFromDetail(detail: SearchDef | null, fallback: string): string {
  if (detail?.type && detail.type.trim()) return detail.type.trim();
  if (detail?.customSearch) return "CustomSearch";
  if (detail?.userSearch) return "Search";
  if (detail?.standardSearch) return SEARCH_TYPE_STANDARD;
  return fallback;
}

export function SearchDetailPanel({
  idOrName,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  onBack: () => void;
  onSaved?: (detail: SearchDef) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<SearchDef | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState(SEARCH_TYPE_STANDARD);
  const [displayFormatId, setDisplayFormatId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setLoading(true);
    getSearchDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || idOrName);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setType(typeFromDetail(d, SEARCH_TYPE_STANDARD));
        setDisplayFormatId(d.displayFormatId || "");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.SR_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedName = normalizeSearchName(detail?.name || idOrName || "");
  const loadedLabel = detail?.label || "";
  const loadedDescription = detail?.description || "";
  const loadedType = typeFromDetail(detail, SEARCH_TYPE_STANDARD);
  const loadedDf = detail?.displayFormatId || "";
  const dirty =
    isNew ||
    normalizeSearchName(name) !== loadedName ||
    label !== loadedLabel ||
    description !== loadedDescription ||
    type !== loadedType ||
    displayFormatId !== loadedDf;
  const canSave = !busy && dirty && isSearchWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeSearchName(name);

  function writeBody(): SearchWriteBody {
    const body: SearchWriteBody = {
      name: isNew ? normalizeSearchName(name) : detail?.name || normalizeSearchName(name),
      label,
      description,
      type,
    };
    if (displayFormatId.trim()) {
      body.displayFormatId = displayFormatId.trim();
    }
    return body;
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409 && isNew) return DEV_MSG.SR_DUPLICATE;
    if (isApiError(err) && err.status === 400) return DEV_MSG.SR_INVALID_NAME;
    if (isApiError(err) && err.status === 403) return DEV_MSG.SR_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.SR_NOT_FOUND;
    return DEV_MSG.SR_SAVE_ERROR;
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved =
        isNew || !writeKey
          ? await createSearch(writeBody())
          : await saveSearch(writeKey, writeBody());
      setDetail(saved);
      if (isNew) {
        setCreatedKey(saved.name || normalizeSearchName(name));
      }
      setName(saved.name || name);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setType(typeFromDetail(saved, type));
      setDisplayFormatId(saved.displayFormatId || "");
      setNotice(DEV_MSG.SR_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, saveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    if (!window.confirm(DEV_MSG.SR_DELETE_CONFIRM)) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteSearch(writeKey);
      setNotice(DEV_MSG.SR_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.SR_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.SR_NOT_FOUND
            : DEV_MSG.SR_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.SR_NEW
    : detail?.label || detail?.name || idOrName || DEV_MSG.SR_EDIT;

  const fields = detail != null && Array.isArray(detail.fields) ? detail.fields : [];
  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? detail.designGaps
      : [DEV_MSG.SR_GAP_FIELDS, DEV_MSG.SR_GAP_VIEWS];

  return (
    <div data-testid="developer-sr-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-sr-back"
        aria-label={DEV_MSG.SR_BACK}
        style={backButton}
      >
        ← {DEV_MSG.SR_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-sr-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-sr-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-sr-detail-loading">{DEV_MSG.SR_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-sr-detail-title">
              {title}
            </h2>
            {!isNew && detail?.description && !dirty ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            {!isNew && detail?.guid?.stringValue ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.SR_COL_MAX}</dt>
                <dd style={{ margin: 0 }}>
                  {detail.maximumResultSize != null ? String(detail.maximumResultSize) : "—"}
                </dd>
                <dt>{DEV_MSG.SR_COL_CASE}</dt>
                <dd style={{ margin: 0 }}>{detail.caseSensitive ? DEV_MSG.YES : DEV_MSG.NO}</dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="sr-name">{DEV_MSG.SR_FORM_NAME}</label>
            <input
              id="sr-name"
              data-testid="developer-sr-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.SR_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.SR_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sr-label">{DEV_MSG.SR_FORM_LABEL}</label>
            <input
              id="sr-label"
              data-testid="developer-sr-label"
              style={inputStyle}
              value={label}
              disabled={busy}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sr-desc">{DEV_MSG.SR_FORM_DESCRIPTION}</label>
            <input
              id="sr-desc"
              data-testid="developer-sr-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sr-type">{DEV_MSG.SR_FORM_TYPE}</label>
            <select
              id="sr-type"
              data-testid="developer-sr-type"
              style={inputStyle}
              value={type}
              disabled={busy}
              onChange={(e) => setType(e.target.value)}
            >
              {TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {typeLabel(opt.kind)}
                </option>
              ))}
            </select>
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sr-df">{DEV_MSG.SR_FORM_DF}</label>
            <input
              id="sr-df"
              data-testid="developer-sr-display-format"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={displayFormatId}
              disabled={busy}
              onChange={(e) => setDisplayFormatId(e.target.value)}
              autoComplete="off"
            />
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-sr-save"
              aria-label={DEV_MSG.SR_SAVE}
              disabled={!canSave}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: canSave ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: canSave ? "pointer" : "not-allowed",
              }}
            >
              {DEV_MSG.SR_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-sr-cancel"
              disabled={busy}
              onClick={onBack}
              style={{
                padding: "8px 16px",
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                cursor: "pointer",
              }}
            >
              {DEV_MSG.SR_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-sr-delete"
                aria-label={DEV_MSG.SR_DELETE}
                disabled={busy}
                onClick={() => void handleDelete()}
                style={{
                  padding: "8px 16px",
                  background: "#c53030",
                  color: "#fff",
                  border: "none",
                  borderRadius: "4px",
                  cursor: busy ? "wait" : "pointer",
                  marginLeft: "auto",
                }}
              >
                {DEV_MSG.SR_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section data-testid="developer-sr-fields">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SR_FIELDS}</h3>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.SR_FIELDS_HINT}
                </p>
                {fields.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-sr-fields-empty">
                    {DEV_MSG.SR_NONE}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-sr-fields-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_FIELD}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_OP}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_VALUE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_FTYPE}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fields.map((f, i) => (
                          <tr
                            key={`${f.fieldName ?? "f"}-${i}`}
                            data-testid={`developer-sr-field-row-${i}`}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {f.fieldName || f.displayName || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{f.operator || "—"}</td>
                            <td style={{ padding: "8px" }}>{f.fieldValue || "—"}</td>
                            <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              <ObjectAclSection
                objectGuid={detail.guid?.stringValue}
                objectKind="search"
                testIdPrefix="developer-sr-acl"
              />

              <section style={{ marginTop: "16px" }} data-testid="developer-sr-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SR_GAPS}</h3>
                <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {gapList.map((g) => (
                    <li key={g}>{g}</li>
                  ))}
                </ul>
              </section>
            </>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
