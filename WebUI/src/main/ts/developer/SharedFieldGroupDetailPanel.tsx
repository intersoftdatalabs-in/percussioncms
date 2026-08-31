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
import {
  createSharedFieldGroup,
  deleteSharedFieldGroup,
  getSharedFieldGroupDetail,
  isSharedFieldGroupWriteReady,
  normalizeGroupName,
  updateSharedFieldGroup,
  type SharedFieldGroupWriteBody,
} from "../api/developer/sharedFieldsApi";
import type { SharedFieldGroupDetail } from "../api/developer/types";
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

export function SharedFieldGroupDetailPanel({
  name,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  name: string | null;
  onBack: () => void;
  onSaved?: (detail: SharedFieldGroupDetail) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [pathKey, setPathKey] = useState<string | null>(name);
  const isNew = name == null && pathKey == null;
  const [detail, setDetail] = useState<SharedFieldGroupDetail | null>(null);
  const [groupName, setGroupName] = useState("");
  const [filename, setFilename] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(name != null);
  const inflight = useRef(false);

  useEffect(() => {
    setPathKey(name);
    if (name == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setLoading(true);
    getSharedFieldGroupDetail(name)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setGroupName(d.name || name);
        setFilename(d.filename || "");
        setPathKey(d.name || name);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.SF_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const loadedName = normalizeGroupName(detail?.name || name || "");
  const loadedFilename = (detail?.filename || "").trim();
  const dirty =
    isNew ||
    normalizeGroupName(groupName) !== loadedName ||
    filename.trim() !== loadedFilename;
  const canSave =
    !busy &&
    dirty &&
    isSharedFieldGroupWriteReady({ name: groupName, filename });
  const writeKey = pathKey || normalizeGroupName(groupName);

  function writeBody(): SharedFieldGroupWriteBody {
    const body: SharedFieldGroupWriteBody = {
      name: normalizeGroupName(groupName),
    };
    const fn = filename.trim();
    if (fn) {
      body.filename = fn;
    }
    return body;
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
          ? await createSharedFieldGroup(writeBody())
          : await updateSharedFieldGroup(writeKey, writeBody());
      setDetail(saved);
      const savedName = saved.name || normalizeGroupName(groupName);
      setPathKey(savedName);
      setGroupName(savedName);
      setFilename(saved.filename || filename);
      setNotice(DEV_MSG.SF_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 409 && isNew
          ? DEV_MSG.SF_DUPLICATE
          : DEV_MSG.SF_SAVE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    if (!window.confirm(DEV_MSG.SF_DELETE_CONFIRM)) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteSharedFieldGroup(writeKey);
      setNotice(DEV_MSG.SF_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.SF_DELETE_ERROR));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.SF_NEW
    : detail?.name || name || DEV_MSG.SF_EDIT;

  return (
    <div data-testid="developer-sf-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-sf-back"
        aria-label={DEV_MSG.SF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.SF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-sf-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-sf-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-sf-detail-loading">{DEV_MSG.SF_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-sf-detail-title">
              {title}
            </h2>
            {!isNew && detail?.filename ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.SF_COL_FILENAME}</dt>
                <dd style={{ margin: 0, ...monoCell }}>{detail.filename}</dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="sf-name">{DEV_MSG.SF_FORM_NAME}</label>
            <input
              id="sf-name"
              data-testid="developer-sf-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={groupName}
              disabled={busy}
              onChange={(e) => setGroupName(e.target.value)}
              autoComplete="off"
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sf-filename">{DEV_MSG.SF_FORM_FILENAME}</label>
            <input
              id="sf-filename"
              data-testid="developer-sf-filename"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={filename}
              disabled={busy}
              onChange={(e) => setFilename(e.target.value)}
              autoComplete="off"
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {DEV_MSG.SF_FILENAME_HINT}
            </span>
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-sf-save"
              aria-label={DEV_MSG.SF_SAVE}
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
              {DEV_MSG.SF_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-sf-cancel"
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
              {DEV_MSG.SF_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-sf-delete"
                aria-label={DEV_MSG.SF_DELETE}
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
                {DEV_MSG.SF_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section style={{ marginBottom: "16px" }} data-testid="developer-sf-fields">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SF_FIELDS}</h3>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.SF_FIELDS_HINT}
                </p>
                {(detail.fields || []).length === 0 ? (
                  <p
                    style={{ color: catalogColors.empty }}
                    data-testid="developer-sf-fields-empty"
                  >
                    {DEV_MSG.SF_NONE}
                  </p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-sf-fields-table"
                      style={{
                        width: "100%",
                        borderCollapse: "collapse",
                        fontSize: "0.9rem",
                      }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_FIELD}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_DATATYPE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_OCCURRENCE}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_REQUIRED}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_SEARCH}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_READONLY}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(detail.fields || []).map((f, i) => (
                          <tr
                            key={f.name || `f-${i}`}
                            data-testid={`developer-sf-field-row-${i}`}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {f.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{f.dataType || "—"}</td>
                            <td style={{ padding: "8px" }}>{f.occurrence || "—"}</td>
                            <td style={{ padding: "8px" }}>
                              {f.required == null
                                ? "—"
                                : f.required
                                  ? DEV_MSG.YES
                                  : DEV_MSG.NO}
                            </td>
                            <td style={{ padding: "8px" }}>
                              {f.searchable == null
                                ? "—"
                                : f.searchable
                                  ? DEV_MSG.YES
                                  : DEV_MSG.NO}
                            </td>
                            <td style={{ padding: "8px" }}>
                              {f.readOnly == null
                                ? "—"
                                : f.readOnly
                                  ? DEV_MSG.YES
                                  : DEV_MSG.NO}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              {detail.designGaps && detail.designGaps.length > 0 ? (
                <section data-testid="developer-sf-gaps">
                  <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SF_GAPS}</h3>
                  <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                    {detail.designGaps.map((g) => (
                      <li key={g}>{g}</li>
                    ))}
                  </ul>
                </section>
              ) : null}
            </>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
