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
  listVelocitySnippets,
  type VelocitySnippet,
} from "../api/developer/velocitySnippetsApi";
import {
  captureDialogOpener,
  useDialogEscape,
  useDialogFocusTrap,
} from "../architecture/useDialogEscape";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export interface SnippetLibraryDialogProps {
  open: boolean;
  onCancel: () => void;
  /** Called with the selected catalog insert text. */
  onInsert: (insertText: string, snippet: VelocitySnippet) => void;
}

const CATEGORIES = ["all", "field", "slot", "misc"] as const;
type CategoryFilter = (typeof CATEGORIES)[number];

const overlayStyle: React.CSSProperties = {
  position: "fixed",
  inset: 0,
  background: "rgba(15, 23, 42, 0.45)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
  padding: 16,
};

const panelStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  border: `1px solid ${catalogColors.headerBorder}`,
  maxWidth: 720,
  width: "100%",
  maxHeight: "90vh",
  overflow: "auto",
  padding: "1.25rem 1.5rem",
  boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
};

const filterBtn = (active: boolean): React.CSSProperties => ({
  background: active ? catalogColors.accent : "transparent",
  color: active ? "#fff" : catalogColors.text,
  border: `1px solid ${active ? catalogColors.accent : catalogColors.softBorder}`,
  borderRadius: 4,
  padding: "4px 10px",
  cursor: "pointer",
  font: "inherit",
  fontSize: "0.85rem",
});

function categoryLabel(cat: CategoryFilter): string {
  switch (cat) {
    case "all":
      return DEV_MSG.TPL_SNIPPET_CAT_ALL;
    case "field":
      return DEV_MSG.TPL_SNIPPET_CAT_FIELD;
    case "slot":
      return DEV_MSG.TPL_SNIPPET_CAT_SLOT;
    case "misc":
      return DEV_MSG.TPL_SNIPPET_CAT_MISC;
    default:
      return cat;
  }
}

/** Table category uses the same labels as filter buttons (server casing ignored). */
function tableCategoryLabel(raw: string | undefined): string {
  const c = (raw || "").trim().toLowerCase();
  if (c === "field" || c === "slot" || c === "misc") {
    return categoryLabel(c);
  }
  return raw || "—";
}

/**
 * AS-09 snippet library picker for Developer template source.
 * Loads {@code GET /services/velocity/snippets} and inserts catalog text.
 */
