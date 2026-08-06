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

import React, { useEffect, useMemo, useState } from "react";
import { listDisplayFormats, normalizeColumns } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
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
      <SimpleCatalogTable
        tableTestId="developer-df-table"
        rowTestId="developer-df-row"
        columns={[
          DEV_MSG.DF_COL_NAME,
          DEV_MSG.DF_COL_LABEL,
          DEV_MSG.DF_COL_COLUMNS,
          DEV_MSG.DF_COL_USAGE,
          DEV_MSG.DF_COL_DESCRIPTION,
        ]}
        rows={sorted.map((f, index) => {
          const openKey = f.name || f.internalName || f.guid?.stringValue || "";
          const interactive = openKey.length > 0;
          const colCount = normalizeColumns(f.columns).length;
          const usage: string[] = [];
          if (f.validForFolder) usage.push(DEV_MSG.DF_USAGE_FOLDER);
          if (f.validForViewsAndSearches) usage.push(DEV_MSG.DF_USAGE_VIEWS);
          if (f.validForRelatedContent) usage.push(DEV_MSG.DF_USAGE_RELATED);
          return {
            key: f.guid?.stringValue || f.name || `df-${index}`,
            onClick: interactive ? () => setSelected(openKey) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  data-testid="developer-df-open"
                  aria-label={`Open ${f.name || openKey}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelected(openKey);
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {f.name || "—"}
                </button>
              ) : (
                <span key="n" style={monoCell}>
                  {f.name || "—"}
                </span>
              ),
              f.label || f.displayName || "",
              colCount,
              <span key="u" style={{ fontSize: "0.85rem" }}>
                {usage.length ? usage.join(", ") : "—"}
              </span>,
              <span key="d" style={mutedCell}>
                {f.description || ""}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
