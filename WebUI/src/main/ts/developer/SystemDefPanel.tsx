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
  addSystemDefField,
  deleteSystemDefField,
  getSystemDef,
  isSystemDefFieldAddReady,
  updateSystemDef,
  type SystemDefFieldPatch,
} from "../api/developer/systemDefApi";
import type { SystemDefDetail, SystemDefFieldSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import {
  catalogColors,
  errorAlert,
  metaGrid,
  monoCell,
} from "./catalogStyles";
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
  const [newName, setNewName] = useState("");
  const [newDataType, setNewDataType] = useState<string>("text");
  const [newSearchable, setNewSearchable] = useState(true);
  const [newRequired, setNewRequired] = useState(false);
  const inflight = useRef(false);

  function applyDetail(d: SystemDefDetail): void {
    setDetail(d);
    setEdits(editsFromFields(d.fields || []));
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
    return () => {
      cancelled = true;
    };
  }, []);

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

  async function handleDelete(fieldName: string): Promise<void> {
    if (!fieldName || inflight.current) return;
    if (!window.confirm(DEV_MSG.SYS_DELETE_CONFIRM)) return;
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
                    onClick={() => void handleDelete(name)}
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
    </div>
  );
}
