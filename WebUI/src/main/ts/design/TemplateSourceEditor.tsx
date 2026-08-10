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

import React, { useCallback, useEffect, useState } from "react";
import {
  getTemplateDetail,
  updateSlotDetail,
  updateTemplateDetail,
} from "../api/developer/assemblyApi";
import type {
  TemplateBindingSummary,
  TemplateDetail,
} from "../api/developer/types";
import {
  extractRestErrorMessage,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import {
  backButton,
  catalogColors,
  monoCell,
  mutedCell,
  tableHeaderRow,
  tableRow,
} from "../developer/catalogStyles";
import { AssemblerPicker } from "./AssemblerPicker";
import { isValidAssemblerValue } from "./assemblerOptions";
import { DESIGN_MSG } from "./messages";
import {
  dirtySlotSaves,
  slotRowsDirty,
  TemplateSlotsPanel,
  type SlotEditorRow,
} from "./TemplateSlotsPanel";
import {
  bindingsEqual,
  cloneBindings,
  normalizeBindingsForSave,
  validateBindings,
} from "./templateBindings";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const metaGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  gap: "6px 14px",
  marginTop: "12px",
  fontSize: "0.9rem",
};

const sourceArea: React.CSSProperties = {
  ...inputStyle,
  fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
  fontSize: "0.85rem",
  minHeight: "220px",
  resize: "vertical",
  whiteSpace: "pre",
  lineHeight: 1.45,
};

function editorErrMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DESIGN_MSG.SESSION_REDIRECT;
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) return `${fallback} ${fromBody}`;
    return `${fallback} (${err.status})`;
  }
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

function dash(value: string | number | null | undefined): string {
  if (value == null) return DESIGN_MSG.NONE;
  const s = String(value).trim();
  return s.length > 0 ? s : DESIGN_MSG.NONE;
}

/**
 * Design SPA template editor (#2809 source/JEXL + #2810 assembler/slots).
 * Loads and saves via public REST templates + slots endpoints.
 */
