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

import React, { useEffect, useState } from "react";
import { getSlotDetail } from "../api/developer/assemblyApi";
import type { TemplateSlotSummary } from "../api/developer/types";
import {
  extractRestErrorMessage,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import { catalogColors, monoCell } from "../developer/catalogStyles";
import { DESIGN_MSG } from "./messages";
import {
  emptyLayoutDraft,
  emptyStylesDraft,
  layoutDraftFromMap,
  layoutDraftsEqual,
  layoutMapFromDraft,
  stylesDraftFromMap,
  stylesDraftsEqual,
  stylesMapFromDraft,
  templateSlotKey,
  type SlotLayoutDraft,
  type SlotStylesDraft,
} from "./slotLayoutStyles";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const cardStyle: React.CSSProperties = {
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "6px",
  padding: "12px",
  marginBottom: "12px",
  background: "#fafbfc",
};

export type SlotEditorRow = {
  key: string;
  label: string;
  name: string;
  layout: SlotLayoutDraft;
  styles: SlotStylesDraft;
  initialLayout: SlotLayoutDraft;
  initialStyles: SlotStylesDraft;
  loadError?: string;
};

function errMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DESIGN_MSG.SESSION_REDIRECT;
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) return `${fallback} ${fromBody}`;
    return `${fallback} (${err.status})`;
  }
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

function fieldGrid(): React.CSSProperties {
  return {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
    gap: "10px",
    marginTop: "8px",
  };
}

/**
 * Structured visual editor for template slot_layout / slot_styles (#2810).
 * Loads each slot via GET /services/slots/{idOrName}; parent saves dirty rows.
 */
