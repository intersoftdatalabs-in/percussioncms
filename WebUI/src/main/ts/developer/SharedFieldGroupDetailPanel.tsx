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
import { captureDialogOpener } from "../architecture/useDialogEscape";
import { isApiError } from "../api/client";
import {
  choiceCatalogPayloadError,
  choiceCatalogsEqual,
  cloneChoiceCatalog,
  toChoiceCatalogPayload,
} from "../api/developer/contentTypeChoiceCatalog";
import {
  addSharedField,
  createSharedFieldGroup,
  deleteSharedField,
  deleteSharedFieldGroup,
  getSharedFieldControlProperties,
  getSharedFieldGroupDetail,
  isSharedFieldAddReady,
  isSharedFieldGroupWriteReady,
  normalizeGroupName,
  replaceSharedFieldControlProperties,
  updateSharedFieldGroup,
  type SharedFieldGroupWriteBody,
} from "../api/developer/sharedFieldsApi";
import type {
  ContentTypeChoiceCatalog,
  ContentTypeControlProperty,
  SharedFieldGroupDetail,
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
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import {
  cloneControlProperties,
  controlPropertiesEqual,
  toControlPropertyPayload,
} from "./contentTypeControlProperties";
import { ContentTypeFieldChoicesSection } from "./ContentTypeFieldChoicesSection";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const OCCURRENCE_OPTIONS = ["optional", "required", "oneOrMore", "zeroOrMore", "count"] as const;

const DATA_TYPE_OPTIONS = ["text", "integer", "date", "datetime", "bool", "float", "binary"] as const;

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

function isLockConflict(err: unknown): boolean {
  if (!isApiError(err) || err.status !== 409) return false;
  const msg =
    typeof err.body === "string"
      ? err.body
      : err.body != null && typeof err.body === "object"
        ? JSON.stringify(err.body)
        : "";
  if (/already exists|duplicate/i.test(msg)) {
    return false;
  }
  return /design lock|locked by|lock required/i.test(msg);
}

function writeFallback(
  err: unknown,
  duplicate: boolean,
  add: boolean,
  fallback: string,
): string {
  if (isApiError(err) && err.status === 409) {
    if (isLockConflict(err)) {
      return DEV_MSG.SF_LOCK;
    }
    if (duplicate) {
      return DEV_MSG.SF_FIELD_DUPLICATE;
    }
  }
  if (isApiError(err) && err.status === 400 && add) {
    return DEV_MSG.SF_INVALID_FIELD_NAME;
  }
  return fallback;
}

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
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingDeleteField, setPendingDeleteField] = useState<string | null>(null);
  const [loading, setLoading] = useState(name != null);
  const [newName, setNewName] = useState("");
  const [newDataType, setNewDataType] = useState("text");
  const [newOccurrence, setNewOccurrence] = useState("optional");
  const [selectedFieldName, setSelectedFieldName] = useState("");
  const [controlName, setControlName] = useState<string | null>(null);
  const [controlProps, setControlProps] = useState<ContentTypeControlProperty[]>([]);
  const [controlPropsInitial, setControlPropsInitial] = useState<
    ContentTypeControlProperty[]
  >([]);
  const [choiceCatalog, setChoiceCatalog] = useState<ContentTypeChoiceCatalog | null>(null);
  const [choiceCatalogInitial, setChoiceCatalogInitial] =
    useState<ContentTypeChoiceCatalog | null>(null);
  const [controlPropsLoading, setControlPropsLoading] = useState(false);
  const [controlPropsError, setControlPropsError] = useState<string | null>(null);
  const [newPropName, setNewPropName] = useState("");
  const [newPropValue, setNewPropValue] = useState("");
  const inflight = useRef(false);

  function applyDetail(d: SharedFieldGroupDetail): void {
    setDetail(d);
    const names = (d.fields || []).map((f) => f.name).filter((n): n is string => !!n);
    setSelectedFieldName((prev) => {
      if (prev && names.includes(prev)) {
        return prev;
      }
      return names[0] || "";
    });
  }

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
        applyDetail(d);
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
  const canAdd = !isNew && !!writeKey && !busy && isSharedFieldAddReady(newName);
  const controlPropsDirty = !controlPropertiesEqual(controlProps, controlPropsInitial);
  const choicesDirty = !choiceCatalogsEqual(choiceCatalog, choiceCatalogInitial);
  const canSaveControl =
    !busy &&
    !controlPropsLoading &&
    !!writeKey &&
    !!selectedFieldName &&
    (controlPropsDirty || choicesDirty);

  useEffect(() => {
    if (isNew || !writeKey || !selectedFieldName) {
      setControlProps([]);
      setControlPropsInitial([]);
      setChoiceCatalog(null);
      setChoiceCatalogInitial(null);
      setControlName(null);
      setControlPropsError(null);
      setControlPropsLoading(false);
      return;
    }
    let cancelled = false;
    setControlPropsLoading(true);
    setControlPropsError(null);
    getSharedFieldControlProperties(writeKey, selectedFieldName)
      .then((loaded) => {
        if (cancelled) return;
        const props = cloneControlProperties(loaded.properties);
        setControlProps(props);
        setControlPropsInitial(cloneControlProperties(props));
        const choices = cloneChoiceCatalog(loaded.choices);
        setChoiceCatalog(choices);
        setChoiceCatalogInitial(cloneChoiceCatalog(choices));
        setControlName(loaded.control || null);
        setNewPropName("");
        setNewPropValue("");
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setControlProps([]);
        setControlPropsInitial([]);
        setChoiceCatalog(null);
        setChoiceCatalogInitial(null);
        setControlName(null);
        setControlPropsError(panelErrMsg(e, DEV_MSG.SF_CONTROL_PROPS_ERROR));
      })
      .finally(() => {
        if (!cancelled) setControlPropsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isNew, writeKey, selectedFieldName]);

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
      applyDetail(saved);
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

  async function handleAdd(): Promise<void> {
    if (!canAdd || inflight.current || !writeKey) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await addSharedField(writeKey, {
        name: newName.trim(),
        dataType: newDataType,
        occurrence: newOccurrence,
      });
      applyDetail(saved);
      setSelectedFieldName(newName.trim());
      setNewName("");
      setNewDataType("text");
      setNewOccurrence("optional");
      setNotice(DEV_MSG.SF_ADDED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, writeFallback(err, true, true, DEV_MSG.SF_ADD_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function setControlPropValue(index: number, value: string): void {
    setControlProps((prev) => prev.map((p, i) => (i === index ? { ...p, value } : p)));
  }

  function removeControlProp(index: number): void {
    setControlProps((prev) => prev.filter((_, i) => i !== index));
  }

  function addControlProp(): void {
    const propName = newPropName.trim();
    if (!propName) return;
    if (controlProps.some((p) => (p.name || "").toLowerCase() === propName.toLowerCase())) {
      return;
    }
    setControlProps((prev) => [...prev, { name: propName, value: newPropValue }]);
    setNewPropName("");
    setNewPropValue("");
  }

  async function handleSaveControlProperties(): Promise<void> {
    if (!canSaveControl || inflight.current || !writeKey || !selectedFieldName) return;
    if (choicesDirty) {
      const payloadErr = choiceCatalogPayloadError(choiceCatalog);
      if (payloadErr === "local-entries") {
        setError(DEV_MSG.CT_CHOICES_INVALID_LOCAL);
        return;
      }
      if (payloadErr === "global-id") {
        setError(DEV_MSG.CT_CHOICES_INVALID_GLOBAL);
        return;
      }
      if (payloadErr === "lookup-href") {
        setError(DEV_MSG.CT_CHOICES_INVALID_LOOKUP);
        return;
      }
      if (payloadErr === "table") {
        setError(DEV_MSG.CT_CHOICES_INVALID_TABLE);
        return;
      }
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const body: {
        properties: ContentTypeControlProperty[];
        choices?: ContentTypeChoiceCatalog;
      } = { properties: toControlPropertyPayload(controlProps) };
      if (choicesDirty) {
        body.choices = toChoiceCatalogPayload(choiceCatalog);
      }
      const saved = await replaceSharedFieldControlProperties(
        writeKey,
        selectedFieldName,
        body,
      );
      const nextProps = cloneControlProperties(saved.properties);
      setControlProps(nextProps);
      setControlPropsInitial(cloneControlProperties(nextProps));
      const nextChoices = cloneChoiceCatalog(saved.choices ?? choiceCatalog);
      setChoiceCatalog(nextChoices);
      setChoiceCatalogInitial(cloneChoiceCatalog(nextChoices));
      setControlName(saved.control || null);
      setNotice(DEV_MSG.SF_CONTROL_PROPS_SAVED);
    } catch (err: unknown) {
      setError(
        panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SF_CONTROL_PROPS_SAVE_ERROR)),
      );
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setPendingDeleteField(null);
    setConfirmOpen(true);
  }

  function requestDeleteField(ev: React.MouseEvent<HTMLElement>, fieldName: string): void {
    if (!fieldName || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(false);
    setPendingDeleteField(fieldName);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    setConfirmOpen(false);
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

  async function handleDeleteField(): Promise<void> {
    const fieldName = pendingDeleteField;
    if (!fieldName || !writeKey || inflight.current) return;
    setPendingDeleteField(null);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteSharedField(writeKey, fieldName);
      const saved = await getSharedFieldGroupDetail(writeKey);
      applyDetail(saved);
      setNotice(DEV_MSG.SF_FIELD_DELETED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SF_FIELD_DELETE_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const title = isNew
    ? DEV_MSG.SF_NEW
    : detail?.name || name || DEV_MSG.SF_EDIT;
  const fields = detail?.fields || [];
  const confirmOpenAny = confirmOpen || pendingDeleteField != null;

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
                {DEV_MSG.SF_DELETE}
              </button>
            ) : null}
          </div>

          {detail ? (
            <>
              <section
                data-testid="developer-sf-add"
                style={{
                  marginBottom: "16px",
                  padding: "12px",
                  border: `1px solid ${catalogColors.headerBorder}`,
                  borderRadius: "4px",
                }}
              >
                <h3 style={{ fontSize: "1rem", marginTop: 0 }}>{DEV_MSG.SF_ADD}</h3>
                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "12px",
                    alignItems: "flex-end",
                  }}
                >
                  <div style={{ ...fieldStyle, marginBottom: 0 }}>
                    <label htmlFor="sf-new-name">{DEV_MSG.SF_NEW_NAME}</label>
                    <input
                      id="sf-new-name"
                      data-testid="developer-sf-new-name"
                      style={{ ...inputStyle, fontFamily: "monospace" }}
                      value={newName}
                      disabled={busy}
                      onChange={(e) => setNewName(e.target.value)}
                      autoComplete="off"
                    />
                  </div>
                  <div style={{ ...fieldStyle, marginBottom: 0 }}>
                    <label htmlFor="sf-new-datatype">{DEV_MSG.SF_NEW_DATATYPE}</label>
                    <select
                      id="sf-new-datatype"
                      data-testid="developer-sf-new-datatype"
                      style={inputStyle}
                      value={newDataType}
                      disabled={busy}
                      onChange={(e) => setNewDataType(e.target.value)}
                    >
                      {DATA_TYPE_OPTIONS.map((dt) => (
                        <option key={dt} value={dt}>
                          {dt}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div style={{ ...fieldStyle, marginBottom: 0 }}>
                    <label htmlFor="sf-new-occurrence">{DEV_MSG.SF_NEW_OCCURRENCE}</label>
                    <select
                      id="sf-new-occurrence"
                      data-testid="developer-sf-new-occurrence"
                      style={inputStyle}
                      value={newOccurrence}
                      disabled={busy}
                      onChange={(e) => setNewOccurrence(e.target.value)}
                    >
                      {OCCURRENCE_OPTIONS.map((opt) => (
                        <option key={opt} value={opt}>
                          {opt}
                        </option>
                      ))}
                    </select>
                  </div>
                  <button
                    type="button"
                    data-testid="developer-sf-add-btn"
                    aria-label={DEV_MSG.SF_ADD}
                    disabled={!canAdd}
                    onClick={() => void handleAdd()}
                    style={{
                      padding: "8px 16px",
                      background: canAdd ? catalogColors.accent : catalogColors.disabled,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: canAdd ? "pointer" : "not-allowed",
                    }}
                  >
                    {DEV_MSG.SF_ADD}
                  </button>
                </div>
              </section>

              <section style={{ marginBottom: "16px" }} data-testid="developer-sf-fields">
                <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SF_FIELDS}</h3>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.SF_FIELDS_HINT}
                </p>
                {fields.length === 0 ? (
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
                          <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_ACTIONS}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {fields.map((f, i) => (
                          <tr
                            key={f.name || `f-${i}`}
                            data-testid={`developer-sf-field-row-${i}`}
                            data-sf-field={f.name || undefined}
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
                            <td style={{ padding: "8px" }}>
                              <button
                                type="button"
                                data-testid="developer-sf-field-delete"
                                aria-label={`${DEV_MSG.SF_FIELD_DELETE} ${f.name || ""}`}
                                disabled={busy || !f.name}
                                onClick={(ev) => requestDeleteField(ev, f.name || "")}
                                style={{
                                  padding: "4px 10px",
                                  background: "#c53030",
                                  color: "#fff",
                                  border: "none",
                                  borderRadius: "4px",
                                  cursor: busy || !f.name ? "not-allowed" : "pointer",
                                }}
                              >
                                {DEV_MSG.SF_FIELD_DELETE}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>

              <section
                data-testid="developer-sf-control-props"
                style={{
                  marginBottom: "16px",
                  padding: "12px",
                  border: `1px solid ${catalogColors.headerBorder}`,
                  borderRadius: "4px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: "12px",
                    flexWrap: "wrap",
                  }}
                >
                  <h3 style={{ fontSize: "1rem", marginTop: 0 }}>{DEV_MSG.SF_CONTROL_PROPS}</h3>
                  <button
                    type="button"
                    data-testid="developer-sf-cp-save"
                    aria-label={DEV_MSG.SF_CONTROL_PROPS_SAVE}
                    disabled={!canSaveControl}
                    onClick={() => void handleSaveControlProperties()}
                    style={{
                      padding: "8px 16px",
                      background: canSaveControl ? catalogColors.accent : catalogColors.disabled,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: canSaveControl ? "pointer" : "not-allowed",
                    }}
                  >
                    {DEV_MSG.SF_CONTROL_PROPS_SAVE}
                  </button>
                </div>
                <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                  {DEV_MSG.SF_CONTROL_PROPS_HINT}
                </p>
                <div style={{ marginBottom: "12px" }}>
                  <label htmlFor="sf-cp-field" style={{ display: "block", marginBottom: 4 }}>
                    {DEV_MSG.SF_CONTROL_PROPS_FIELD}
                  </label>
                  <select
                    id="sf-cp-field"
                    data-testid="developer-sf-cp-field"
                    aria-label={DEV_MSG.SF_CONTROL_PROPS_FIELD}
                    style={inputStyle}
                    value={selectedFieldName}
                    disabled={busy || controlPropsDirty || choicesDirty}
                    onChange={(e) => {
                      if (busy || controlPropsDirty || choicesDirty) return;
                      setSelectedFieldName(e.target.value);
                      setNotice(null);
                    }}
                  >
                    {fields.filter((f) => !!f.name).length === 0 ? (
                      <option value="">{DEV_MSG.SF_CONTROL_PROPS_NO_FIELD}</option>
                    ) : (
                      fields
                        .filter((f) => !!f.name)
                        .map((f) => (
                          <option key={f.name} value={f.name}>
                            {f.name}
                          </option>
                        ))
                    )}
                  </select>
                </div>
                {controlName ? (
                  <p
                    data-testid="developer-sf-cp-control"
                    style={{
                      color: catalogColors.muted,
                      fontSize: "0.9rem",
                      fontFamily: "monospace",
                    }}
                  >
                    {DEV_MSG.SF_CONTROL_PROPS_CONTROL}: {controlName}
                  </p>
                ) : null}
                {controlPropsError ? (
                  <p
                    role="status"
                    data-testid="developer-sf-cp-error"
                    style={{ color: catalogColors.error }}
                  >
                    {controlPropsError}
                  </p>
                ) : null}
                {controlPropsLoading ? (
                  <p data-testid="developer-sf-cp-loading" style={{ color: catalogColors.muted }}>
                    {DEV_MSG.SF_CONTROL_PROPS_LOADING}
                  </p>
                ) : null}
                {!controlPropsLoading && selectedFieldName && controlProps.length === 0 ? (
                  <p style={{ color: catalogColors.empty }} data-testid="developer-sf-cp-empty">
                    {DEV_MSG.SF_CONTROL_PROPS_EMPTY}
                  </p>
                ) : null}
                {!controlPropsLoading && controlProps.length > 0 ? (
                  <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                    {controlProps.map((p, i) => (
                      <li
                        key={`${p.name || "prop"}-${i}`}
                        data-testid={`developer-sf-cp-row-${i}`}
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr auto",
                          gap: 8,
                          alignItems: "center",
                          padding: "6px 0",
                        }}
                      >
                        <span
                          data-testid={`developer-sf-cp-name-${i}`}
                          style={{ fontFamily: "monospace" }}
                        >
                          {p.name}
                        </span>
                        <input
                          type="text"
                          data-testid={`developer-sf-cp-value-${i}`}
                          aria-label={`${DEV_MSG.SF_CONTROL_PROPS_VALUE} ${p.name || i}`}
                          style={inputStyle}
                          value={p.value || ""}
                          disabled={busy}
                          onChange={(e) => setControlPropValue(i, e.target.value)}
                        />
                        <button
                          type="button"
                          data-testid={`developer-sf-cp-remove-${i}`}
                          aria-label={`${DEV_MSG.SF_CONTROL_PROPS_REMOVE} ${p.name || i}`}
                          disabled={busy}
                          onClick={() => removeControlProp(i)}
                          style={{
                            padding: "4px 10px",
                            background: "#c53030",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: busy ? "not-allowed" : "pointer",
                          }}
                        >
                          {DEV_MSG.SF_CONTROL_PROPS_REMOVE}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
                <div
                  style={{
                    marginTop: "12px",
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr auto",
                    gap: "8px",
                    alignItems: "end",
                  }}
                >
                  <div>
                    <label htmlFor="sf-cp-add-name" style={{ display: "block", marginBottom: 4 }}>
                      {DEV_MSG.SF_CONTROL_PROPS_NAME}
                    </label>
                    <input
                      id="sf-cp-add-name"
                      type="text"
                      autoComplete="off"
                      data-testid="developer-sf-cp-add-name"
                      style={inputStyle}
                      placeholder={DEV_MSG.SF_CONTROL_PROPS_NAME_PLACEHOLDER}
                      value={newPropName}
                      disabled={busy || !selectedFieldName}
                      onChange={(e) => setNewPropName(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          addControlProp();
                        }
                      }}
                    />
                  </div>
                  <div>
                    <label htmlFor="sf-cp-add-value" style={{ display: "block", marginBottom: 4 }}>
                      {DEV_MSG.SF_CONTROL_PROPS_VALUE}
                    </label>
                    <input
                      id="sf-cp-add-value"
                      type="text"
                      autoComplete="off"
                      data-testid="developer-sf-cp-add-value"
                      style={inputStyle}
                      placeholder={DEV_MSG.SF_CONTROL_PROPS_VALUE_PLACEHOLDER}
                      value={newPropValue}
                      disabled={busy || !selectedFieldName}
                      onChange={(e) => setNewPropValue(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          addControlProp();
                        }
                      }}
                    />
                  </div>
                  <button
                    type="button"
                    data-testid="developer-sf-cp-add"
                    disabled={busy || !selectedFieldName || !newPropName.trim()}
                    onClick={addControlProp}
                    style={{
                      padding: "8px 16px",
                      background:
                        busy || !selectedFieldName || !newPropName.trim()
                          ? catalogColors.disabled
                          : catalogColors.accent,
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor:
                        busy || !selectedFieldName || !newPropName.trim()
                          ? "not-allowed"
                          : "pointer",
                    }}
                  >
                    {DEV_MSG.SF_CONTROL_PROPS_ADD}
                  </button>
                </div>
                <ContentTypeFieldChoicesSection
                  catalog={choiceCatalog}
                  canEdit={!busy && !!selectedFieldName && !controlPropsLoading}
                  onChange={setChoiceCatalog}
                />
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
      <CatalogConfirmDialog
        open={confirmOpenAny}
        busy={busy}
        message={
          pendingDeleteField != null
            ? DEV_MSG.SF_FIELD_DELETE_CONFIRM
            : DEV_MSG.SF_DELETE_CONFIRM
        }
        onCancel={() => {
          setConfirmOpen(false);
          setPendingDeleteField(null);
        }}
        onConfirm={() =>
          pendingDeleteField != null ? void handleDeleteField() : void handleDelete()
        }
      />
    </div>
  );
}
