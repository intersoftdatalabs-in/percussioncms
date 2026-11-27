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
  CONTROL_CHOICE_SETS,
  CONTROL_DIMENSIONS,
  deleteControl,
  getControlDetail,
  isControlSaveReady,
  isSystemControl,
  updateControl,
  type ControlWriteBody,
} from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
import {
  backButton,
  catalogColors,
  emptyStateText,
  errorAlert,
  metaGrid,
  monoCell,
} from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { SimpleCatalogTable } from "./CatalogTable";
import { CTL_PROP_REQUIRED_MSG } from "./controlPropRequiredMessages";
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

function loadErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 404) {
      return DEV_MSG.CTL_NOT_FOUND;
    }
    if (err.status === 403) {
      return DEV_MSG.CTL_FORBIDDEN;
    }
  }
  return DEV_MSG.CTL_DETAIL_ERROR;
}

function writeErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return DEV_MSG.CTL_SYSTEM_CONFLICT;
    }
    if (err.status === 404) {
      return DEV_MSG.CTL_NOT_FOUND;
    }
    if (err.status === 403) {
      return DEV_MSG.CTL_FORBIDDEN;
    }
    if (err.status === 400) {
      return DEV_MSG.CTL_INVALID_XSL;
    }
  }
  return DEV_MSG.CTL_SAVE_ERROR;
}

function deleteErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 409) {
      return DEV_MSG.CTL_SYSTEM_CONFLICT;
    }
    if (err.status === 404) {
      return DEV_MSG.CTL_NOT_FOUND;
    }
    if (err.status === 403) {
      return DEV_MSG.CTL_FORBIDDEN;
    }
  }
  return DEV_MSG.CTL_DELETE_ERROR;
}

