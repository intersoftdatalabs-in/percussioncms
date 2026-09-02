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
import { listViews } from "../api/developer/viewsApi";
import { resolveViewObjectGuid } from "../api/displayFormatGuid";
import type { ViewDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ViewDetailPanel } from "./ViewDetailPanel";

type SelectedView = {
  idOrName: string | "new";
  /** List-row GUID fallback when detail payload omits stringValue (#3380). */
  catalogGuid?: string;
};

/**
 * UI-07 — CX view catalog with create / save / delete.
 */
export function ViewsPanel(): React.ReactElement {
  const [items, setItems] = useState<ViewDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedView | null>(null);
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
    return listViews()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.VW_ERROR));
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

  /**
   * Keep a just-saved row in the catalog when GET list lags (H2 XML cache).
   * A later reload that already includes the name replaces this merge.
   */
  function handleSaved(detail: ViewDef): void {
    setItems((prev) => upsertViewRow(prev, detail));
    void listViews()
      .then((list) => {
        if (!mountedRef.current) return;
        const name = (detail.name || "").trim();
        const listed =
          !name ||
          list.some((v) => (v.name || "").trim().toLowerCase() === name.toLowerCase());
        setItems(listed ? list : upsertViewRow(list, detail));
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.VW_ERROR));
      });
  }

  const openView = (v: ViewDef) => {
    const idOrName = v.name || resolveViewObjectGuid(v) || "";
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveViewObjectGuid(v),
    });
  };

  if (selected?.idOrName === "new") {
    return (
      <ViewDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={handleSaved}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <ViewDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
        onSaved={handleSaved}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-vw-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-vw-loading">{DEV_MSG.VW_LOADING}</CatalogStatus>;

  return (
    <div data-testid="developer-vw-panel">
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
        <CatalogHint>{DEV_MSG.VW_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-vw-new"
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
          {DEV_MSG.VW_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-vw-empty">{DEV_MSG.VW_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-vw-table"
          rowTestId="developer-vw-row"
          columns={[
            DEV_MSG.VW_COL_NAME,
            DEV_MSG.VW_COL_LABEL,
            DEV_MSG.VW_COL_KIND,
            DEV_MSG.VW_COL_FIELDS,
            DEV_MSG.VW_COL_DESCRIPTION,
          ]}
          rows={sorted.map((v, index) => {
            const openKey = v.name || resolveViewObjectGuid(v) || "";
            const interactive = openKey.length > 0;
            const fieldCount = Array.isArray(v.fields) ? v.fields.length : 0;
            const kind = v.customView
              ? DEV_MSG.VW_KIND_CUSTOM
              : v.standardView
                ? DEV_MSG.VW_KIND_STANDARD
                : v.type || "—";
            return {
              key: resolveViewObjectGuid(v) || v.name || `vw-${index}`,
              onClick: interactive ? () => openView(v) : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-vw-open"
                    data-vw-name={v.name || openKey}
                    aria-label={`Open ${v.name || openKey}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      openView(v);
                    }}
                    style={{ ...openButtonStyle, fontFamily: "monospace" }}
                  >
                    {v.name || "—"}
                  </button>
                ) : (
                  <span key="n" style={monoCell}>
                    {v.name || "—"}
                  </span>
                ),
                v.label || "",
                kind,
                fieldCount,
                <span key="d" style={mutedCell}>
                  {v.description || ""}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}

/** Merge a saved view into a catalog snapshot (create or update by name). */
export function upsertViewRow(items: ViewDef[] | null, detail: ViewDef): ViewDef[] {
  const current = items == null ? [] : items;
  const name = (detail?.name || "").trim();
  if (!name) {
    return current;
  }
  const key = name.toLowerCase();
  const idx = current.findIndex((v) => (v.name || "").trim().toLowerCase() === key);
  if (idx >= 0) {
    const next = current.slice();
    next[idx] = { ...current[idx], ...detail, name: current[idx].name || detail.name };
    return next;
  }
  return [...current, detail];
}
