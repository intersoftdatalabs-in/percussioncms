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
import { listLocales } from "../api/developer/localesApi";
import type { LocaleSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { LocaleDetailPanel } from "./LocaleDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.9 — CMS locale catalog (CD-18 read) including ISBASE + format profile flag.
 */
export function LocalesPanel(): React.ReactElement {
  const [items, setItems] = useState<LocaleSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listLocales()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.LOC_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.languageString || "").localeCompare(b.languageString || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  if (selected) {
    return <LocaleDetailPanel idOrLang={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-loc-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-loc-loading">{DEV_MSG.LOC_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return <CatalogStatus testId="developer-loc-empty">{DEV_MSG.LOC_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-loc-panel">
      <CatalogHint>{DEV_MSG.LOC_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-loc-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_LANG}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_STATUS}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_BASE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_FORMAT}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.LOC_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((loc, index) => {
              const openKey =
                loc.languageString || (loc.id != null ? String(loc.id) : "");
              const interactive = openKey.length > 0;
              return (
                <tr
                  key={String(loc.id ?? loc.languageString ?? `loc-${index}`)}
                  data-testid="developer-loc-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-loc-open"
                        aria-label={`Open ${loc.languageString || openKey}`}
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
                        {loc.languageString || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{loc.languageString || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{loc.label || "—"}</td>
                  <td style={{ padding: "8px" }}>{loc.status || "—"}</td>
                  <td style={{ padding: "8px" }}>
                    {loc.baseLocale == null
                      ? "—"
                      : loc.baseLocale
                        ? DEV_MSG.YES
                        : DEV_MSG.NO}
                  </td>
                  <td style={{ padding: "8px" }}>
                    {loc.hasFormatProfile
                      ? DEV_MSG.LOC_FORMAT_YES
                      : DEV_MSG.LOC_FORMAT_NO}
                  </td>
                  <td style={{ padding: "8px", ...mutedCell }}>
                    {loc.description || ""}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