export function TemplateSlotsPanel({
  slots,
  disabled,
  onRowsChange,
}: {
  slots: TemplateSlotSummary[] | undefined;
  disabled?: boolean;
  /** Called when drafts load or user edits (parent tracks dirty + save). */
  onRowsChange: (rows: SlotEditorRow[]) => void;
}): React.ReactElement {
  const [rows, setRows] = useState<SlotEditorRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const list = slots || [];
    if (list.length === 0) {
      setRows([]);
      setLoading(false);
      setError(null);
      onRowsChange([]);
      return;
    }

    setLoading(true);
    setError(null);

    (async () => {
      const next: SlotEditorRow[] = [];
      for (let i = 0; i < list.length; i++) {
        const s = list[i]!;
        const key = templateSlotKey(s) || `slot-${i}`;
        const label = s.label || s.name || key;
        try {
          const detail = await getSlotDetail(key);
          const layout = layoutDraftFromMap(
            detail.slotLayout as Record<string, unknown> | undefined,
          );
          const styles = stylesDraftFromMap(
            detail.slotStyles as Record<string, unknown> | undefined,
          );
          next.push({
            key,
            label,
            name: detail.name || s.name || key,
            layout,
            styles,
            initialLayout: { ...layout },
            initialStyles: { ...styles },
          });
        } catch (e: unknown) {
          next.push({
            key,
            label,
            name: s.name || key,
            layout: emptyLayoutDraft(),
            styles: emptyStylesDraft(),
            initialLayout: emptyLayoutDraft(),
            initialStyles: emptyStylesDraft(),
            loadError: errMsg(e, DESIGN_MSG.EDITOR_SLOTS_ERROR),
          });
        }
      }
      if (cancelled) return;
      setRows(next);
      setLoading(false);
      onRowsChange(next);
    })().catch((e: unknown) => {
      if (cancelled) return;
      setError(errMsg(e, DESIGN_MSG.EDITOR_SLOTS_ERROR));
      setLoading(false);
      setRows([]);
      onRowsChange([]);
    });

    return () => {
      cancelled = true;
    };
    // Parent remounts editor when template id changes; avoid thrashing on callback identity.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slots]);

  function patchRow(index: number, patch: Partial<SlotEditorRow>) {
    setRows((prev) => {
      const next = prev.map((r, i) => (i === index ? { ...r, ...patch } : r));
      onRowsChange(next);
      return next;
    });
  }

  function patchLayout(index: number, field: keyof SlotLayoutDraft, value: string) {
    setRows((prev) => {
      const next = prev.map((r, i) =>
        i === index ? { ...r, layout: { ...r.layout, [field]: value } } : r,
      );
      onRowsChange(next);
      return next;
    });
  }

  function patchStyles(index: number, field: keyof SlotStylesDraft, value: string) {
    setRows((prev) => {
      const next = prev.map((r, i) =>
        i === index ? { ...r, styles: { ...r.styles, [field]: value } } : r,
      );
      onRowsChange(next);
      return next;
    });
  }

  return (
    <section data-testid="design-tpl-slots" style={{ marginBottom: "16px" }}>
      <h3 style={{ fontSize: "1rem" }}>{DESIGN_MSG.EDITOR_SLOTS}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
        {DESIGN_MSG.EDITOR_SLOTS_HINT}
      </p>

      {error ? (
        <div role="alert" data-testid="design-tpl-slots-error" style={{ color: catalogColors.error }}>
          {error}
        </div>
      ) : null}

      {loading ? (
        <div data-testid="design-tpl-slots-loading">{DESIGN_MSG.EDITOR_SLOTS_LOADING}</div>
      ) : null}

      {!loading && !error && (slots || []).length === 0 ? (
        <p style={{ color: catalogColors.empty }} data-testid="design-tpl-slots-empty">
          {DESIGN_MSG.EDITOR_SLOTS_EMPTY}
        </p>
      ) : null}

      {!loading &&
        rows.map((row, i) => (
          <div
            key={row.key}
            data-testid={`design-tpl-slot-card-${i}`}
            style={cardStyle}
          >
            <div style={{ fontWeight: 600 }} data-testid={`design-tpl-slot-title-${i}`}>
              {row.label}
              <span style={{ ...monoCell, marginLeft: 8, fontWeight: 400 }}>
                {row.name}
              </span>
            </div>
            {row.loadError ? (
              <div
                role="alert"
                data-testid={`design-tpl-slot-error-${i}`}
                style={{ color: catalogColors.error, marginTop: 6 }}
              >
                {row.loadError}
              </div>
            ) : null}

            <div style={{ marginTop: 10 }}>
              <div style={{ fontSize: "0.9rem", color: catalogColors.muted }}>
                {DESIGN_MSG.EDITOR_SLOT_LAYOUT}
              </div>
              <div style={fieldGrid()}>
                <div>
                  <label htmlFor={`slot-orient-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_ORIENTATION}
                  </label>
                  <select
                    id={`slot-orient-${i}`}
                    data-testid={`design-tpl-slot-orientation-${i}`}
                    style={inputStyle}
                    value={row.layout.orientation}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchLayout(i, "orientation", e.target.value)}
                  >
                    <option value="">{DESIGN_MSG.EDITOR_SLOT_ORIENTATION_NONE}</option>
                    <option value="horizontal">{DESIGN_MSG.EDITOR_SLOT_ORIENTATION_H}</option>
                    <option value="vertical">{DESIGN_MSG.EDITOR_SLOT_ORIENTATION_V}</option>
                  </select>
                </div>
                <div>
                  <label htmlFor={`slot-cols-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_COLUMNS}
                  </label>
                  <input
                    id={`slot-cols-${i}`}
                    data-testid={`design-tpl-slot-columns-${i}`}
                    style={inputStyle}
                    value={row.layout.columns}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchLayout(i, "columns", e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor={`slot-max-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_MAX_ITEMS}
                  </label>
                  <input
                    id={`slot-max-${i}`}
                    data-testid={`design-tpl-slot-maxitems-${i}`}
                    style={inputStyle}
                    value={row.layout.maxItems}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchLayout(i, "maxItems", e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor={`slot-empty-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_EMPTY_STATE}
                  </label>
                  <input
                    id={`slot-empty-${i}`}
                    data-testid={`design-tpl-slot-emptystate-${i}`}
                    style={inputStyle}
                    value={row.layout.emptyState}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchLayout(i, "emptyState", e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor={`slot-wrap-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_WRAPPER}
                  </label>
                  <input
                    id={`slot-wrap-${i}`}
                    data-testid={`design-tpl-slot-wrapper-${i}`}
                    style={inputStyle}
                    value={row.layout.wrapperClassPolicy}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchLayout(i, "wrapperClassPolicy", e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div style={{ marginTop: 12 }}>
              <div style={{ fontSize: "0.9rem", color: catalogColors.muted }}>
                {DESIGN_MSG.EDITOR_SLOT_STYLES}
              </div>
              <div style={fieldGrid()}>
                <div>
                  <label htmlFor={`slot-root-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_ROOTCLASS}
                  </label>
                  <input
                    id={`slot-root-${i}`}
                    data-testid={`design-tpl-slot-rootclass-${i}`}
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={row.styles.rootclass}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchStyles(i, "rootclass", e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor={`slot-item-${i}`} style={{ display: "block", marginBottom: 4 }}>
                    {DESIGN_MSG.EDITOR_SLOT_ITEMCLASS}
                  </label>
                  <input
                    id={`slot-item-${i}`}
                    data-testid={`design-tpl-slot-itemclass-${i}`}
                    style={{ ...inputStyle, fontFamily: "monospace" }}
                    value={row.styles.itemclass}
                    disabled={disabled || Boolean(row.loadError)}
                    onChange={(e) => patchStyles(i, "itemclass", e.target.value)}
                  />
                </div>
              </div>
            </div>
          </div>
        ))}
    </section>
  );
}

/** True when any loaded row has layout/styles edits. */
export function slotRowsDirty(rows: SlotEditorRow[]): boolean {
  return rows.some(
    (r) =>
      !r.loadError &&
      (!layoutDraftsEqual(r.layout, r.initialLayout) ||
        !stylesDraftsEqual(r.styles, r.initialStyles)),
  );
}

/** Payload pieces for dirty slot PUTs. */
export function dirtySlotSaves(
  rows: SlotEditorRow[],
): { key: string; slotLayout: Record<string, unknown>; slotStyles: Record<string, unknown> }[] {
  return rows
    .filter(
      (r) =>
        !r.loadError &&
        (!layoutDraftsEqual(r.layout, r.initialLayout) ||
          !stylesDraftsEqual(r.styles, r.initialStyles)),
    )
    .map((r) => ({
      key: r.key,
      slotLayout: layoutMapFromDraft(r.layout),
      slotStyles: stylesMapFromDraft(r.styles),
    }));
}
