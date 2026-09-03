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

import React, { useEffect, useMemo, useRef, useState } from "react";
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { isApiError } from "../api/client";
import {
  RELATIONSHIP_TYPE_CATEGORIES,
  createRelationshipType,
  deleteRelationshipType,
  getRelationshipTypeDetail,
  isRelationshipTypeWriteReady,
  isSystemRelationshipType,
  isValidRelationshipTypeName,
  normalizeRelationshipTypeName,
  updateRelationshipType,
  type RelationshipTypeWriteBody,
} from "../api/developer/relationshipTypesApi";
import type { RelationshipTypeDef } from "../api/developer/types";
import {
  backButton,
  catalogColors,
  errorAlert,
  monoCell,
  tableHeaderRow,
  tableRow,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
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

function writeErrorFallback(err: unknown, isNew: boolean): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return isNew ? DEV_MSG.RT_DUPLICATE : DEV_MSG.RT_IMMUTABLE;
    }
    if (err.status === 403) {
      return DEV_MSG.RT_FORBIDDEN;
    }
    if (err.status === 400) {
      return DEV_MSG.RT_INVALID;
    }
    if (err.status === 404) {
      return DEV_MSG.RT_NOT_FOUND;
    }
  }
  return isNew ? DEV_MSG.RT_CREATE_ERROR : DEV_MSG.RT_SAVE_ERROR;
}

function deleteErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return DEV_MSG.RT_IMMUTABLE;
    }
    if (err.status === 403) {
      return DEV_MSG.RT_FORBIDDEN;
    }
    if (err.status === 404) {
      return DEV_MSG.RT_NOT_FOUND;
    }
  }
  return DEV_MSG.RT_DELETE_ERROR;
}

