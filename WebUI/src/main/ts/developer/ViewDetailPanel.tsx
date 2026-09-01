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
import { isApiError } from "../api/client";
import { resolveViewObjectGuid } from "../api/displayFormatGuid";
import {
  VIEW_TYPE_STANDARD,
  createView,
  deleteView,
  getViewDetail,
  isProtectedViewWrite,
  isViewWriteReady,
  normalizeViewName,
  saveView,
  type ViewWriteBody,
} from "../api/developer/viewsApi";
import type { ViewDef } from "../api/developer/types";
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

function typeFromDetail(detail: ViewDef | null, fallback: string): string {
  if (detail?.type && detail.type.trim()) return detail.type.trim();
  if (detail?.customView) return "CustomView";
  if (detail?.standardView) return VIEW_TYPE_STANDARD;
  return fallback;
}

export function ViewDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  /** GUID from catalog list row when detail wire omits stringValue (#3380). */
  catalogGuid?: string | null;
  onBack: () => void;
  onSaved?: (detail: ViewDef) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<ViewDef | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState(VIEW_TYPE_STANDARD);
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
    getViewDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || idOrName);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setType(typeFromDetail(d, VIEW_TYPE_STANDARD));
        setDisplayFormatId(d.displayFormatId || "");
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.VW_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedName = normalizeViewName(detail?.name || idOrName || "");
  const loadedLabel = detail?.label || "";
  const loadedDescription = detail?.description || "";
  const loadedType = typeFromDetail(detail, VIEW_TYPE_STANDARD);
  const loadedDf = detail?.displayFormatId || "";
  const protectedWrite = isProtectedViewWrite(detail);
  const dirty =
    isNew ||
    normalizeViewName(name) !== loadedName ||
    label !== loadedLabel ||
    description !== loadedDescription ||
    type !== loadedType ||
    displayFormatId !== loadedDf;
  const canSave = !busy && !protectedWrite && dirty && isViewWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeViewName(name);
  const objectGuid = resolveViewObjectGuid(detail, catalogGuid);

  function writeBody(): ViewWriteBody {
    const body: ViewWriteBody = {
      name: isNew ? normalizeViewName(name) : detail?.name || normalizeViewName(name),
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
    if (isApiError(err) && err.status === 409 && isNew) return DEV_MSG.VW_DUPLICATE;
    if (isApiError(err) && err.status === 409) return DEV_MSG.VW_PROTECTED;
    if (isApiError(err) && err.status === 400) return DEV_MSG.VW_INVALID_NAME;
    if (isApiError(err) && err.status === 403) return DEV_MSG.VW_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.VW_NOT_FOUND;
    return DEV_MSG.VW_SAVE_ERROR;
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
          ? await createView(writeBody())
          : await saveView(writeKey, writeBody());
      setDetail(saved);
      if (isNew) {
        setCreatedKey(saved.name || normalizeViewName(name));
      }
      setName(saved.name || name);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setType(typeFromDetail(saved, type));
      setDisplayFormatId(saved.displayFormatId || "");
      setNotice(DEV_MSG.VW_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, saveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current || protectedWrite) return;
    if (!window.confirm(DEV_MSG.VW_DELETE_CONFIRM)) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteView(writeKey);
      setNotice(DEV_MSG.VW_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.VW_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.VW_NOT_FOUND
            : isApiError(err) && err.status === 409
              ? DEV_MSG.VW_PROTECTED
              : DEV_MSG.VW_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.VW_NEW
    : detail?.label || detail?.name || idOrName || DEV_MSG.VW_EDIT;

  const fields = detail != null && Array.isArray(detail.fields) ? detail.fields : [];
  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? detail.designGaps
      : [DEV_MSG.VW_GAP_FIELDS, DEV_MSG.VW_GAP_PROTECTED, DEV_MSG.VW_GAP_SEARCHES];

  return (
    <div data-testid="developer-vw-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-vw-back"
        aria-label={DEV_MSG.VW_BACK}
        style={backButton}
      >
        ← {DEV_MSG.VW_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-vw-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-vw-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-vw-detail-loading">{DEV_MSG.VW_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-vw-detail-title">
              {title}
            </h2>
            {!isNew && detail?.description && !dirty ? (
              <p style={{ marginTop: "8px", color: catalogColors.muted }}>{detail.description}</p>
            ) : null}
            {!isNew ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.VW_COL_GUID}</dt>
                <dd
                  style={{ margin: 0, ...monoCell }}
                  data-testid="developer-vw-detail-guid"
                >
                  {objectGuid || "—"}
                </dd>
                <dt>{DEV_MSG.VW_COL_MAX}</dt>
                <dd style={{ margin: 0 }}>
                  {detail?.maximumResultSize != null ? String(detail.maximumResultSize) : "—"}
                </dd>
                <dt>{DEV_MSG.VW_COL_CASE}</dt>
                <dd style={{ margin: 0 }}>
                  {detail?.caseSensitive ? DEV_MSG.YES : DEV_MSG.NO}
                </dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="vw-name">{DEV_MSG.VW_FORM_NAME}</label>
            <input
              id="vw-name"
              data-testid="developer-vw-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.VW_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.VW_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="vw-label">{DEV_MSG.VW_FORM_LABEL}</label>
            <input
              id="vw-label"
              data-testid="developer-vw-label"
              style={inputStyle}
              value={label}
              disabled={busy || protectedWrite}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="vw-desc">{DEV_MSG.VW_FORM_DESCRIPTION}</label>
            <input
              id="vw-desc"
              data-testid="developer-vw-description"
              style={inputStyle}
              value={description}
              disabled={busy || protectedWrite}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="vw-type">{DEV_MSG.VW_FORM_TYPE}</label>
            <select
              id="vw-type"
              data-testid="developer-vw-type"
              style={inputStyle}
              value={type}
              disabled={busy || protectedWrite || !isNew}
              onChange={(e) => setType(e.target.value)}
            >
              <option value={VIEW_TYPE_STANDARD}>{DEV_MSG.VW_KIND_STANDARD}</option>
              {type && type !== VIEW_TYPE_STANDARD ? (
                <option value={type}>
                  {detail?.customView ? DEV_MSG.VW_KIND_CUSTOM : type}
                </option>
              ) : null}
            </select>
          </div>
          <div style={fieldStyle}>
            <label htmlFor="vw-df">{DEV_MSG.VW_FORM_DF}</label>
            <input
              id="vw-df"
              data-testid="developer-vw-display-format"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={displayFormatId}
              disabled={busy || protectedWrite}
              onChange={(e) => setDisplayFormatId(e.target.value)}
              autoComplete="off"
            />
          </div>

          {protectedWrite ? (
            <p
              data-testid="developer-vw-protected-hint"
              style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
            >
              {DEV_MSG.VW_PROTECTED}
            </p>
          ) : null}

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-vw-save"
              aria-label={DEV_MSG.VW_SAVE}
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
              {DEV_MSG.VW_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-vw-cancel"
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
              {DEV_MSG.VW_CANCEL}
            </button>
            {!isNew && writeKey && !protectedWrite ? (
              <button
                type="button"
                data-testid="developer-vw-delete"
                aria-label={DEV_MSG.VW_DELETE}
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
                {DEV_MSG.VW_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section data-testid="developer-vw-fields">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.VW_FIELDS}</h3>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.VW_FIELDS_HINT}
                </p>
                {fields.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-vw-fields-empty">
                    {DEV_MSG.VW_NONE}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-vw-fields-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_FIELD}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_OP}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_VALUE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_FTYPE}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fields.map((f, i) => (
                          <tr
                            key={`${f.fieldName ?? "f"}-${i}`}
                            data-testid={`developer-vw-field-row-${i}`}
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
                objectGuid={objectGuid}
                objectKind="view"
                testIdPrefix="developer-vw-acl"
              />

              <section style={{ marginTop: "16px" }} data-testid="developer-vw-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.VW_GAPS}</h3>
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
