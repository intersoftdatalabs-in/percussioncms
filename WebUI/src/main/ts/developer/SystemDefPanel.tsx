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
  addSystemDefField,
  deleteSystemDefField,
  getSystemDef,
  getSystemDefFieldControlProperties,
  getSystemDefStylesheets,
  isSystemDefFieldAddReady,
  isValidSystemDefCommandHandler,
  isValidSystemDefStylesheetHref,
  replaceSystemDefFieldControlProperties,
  replaceSystemDefStylesheets,
  updateSystemDef,
  type SystemDefFieldPatch,
} from "../api/developer/systemDefApi";
import type {
  ContentTypeControlProperty,
  SystemDefCommandHandlerStylesheet,
  SystemDefDetail,
  SystemDefFieldSummary,
} from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import {
  catalogColors,
  errorAlert,
  metaGrid,
  monoCell,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import {
  cloneControlProperties,
  controlPropertiesEqual,
  toControlPropertyPayload,
} from "./contentTypeControlProperties";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const OCCURRENCE_OPTIONS = ["optional", "required", "oneOrMore", "zeroOrMore", "count"] as const;

const DATA_TYPE_OPTIONS = ["text", "integer", "date", "datetime", "bool", "float", "binary"] as const;

const fieldStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "4px",
};

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

type FieldEdits = Record<string, { searchable: boolean; occurrence: string }>;

function editsFromFields(fields: SystemDefFieldSummary[]): FieldEdits {
  const next: FieldEdits = {};
  for (const f of fields) {
    if (!f.name) continue;
    next[f.name] = {
      searchable: Boolean(f.searchable),
      occurrence: f.occurrence || "optional",
    };
  }
  return next;
}

function occurrenceImpliesRequired(occurrence: string): boolean {
  return occurrence === "required" || occurrence === "oneOrMore";
}

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

function writeFallback(err: unknown, duplicate: boolean, add: boolean, fallback: string): string {
  if (isApiError(err) && err.status === 409) {
    if (isLockConflict(err)) {
      return DEV_MSG.SYS_LOCK;
    }
    if (duplicate) {
      return DEV_MSG.SYS_DUPLICATE;
    }
  }
  if (isApiError(err) && err.status === 400 && add) {
    return DEV_MSG.SYS_INVALID_NAME;
  }
  return fallback;
}

/**
 * P0.8 / CD-16 — content-editor system definition field catalog with save / add / delete.
 * Request lock is acquired and released on each REST write (no explicit Lock chrome).
 */