export function RelationshipTypeDetailPanel({
  idOrName,
  catalog,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  /** Catalog rows for copy-from select (optional). */
  catalog?: RelationshipTypeDef[];
  onBack: () => void;
  onSaved?: (detail: RelationshipTypeDef) => void | Promise<void>;
  onDeleted?: () => void | Promise<void>;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<RelationshipTypeDef | null>(null);
  const [name, setName] = useState("");
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [copyFrom, setCopyFrom] = useState("");
  const [allowCloning, setAllowCloning] = useState(false);
  const [useOwnerRevision, setUseOwnerRevision] = useState(false);
  const [useDependentRevision, setUseDependentRevision] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setConfirmOpen(false);
    setLoading(true);
    getRelationshipTypeDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setName(d.name || "");
        setLabel(d.label || "");
        setDescription(d.description || "");
        setCategory(d.category || "");
        setCopyFrom("");
        setAllowCloning(Boolean(d.allowCloning));
        setUseOwnerRevision(Boolean(d.useOwnerRevision));
        setUseDependentRevision(Boolean(d.useDependentRevision));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.RT_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const system = isSystemRelationshipType(detail);
  const userWritable = isNew || (detail != null && !system);
  const writeKey = idOrName || createdKey || normalizeRelationshipTypeName(name);

  const canSave =
    userWritable &&
    !busy &&
    isRelationshipTypeWriteReady({
      isNew,
      name,
      category,
      copyFrom,
    });

  const copyFromOptions = useMemo(() => {
    const rows = catalog ?? [];
    return [...rows]
      .filter((t) => (t.name || "").trim().length > 0)
      .sort((a, b) =>
        (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
      );
  }, [catalog]);

  function writeBody(): RelationshipTypeWriteBody {
    const body: RelationshipTypeWriteBody = {
      label: label.trim() || undefined,
      description,
      allowCloning,
      useOwnerRevision,
      useDependentRevision,
    };
    if (isNew) {
      body.name = normalizeRelationshipTypeName(name);
      const cf = normalizeRelationshipTypeName(copyFrom);
      if (cf) {
        body.copyFrom = cf;
      } else if (category.trim()) {
        body.category = category.trim();
      }
    } else {
      if (category.trim()) {
        body.category = category.trim();
      }
      if (detail?.name) {
        body.name = detail.name;
      }
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
          ? await createRelationshipType(writeBody())
          : await updateRelationshipType(writeKey, writeBody());
      setDetail(saved);
      const savedName = saved.name || normalizeRelationshipTypeName(name);
      if (isNew) {
        setCreatedKey(savedName);
      }
      setName(savedName);
      setLabel(saved.label || label);
      setDescription(saved.description || "");
      setCategory(saved.category || category);
      setAllowCloning(Boolean(saved.allowCloning));
      setUseOwnerRevision(Boolean(saved.useOwnerRevision));
      setUseDependentRevision(Boolean(saved.useDependentRevision));
      setNotice(DEV_MSG.RT_SAVED);
      await onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, writeErrorFallback(err, isNew)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !userWritable || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !userWritable || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteRelationshipType(writeKey);
      setNotice(DEV_MSG.RT_DELETED);
      if (onDeleted) {
        await onDeleted();
      } else {
        onBack();
      }
    } catch (err: unknown) {
      setError(panelErrMsg(err, deleteErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const effects = detail != null && Array.isArray(detail.effects) ? detail.effects : [];
  const sysProps =
    detail != null && Array.isArray(detail.systemProperties) ? detail.systemProperties : [];
  const userProps =
    detail != null && Array.isArray(detail.userProperties) ? detail.userProperties : [];
  const gaps =
    detail != null && detail.designGaps && detail.designGaps.length
      ? detail.designGaps
      : [DEV_MSG.RT_GAP_CLONE, DEV_MSG.RT_GAP_EFFECTS];

  const title = isNew
    ? DEV_MSG.RT_NEW
    : detail?.label || detail?.name || idOrName || DEV_MSG.RT_EDIT;

  const fieldsDisabled = busy || !userWritable;

  return (
    <div data-testid="developer-rt-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-rt-back"
        aria-label={DEV_MSG.RT_BACK}
        style={backButton}
      >
        ← {DEV_MSG.RT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-rt-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div
          role="status"
          aria-live="polite"
          data-testid="developer-rt-editor-notice"
          style={{ color: "#276749" }}
        >
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-rt-detail-loading">{DEV_MSG.RT_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-rt-detail-title">
              {title}
            </h2>
            {!isNew && detail?.name ? (
              <div style={{ fontFamily: "monospace", color: catalogColors.muted }}>
                {detail.name}
              </div>
            ) : null}
          </header>

          {system ? (
            <p
              data-testid="developer-rt-system-readonly"
              style={{ color: catalogColors.muted, marginBottom: "12px" }}
            >
              {DEV_MSG.RT_SYSTEM_READONLY}
            </p>
          ) : null}

          <div style={fieldStyle}>
            <label htmlFor="rt-name">{DEV_MSG.RT_FORM_NAME}</label>
            <input
              id="rt-name"
              data-testid="developer-rt-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              readOnly={!isNew}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
              {isNew ? DEV_MSG.RT_NAME_HINT : DEV_MSG.RT_NAME_READONLY}
            </span>
            {isNew && name.trim() && !isValidRelationshipTypeName(name) ? (
              <span
                data-testid="developer-rt-name-invalid"
                style={{ color: "#c53030", fontSize: "0.85rem" }}
              >
                {DEV_MSG.RT_INVALID}
              </span>
            ) : null}
          </div>

          <div style={fieldStyle}>
            <label htmlFor="rt-label">{DEV_MSG.RT_FORM_LABEL}</label>
            <input
              id="rt-label"
              data-testid="developer-rt-label"
              style={inputStyle}
              value={label}
              disabled={fieldsDisabled}
              onChange={(e) => setLabel(e.target.value)}
            />
          </div>

          <div style={fieldStyle}>
            <label htmlFor="rt-desc">{DEV_MSG.RT_FORM_DESCRIPTION}</label>
            <input
              id="rt-desc"
              data-testid="developer-rt-description"
              style={inputStyle}
              value={description}
              disabled={fieldsDisabled}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {isNew ? (
            <div style={fieldStyle}>
              <label htmlFor="rt-copy-from">{DEV_MSG.RT_FORM_COPY_FROM}</label>
              <select
                id="rt-copy-from"
                data-testid="developer-rt-copy-from"
                style={inputStyle}
                value={copyFrom}
                disabled={busy}
                onChange={(e) => {
                  const next = e.target.value;
                  setCopyFrom(next);
                  if (next) {
                    setCategory("");
                  }
                }}
              >
                <option value="">{DEV_MSG.RT_COPY_NONE}</option>
                {copyFromOptions.map((t) => (
                  <option key={t.name} value={t.name}>
                    {t.name}
                    {t.systemType ? ` (${DEV_MSG.RT_FLAG_SYSTEM})` : ""}
                    {t.label && t.label !== t.name ? ` — ${t.label}` : ""}
                  </option>
                ))}
              </select>
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.RT_COPY_FROM_HINT}
              </span>
            </div>
          ) : null}

          <div style={fieldStyle}>
            <label htmlFor="rt-category">{DEV_MSG.RT_FORM_CATEGORY}</label>
            <select
              id="rt-category"
              data-testid="developer-rt-category"
              style={inputStyle}
              value={category}
              disabled={fieldsDisabled || (isNew && copyFrom.trim().length > 0)}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="">{isNew ? DEV_MSG.RT_CATEGORY_REQUIRED : "—"}</option>
              {RELATIONSHIP_TYPE_CATEGORIES.map((c) => (
                <option key={c.code} value={c.code}>
                  {c.label} ({c.code})
                </option>
              ))}
            </select>
          </div>

          {!isNew || !copyFrom.trim() ? (
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "8px",
                marginBottom: "12px",
              }}
            >
              <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <input
                  type="checkbox"
                  data-testid="developer-rt-allow-cloning"
                  checked={allowCloning}
                  disabled={fieldsDisabled}
                  onChange={(e) => setAllowCloning(e.target.checked)}
                />
                {DEV_MSG.RT_FORM_ALLOW_CLONE}
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <input
                  type="checkbox"
                  data-testid="developer-rt-owner-revision"
                  checked={useOwnerRevision}
                  disabled={fieldsDisabled}
                  onChange={(e) => setUseOwnerRevision(e.target.checked)}
                />
                {DEV_MSG.RT_FORM_OWNER_REV}
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <input
                  type="checkbox"
                  data-testid="developer-rt-dependent-revision"
                  checked={useDependentRevision}
                  disabled={fieldsDisabled}
                  onChange={(e) => setUseDependentRevision(e.target.checked)}
                />
                {DEV_MSG.RT_FORM_DEP_REV}
              </label>
            </div>
          ) : null}

          {userWritable ? (
            <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
              <button
                type="button"
                data-testid="developer-rt-save"
                aria-label={DEV_MSG.RT_SAVE}
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
                {DEV_MSG.RT_SAVE}
              </button>
              <button
                type="button"
                data-testid="developer-rt-cancel"
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
                {DEV_MSG.RT_CANCEL}
              </button>
              {!isNew && writeKey ? (
                <button
                  type="button"
                  data-testid="developer-rt-delete"
                  aria-label={DEV_MSG.RT_DELETE}
                  disabled={busy}
                  onClick={requestDelete}
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
                  {DEV_MSG.RT_DELETE}
                </button>
              ) : null}
            </div>
          ) : null}

          {!isNew && detail ? (
            <>
              <section data-testid="developer-rt-effects">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_EFFECTS}</h3>
                {effects.length === 0 ? (
                  <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-rt-effects-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_EFFECT}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_ENDPOINT}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_EXTREF}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {effects.map((e, i) => (
                          <tr key={`${e.name ?? "e"}-${i}`} style={tableRow}>
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {e.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{e.activationEndPoint || "—"}</td>
                            <td style={{ padding: "8px", ...monoCell, fontSize: "0.85rem" }}>
                              {e.extensionRef || "—"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              <section style={{ marginTop: "16px" }} data-testid="developer-rt-sysprops">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_SYS_PROPS}</h3>
                {sysProps.length === 0 ? (
                  <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-rt-sysprops-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_PROP}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_VALUE}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sysProps.map((p, i) => (
                          <tr key={`${p.name ?? "p"}-${i}`} style={tableRow}>
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {p.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{p.value ?? "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              <section style={{ marginTop: "16px" }} data-testid="developer-rt-userprops">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_USER_PROPS}</h3>
                {userProps.length === 0 ? (
                  <p style={{ color: catalogColors.empty }}>{DEV_MSG.RT_NONE}</p>
                ) : (
                  <div style={{ overflowX: "auto" }}>
                    <table
                      data-testid="developer-rt-userprops-table"
                      style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
                    >
                      <thead>
                        <tr style={tableHeaderRow}>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_PROP}</th>
                          <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_VALUE}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {userProps.map((p, i) => (
                          <tr key={`${p.name ?? "up"}-${i}`} style={tableRow}>
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {p.name || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>{p.value ?? "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              <section style={{ marginTop: "16px" }} data-testid="developer-rt-gaps">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.RT_GAPS}</h3>
                <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {gaps.map((g) => (
                    <li key={g}>{g}</li>
                  ))}
                </ul>
              </section>
            </>
          ) : null}
        </>
      ) : null}

      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.RT_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
