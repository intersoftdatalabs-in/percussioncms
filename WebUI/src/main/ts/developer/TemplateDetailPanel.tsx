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
import {
  getTemplateDetail,
  listSlots,
  updateTemplateDetail,
} from "../api/developer/assemblyApi";
import { designGapCode, designGapKey, formatDesignGap } from "../api/developer/designGaps";
import type {
  SlotSummary,
  TemplateBindingSummary,
  TemplateDetail,
  TemplateSlotSummary,
} from "../api/developer/types";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ObjectAclSection } from "./ObjectAclSection";
import {
  EXPRESSION_CLAMP_MAX_WIDTH_PX,
  bindingExpressionEditorStyle,
  isLongBindingExpression,
} from "./bindingExpressionPreview";
import {
  SOURCE_TOKEN_COLORS,
  copyTextToClipboard,
  highlightTemplateSource,
  lineNumberGutterWidth,
  lineNumbersForSource,
  type SourceToken,
} from "./templateSourceViewer";

const metaGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  gap: "4px 16px",
  marginTop: "12px",
  fontSize: "0.9rem",
};

const sourceShell: React.CSSProperties = {
  background: "#f7fafc",
  border: `1px solid ${catalogColors.headerBorder}`,
  borderRadius: "4px",
  overflow: "hidden",
  maxHeight: "320px",
  fontSize: "0.85rem",
  fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
};

const sourceToolbarBtn: React.CSSProperties = {
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "4px 10px",
  cursor: "pointer",
  font: "inherit",
  fontSize: "0.85rem",
  color: catalogColors.text,
};

function renderSourceTokens(tokens: SourceToken[]): React.ReactNode {
  return tokens.map((tok, i) => (
    <span key={i} style={{ color: SOURCE_TOKEN_COLORS[tok.kind] }}>
      {tok.text.length === 0 ? "\u00a0" : tok.text}
    </span>
  ));
}

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

function cloneBindings(list: TemplateBindingSummary[] | undefined): TemplateBindingSummary[] {
  return (list || []).map((b) => ({
    executionOrder: b.executionOrder,
    variable: b.variable || "",
    expression: b.expression || "",
  }));
}

function BindingExpressionCell({
  index,
  value,
  busy,
  expanded,
  onChange,
  onToggleExpand,
}: {
  index: number;
  value: string;
  busy: boolean;
  expanded: boolean;
  onChange: (next: string) => void;
  onToggleExpand: () => void;
}): React.ReactElement {
  const long = isLongBindingExpression(value);
  const effectiveExpanded = long ? expanded : true;
  const exprStyle = bindingExpressionEditorStyle(effectiveExpanded);
  return (
    <td
      style={{
        padding: "8px",
        maxWidth: EXPRESSION_CLAMP_MAX_WIDTH_PX,
        verticalAlign: "top",
      }}
    >
      <textarea
        data-testid={`developer-tpl-binding-expr-${index}`}
        aria-expanded={long ? expanded : undefined}
        rows={long && !expanded ? 2 : 3}
        style={{
          ...inputStyle,
          fontFamily: "monospace",
          width: "100%",
          minHeight: long && !expanded ? 40 : 56,
          ...exprStyle,
        }}
        value={value}
        disabled={busy}
        spellCheck={false}
        onChange={(e) => onChange(e.target.value)}
      />
      {long ? (
        <button
          type="button"
          data-testid={`developer-tpl-binding-expr-expand-${index}`}
          aria-expanded={expanded}
          disabled={busy}
          onClick={onToggleExpand}
          style={{
            marginTop: 4,
            background: "transparent",
            border: "none",
            padding: 0,
            color: catalogColors.accent,
            cursor: busy ? "not-allowed" : "pointer",
            font: "inherit",
            fontSize: "0.85rem",
            textDecoration: "underline",
          }}
        >
          {expanded ? DEV_MSG.TPL_EXPRESSION_SHOW_LESS : DEV_MSG.TPL_EXPRESSION_SHOW_MORE}
        </button>
      ) : null}
    </td>
  );
}

function bindingsEqual(a: TemplateBindingSummary[], b: TemplateBindingSummary[]): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (
      (a[i].executionOrder ?? null) !== (b[i].executionOrder ?? null) ||
      (a[i].variable || "") !== (b[i].variable || "") ||
      (a[i].expression || "") !== (b[i].expression || "")
    ) {
      return false;
    }
  }
  return true;
}

function slotKey(s: { name?: string; guid?: { stringValue?: string; uuid?: number } }): string {
  if (s.name) return `name:${s.name}`;
  if (s.guid?.stringValue) return `guid:${s.guid.stringValue}`;
  if (s.guid?.uuid != null) return `uuid:${s.guid.uuid}`;
  return "";
}

function slotsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const k of a) {
    if (!b.has(k)) return false;
  }
  return true;
}

export function TemplateDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<TemplateDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [source, setSource] = useState("");
  const [bindings, setBindings] = useState<TemplateBindingSummary[]>([]);
  const [slotKeys, setSlotKeys] = useState<Set<string>>(new Set());
  const [allSlots, setAllSlots] = useState<SlotSummary[]>([]);
  /** When true, show editable textarea; otherwise highlighted preview. */
  const [sourceEditing, setSourceEditing] = useState(true);
  const [copyFeedback, setCopyFeedback] = useState<"idle" | "ok" | "err">("idle");
  /** Row indices with expanded long binding expressions (UI-SRC-02). */
  const [expandedExprRows, setExpandedExprRows] = useState<Set<number>>(
    () => new Set(),
  );
  const copyFeedbackTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const gutterRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    return () => {
      if (copyFeedbackTimer.current != null) {
        clearTimeout(copyFeedbackTimer.current);
      }
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setNotice(null);
    Promise.all([getTemplateDetail(idOrName), listSlots().catch(() => [] as SlotSummary[])])
      .then(([d, slots]) => {
        if (cancelled) return;
        setDetail(d);
        setLabel(d.label || "");
        setDescription(d.description || "");
        setSource(d.templateSource || "");
        setBindings(cloneBindings(d.bindings));
        setExpandedExprRows(new Set());
        setSlotKeys(
          new Set(
            (d.slots || [])
              .map((s) => slotKey(s))
              .filter((k) => k.length > 0),
          ),
        );
        setAllSlots(slots);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.TPL_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const initialBindings = cloneBindings(detail?.bindings);
  const initialSlotKeys = new Set(
    (detail?.slots || []).map((s) => slotKey(s)).filter((k) => k.length > 0),
  );

  const dirty =
    detail != null &&
    (label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      source !== (detail.templateSource || "") ||
      !bindingsEqual(bindings, initialBindings) ||
      !slotsEqual(slotKeys, initialSlotKeys));

  function updateBinding(index: number, patch: Partial<TemplateBindingSummary>) {
    setBindings((prev) => prev.map((b, i) => (i === index ? { ...b, ...patch } : b)));
    setNotice(null);
  }

  function removeBinding(index: number) {
    setBindings((prev) => prev.filter((_, i) => i !== index));
    setExpandedExprRows((prev) => {
      const next = new Set<number>();
      for (const i of prev) {
        if (i < index) next.add(i);
        else if (i > index) next.add(i - 1);
      }
      return next;
    });
    setNotice(null);
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
  }

  function toggleExprExpanded(index: number) {
    setExpandedExprRows((prev) => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  }

  function toggleSlot(key: string) {
    setSlotKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
    setNotice(null);
  }

  async function handleSave() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const slotPayload: TemplateSlotSummary[] = [];
      const seen = new Set<string>();
      for (const s of allSlots) {
        const k = slotKey(s);
        if (!k || !slotKeys.has(k) || seen.has(k)) continue;
        seen.add(k);
        slotPayload.push({
          name: s.name,
          label: s.label,
          guid: s.guid,
        });
      }
      // Keep any selected keys not in catalog (orphan names)
      for (const k of slotKeys) {
        if (seen.has(k)) continue;
        if (k.startsWith("name:")) {
          slotPayload.push({ name: k.slice(5) });
        } else if (k.startsWith("guid:")) {
          slotPayload.push({ guid: { stringValue: k.slice(5) } });
        }
      }

      const saved = await updateTemplateDetail(idOrName, {
        label,
        description,
        templateSource: source,
        bindings: bindings.map((b, i) => ({
          executionOrder: b.executionOrder != null && b.executionOrder > 0 ? b.executionOrder : i + 1,
          variable: (b.variable || "").trim(),
          expression: (b.expression || "").trim(),
        })),
        slots: slotPayload,
      });
      setDetail(saved);
      setLabel(saved.label || "");
      setDescription(saved.description || "");
      setSource(saved.templateSource || "");
      setBindings(cloneBindings(saved.bindings));
      setSlotKeys(
        new Set(
          (saved.slots || [])
            .map((s) => slotKey(s))
            .filter((x) => x.length > 0),
        ),
      );
      setNotice(DEV_MSG.TPL_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.TPL_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  const slotsForTable =
    allSlots.length > 0
      ? allSlots
      : (detail?.slots || []).map((s) => ({
          name: s.name,
          label: s.label,
          guid: s.guid,
          description: s.description,
        }));

  const sourceLines = useMemo(() => lineNumbersForSource(source), [source]);
  const highlighted = useMemo(
    () => (sourceEditing ? null : highlightTemplateSource(source)),
    [source, sourceEditing],
  );
  const gutterDigits = lineNumberGutterWidth(sourceLines.length);

  function syncSourceScroll(scrollTop: number) {
    if (gutterRef.current) {
      gutterRef.current.scrollTop = scrollTop;
    }
  }

  async function handleCopySource() {
    const ok = await copyTextToClipboard(source);
    setCopyFeedback(ok ? "ok" : "err");
    if (copyFeedbackTimer.current != null) {
      clearTimeout(copyFeedbackTimer.current);
    }
    copyFeedbackTimer.current = setTimeout(() => setCopyFeedback("idle"), 2000);
  }

  return (
    <div data-testid="developer-tpl-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-tpl-back"
        aria-label="Back to templates list"
        style={{
          marginBottom: "12px",
          background: "transparent",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: "4px",
          padding: "6px 12px",
          cursor: "pointer",
        }}
      >
        ← {DEV_MSG.TPL_BACK}
      </button>

      {error ? (
        <div
          role="alert"
          data-testid="developer-tpl-detail-error"
          style={{ color: catalogColors.error }}
        >
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-tpl-detail-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {loading && detail == null ? (
        <div data-testid="developer-tpl-detail-loading">
          {DEV_MSG.TPL_DETAIL_LOADING}
        </div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-tpl-detail-title">
              {label || detail.name || idOrName}
            </h2>
            <div style={mutedCell}>
              <span style={monoCell}>
                {detail.name}
                {detail.templateId != null ? ` · ${detail.templateId}` : ""}
                {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
              </span>
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="tpl-label" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.TPL_FORM_LABEL}
              </label>
              <input
                id="tpl-label"
                data-testid="developer-tpl-label"
                style={inputStyle}
                value={label}
                onChange={(e) => setLabel(e.target.value)}
              />
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="tpl-desc" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.TPL_FORM_DESCRIPTION}
              </label>
              <input
                id="tpl-desc"
                data-testid="developer-tpl-description"
                style={inputStyle}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.TPL_META_ASSEMBLER}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.assembler || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_OUTPUT}</dt>
              <dd style={{ margin: 0 }}>{detail.outputFormat || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.templateType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_AA}</dt>
              <dd style={{ margin: 0 }}>{detail.aaType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_MIME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.mimeType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_VARIANT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.variant ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-bindings">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_BINDINGS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.TPL_BINDINGS_HINT}</p>
            {bindings.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.TPL_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-tpl-bindings-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_ORDER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_VARIABLE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_EXPRESSION}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_ACTIONS}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bindings.map((b, i) => (
                      <tr
                        key={`binding-${i}`}
                        style={tableRow}
                        data-testid={`developer-tpl-binding-row-${i}`}
                      >
                        <td style={{ padding: "8px", width: 72 }}>
                          <input
                            type="number"
                            data-testid={`developer-tpl-binding-order-${i}`}
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
                            data-testid={`developer-tpl-binding-var-${i}`}
                            style={{ ...inputStyle, fontFamily: "monospace" }}
                            value={b.variable || ""}
                            disabled={busy}
                            onChange={(e) => updateBinding(i, { variable: e.target.value })}
                          />
                        </td>
                        <BindingExpressionCell
                          index={i}
                          value={b.expression || ""}
                          busy={busy}
                          expanded={expandedExprRows.has(i)}
                          onChange={(next) => updateBinding(i, { expression: next })}
                          onToggleExpand={() => toggleExprExpanded(i)}
                        />
                        <td style={{ padding: "8px" }}>
                          <button
                            type="button"
                            data-testid={`developer-tpl-binding-remove-${i}`}
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
                            {DEV_MSG.TPL_BINDING_REMOVE}
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
              data-testid="developer-tpl-binding-add"
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
              {DEV_MSG.TPL_BINDING_ADD}
            </button>
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-slots">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_SLOTS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.TPL_SLOTS_HINT}</p>
            {slotsForTable.length === 0 ? (
              <p style={{ color: catalogColors.empty }}>{DEV_MSG.TPL_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-tpl-slots-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={tableHeaderRow}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_MEMBER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_LABEL}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_NAME}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {slotsForTable.map((s, i) => {
                      const key = slotKey(s) || `idx:${i}`;
                      const checked = slotKeys.has(key);
                      return (
                        <tr
                          key={key}
                          style={tableRow}
                        >
                          <td style={{ padding: "8px" }}>
                            <input
                              type="checkbox"
                              data-testid={`developer-tpl-slot-check-${key}`}
                              checked={checked}
                              disabled={busy}
                              onChange={() => toggleSlot(key)}
                              aria-label={`Include slot ${s.label || s.name || key}`}
                            />
                          </td>
                          <td style={{ padding: "8px" }}>{s.label || "—"}</td>
                          <td style={{ padding: "8px", fontFamily: "monospace" }}>
                            {s.name || "—"}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-source">
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "8px",
                flexWrap: "wrap",
                marginBottom: "8px",
              }}
            >
              <h3 style={{ fontSize: "1rem", margin: 0 }}>{DEV_MSG.TPL_SOURCE}</h3>
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-tpl-source-mode"
                  onClick={() => setSourceEditing((v) => !v)}
                  style={sourceToolbarBtn}
                >
                  {sourceEditing ? DEV_MSG.TPL_SOURCE_PREVIEW : DEV_MSG.TPL_SOURCE_EDIT}
                </button>
                <button
                  type="button"
                  data-testid="developer-tpl-source-copy"
                  onClick={() => void handleCopySource()}
                  style={sourceToolbarBtn}
                  aria-label={DEV_MSG.TPL_SOURCE_COPY}
                >
                  {DEV_MSG.TPL_SOURCE_COPY}
                </button>
                {copyFeedback !== "idle" ? (
                  <span
                    data-testid="developer-tpl-source-copy-feedback"
                    role="status"
                    style={{
                      fontSize: "0.85rem",
                      color: copyFeedback === "ok" ? "#276749" : catalogColors.error,
                    }}
                  >
                    {copyFeedback === "ok"
                      ? DEV_MSG.TPL_SOURCE_COPIED
                      : DEV_MSG.TPL_SOURCE_COPY_FAILED}
                  </span>
                ) : null}
              </div>
            </div>
            <div style={{ ...sourceShell, display: "flex", minHeight: 200 }}>
              <div
                ref={gutterRef}
                data-testid="developer-tpl-source-lines"
                aria-label={DEV_MSG.TPL_SOURCE_LINE_NUMBERS_ARIA}
                style={{
                  flex: "0 0 auto",
                  minWidth: `${Math.max(2, gutterDigits) + 1}ch`,
                  padding: "12px 8px",
                  textAlign: "right",
                  color: catalogColors.empty,
                  background: "#edf2f7",
                  borderRight: `1px solid ${catalogColors.headerBorder}`,
                  overflow: "hidden",
                  userSelect: "none",
                  lineHeight: 1.45,
                  whiteSpace: "pre",
                }}
              >
                {sourceLines.map((n) => (
                  <div key={n} data-testid={`developer-tpl-source-ln-${n}`}>
                    {n}
                  </div>
                ))}
              </div>
              {sourceEditing ? (
                <textarea
                  data-testid="developer-tpl-source-edit"
                  style={{
                    flex: 1,
                    minWidth: 0,
                    border: "none",
                    outline: "none",
                    resize: "vertical",
                    padding: "12px",
                    background: "transparent",
                    font: "inherit",
                    lineHeight: 1.45,
                    minHeight: 200,
                    boxSizing: "border-box",
                    whiteSpace: "pre",
                    overflow: "auto",
                  }}
                  value={source}
                  onChange={(e) => {
                    setSource(e.target.value);
                    setNotice(null);
                  }}
                  onScroll={(e) => syncSourceScroll(e.currentTarget.scrollTop)}
                  spellCheck={false}
                  disabled={busy}
                />
              ) : (
                <pre
                  data-testid="developer-tpl-source-preview"
                  style={{
                    flex: 1,
                    minWidth: 0,
                    margin: 0,
                    padding: "12px",
                    overflow: "auto",
                    lineHeight: 1.45,
                    whiteSpace: "pre",
                    font: "inherit",
                  }}
                  onScroll={(e) => syncSourceScroll(e.currentTarget.scrollTop)}
                >
                  {(highlighted || []).map((line) => (
                    <div key={line.lineNumber} data-testid={`developer-tpl-source-hl-${line.lineNumber}`}>
                      {renderSourceTokens(line.tokens)}
                    </div>
                  ))}
                </pre>
              )}
            </div>
          </section>

          <div style={{ marginBottom: "16px" }}>
            <button
              type="button"
              data-testid="developer-tpl-save"
              aria-label="Save template"
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
              {DEV_MSG.TPL_SAVE}
            </button>
          </div>

          <ObjectAclSection
            objectGuid={detail.guid?.stringValue}
            objectKind="template"
            testIdPrefix="developer-tpl-acl"
          />

          {(detail.designGaps || []).length > 0 ? (
            <section data-testid="developer-tpl-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {(detail.designGaps || []).map((g, i) => (
                  <li key={designGapKey(g, i)} data-gap-code={designGapCode(g)}>
                    {formatDesignGap(g)}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
