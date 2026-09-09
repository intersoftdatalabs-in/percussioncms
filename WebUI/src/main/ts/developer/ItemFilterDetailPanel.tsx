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
  createItemFilter,
  deleteItemFilter,
  getItemFilterDetail,
  isItemFilterWriteReady,
  normalizeFilterName,
  updateItemFilter,
  type ItemFilterWriteBody,
} from "../api/developer/itemFiltersApi";
import type { ItemFilter, ItemFilterRule } from "../api/developer/types";
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
import { panelErrMsg } from "./errors";
import {
  cloneRules,
  emptyParam,
  emptyRule,
  rulesFingerprint,
  toWireRules,
} from "./itemFilterRules";
import { DEV_MSG } from "./messages";

const AUTHTYPE_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "—" },
  { value: "0", label: "All Content" },
  { value: "1", label: "All Public Content" },
  { value: "2", label: "Custom" },
  { value: "101", label: "Site Folder" },
];

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
  padding: "6px 10px",
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  cursor: "pointer",
  font: "inherit",
};

function authtypeValue(raw: number | undefined | null): string {
  return raw == null ? "" : String(raw);
}

export function ItemFilterDetailPanel({
  idOrName,
  onBack,
  onSaved,
  onDeleted,
}: {
  /** null = create mode */
  idOrName: string | null;
  onBack: () => void;
  onSaved?: (detail: ItemFilter) => void;
  onDeleted?: () => void;
}): React.ReactElement {
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const isNew = idOrName == null && createdKey == null;
  const [detail, setDetail] = useState<ItemFilter | null>(null);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [parentName, setParentName] = useState("");
  const [legacyAuthtype, setLegacyAuthtype] = useState("");
  const [draftRules, setDraftRules] = useState<ItemFilterRule[]>([]);
  const [loadedRulesFp, setLoadedRulesFp] = useState(() => rulesFingerprint([]));
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(idOrName != null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const inflight = useRef(false);

  useEffect(() => {
    if (idOrName == null) {
      setDraftRules([]);
      setLoadedRulesFp(rulesFingerprint([]));
      return;
    }
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setLoading(true);
    getItemFilterDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        const rules = cloneRules(d.rules);
        setDetail(d);
        setName(d.name || idOrName);
        setDescription(d.description || "");
        setParentName(d.parentFilter?.name || "");
        setLegacyAuthtype(authtypeValue(d.legacyAuthtype));
        setDraftRules(rules);
        setLoadedRulesFp(rulesFingerprint(rules));
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.IF_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const loadedName = normalizeFilterName(detail?.name || idOrName || "");
  const loadedDescription = detail?.description || "";
  const loadedParent = detail?.parentFilter?.name || "";
  const loadedAuthtype = authtypeValue(detail?.legacyAuthtype);
  const rulesDirty = rulesFingerprint(draftRules) !== loadedRulesFp;
  const dirty =
    isNew ||
    normalizeFilterName(name) !== loadedName ||
    description !== loadedDescription ||
    parentName.trim() !== loadedParent ||
    legacyAuthtype !== loadedAuthtype ||
    rulesDirty;
  const canSave =
    !busy && dirty && isItemFilterWriteReady({ isNew, name });
  const writeKey = idOrName || createdKey || normalizeFilterName(name);

  function applySaved(saved: ItemFilter): void {
    const rules = cloneRules(saved.rules);
    setDetail(saved);
    setName(saved.name || name);
    setDescription(saved.description || "");
    setParentName(saved.parentFilter?.name || "");
    setLegacyAuthtype(authtypeValue(saved.legacyAuthtype));
    setDraftRules(rules);
    setLoadedRulesFp(rulesFingerprint(rules));
  }

  function writeBody(): ItemFilterWriteBody {
    const body: ItemFilterWriteBody = {
      name: isNew ? normalizeFilterName(name) : detail?.name || normalizeFilterName(name),
      description,
    };
    if (legacyAuthtype) {
      const parsed = Number(legacyAuthtype);
      if (!Number.isNaN(parsed)) {
        body.legacyAuthtype = parsed;
      }
    }
    const parent = parentName.trim();
    if (parent) {
      body.parentFilter = { name: parent };
    } else if (!isNew) {
      body.parentFilter = {};
    }
    // Always send rules on write so add/edit/clear persist. Empty + clearRules
    // clears (some JAX-RS paths drop rules:[] before the adaptor).
    const wireRules = toWireRules(draftRules);
    body.rules = wireRules;
    if (wireRules.length === 0) {
      body.clearRules = true;
    }
    return body;
  }

  function saveFallback(err: unknown): string {
    if (isApiError(err) && err.status === 409 && isNew) return DEV_MSG.IF_DUPLICATE;
    if (isApiError(err) && err.status === 400) return DEV_MSG.IF_INVALID_NAME;
    if (isApiError(err) && err.status === 403) return DEV_MSG.IF_FORBIDDEN;
    if (isApiError(err) && err.status === 404) return DEV_MSG.IF_NOT_FOUND;
    return DEV_MSG.IF_SAVE_ERROR;
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
          ? await createItemFilter(writeBody())
          : await updateItemFilter(writeKey, writeBody());
      if (isNew) {
        setCreatedKey(saved.name || normalizeFilterName(name));
      }
      applySaved(saved);
      setNotice(DEV_MSG.IF_SAVED);
      onSaved?.(saved);
    } catch (err: unknown) {
      setError(panelErrMsg(err, saveFallback(err)));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function requestDelete(ev: React.MouseEvent<HTMLElement>): void {
    if (isNew || !writeKey || inflight.current) return;
    captureDialogOpener(ev.currentTarget);
    setConfirmOpen(true);
  }

  async function handleDelete(): Promise<void> {
    if (isNew || !writeKey || inflight.current) return;
    setConfirmOpen(false);
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await deleteItemFilter(writeKey);
      setNotice(DEV_MSG.IF_DELETED);
      onDeleted?.();
    } catch (err: unknown) {
      const fallback =
        isApiError(err) && err.status === 403
          ? DEV_MSG.IF_FORBIDDEN
          : isApiError(err) && err.status === 404
            ? DEV_MSG.IF_NOT_FOUND
            : DEV_MSG.IF_DELETE_ERROR;
      setError(panelErrMsg(err, fallback));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  function updateRule(index: number, patch: Partial<ItemFilterRule>): void {
    setDraftRules((prev) =>
      prev.map((r, i) => (i === index ? { ...r, ...patch } : r)),
    );
  }

  function updateRuleParam(
    ruleIndex: number,
    paramIndex: number,
    patch: { name?: string; value?: string },
  ): void {
    setDraftRules((prev) =>
      prev.map((r, i) => {
        if (i !== ruleIndex) return r;
        const params = [...(r.params || [])];
        params[paramIndex] = { ...params[paramIndex], ...patch };
        return { ...r, params };
      }),
    );
  }

  const title = isNew
    ? DEV_MSG.IF_NEW
    : detail?.name || idOrName || DEV_MSG.IF_EDIT;

  return (
    <div data-testid="developer-if-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-if-back"
        aria-label={DEV_MSG.IF_BACK}
        style={backButton}
      >
        ← {DEV_MSG.IF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-if-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {notice ? (
        <div data-testid="developer-if-editor-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="developer-if-detail-loading">{DEV_MSG.IF_DETAIL_LOADING}</div>
      ) : null}

      {!loading && (isNew || detail) ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-if-detail-title">
              {title}
            </h2>
            {!isNew && detail?.filterId?.stringValue ? (
              <dl style={metaGrid}>
                <dt>{DEV_MSG.IF_COL_GUID}</dt>
                <dd style={{ margin: 0, ...monoCell }}>{detail.filterId.stringValue}</dd>
              </dl>
            ) : null}
          </header>

          <div style={fieldStyle}>
            <label htmlFor="if-name">{DEV_MSG.IF_FORM_NAME}</label>
            <input
              id="if-name"
              data-testid="developer-if-name"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={name}
              disabled={!isNew || busy}
              onChange={(e) => setName(e.target.value)}
              autoComplete="off"
            />
            {!isNew ? (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.IF_NAME_READONLY}
              </span>
            ) : (
              <span style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
                {DEV_MSG.IF_NAME_HINT}
              </span>
            )}
          </div>
          <div style={fieldStyle}>
            <label htmlFor="if-desc">{DEV_MSG.IF_FORM_DESCRIPTION}</label>
            <input
              id="if-desc"
              data-testid="developer-if-description"
              style={inputStyle}
              value={description}
              disabled={busy}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="if-parent">{DEV_MSG.IF_FORM_PARENT}</label>
            <input
              id="if-parent"
              data-testid="developer-if-parent"
              style={{ ...inputStyle, fontFamily: "monospace" }}
              value={parentName}
              disabled={busy}
              onChange={(e) => setParentName(e.target.value)}
              autoComplete="off"
            />
          </div>
          <div style={fieldStyle}>
            <label htmlFor="if-authtype">{DEV_MSG.IF_FORM_AUTHTYPE}</label>
            <select
              id="if-authtype"
              data-testid="developer-if-authtype"
              style={inputStyle}
              value={legacyAuthtype}
              disabled={busy}
              onChange={(e) => setLegacyAuthtype(e.target.value)}
            >
              {AUTHTYPE_OPTIONS.map((opt) => (
                <option key={opt.value || "none"} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-if-save"
              aria-label={DEV_MSG.IF_SAVE}
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
              {DEV_MSG.IF_SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-if-cancel"
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
              {DEV_MSG.IF_CANCEL}
            </button>
            {!isNew && writeKey ? (
              <button
                type="button"
                data-testid="developer-if-delete"
                aria-label={DEV_MSG.IF_DELETE}
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
                {DEV_MSG.IF_DELETE}
              </button>
            ) : null}
          </div>

          <section data-testid="developer-if-rules">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.IF_RULES}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DEV_MSG.IF_RULES_HINT}
            </p>
            {draftRules.length === 0 ? (
              <p
                style={{ color: catalogColors.empty }}
                data-testid="developer-if-rules-empty"
              >
                {DEV_MSG.IF_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-if-rules-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_RULE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_PARAMS}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_ACTIONS}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {draftRules.map((r, i) => (
                      <tr
                        key={r.ruleId?.stringValue || `draft-rule-${i}`}
                        data-testid={`developer-if-rule-row-${i}`}
                        style={tableRow}
                      >
                        <td style={{ padding: "8px", verticalAlign: "top" }}>
                          <input
                            data-testid={`developer-if-rule-name-${i}`}
                            style={{ ...inputStyle, fontFamily: "monospace", width: "100%" }}
                            value={r.name ?? ""}
                            disabled={busy}
                            onChange={(e) => updateRule(i, { name: e.target.value })}
                            autoComplete="off"
                            aria-label={DEV_MSG.IF_COL_RULE}
                          />
                        </td>
                        <td style={{ padding: "8px", verticalAlign: "top" }}>
                          <div data-testid={`developer-if-rule-params-${i}`}>
                            {(r.params || []).map((p, pi) => (
                              <div
                                key={`param-${i}-${pi}`}
                                data-testid={`developer-if-rule-param-${i}-${pi}`}
                                style={{
                                  display: "flex",
                                  gap: "6px",
                                  flexWrap: "wrap",
                                  marginBottom: "6px",
                                  alignItems: "center",
                                }}
                              >
                                <input
                                  data-testid={`developer-if-rule-param-name-${i}-${pi}`}
                                  style={{ ...inputStyle, fontFamily: "monospace", flex: "1 1 100px" }}
                                  value={p.name ?? ""}
                                  disabled={busy}
                                  placeholder={DEV_MSG.IF_PARAM_NAME}
                                  onChange={(e) =>
                                    updateRuleParam(i, pi, { name: e.target.value })
                                  }
                                  autoComplete="off"
                                />
                                <input
                                  data-testid={`developer-if-rule-param-value-${i}-${pi}`}
                                  style={{ ...inputStyle, flex: "1 1 100px" }}
                                  value={p.value ?? ""}
                                  disabled={busy}
                                  placeholder={DEV_MSG.IF_PARAM_VALUE}
                                  onChange={(e) =>
                                    updateRuleParam(i, pi, { value: e.target.value })
                                  }
                                  autoComplete="off"
                                />
                                <button
                                  type="button"
                                  data-testid={`developer-if-rule-param-remove-${i}-${pi}`}
                                  aria-label={DEV_MSG.IF_PARAM_REMOVE}
                                  disabled={busy}
                                  onClick={() =>
                                    updateRule(i, {
                                      params: (r.params || []).filter((_, idx) => idx !== pi),
                                    })
                                  }
                                  style={actionButton}
                                >
                                  {DEV_MSG.IF_PARAM_REMOVE}
                                </button>
                              </div>
                            ))}
                            <button
                              type="button"
                              data-testid={`developer-if-rule-param-add-${i}`}
                              aria-label={DEV_MSG.IF_PARAM_ADD}
                              disabled={busy}
                              onClick={() =>
                                updateRule(i, {
                                  params: [...(r.params || []), emptyParam()],
                                })
                              }
                              style={actionButton}
                            >
                              {DEV_MSG.IF_PARAM_ADD}
                            </button>
                          </div>
                        </td>
                        <td style={{ padding: "8px", verticalAlign: "top" }}>
                          <button
                            type="button"
                            data-testid={`developer-if-rule-remove-${i}`}
                            aria-label={DEV_MSG.IF_RULE_REMOVE}
                            disabled={busy}
                            onClick={() =>
                              setDraftRules((prev) => prev.filter((_, idx) => idx !== i))
                            }
                            style={actionButton}
                          >
                            {DEV_MSG.IF_RULE_REMOVE}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div
              style={{
                marginTop: "12px",
                display: "flex",
                gap: "8px",
                flexWrap: "wrap",
              }}
            >
              <button
                type="button"
                data-testid="developer-if-rule-add"
                aria-label={DEV_MSG.IF_RULE_ADD}
                disabled={busy}
                onClick={() => setDraftRules((prev) => [...prev, emptyRule()])}
                style={{
                  ...actionButton,
                  padding: "8px 12px",
                  background: catalogColors.accent,
                  color: "#fff",
                  border: "none",
                  cursor: busy ? "wait" : "pointer",
                }}
              >
                {DEV_MSG.IF_RULE_ADD}
              </button>
              {draftRules.length > 0 ? (
                <button
                  type="button"
                  data-testid="developer-if-rules-clear"
                  aria-label={DEV_MSG.IF_RULES_CLEAR}
                  disabled={busy}
                  onClick={() => setDraftRules([])}
                  style={actionButton}
                >
                  {DEV_MSG.IF_RULES_CLEAR}
                </button>
              ) : null}
            </div>
          </section>
        </>
      ) : null}
      <CatalogConfirmDialog
        open={confirmOpen}
        busy={busy}
        message={DEV_MSG.IF_DELETE_CONFIRM}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => void handleDelete()}
      />
    </div>
  );
}
