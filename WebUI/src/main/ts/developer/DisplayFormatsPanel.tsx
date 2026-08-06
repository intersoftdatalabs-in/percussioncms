/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React, { useEffect, useMemo, useState } from "react";
import { listDisplayFormats, normalizeColumns } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DisplayFormatDetailPanel } from "./DisplayFormatDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.11 — display format catalog + read-only detail (UI-05 read).
 */
export function DisplayFormatsPanel(): React.ReactElement {
  const [items, setItems] = useState<DisplayFormat[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listDisplayFormats()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.DF_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || a.displayName || "").localeCompare(
        b.name || b.displayName || "",
        undefined,
        { sensitivity: "base" },
      ),
    );
  }, [items]);

  if (selected) {
    return (
      <DisplayFormatDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-df-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-df-loading">{DEV_MSG.DF_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return <CatalogStatus testId="developer-df-empty">{DEV_MSG.DF_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-df-panel">
      <CatalogHint>{DEV_MSG.DF_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-df-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_COLUMNS}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_USAGE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((f, index) => {
              const openKey = f.name || f.internalName || f.guid?.stringValue || "";
              const interactive = openKey.length > 0;
              const colCount = normalizeColumns(f.columns).length;
              const usage: string[] = [];
              if (f.validForFolder) usage.push(DEV_MSG.DF_USAGE_FOLDER);
              if (f.validForViewsAndSearches) usage.push(DEV_MSG.DF_USAGE_VIEWS);
              if (f.validForRelatedContent) usage.push(DEV_MSG.DF_USAGE_RELATED);
              return (
                <tr
                  key={f.guid?.stringValue || f.name || `df-${index}`}
                  data-testid="developer-df-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-df-open"
                        aria-label={`Open ${f.name || openKey}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelected(openKey);
                        }}
                        style={{
                          background: "transparent",
                          border: "none",
                          color: catalogColors.accent,
                          cursor: "pointer",
                          font: "inherit",
                          padding: 0,
                          fontFamily: "monospace",
                        }}
                      >
                        {f.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{f.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{f.label || f.displayName || ""}</td>
                  <td style={{ padding: "8px" }}>{colCount}</td>
                  <td style={{ padding: "8px", fontSize: "0.85rem" }}>
                    {usage.length ? usage.join(", ") : "—"}
                  </td>
                  <td style={{ padding: "8px", ...mutedCell }}>{f.description || ""}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
