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

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  listDisplayFormats,
  normalizeColumns,
  resolveDisplayFormatObjectGuid,
} from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DisplayFormatDetailPanel } from "./DisplayFormatDetailPanel";
import { DEV_MSG } from "./messages";

type SelectedFormat = {
  /** null = catalog; "new" = create; otherwise name or GUID. */
  idOrName: string | "new";
  /** List-row GUID fallback when detail payload omits stringValue (#2951). */
  catalogGuid?: string;
};

/**
 * UI-05 — display format catalog with create / save / delete.
 */
export function DisplayFormatsPanel(): React.ReactElement {
  const [items, setItems] = useState<DisplayFormat[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedFormat | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback((opts?: { showLoading?: boolean }) => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    if (opts?.showLoading) {
      setItems(null);
    }
    setError(null);
    return listDisplayFormats()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.DF_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

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

  const openFormat = (f: DisplayFormat) => {
    const idOrName = f.name || f.internalName || resolveDisplayFormatObjectGuid(f) || "";
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveDisplayFormatObjectGuid(f),
    });
  };

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected?.idOrName === "new") {
    return (
      <DisplayFormatDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <DisplayFormatDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
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

  return (
    <div data-testid="developer-df-panel">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
          gap: "12px",
          flexWrap: "wrap",
        }}
      >
        <CatalogHint>{DEV_MSG.DF_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-df-new"
          onClick={() => setSelected({ idOrName: "new" })}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.DF_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-df-empty">{DEV_MSG.DF_EMPTY}</CatalogStatus>
      ) : (
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
            const openKey =
              f.name || f.internalName || resolveDisplayFormatObjectGuid(f) || "";
            const interactive = openKey.length > 0;
            const colCount = normalizeColumns(f.columns).length;
            const usage: string[] = [];
            if (f.validForFolder) usage.push(DEV_MSG.DF_USAGE_FOLDER);
            if (f.validForViewsAndSearches) usage.push(DEV_MSG.DF_USAGE_VIEWS);
            if (f.validForRelatedContent) usage.push(DEV_MSG.DF_USAGE_RELATED);
            return {
              key: resolveDisplayFormatObjectGuid(f) || f.name || `df-${index}`,
              onClick: interactive ? () => openFormat(f) : undefined,
              // Exact name on the row — Playwright must not use hasText substring (#3269).
              dataAttrs: openKey ? { "data-df-name": openKey } : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-df-open"
                    data-df-name={openKey}
                    aria-label={`Open ${f.name || openKey}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      openFormat(f);
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
      )}
    </div>
  );
}