export function SystemDefPanel(): React.ReactElement {
  const [detail, setDetail] = useState<SystemDefDetail | null>(null);
  const [edits, setEdits] = useState<FieldEdits>({});
  const [error, setError] = useState<string | null>(null);
  const [writeError, setWriteError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [pendingDeleteName, setPendingDeleteName] = useState<string | null>(null);
  const [newName, setNewName] = useState("");
  const [newDataType, setNewDataType] = useState<string>("text");
  const [newSearchable, setNewSearchable] = useState(true);
  const [newRequired, setNewRequired] = useState(false);
  const [selectedFieldName, setSelectedFieldName] = useState("");
  const [controlName, setControlName] = useState<string | null>(null);
  const [controlProps, setControlProps] = useState<ContentTypeControlProperty[]>([]);
  const [controlPropsInitial, setControlPropsInitial] = useState<
    ContentTypeControlProperty[]
  >([]);
  const [controlPropsLoading, setControlPropsLoading] = useState(false);
  const [controlPropsError, setControlPropsError] = useState<string | null>(null);
  const [newPropName, setNewPropName] = useState("");
  const [newPropValue, setNewPropValue] = useState("");
  const [ssHandlers, setSsHandlers] = useState<SystemDefCommandHandlerStylesheet[]>([]);
  const [ssInitial, setSsInitial] = useState<SystemDefCommandHandlerStylesheet[]>([]);
  const [ssLoading, setSsLoading] = useState(true);
  const [ssError, setSsError] = useState<string | null>(null);
  const [ssNewHandler, setSsNewHandler] = useState("");
  const [ssNewHref, setSsNewHref] = useState(
    "file:../sys_resources/stylesheets/activeEdit.xsl",
  );
  const inflight = useRef(false);

  function applyDetail(d: SystemDefDetail): void {
    setDetail(d);
    setEdits(editsFromFields(d.fields || []));
    const names = (d.fields || []).map((f) => f.name).filter((n): n is string => !!n);
    setSelectedFieldName((prev) => {
      if (prev && names.includes(prev)) {
        return prev;
      }
      return names[0] || "";
    });
  }

  function applyStylesheets(handlers: SystemDefCommandHandlerStylesheet[]): void {
    const next = handlers.map((h) => ({
      commandHandler: h.commandHandler,
      href: h.href,
      conditionals: h.conditionals,
    }));
    setSsHandlers(next);
    setSsInitial(next.map((h) => ({ commandHandler: h.commandHandler, href: h.href })));
  }

  useEffect(() => {
    let cancelled = false;
    getSystemDef()
      .then((d) => {
        if (!cancelled) applyDetail(d);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SYS_ERROR));
        setDetail(null);
      });
    setSsLoading(true);
    getSystemDefStylesheets()
      .then((loaded) => {
        if (cancelled) return;
        applyStylesheets(loaded.handlers || []);
        setSsError(null);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setSsHandlers([]);
        setSsInitial([]);
        setSsError(panelErrMsg(e, DEV_MSG.SYS_SS_ERROR));
      })
      .finally(() => {
        if (!cancelled) setSsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedFieldName) {
      setControlProps([]);
      setControlPropsInitial([]);
      setControlName(null);
      setControlPropsError(null);
      setControlPropsLoading(false);
      return;
    }
    let cancelled = false;
    setControlPropsLoading(true);
    setControlPropsError(null);
    getSystemDefFieldControlProperties(selectedFieldName)
      .then((loaded) => {
        if (cancelled) return;
        const props = cloneControlProperties(loaded.properties);
        setControlProps(props);
        setControlPropsInitial(cloneControlProperties(props));
        setControlName(loaded.control || null);
        setNewPropName("");
        setNewPropValue("");
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setControlProps([]);
        setControlPropsInitial([]);
        setControlName(null);
        setControlPropsError(panelErrMsg(e, DEV_MSG.SYS_CONTROL_PROPS_ERROR));
      })
      .finally(() => {
        if (!cancelled) setControlPropsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedFieldName]);

  const fields = detail?.fields || [];
  const canAdd = !busy && isSystemDefFieldAddReady(newName);
  const dirtyPatches: SystemDefFieldPatch[] = fields
    .filter((f) => f.name && edits[f.name])
    .map((f) => {
      const name = f.name as string;
      const edit = edits[name];
      return {
        name,
        searchable: edit.searchable,
        occurrence: edit.occurrence,
      };
    })
    .filter((p) => {
      const orig = fields.find((f) => f.name === p.name);
      if (!orig) return false;
      return (
        Boolean(orig.searchable) !== Boolean(p.searchable) ||
        (orig.occurrence || "optional") !== p.occurrence
      );
    });
  const canSave = !busy && dirtyPatches.length > 0;
  const controlPropsDirty = !controlPropertiesEqual(controlProps, controlPropsInitial);
  const canSaveControl =
    !busy && !controlPropsLoading && !!selectedFieldName && controlPropsDirty;
  const ssDirty =
    ssHandlers.length !== ssInitial.length ||
    ssHandlers.some((h, i) => {
      const orig = ssInitial[i];
      return (
        !orig ||
        (h.commandHandler || "") !== (orig.commandHandler || "") ||
        (h.href || "") !== (orig.href || "")
      );
    });
  const canSaveSs =
    !busy &&
    !ssLoading &&
    ssDirty &&
    ssHandlers.length > 0 &&
    ssHandlers.every(
      (h) =>
        isValidSystemDefCommandHandler(h.commandHandler) &&
        isValidSystemDefStylesheetHref(h.href),
    );
  const canAddSs =
    !busy &&
    !ssLoading &&
    isValidSystemDefCommandHandler(ssNewHandler.trim()) &&
    isValidSystemDefStylesheetHref(ssNewHref) &&
    !ssHandlers.some(
      (h) => (h.commandHandler || "").toLowerCase() === ssNewHandler.trim().toLowerCase(),
    );

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setWriteError(null);
    setNotice(null);
    try {
      const saved = await updateSystemDef({ fields: dirtyPatches });
      applyDetail(saved);
      setNotice(DEV_MSG.SYS_SAVED);
    } catch (err: unknown) {
      setWriteError(panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SYS_SAVE_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  async function handleAdd(): Promise<void> {
    if (!canAdd || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setWriteError(null);
    setNotice(null);
    try {
      const saved = await addSystemDefField({
        name: newName.trim(),
        dataType: newDataType,
        searchable: newSearchable,
        required: newRequired,
      });
      applyDetail(saved);
      setNewName("");
      setNewDataType("text");
      setNewSearchable(true);
      setNewRequired(false);
      setNotice(DEV_MSG.SYS_ADDED);
    } catch (err: unknown) {
      setWriteError(panelErrMsg(err, writeFallback(err, true, true, DEV_MSG.SYS_ADD_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function setControlPropValue(index: number, value: string): void {
    setControlProps((prev) =>
      prev.map((p, i) => (i === index ? { ...p, value } : p)),
    );
  }

  function removeControlProp(index: number): void {
    setControlProps((prev) => prev.filter((_, i) => i !== index));
  }

  function addControlProp(): void {
    const name = newPropName.trim();
    if (!name) return;
    if (controlProps.some((p) => (p.name || "").toLowerCase() === name.toLowerCase())) {
      return;
    }
    setControlProps((prev) => [...prev, { name, value: newPropValue }]);
    setNewPropName("");
    setNewPropValue("");
  }

  async function handleSaveControlProperties(): Promise<void> {
    if (!canSaveControl || inflight.current || !selectedFieldName) return;
    inflight.current = true;
    setBusy(true);
    setWriteError(null);
    setNotice(null);
    try {
      const saved = await replaceSystemDefFieldControlProperties(
        selectedFieldName,
        { properties: toControlPropertyPayload(controlProps) },
      );
      const nextProps = cloneControlProperties(saved.properties);
      setControlProps(nextProps);
      setControlPropsInitial(cloneControlProperties(nextProps));
      setControlName(saved.control || null);
      setNotice(DEV_MSG.SYS_CONTROL_PROPS_SAVED);
    } catch (err: unknown) {
      setWriteError(
        panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SYS_CONTROL_PROPS_SAVE_ERROR)),
      );
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function setSsHref(index: number, href: string): void {
    setSsHandlers((prev) => prev.map((h, i) => (i === index ? { ...h, href } : h)));
  }

  function removeSsHandler(index: number): void {
    setSsHandlers((prev) => (prev.length <= 1 ? prev : prev.filter((_, i) => i !== index)));
  }

  function addSsHandler(): void {
    if (!canAddSs) return;
    const commandHandler = ssNewHandler.trim();
    setSsHandlers((prev) => [...prev, { commandHandler, href: ssNewHref.trim() }]);
    setSsNewHandler("");
  }

  async function handleSaveStylesheets(): Promise<void> {
    if (!canSaveSs || inflight.current) return;
    inflight.current = true;
    setBusy(true);
    setWriteError(null);
    setNotice(null);
    try {
      const saved = await replaceSystemDefStylesheets({
        handlers: ssHandlers.map((h) => ({
          commandHandler: h.commandHandler,
          href: h.href,
        })),
      });
      applyStylesheets(saved.handlers || []);
      setNotice(DEV_MSG.SYS_SS_SAVED);
    } catch (err: unknown) {
      setWriteError(panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SYS_SS_SAVE_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>, fieldName: string): void {
    if (!fieldName || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setPendingDeleteName(fieldName);
  }

  async function handleDelete(): Promise<void> {
    const fieldName = pendingDeleteName;
    if (!fieldName || inflight.current) return;
    setPendingDeleteName(null);
    inflight.current = true;
    setBusy(true);
    setWriteError(null);
    setNotice(null);
    try {
      await deleteSystemDefField(fieldName);
      const saved = await getSystemDef();
      applyDetail(saved);
      setNotice(DEV_MSG.SYS_DELETED);
    } catch (err: unknown) {
      setWriteError(panelErrMsg(err, writeFallback(err, false, false, DEV_MSG.SYS_DELETE_ERROR)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  if (error)
    return (
      <CatalogStatus testId="developer-sys-error" error>
        {error}
      </CatalogStatus>
    );
  if (detail == null)
    return (
      <CatalogStatus testId="developer-sys-loading">{DEV_MSG.SYS_LOADING}</CatalogStatus>
    );

  return (
    <div data-testid="developer-sys-panel">
      <CatalogHint>{DEV_MSG.SYS_HINT}</CatalogHint>

      <header style={{ marginBottom: "16px" }}>
        <h2 style={{ margin: "0 0 8px" }} data-testid="developer-sys-title">
          {DEV_MSG.SYS_TITLE}
        </h2>
        <dl style={metaGrid}>
          <dt>{DEV_MSG.SYS_META_FIELD_COUNT}</dt>
          <dd style={{ margin: 0 }}>
            {detail.fieldCount != null ? String(detail.fieldCount) : String(fields.length)}
          </dd>
          <dt>{DEV_MSG.SYS_META_CACHE}</dt>
          <dd style={{ margin: 0, ...monoCell }}>
            {detail.cacheTimeoutMinutes != null
              ? `${detail.cacheTimeoutMinutes} ${DEV_MSG.SYS_META_CACHE_UNIT}`
              : "—"}
          </dd>
        </dl>
      </header>

      {writeError ? (
        <div role="alert" data-testid="developer-sys-write-error" style={errorAlert}>
          {writeError}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-sys-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      <section
        data-testid="developer-sys-add"
        style={{
          marginBottom: "16px",
          padding: "12px",
          border: `1px solid ${catalogColors.headerBorder}`,
          borderRadius: "4px",
        }}
      >
        <h3 style={{ fontSize: "1rem", marginTop: 0 }}>{DEV_MSG.SYS_ADD}</h3>
        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "12px",
            alignItems: "flex-end",
          }}
        >
          <div style={fieldStyle}>
            <label htmlFor="sys-new-name">{DEV_MSG.SYS_NEW_NAME}</label>
            <input
              id="sys-new-name"
              data-testid="developer-sys-new-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={newName}
              disabled={busy}
              onChange={(e) => setNewName(e.target.value)}
              autoComplete="off"
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="sys-new-datatype">{DEV_MSG.SYS_NEW_DATATYPE}</label>
            <select
              id="sys-new-datatype"
              data-testid="developer-sys-new-datatype"
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
          <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <input
              type="checkbox"
              data-testid="developer-sys-new-searchable"
              checked={newSearchable}
              disabled={busy}
              onChange={(e) => setNewSearchable(e.target.checked)}
            />
            {DEV_MSG.SYS_NEW_SEARCH}
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <input
              type="checkbox"
              data-testid="developer-sys-new-required"
              checked={newRequired}
              disabled={busy}
              onChange={(e) => setNewRequired(e.target.checked)}
            />
            {DEV_MSG.SYS_NEW_REQUIRED}
          </label>
          <button
            type="button"
            data-testid="developer-sys-add-btn"
            aria-label={DEV_MSG.SYS_ADD}
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
            {DEV_MSG.SYS_ADD}
          </button>
        </div>
      </section>

      <section data-testid="developer-sys-fields">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SYS_FIELDS}</h3>
          <button
            type="button"
            data-testid="developer-sys-save"
            aria-label={DEV_MSG.SYS_SAVE}
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
            {DEV_MSG.SYS_SAVE}
          </button>
        </div>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SYS_FIELDS_HINT}</p>
        {fields.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-sys-empty">
            {DEV_MSG.SYS_EMPTY}
          </p>
        ) : (
          <SimpleCatalogTable
            tableTestId="developer-sys-fields-table"
            rowTestId="developer-sys-field-row"
            columns={[
              DEV_MSG.SYS_COL_FIELD,
              DEV_MSG.SYS_COL_DATATYPE,
              DEV_MSG.SYS_COL_OCCURRENCE,
              DEV_MSG.SYS_COL_REQUIRED,
              DEV_MSG.SYS_COL_SEARCH,
              DEV_MSG.SYS_COL_READONLY,
              DEV_MSG.SYS_COL_ACTIONS,
            ]}
            rows={fields.map((f, i) => {
              const name = f.name || "";
              const edit = name ? edits[name] : undefined;
              const occurrence = edit?.occurrence || f.occurrence || "optional";
              const origOccurrence = f.occurrence || "optional";
              const requiredDisplay =
                occurrence === origOccurrence
                  ? (f.required ?? occurrenceImpliesRequired(occurrence))
                  : occurrenceImpliesRequired(occurrence);
              const searchable = edit ? edit.searchable : Boolean(f.searchable);
              const occurrenceChoices = OCCURRENCE_OPTIONS.includes(
                occurrence as (typeof OCCURRENCE_OPTIONS)[number],
              )
                ? OCCURRENCE_OPTIONS
                : ([occurrence, ...OCCURRENCE_OPTIONS] as readonly string[]);
              return {
                key: name || `f-${i}`,
                dataAttrs: name ? { "data-sys-field": name } : undefined,
                cells: [
                  <span key="n" style={monoCell}>
                    {name || "—"}
                  </span>,
                  f.dataType || "—",
                  <select
                    key="occ"
                    data-testid="developer-sys-occurrence"
                    aria-label={DEV_MSG.SYS_COL_OCCURRENCE}
                    style={inputStyle}
                    value={occurrence}
                    disabled={busy || !name}
                    onChange={(e) => {
                      if (!name) return;
                      setEdits((prev) => ({
                        ...prev,
                        [name]: {
                          searchable: prev[name]?.searchable ?? Boolean(f.searchable),
                          occurrence: e.target.value,
                        },
                      }));
                    }}
                  >
                    {occurrenceChoices.map((opt) => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                  </select>,
                  <span key="req" data-testid="developer-sys-required">
                    {requiredDisplay ? DEV_MSG.YES : DEV_MSG.NO}
                  </span>,
                  <input
                    key="s"
                    type="checkbox"
                    data-testid="developer-sys-searchable"
                    aria-label={DEV_MSG.SYS_COL_SEARCH}
                    checked={searchable}
                    disabled={busy || !name}
                    onChange={(e) => {
                      if (!name) return;
                      setEdits((prev) => ({
                        ...prev,
                        [name]: {
                          searchable: e.target.checked,
                          occurrence: prev[name]?.occurrence || f.occurrence || "optional",
                        },
                      }));
                    }}
                  />,
                  f.readOnly == null ? "—" : f.readOnly ? DEV_MSG.YES : DEV_MSG.NO,
                  <button
                    key="del"
                    type="button"
                    data-testid="developer-sys-delete"
                    aria-label={`${DEV_MSG.SYS_DELETE} ${name}`}
                    disabled={busy || !name}
                    onClick={(ev) => requestDelete(ev, name)}
                    style={{
                      padding: "4px 10px",
                      background: "#c53030",
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: busy || !name ? "not-allowed" : "pointer",
                    }}
                  >
                    {DEV_MSG.SYS_DELETE}
                  </button>,
                ],
              };
            })}
          />
        )}
      </section>

      <section
        data-testid="developer-sys-control-props"
        style={{
          marginTop: "16px",
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
          <h3 style={{ fontSize: "1rem", marginTop: 0 }}>{DEV_MSG.SYS_CONTROL_PROPS}</h3>
          <button
            type="button"
            data-testid="developer-sys-cp-save"
            aria-label={DEV_MSG.SYS_CONTROL_PROPS_SAVE}
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
            {DEV_MSG.SYS_CONTROL_PROPS_SAVE}
          </button>
        </div>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {DEV_MSG.SYS_CONTROL_PROPS_HINT}
        </p>
        <div style={{ marginBottom: "12px" }}>
          <label htmlFor="sys-cp-field" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.SYS_CONTROL_PROPS_FIELD}
          </label>
          <select
            id="sys-cp-field"
            data-testid="developer-sys-cp-field"
            aria-label={DEV_MSG.SYS_CONTROL_PROPS_FIELD}
            style={inputStyle}
            value={selectedFieldName}
            disabled={busy || controlPropsDirty}
            onChange={(e) => {
              if (busy || controlPropsDirty) return;
              setSelectedFieldName(e.target.value);
              setNotice(null);
            }}
          >
            {fields.filter((f) => !!f.name).length === 0 ? (
              <option value="">{DEV_MSG.SYS_CONTROL_PROPS_NO_FIELD}</option>
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
            data-testid="developer-sys-cp-control"
            style={{ color: catalogColors.muted, fontSize: "0.9rem", fontFamily: "monospace" }}
          >
            {DEV_MSG.SYS_CONTROL_PROPS_CONTROL}: {controlName}
          </p>
        ) : null}
        {controlPropsError ? (
          <p
            role="status"
            data-testid="developer-sys-cp-error"
            style={{ color: catalogColors.error }}
          >
            {controlPropsError}
          </p>
        ) : null}
        {controlPropsLoading ? (
          <p data-testid="developer-sys-cp-loading" style={{ color: catalogColors.muted }}>
            {DEV_MSG.SYS_CONTROL_PROPS_LOADING}
          </p>
        ) : null}
        {!controlPropsLoading && selectedFieldName && controlProps.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-sys-cp-empty">
            {DEV_MSG.SYS_CONTROL_PROPS_EMPTY}
          </p>
        ) : null}
        {!controlPropsLoading && controlProps.length > 0 ? (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {controlProps.map((p, i) => (
              <li
                key={`${p.name || "prop"}-${i}`}
                data-testid={`developer-sys-cp-row-${i}`}
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr auto",
                  gap: 8,
                  alignItems: "center",
                  padding: "6px 0",
                }}
              >
                <span
                  data-testid={`developer-sys-cp-name-${i}`}
                  style={{ fontFamily: "monospace" }}
                >
                  {p.name}
                </span>
                <input
                  type="text"
                  data-testid={`developer-sys-cp-value-${i}`}
                  aria-label={`${DEV_MSG.SYS_CONTROL_PROPS_VALUE} ${p.name || i}`}
                  style={inputStyle}
                  value={p.value || ""}
                  disabled={busy}
                  onChange={(e) => setControlPropValue(i, e.target.value)}
                />
                <button
                  type="button"
                  data-testid={`developer-sys-cp-remove-${i}`}
                  aria-label={`${DEV_MSG.SYS_CONTROL_PROPS_REMOVE} ${p.name || i}`}
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
                  {DEV_MSG.SYS_CONTROL_PROPS_REMOVE}
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
            <label htmlFor="sys-cp-add-name" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.SYS_CONTROL_PROPS_NAME}
            </label>
            <input
              id="sys-cp-add-name"
              type="text"
              autoComplete="off"
              data-testid="developer-sys-cp-add-name"
              style={inputStyle}
              placeholder={DEV_MSG.SYS_CONTROL_PROPS_NAME_PLACEHOLDER}
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
            <label htmlFor="sys-cp-add-value" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.SYS_CONTROL_PROPS_VALUE}
            </label>
            <input
              id="sys-cp-add-value"
              type="text"
              autoComplete="off"
              data-testid="developer-sys-cp-add-value"
              style={inputStyle}
              placeholder={DEV_MSG.SYS_CONTROL_PROPS_VALUE_PLACEHOLDER}
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
            data-testid="developer-sys-cp-add"
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
                busy || !selectedFieldName || !newPropName.trim() ? "not-allowed" : "pointer",
            }}
          >
            {DEV_MSG.SYS_CONTROL_PROPS_ADD}
          </button>
        </div>
      </section>

      <section
        data-testid="developer-sys-ss"
        style={{
          marginTop: "16px",
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
          <h3 style={{ fontSize: "1rem", marginTop: 0 }}>{DEV_MSG.SYS_SS}</h3>
          <button
            type="button"
            data-testid="developer-sys-ss-save"
            aria-label={DEV_MSG.SYS_SS_SAVE}
            disabled={!canSaveSs}
            onClick={() => void handleSaveStylesheets()}
            style={{
              padding: "8px 16px",
              background: canSaveSs ? catalogColors.accent : catalogColors.disabled,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: canSaveSs ? "pointer" : "not-allowed",
            }}
          >
            {DEV_MSG.SYS_SS_SAVE}
          </button>
        </div>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.SYS_SS_HINT}</p>
        {ssError ? (
          <p role="status" data-testid="developer-sys-ss-error" style={{ color: catalogColors.error }}>
            {ssError}
          </p>
        ) : null}
        {ssLoading ? (
          <p data-testid="developer-sys-ss-loading" style={{ color: catalogColors.muted }}>
            {DEV_MSG.SYS_SS_LOADING}
          </p>
        ) : null}
        {!ssLoading && ssHandlers.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-sys-ss-empty">
            {DEV_MSG.SYS_SS_EMPTY}
          </p>
        ) : null}
        {!ssLoading && ssHandlers.length > 0 ? (
          <SimpleCatalogTable
            tableTestId="developer-sys-ss-table"
            rowTestId="developer-sys-ss-row"
            columns={[
              DEV_MSG.SYS_SS_COL_HANDLER,
              DEV_MSG.SYS_SS_COL_HREF,
              DEV_MSG.SYS_SS_COL_CONDITIONALS,
              DEV_MSG.SYS_SS_COL_ACTIONS,
            ]}
            rows={ssHandlers.map((h, i) => {
              const name = h.commandHandler || "";
              const condCount = h.conditionals?.length || 0;
              return {
                key: name || `ss-${i}`,
                dataAttrs: name ? { "data-sys-ss-handler": name } : undefined,
                cells: [
                  <span key="n" style={monoCell}>
                    {name || "—"}
                  </span>,
                  <input
                    key="href"
                    type="text"
                    data-testid={`developer-sys-ss-href-${i}`}
                    aria-label={`${DEV_MSG.SYS_SS_COL_HREF} ${name || i}`}
                    style={{ ...inputStyle, fontFamily: "monospace", minWidth: "22rem" }}
                    value={h.href || ""}
                    disabled={busy}
                    onChange={(e) => setSsHref(i, e.target.value)}
                  />,
                  <span key="c" data-testid={`developer-sys-ss-cond-${i}`}>
                    {condCount}
                  </span>,
                  <button
                    key="rm"
                    type="button"
                    data-testid={`developer-sys-ss-remove-${i}`}
                    aria-label={`${DEV_MSG.SYS_SS_REMOVE} ${name}`}
                    disabled={busy || ssHandlers.length <= 1}
                    onClick={() => removeSsHandler(i)}
                    title={ssHandlers.length <= 1 ? DEV_MSG.SYS_SS_LAST : DEV_MSG.SYS_SS_REMOVE}
                    style={{
                      padding: "4px 10px",
                      background: "#c53030",
                      color: "#fff",
                      border: "none",
                      borderRadius: "4px",
                      cursor: busy || ssHandlers.length <= 1 ? "not-allowed" : "pointer",
                    }}
                  >
                    {DEV_MSG.SYS_SS_REMOVE}
                  </button>,
                ],
              };
            })}
          />
        ) : null}
        <div
          style={{
            marginTop: "12px",
            display: "grid",
            gridTemplateColumns: "1fr 2fr auto",
            gap: "8px",
            alignItems: "end",
          }}
        >
          <div>
            <label htmlFor="sys-ss-add-name" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.SYS_SS_NEW_HANDLER}
            </label>
            <input
              id="sys-ss-add-name"
              type="text"
              autoComplete="off"
              data-testid="developer-sys-ss-add-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={ssNewHandler}
              disabled={busy}
              onChange={(e) => setSsNewHandler(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="sys-ss-add-href" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.SYS_SS_NEW_HREF}
            </label>
            <input
              id="sys-ss-add-href"
              type="text"
              autoComplete="off"
              data-testid="developer-sys-ss-add-href"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              placeholder={DEV_MSG.SYS_SS_HREF_PLACEHOLDER}
              value={ssNewHref}
              disabled={busy}
              onChange={(e) => setSsNewHref(e.target.value)}
            />
          </div>
          <button
            type="button"
            data-testid="developer-sys-ss-add"
            disabled={!canAddSs}
            onClick={addSsHandler}
            style={{
              padding: "8px 16px",
              background: canAddSs ? catalogColors.accent : catalogColors.disabled,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: canAddSs ? "pointer" : "not-allowed",
            }}
          >
            {DEV_MSG.SYS_SS_ADD}
          </button>
        </div>
      </section>

      {detail.designGaps && detail.designGaps.length > 0 ? (
        <section style={{ marginTop: "16px" }} data-testid="developer-sys-gaps">
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SYS_GAPS}</h3>
          <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
            {detail.designGaps.map((g) => (
              <li key={g}>{g}</li>
            ))}
          </ul>
        </section>
      ) : null}
      <CatalogConfirmDialog
        open={pendingDeleteName != null}
        busy={busy}
        message={DEV_MSG.SYS_DELETE_CONFIRM}
        onCancel={() => setPendingDeleteName(null)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
