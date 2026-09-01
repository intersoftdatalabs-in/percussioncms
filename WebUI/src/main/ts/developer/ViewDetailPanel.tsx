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
import type { ViewDef, ViewFieldSummary } from "../api/developer/types";
import {
  addViewFieldCriterion,
  catalogViewFieldsNotInUse,
  isKnownViewFieldName,
  isValidViewFieldName,
  moveViewFieldCriterion,
  normalizeViewFields,
  patchViewFieldCriterion,
  removeViewFieldCriterion,
  viewFieldsEqual,
  VIEW_FIELD_OPERATORS,
  VIEW_FIELD_TYPES,
} from "./viewFieldCriteria";
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

const actionButton: React.CSSProperties = {
  padding: "4px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
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
  const [draftFields, setDraftFields] = useState<ViewFieldSummary[]>([]);
  const [addFieldName, setAddFieldName] = useState("");
  const [addOperator, setAddOperator] = useState("equal");
  const [addValue, setAddValue] = useState("");
  const [addType, setAddType] = useState("Text");
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setDraftFields([]);
    setAddFieldName("");
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
        const nextFields = normalizeViewFields(d.fields);
        setDraftFields(nextFields);
        setAddFieldName(catalogViewFieldsNotInUse(nextFields)[0]?.source || "");
        setAddOperator("equal");
        setAddValue("");
        setAddType("Text");
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
  const objectGuid = resolveViewObjectGuid(detail, catalogGuid);
  const writeKey = objectGuid || idOrName || createdKey || normalizeViewName(name);
  const loadedFields = useMemo(
    () => (detail != null ? normalizeViewFields(detail.fields) : []),
    [detail],
  );
  const fieldsEditable = !protectedWrite && !isNew && detail != null;
  const fields = fieldsEditable ? draftFields : loadedFields;
  const availableFields = catalogViewFieldsNotInUse(draftFields);
  const fieldsDirty = fieldsEditable && !viewFieldsEqual(draftFields, loadedFields);
  const canSaveFields = !busy && fieldsDirty;

  function writeBody(): ViewWriteBody {
    const df = displayFormatId == null ? "" : String(displayFormatId);
    const body: ViewWriteBody = {
      name: isNew ? normalizeViewName(name) : detail?.name || normalizeViewName(name),
      label: label == null ? "" : String(label),
      description: description == null ? "" : String(description),
      type: type == null ? "" : String(type),
    };
    if (df.trim()) {
      body.displayFormatId = df.trim();
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

  function fieldsSaveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 400) return DEV_MSG.VW_FIELDS_INVALID;
    if (isApiError(err) && err.status === 403) return DEV_MSG.VW_FIELDS_FORBIDDEN;
    if (isApiError(err) && err.status === 409) return DEV_MSG.VW_PROTECTED;
    if (isApiError(err) && err.status === 404) return DEV_MSG.VW_NOT_FOUND;
    return DEV_MSG.VW_FIELDS_SAVE_ERROR;
  }

  function handleAddField(): void {
    if (!isValidViewFieldName(addFieldName) || !isKnownViewFieldName(addFieldName)) {
      return;
    }
    const next = addViewFieldCriterion(draftFields, addFieldName, addOperator, addValue, addType);
    setDraftFields(next);
    setAddFieldName(catalogViewFieldsNotInUse(next)[0]?.source || "");
    setAddValue("");
  }

  async function handleSaveFields(): Promise<void> {
    if (!canSaveFields || inflight.current || !writeKey) return;
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await saveView(writeKey, {
        ...writeBody(),
        fields: normalizeViewFields(draftFields),
      });
      setDetail(saved);
      const nextFields = normalizeViewFields(saved.fields);
      setDraftFields(nextFields);
      setAddFieldName(catalogViewFieldsNotInUse(nextFields)[0]?.source || "");
      setNotice(DEV_MSG.VW_FIELDS_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, fieldsSaveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
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
      const nextFields = normalizeViewFields(saved.fields);
      setDraftFields(nextFields);
      setAddFieldName(catalogViewFieldsNotInUse(nextFields)[0]?.source || "");
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

  const gapList =
    detail != null && detail.designGaps && detail.designGaps.length > 0
      ? detail.designGaps
      : [DEV_MSG.VW_GAP_PROTECTED, DEV_MSG.VW_GAP_SEARCHES];

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
                <p
                  style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
                  data-testid={protectedWrite ? "developer-vw-fields-readonly" : undefined}
                >
                  {protectedWrite ? DEV_MSG.VW_FIELDS_READONLY : DEV_MSG.VW_FIELDS_HINT}
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
                          {fieldsEditable ? <th style={{ padding: "8px" }} /> : null}
                        </tr>
                      </thead>
                      <tbody>
                        {fields.map((f, i) => (
                          <tr
                            key={`${f.fieldName ?? "f"}-${i}`}
                            data-testid={`developer-vw-field-row-${i}`}
                            data-vw-field-name={f.fieldName || ""}
                            style={tableRow}
                          >
                            <td style={{ padding: "8px", fontFamily: "monospace" }}>
                              {f.fieldName || f.displayName || "—"}
                            </td>
                            <td style={{ padding: "8px" }}>
                              {fieldsEditable ? (
                                <select
                                  data-testid={`developer-vw-field-op-${i}`}
                                  style={inputStyle}
                                  value={f.operator || "equal"}
                                  disabled={busy}
                                  onChange={(e) =>
                                    setDraftFields(
                                      patchViewFieldCriterion(draftFields, i, {
                                        operator: e.target.value,
                                      }),
                                    )
                                  }
                                >
                                  {VIEW_FIELD_OPERATORS.map((op) => (
                                    <option key={op.value} value={op.value}>
                                      {op.label}
                                    </option>
                                  ))}
                                </select>
                              ) : (
                                f.operator || "—"
                              )}
                            </td>
                            <td style={{ padding: "8px" }}>
                              {fieldsEditable ? (
                                <input
                                  data-testid={`developer-vw-field-value-${i}`}
                                  style={inputStyle}
                                  value={f.fieldValue || ""}
                                  disabled={busy}
                                  onChange={(e) =>
                                    setDraftFields(
                                      patchViewFieldCriterion(draftFields, i, {
                                        fieldValue: e.target.value,
                                      }),
                                    )
                                  }
                                />
                              ) : (
                                f.fieldValue || "—"
                              )}
                            </td>
                            <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                            {fieldsEditable ? (
                              <td style={{ padding: "8px" }}>
                                <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
                                  <button
                                    type="button"
                                    data-testid={`developer-vw-field-up-${i}`}
                                    aria-label={DEV_MSG.VW_FIELDS_MOVE_UP}
                                    disabled={busy || i === 0}
                                    onClick={() =>
                                      setDraftFields(moveViewFieldCriterion(draftFields, i, -1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.VW_FIELDS_MOVE_UP}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-vw-field-down-${i}`}
                                    aria-label={DEV_MSG.VW_FIELDS_MOVE_DOWN}
                                    disabled={busy || i === fields.length - 1}
                                    onClick={() =>
                                      setDraftFields(moveViewFieldCriterion(draftFields, i, 1))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.VW_FIELDS_MOVE_DOWN}
                                  </button>
                                  <button
                                    type="button"
                                    data-testid={`developer-vw-field-remove-${i}`}
                                    aria-label={DEV_MSG.VW_FIELDS_REMOVE}
                                    disabled={busy}
                                    onClick={() =>
                                      setDraftFields(removeViewFieldCriterion(draftFields, i))
                                    }
                                    style={actionButton}
                                  >
                                    {DEV_MSG.VW_FIELDS_REMOVE}
                                  </button>
                                </div>
                              </td>
                            ) : null}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                {fieldsEditable ? (
                  <div
                    style={{
                      marginTop: "12px",
                      display: "flex",
                      gap: "8px",
                      flexWrap: "wrap",
                      alignItems: "flex-end",
                    }}
                    data-testid="developer-vw-field-editor"
                  >
                    <label
                      htmlFor="vw-field-source"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.VW_FIELDS_SOURCE_PICKER}
                      <select
                        id="vw-field-source"
                        data-testid="developer-vw-field-source"
                        style={inputStyle}
                        value={addFieldName}
                        disabled={busy || availableFields.length === 0}
                        onChange={(e) => setAddFieldName(e.target.value)}
                      >
                        <option value="">{availableFields.length ? "—" : DEV_MSG.VW_NONE}</option>
                        {availableFields.map((f) => (
                          <option key={f.source} value={f.source}>
                            {f.label} ({f.source})
                          </option>
                        ))}
                      </select>
                    </label>
                    <label
                      htmlFor="vw-field-add-op"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.VW_COL_OP}
                      <select
                        id="vw-field-add-op"
                        data-testid="developer-vw-field-add-op"
                        style={inputStyle}
                        value={addOperator}
                        disabled={busy}
                        onChange={(e) => setAddOperator(e.target.value)}
                      >
                        {VIEW_FIELD_OPERATORS.map((op) => (
                          <option key={op.value} value={op.value}>
                            {op.label}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label
                      htmlFor="vw-field-add-value"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.VW_COL_VALUE}
                      <input
                        id="vw-field-add-value"
                        data-testid="developer-vw-field-add-value"
                        style={inputStyle}
                        value={addValue}
                        disabled={busy}
                        onChange={(e) => setAddValue(e.target.value)}
                      />
                    </label>
                    <label
                      htmlFor="vw-field-add-type"
                      style={{ display: "flex", flexDirection: "column", gap: "4px" }}
                    >
                      {DEV_MSG.VW_COL_FTYPE}
                      <select
                        id="vw-field-add-type"
                        data-testid="developer-vw-field-add-type"
                        style={inputStyle}
                        value={addType}
                        disabled={busy}
                        onChange={(e) => setAddType(e.target.value)}
                      >
                        {VIEW_FIELD_TYPES.map((t) => (
                          <option key={t} value={t}>
                            {t}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-vw-field-add"
                      aria-label={DEV_MSG.VW_FIELDS_ADD}
                      disabled={busy || !isValidViewFieldName(addFieldName)}
                      onClick={handleAddField}
                      style={{
                        ...actionButton,
                        padding: "8px 12px",
                        background: isValidViewFieldName(addFieldName)
                          ? catalogColors.accent
                          : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        cursor:
                          isValidViewFieldName(addFieldName) && !busy ? "pointer" : "not-allowed",
                      }}
                    >
                      {DEV_MSG.VW_FIELDS_ADD}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-vw-fields-save"
                      aria-label={DEV_MSG.VW_FIELDS_SAVE}
                      disabled={!canSaveFields}
                      onClick={() => void handleSaveFields()}
                      style={{
                        padding: "8px 16px",
                        background: canSaveFields ? catalogColors.accent : catalogColors.disabled,
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: canSaveFields ? "pointer" : "not-allowed",
                      }}
                    >
                      {busy ? DEV_MSG.VW_FIELDS_SAVING : DEV_MSG.VW_FIELDS_SAVE}
                    </button>
                  </div>
                ) : null}
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
