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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { listSearches } from "../api/developer/searchesApi";
import type { SearchDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SearchDetailPanel } from "./SearchDetailPanel";

/**
 * UI-06 — CX search catalog with create / save / delete.
 */
export function SearchesPanel(): React.ReactElement {
  const [items, setItems] = useState<SearchDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = catalog; "new" = create; otherwise name or GUID. */
  const [selected, setSelected] = useState<string | "new" | null>(null);
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
    return listSearches()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.SR_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || a.label || "").localeCompare(b.name || b.label || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected === "new") {
    return (
      <SearchDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <SearchDetailPanel
        idOrName={selected}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-sr-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-sr-loading">{DEV_MSG.SR_LOADING}</CatalogStatus>;

  return (
    <div data-testid="developer-sr-panel">
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
        <CatalogHint>{DEV_MSG.SR_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-sr-new"
          onClick={() => setSelected("new")}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.SR_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-sr-empty">{DEV_MSG.SR_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-sr-table"
          rowTestId="developer-sr-row"
          columns={[
            DEV_MSG.SR_COL_NAME,
            DEV_MSG.SR_COL_LABEL,
            DEV_MSG.SR_COL_KIND,
            DEV_MSG.SR_COL_FIELDS,
            DEV_MSG.SR_COL_DESCRIPTION,
          ]}
          rows={sorted.map((s, index) => {
            const openKey = s.name || s.guid?.stringValue || "";
            const interactive = openKey.length > 0;
            const fieldCount = Array.isArray(s.fields) ? s.fields.length : 0;
            const kind = s.customSearch
              ? DEV_MSG.SR_KIND_CUSTOM
              : s.userSearch
                ? DEV_MSG.SR_KIND_USER
                : s.standardSearch
                  ? DEV_MSG.SR_KIND_STANDARD
                  : s.type || "—";
            return {
              key: s.guid?.stringValue || s.name || `sr-${index}`,
              onClick: interactive ? () => setSelected(openKey) : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-sr-open"
                    data-sr-name={s.name || openKey}
                    aria-label={`Open ${s.name || openKey}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelected(openKey);
                    }}
                    style={{ ...openButtonStyle, fontFamily: "monospace" }}
                  >
                    {s.name || "—"}
                  </button>
                ) : (
                  <span key="n" style={monoCell}>
                    {s.name || "—"}
                  </span>
                ),
                s.label || "",
                kind,
                fieldCount,
                <span key="d" style={mutedCell}>
                  {s.description || ""}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}