export function TemplateSourceEditor({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<TemplateDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [source, setSource] = useState("");
  const [bindings, setBindings] = useState<TemplateBindingSummary[]>([]);
  const [assembler, setAssembler] = useState("");
  const [slotRows, setSlotRows] = useState<SlotEditorRow[]>([]);

  const onSlotRowsChange = useCallback((rows: SlotEditorRow[]) => {
    setSlotRows(rows);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setValidationError(null);
    setNotice(null);
    setDetail(null);
    setSlotRows([]);
    getTemplateDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setSource(d.templateSource || "");
        setBindings(cloneBindings(d.bindings));
        setAssembler((d.assembler || "").trim());
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(editorErrMsg(e, DESIGN_MSG.EDITOR_LOAD_ERROR));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const initialBindings = cloneBindings(detail?.bindings);
  const initialAssembler = (detail?.assembler || "").trim();
  const dirty =
    detail != null &&
    (source !== (detail.templateSource || "") ||
      assembler !== initialAssembler ||
      !bindingsEqual(bindings, initialBindings) ||
      slotRowsDirty(slotRows));

  function updateBinding(index: number, patch: Partial<TemplateBindingSummary>) {
    setBindings((prev) => prev.map((b, i) => (i === index ? { ...b, ...patch } : b)));
    setNotice(null);
    setValidationError(null);
  }

  function removeBinding(index: number) {
    setBindings((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
    setValidationError(null);
  }

  function addBinding() {
    setBindings((prev) => [
      ...prev,
      {
        executionOrder: prev.length + 1,
        variable: "",
        expression: "",
      },
    ]);
    setNotice(null);
    setValidationError(null);
  }

  async function handleSave() {
    if (!isValidAssemblerValue(assembler)) {
      setValidationError("Assembler is required");
      setError(null);
      setNotice(null);
      return;
    }
    const vErr = validateBindings(bindings);
    if (vErr) {
      setValidationError(vErr);
      setError(null);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setValidationError(null);
    setNotice(null);
    try {
      const saved = await updateTemplateDetail(idOrName, {
        templateSource: source,
        assembler: assembler.trim(),
        bindings: normalizeBindingsForSave(bindings),
      });

      const slotPuts = dirtySlotSaves(slotRows);
      for (const put of slotPuts) {
        try {
          await updateSlotDetail(put.key, {
            slotLayout: put.slotLayout,
            slotStyles: put.slotStyles,
          });
        } catch (slotErr: unknown) {
          setError(editorErrMsg(slotErr, DESIGN_MSG.EDITOR_SLOT_SAVE_ERROR));
          // Keep template save; re-sync template state so slots can retry.
          setDetail(saved);
          setSource(saved.templateSource || "");
          setBindings(cloneBindings(saved.bindings));
          setAssembler((saved.assembler || "").trim());
          setBusy(false);
          return;
        }
      }

      setDetail(saved);
      setSource(saved.templateSource || "");
      setBindings(cloneBindings(saved.bindings));
      setAssembler((saved.assembler || "").trim());
      // Refresh slot baselines after successful puts
      setSlotRows((prev) =>
        prev.map((r) => {
          const put = slotPuts.find((p) => p.key === r.key);
          if (!put) return r;
          return {
            ...r,
            initialLayout: { ...r.layout },
            initialStyles: { ...r.styles },
          };
        }),
      );
      setNotice(DESIGN_MSG.EDITOR_SAVED);
    } catch (err: unknown) {
      setError(editorErrMsg(err, DESIGN_MSG.EDITOR_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="design-tpl-editor">
      <button
        type="button"
        onClick={onBack}
        data-testid="design-tpl-editor-back"
        aria-label={DESIGN_MSG.EDITOR_BACK_ARIA}
        style={backButton}
      >
        {DESIGN_MSG.EDITOR_BACK}
      </button>

      {error ? (
        <div
          role="alert"
          data-testid="design-tpl-editor-error"
          style={{ color: catalogColors.error, marginBottom: "8px" }}
        >
          {error}
        </div>
      ) : null}
      {validationError ? (
        <div
          role="alert"
          data-testid="design-tpl-editor-validation"
          style={{ color: catalogColors.error, marginBottom: "8px" }}
        >
          {validationError}
        </div>
      ) : null}
      {notice ? (
        <div
          data-testid="design-tpl-editor-notice"
          role="status"
          style={{ color: "#276749", marginBottom: "8px" }}
        >
          {notice}
        </div>
      ) : null}

      {loading && detail == null ? (
        <div data-testid="design-tpl-editor-loading">{DESIGN_MSG.EDITOR_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2
              style={{ margin: "0 0 4px" }}
              data-testid="design-tpl-editor-title"
            >
              {detail.label || detail.name || idOrName}
            </h2>
            <div style={mutedCell}>
              <span style={monoCell} data-testid="design-tpl-editor-name">
                {detail.name}
                {detail.templateId != null ? ` · ${detail.templateId}` : ""}
              </span>
            </div>
            <dl style={metaGrid}>
              <dt style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_MIME}</dt>
              <dd style={{ margin: 0, ...monoCell }} data-testid="design-tpl-editor-mime">
                {dash(detail.mimeType)}
              </dd>
              <dt style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_TYPE}</dt>
              <dd style={{ margin: 0, ...monoCell }} data-testid="design-tpl-editor-type">
                {dash(detail.templateType)}
              </dd>
              <dt style={{ color: catalogColors.muted }}>{DESIGN_MSG.FIELD_SLOTS}</dt>
              <dd style={{ margin: 0 }} data-testid="design-tpl-editor-slots">
                {detail.slots?.length ?? 0}
              </dd>
            </dl>
          </header>

          <AssemblerPicker
            value={assembler}
            disabled={busy}
            onChange={(next) => {
              setAssembler(next);
              setNotice(null);
              setValidationError(null);
            }}
          />

          <TemplateSlotsPanel
            slots={detail.slots}
            disabled={busy}
            onRowsChange={onSlotRowsChange}
          />

          <section style={{ marginBottom: "16px" }} data-testid="design-tpl-editor-bindings">
            <h3 style={{ fontSize: "1rem" }}>{DESIGN_MSG.EDITOR_BINDINGS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DESIGN_MSG.EDITOR_BINDINGS_HINT}
            </p>
            {bindings.length === 0 ? (
              <p
                style={{ color: catalogColors.empty }}
                data-testid="design-tpl-editor-bindings-empty"
              >
                {DESIGN_MSG.EDITOR_BINDINGS_EMPTY}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="design-tpl-editor-bindings-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DESIGN_MSG.EDITOR_COL_ORDER}</th>
                      <th style={{ padding: "8px" }}>{DESIGN_MSG.EDITOR_COL_VARIABLE}</th>
                      <th style={{ padding: "8px" }}>{DESIGN_MSG.EDITOR_COL_EXPRESSION}</th>
                      <th style={{ padding: "8px" }}>{DESIGN_MSG.EDITOR_COL_ACTIONS}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bindings.map((b, i) => (
                      <tr
                        key={`design-binding-${i}`}
                        style={tableRow}
                        data-testid={`design-tpl-binding-row-${i}`}
                      >
                        <td style={{ padding: "8px", width: 72 }}>
                          <input
                            type="number"
                            data-testid={`design-tpl-binding-order-${i}`}
                            style={{ ...inputStyle, width: 64 }}
                            value={b.executionOrder ?? i + 1}
                            min={1}
                            disabled={busy}
                            onChange={(e) =>
                              updateBinding(i, {
                                executionOrder: Number(e.target.value) || i + 1,
                              })
                            }
                          />
                        </td>
                        <td style={{ padding: "8px" }}>
                          <input
                            data-testid={`design-tpl-binding-var-${i}`}
                            style={{ ...inputStyle, fontFamily: "monospace" }}
                            value={b.variable || ""}
                            disabled={busy}
                            onChange={(e) =>
                              updateBinding(i, { variable: e.target.value })
                            }
                          />
                        </td>
                        <td style={{ padding: "8px" }}>
                          <input
                            data-testid={`design-tpl-binding-expr-${i}`}
                            style={{ ...inputStyle, fontFamily: "monospace" }}
                            value={b.expression || ""}
                            disabled={busy}
                            onChange={(e) =>
                              updateBinding(i, { expression: e.target.value })
                            }
                          />
                        </td>
                        <td style={{ padding: "8px" }}>
                          <button
                            type="button"
                            data-testid={`design-tpl-binding-remove-${i}`}
                            disabled={busy}
                            onClick={() => removeBinding(i)}
                            style={{
                              background: "transparent",
                              border: `1px solid ${catalogColors.softBorder}`,
                              borderRadius: "4px",
                              padding: "4px 8px",
                              cursor: busy ? "not-allowed" : "pointer",
                            }}
                          >
                            {DESIGN_MSG.EDITOR_BINDING_REMOVE}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <button
              type="button"
              data-testid="design-tpl-binding-add"
              disabled={busy}
              onClick={addBinding}
              style={{
                marginTop: 8,
                padding: "6px 12px",
                background: catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy ? "not-allowed" : "pointer",
              }}
            >
              {DESIGN_MSG.EDITOR_BINDING_ADD}
            </button>
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="design-tpl-editor-source">
            <h3 style={{ fontSize: "1rem" }}>{DESIGN_MSG.EDITOR_SOURCE}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
              {DESIGN_MSG.EDITOR_SOURCE_HINT}
            </p>
            <textarea
              data-testid="design-tpl-editor-source-edit"
              aria-label={DESIGN_MSG.EDITOR_SOURCE}
              style={sourceArea}
              value={source}
              disabled={busy}
              spellCheck={false}
              onChange={(e) => {
                setSource(e.target.value);
                setNotice(null);
                setValidationError(null);
              }}
            />
          </section>

          <div style={{ marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="design-tpl-editor-save"
              aria-label={DESIGN_MSG.EDITOR_SAVE}
              disabled={busy || !dirty}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: dirty ? catalogColors.accent : catalogColors.disabled,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy || !dirty ? "not-allowed" : "pointer",
              }}
            >
              {DESIGN_MSG.EDITOR_SAVE}
            </button>
          </div>
        </>
      ) : null}
    </div>
  );
}
