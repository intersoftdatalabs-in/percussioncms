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
import { listItemFilters } from "../api/developer/itemFiltersApi";
import type { ItemFilter } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { ItemFilterDetailPanel } from "./ItemFilterDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.10 — item filter catalog + read-only detail (AS-07 read).
 */
export function ItemFiltersPanel(): React.ReactElement {
  const [items, setItems] = useState<ItemFilter[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listItemFilters()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.IF_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
    );
  }, [items]);

  if (selected) {
    return (
      <ItemFilterDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-if-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-if-loading">{DEV_MSG.IF_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return <CatalogStatus testId="developer-if-empty">{DEV_MSG.IF_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-if-panel">
      <CatalogHint>{DEV_MSG.IF_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-if-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_RULES}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_PARENT}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.IF_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((f, index) => {
              const openKey = f.name || f.filterId?.stringValue || "";
              const interactive = openKey.length > 0;
              const ruleCount = Array.isArray(f.rules) ? f.rules.length : 0;
              return (
                <tr
                  key={f.filterId?.stringValue || f.name || `if-${index}`}
                  data-testid="developer-if-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-if-open"
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
                  <td style={{ padding: "8px" }}>{ruleCount}</td>
                  <td style={{ padding: "8px", ...monoCell }}>
                    {f.parentFilter?.name || ""}
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