export function SnippetLibraryDialog({
  open,
  onCancel,
  onInsert,
}: SnippetLibraryDialogProps): React.ReactElement | null {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [snippets, setSnippets] = useState<VelocitySnippet[]>([]);
  const [category, setCategory] = useState<CategoryFilter>("all");
  const [query, setQuery] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDivElement | null>(null);

  useDialogEscape(open, loading, onCancel);
  useDialogFocusTrap(open, dialogRef);

  useEffect(() => {
    if (!open) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setSelectedId(null);
    setQuery("");
    setCategory("all");
    listVelocitySnippets()
      .then((list) => {
        if (cancelled) return;
        setSnippets(Array.isArray(list) ? list : []);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.TPL_SNIPPET_LOAD_ERROR));
        setSnippets([]);
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return snippets.filter((s) => {
      if (category !== "all" && (s.category || "").toLowerCase() !== category) {
        return false;
      }
      if (!q) return true;
      return (
        (s.title || "").toLowerCase().includes(q) ||
        (s.id || "").toLowerCase().includes(q) ||
        (s.insertText || "").toLowerCase().includes(q)
      );
    });
  }, [snippets, category, query]);

  const selected = useMemo(
    () => filtered.find((s) => s.id === selectedId) ?? null,
    [filtered, selectedId],
  );

  if (!open) {
    return null;
  }

  function handleInsert(snip: VelocitySnippet) {
    const text = snip.insertText || "";
    if (!text) {
      return;
    }
    onInsert(text, snip);
  }

  return (
    <div
      ref={dialogRef}
      style={overlayStyle}
      data-testid="developer-tpl-snippet-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="developer-tpl-snippet-title"
      aria-describedby="developer-tpl-snippet-hint"
    >
      <div style={panelStyle}>
        <h2 id="developer-tpl-snippet-title" style={{ marginTop: 0 }}>
          {DEV_MSG.TPL_SNIPPET_TITLE}
        </h2>
        <p
          id="developer-tpl-snippet-hint"
          style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
        >
          {DEV_MSG.TPL_SNIPPET_HINT}
        </p>

        {error ? (
          <div
            role="alert"
            data-testid="developer-tpl-snippet-error"
            style={{ color: catalogColors.error, marginBottom: 8 }}
          >
            {error}
          </div>
        ) : null}

        <div
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 8,
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <label htmlFor="tpl-snippet-filter" style={{ fontSize: "0.9rem" }}>
            {DEV_MSG.TPL_SNIPPET_FILTER}
          </label>
          <input
            id="tpl-snippet-filter"
            data-testid="developer-tpl-snippet-filter"
            type="search"
            value={query}
            disabled={loading}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={DEV_MSG.TPL_SNIPPET_FILTER_PLACEHOLDER}
            style={{
              flex: "1 1 160px",
              padding: "6px 8px",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              font: "inherit",
            }}
          />
          <div
            role="group"
            aria-label={DEV_MSG.TPL_SNIPPET_CATEGORY}
            style={{ display: "flex", flexWrap: "wrap", gap: 4 }}
          >
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                data-testid={`developer-tpl-snippet-cat-${cat}`}
                aria-pressed={category === cat}
                disabled={loading}
                onClick={() => setCategory(cat)}
                style={filterBtn(category === cat)}
              >
                {categoryLabel(cat)}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div data-testid="developer-tpl-snippet-loading">
            {DEV_MSG.TPL_SNIPPET_LOADING}
          </div>
        ) : filtered.length === 0 ? (
          <p
            data-testid="developer-tpl-snippet-empty"
            style={{ color: catalogColors.empty }}
          >
            {DEV_MSG.TPL_SNIPPET_EMPTY}
          </p>
        ) : (
          <div style={{ overflowX: "auto", maxHeight: 280 }}>
            <table
              data-testid="developer-tpl-snippet-table"
              style={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "0.9rem",
              }}
            >
              <thead>
                <tr style={tableHeaderRow}>
                  <th style={{ padding: "8px", textAlign: "left" }}>
                    {DEV_MSG.TPL_SNIPPET_COL_TITLE}
                  </th>
                  <th style={{ padding: "8px", textAlign: "left" }}>
                    {DEV_MSG.TPL_SNIPPET_COL_CATEGORY}
                  </th>
                  <th style={{ padding: "8px", textAlign: "left" }}>
                    {DEV_MSG.TPL_SNIPPET_COL_ID}
                  </th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s) => {
                  const active = s.id === selectedId;
                  return (
                    <tr
                      key={s.id}
                      style={{
                        ...tableRow,
                        background: active ? "#ebf8ff" : undefined,
                        cursor: "pointer",
                      }}
                      data-testid={`developer-tpl-snippet-row-${s.id}`}
                      aria-selected={active}
                      onClick={() => setSelectedId(s.id)}
                      onDoubleClick={() => handleInsert(s)}
                    >
                      <td style={{ padding: "8px" }}>{s.title || "—"}</td>
                      <td style={{ padding: "8px", ...mutedCell }}>
                        {tableCategoryLabel(s.category)}
                      </td>
                      <td style={{ padding: "8px", ...monoCell }}>{s.id}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {selected ? (
          <pre
            data-testid="developer-tpl-snippet-preview"
            style={{
              marginTop: 12,
              padding: 12,
              background: "#f7fafc",
              border: `1px solid ${catalogColors.headerBorder}`,
              borderRadius: 4,
              fontSize: "0.85rem",
              fontFamily:
                "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace",
              whiteSpace: "pre-wrap",
              maxHeight: 120,
              overflow: "auto",
            }}
          >
            {selected.insertText}
          </pre>
        ) : null}

        <div
          style={{
            display: "flex",
            gap: 8,
            justifyContent: "flex-end",
            marginTop: 16,
          }}
        >
          <button
            type="button"
            data-testid="developer-tpl-snippet-cancel"
            disabled={loading}
            onClick={onCancel}
            style={{
              background: "transparent",
              border: `1px solid ${catalogColors.softBorder}`,
              borderRadius: 4,
              padding: "6px 12px",
              cursor: loading ? "not-allowed" : "pointer",
              font: "inherit",
            }}
          >
            {DEV_MSG.TPL_SNIPPET_CANCEL}
          </button>
          <button
            type="button"
            data-testid="developer-tpl-snippet-insert"
            disabled={loading || !selected || !(selected.insertText || "").length}
            onClick={() => {
              if (selected) {
                handleInsert(selected);
              }
            }}
            style={{
              background:
                selected && (selected.insertText || "").length
                  ? catalogColors.accent
                  : catalogColors.disabled,
              color: "#fff",
              border: "none",
              borderRadius: 4,
              padding: "6px 12px",
              cursor:
                loading || !selected || !(selected.insertText || "").length
                  ? "not-allowed"
                  : "pointer",
              font: "inherit",
            }}
          >
            {DEV_MSG.TPL_SNIPPET_INSERT}
          </button>
        </div>
      </div>
    </div>
  );
}

/** Open helper — capture the toolbar control before setting open=true. */
export function openSnippetLibrary(ev: React.SyntheticEvent): void {
  captureDialogOpener(ev.currentTarget);
}
