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
import { listItemFilters } from "../api/developer/itemFiltersApi";
import type { ItemFilter } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { ItemFilterDetailPanel } from "./ItemFilterDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * AS-07 — item filter catalog with create / save / delete.
 */
export function ItemFiltersPanel(): React.ReactElement {
  const [items, setItems] = useState<ItemFilter[] | null>(null);
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
    return listItemFilters()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.IF_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
    );
  }, [items]);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected === "new") {
    return (
      <ItemFilterDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <ItemFilterDetailPanel
        idOrName={selected}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
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

  return (
    <div data-testid="developer-if-panel">
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
        <CatalogHint>{DEV_MSG.IF_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-if-new"
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
          {DEV_MSG.IF_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-if-empty">{DEV_MSG.IF_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-if-table"
          rowTestId="developer-if-row"
          columns={[
            DEV_MSG.IF_COL_NAME,
            DEV_MSG.IF_COL_RULES,
            DEV_MSG.IF_COL_PARENT,
            DEV_MSG.IF_COL_DESCRIPTION,
          ]}
          rows={sorted.map((f, index) => {
            const openKey = f.name || f.filterId?.stringValue || "";
            const interactive = openKey.length > 0;
            const ruleCount = Array.isArray(f.rules) ? f.rules.length : 0;
            return {
              key: f.filterId?.stringValue || f.name || `if-${index}`,
              onClick: interactive ? () => setSelected(openKey) : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-if-open"
                    data-if-name={f.name || openKey}
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
                ruleCount,
                <span key="p" style={monoCell}>
                  {f.parentFilter?.name || ""}
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