export function ControlDetailPanel({
  name,
  onBack,
  onSaved,
  onDeleted,
}: {
  name: string;
  onBack: () => void;
  onSaved?: (detail: ControlDef) => void | Promise<void>;
  onDeleted?: () => void | Promise<void>;
}): React.ReactElement {
  const [detail, setDetail] = useState<ControlDef | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [dimension, setDimension] = useState("");
  const [choiceSet, setChoiceSet] = useState("");
  const [xslSource, setXslSource] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [propError, setPropError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const inflight = useRef(false);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setPropError(null);
    setConfirmOpen(false);
    getControlDetail(name)
      .then((d) => {
        if (cancelled) {
          return;
        }
        setDetail(d);
        setDisplayName(d.displayName || "");
        setDescription(d.description || "");
        setDimension((d.dimension || "").trim().toLowerCase());
        setChoiceSet((d.choiceSet || "").trim().toLowerCase());
        setXslSource(d.xslSource || "");
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(panelErrMsg(err, loadErrorFallback(err)));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  const system = isSystemControl(detail?.scope);
  const userWritable = detail != null && !system;
  const canSave = userWritable && !busy && isControlSaveReady({ dimension, choiceSet });

  function writeBody(): ControlWriteBody {
    const body: ControlWriteBody = {
      name,
      displayName: displayName.trim() || name,
      dimension: dimension.trim() ? dimension.trim().toLowerCase() : "single",
      choiceSet: choiceSet.trim() ? choiceSet.trim().toLowerCase() : "none",
    };
    if (description.trim()) {
      body.description = description.trim();
    }
    if (xslSource.trim()) {
      body.xslSource = xslSource.trim();
    }
    return body;
  }

  function cancelDisplayEdit(): void {
    // #7345: Escape in the property input cancels the edit without writing
    // (mirrors #6713/#6905) — the stored value is restored.
    if (busy || inflight.current) {
      return;
    }
    setDisplayName(detail?.displayName || "");
    setPropError(null);
  }

  function handleDisplayKeyDown(e: React.KeyboardEvent<HTMLInputElement>): void {
    if (e.key === "Escape") {
      cancelDisplayEdit();
    }
  }

  function cancelDescriptionEdit(): void {
    if (busy || inflight.current) {
      return;
    }
    setDescription(detail?.description || "");
  }

  function handleDescriptionKeyDown(e: React.KeyboardEvent<HTMLInputElement>): void {
    if (e.key === "Escape") {
      cancelDescriptionEdit();
    }
  }

  async function handleSave(): Promise<void> {
    if (!canSave || inflight.current) {
      return;
    }
    if (!displayName.trim()) {
      // #7345: a blank display name blocks the PUT before it fires, so the
      // stored display name is kept. Named alert, same as the content-type
      // required-field pattern.
      setPropError(CTL_PROP_REQUIRED_MSG.REQUIRED);
      setNotice(null);
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    setPropError(null);
    try {
      const body = writeBody();
      const saved = await updateControl(name, body);
      setDetail(saved);
      setDisplayName(saved.displayName || "");
      setDescription(saved.description || "");
      setDimension((saved.dimension || "").trim().toLowerCase());
      setChoiceSet((saved.choiceSet || "").trim().toLowerCase());
      if (saved.xslSource != null) {
        setXslSource(saved.xslSource);
      } else if (!body.xslSource) {
        setXslSource("");
      }
      setNotice(DEV_MSG.CTL_SAVED);
      await onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, writeErrorFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (!userWritable || inflight.current) {
      return;
    }
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (!userWritable || inflight.current) {
      return;
    }
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteControl(name);
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

  const params =
    detail != null && Array.isArray(detail.parameters) ? detail.parameters : [];
  const gaps =
    detail != null && detail.designGaps && detail.designGaps.length
      ? detail.designGaps
      : [DEV_MSG.CTL_GAP_SYS];

  return (
    <div data-testid="developer-ctl-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ctl-back"
        aria-label={DEV_MSG.CTL_BACK}
        style={backButton}
      >
        ← {DEV_MSG.CTL_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ctl-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div
          role="status"
          aria-live="polite"
          aria-atomic="true"
          data-testid="developer-ctl-detail-notice"
          style={{ color: "#276749" }}
        >
          {notice}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-ctl-detail-loading">{DEV_MSG.CTL_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ctl-detail-title">
              {detail.displayName || detail.name || name}
            </h2>
            {system ? (
              <p
                data-testid="developer-ctl-system-readonly"
                style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}
              >
                {DEV_MSG.CTL_SYSTEM_READONLY}
              </p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.CTL_COL_NAME}</dt>
              <dd data-testid="developer-ctl-detail-name" style={{ margin: 0, ...monoCell }}>
                {detail.name || "—"}
              </dd>
              <dt>{DEV_MSG.CTL_COL_SCOPE}</dt>
              <dd style={{ margin: 0 }}>{detail.scope || "—"}</dd>
              {!userWritable ? (
                <>
                  <dt>{DEV_MSG.CTL_COL_DIM}</dt>
                  <dd style={{ margin: 0 }}>{detail.dimension || "—"}</dd>
                  <dt>{DEV_MSG.CTL_COL_CHOICES}</dt>
                  <dd style={{ margin: 0 }}>{detail.choiceSet || "—"}</dd>
                  <dt>{DEV_MSG.CTL_COL_DESC}</dt>
                  <dd style={{ margin: 0 }}>{detail.description || "—"}</dd>
                </>
              ) : null}
            </dl>
          </header>

          {system && (detail.xslSource || xslSource) ? (
            <div style={fieldStyle}>
              <label htmlFor="ctl-view-xsl">{DEV_MSG.CTL_XSL_READONLY}</label>
              <textarea
                id="ctl-view-xsl"
                data-testid="developer-ctl-view-xsl"
                style={{ ...inputStyle, fontFamily: "monospace", minHeight: "160px" }}
                value={detail.xslSource || xslSource}
                readOnly
                spellCheck={false}
              />
            </div>
          ) : null}

          {userWritable ? (
            <>
              <div style={fieldStyle}>
                <label htmlFor="ctl-edit-display">{DEV_MSG.CTL_FORM_DISPLAY}</label>
                <input
                  id="ctl-edit-display"
                  data-testid="developer-ctl-edit-display"
                  style={inputStyle}
                  value={displayName}
                  disabled={busy}
                  onChange={(e) => {
                    setDisplayName(e.target.value);
                    setPropError(null);
                  }}
                  onKeyDown={handleDisplayKeyDown}
                />
                {propError ? (
                  <div
                    role="alert"
                    data-testid="developer-ctl-prop-required-error"
                    aria-label={CTL_PROP_REQUIRED_MSG.ACTION}
                    style={{ color: catalogColors.error }}
                  >
                    {propError}
                  </div>
                ) : null}
              </div>
              <div style={fieldStyle}>
                <label htmlFor="ctl-edit-desc">{DEV_MSG.CTL_FORM_DESCRIPTION}</label>
                <input
                  id="ctl-edit-desc"
                  data-testid="developer-ctl-edit-description"
                  style={inputStyle}
                  value={description}
                  disabled={busy}
                  onChange={(e) => setDescription(e.target.value)}
                  onKeyDown={handleDescriptionKeyDown}
                />
              </div>
              <div style={fieldStyle}>
                <label htmlFor="ctl-edit-dim">{DEV_MSG.CTL_FORM_DIMENSION}</label>
                <select
                  id="ctl-edit-dim"
                  data-testid="developer-ctl-edit-dimension"
                  style={inputStyle}
                  value={dimension}
                  disabled={busy}
                  onChange={(e) => setDimension(e.target.value)}
                >
                  <option value="">{DEV_MSG.CTL_DIM_DEFAULT}</option>
                  {CONTROL_DIMENSIONS.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>
              <div style={fieldStyle}>
                <label htmlFor="ctl-edit-choice">{DEV_MSG.CTL_FORM_CHOICESET}</label>
                <select
                  id="ctl-edit-choice"
                  data-testid="developer-ctl-edit-choiceset"
                  style={inputStyle}
                  value={choiceSet}
                  disabled={busy}
                  onChange={(e) => setChoiceSet(e.target.value)}
                >
                  <option value="">{DEV_MSG.CTL_CHOICE_DEFAULT}</option>
                  {CONTROL_CHOICE_SETS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div style={fieldStyle}>
                <label htmlFor="ctl-edit-xsl">{DEV_MSG.CTL_FORM_XSL}</label>
                <textarea
                  id="ctl-edit-xsl"
                  data-testid="developer-ctl-edit-xsl"
                  style={{ ...inputStyle, fontFamily: "monospace", minHeight: "240px" }}
                  value={xslSource}
                  disabled={busy}
                  onChange={(e) => setXslSource(e.target.value)}
                  spellCheck={false}
                  aria-label={DEV_MSG.CTL_FORM_XSL}
                />
                <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                  {DEV_MSG.CTL_XSL_HINT}
                </span>
              </div>
              <p style={{ color: catalogColors.muted, fontSize: "0.85rem", marginTop: 0 }}>
                {DEV_MSG.CTL_NAME_READONLY}
              </p>
              <p style={{ color: catalogColors.muted, fontSize: "0.85rem", marginTop: 0 }}>
                {DEV_MSG.CTL_SAVE_HINT}
              </p>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
                <button
                  type="button"
                  data-testid="developer-ctl-save"
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
                  {DEV_MSG.CTL_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-ctl-delete"
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
                  {DEV_MSG.CTL_DELETE}
                </button>
              </div>
            </>
          ) : null}

          <section data-testid="developer-ctl-params">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CTL_PARAMS}</h3>
            {params.length === 0 ? (
              <p style={emptyStateText}>{DEV_MSG.CTL_NONE}</p>
            ) : (
              <SimpleCatalogTable
                tableTestId="developer-ctl-params-table"
                rowTestId="developer-ctl-param-row"
                columns={[
                  DEV_MSG.CTL_COL_PARAM,
                  DEV_MSG.CTL_COL_TYPE,
                  DEV_MSG.CTL_COL_REQ,
                  DEV_MSG.CTL_COL_DEFAULT,
                ]}
                rows={params.map((p, i) => ({
                  key: `${p.name ?? "p"}-${i}`,
                  cells: [
                    <span style={monoCell}>{p.name || "—"}</span>,
                    p.dataType || p.paramType || "—",
                    p.required ? DEV_MSG.CTL_YES : DEV_MSG.CTL_NO,
                    p.defaultValue || "—",
                  ],
                }))}
              />
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-ctl-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CTL_GAPS}</h3>
            <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {gaps.map((g, i) => (
                <li key={`${g}-${i}`}>{g}</li>
              ))}
            </ul>
          </section>
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.CTL_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
