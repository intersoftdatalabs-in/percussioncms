/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listViews } from "../api/developer/viewsApi";
import { resolveViewObjectGuid } from "../api/displayFormatGuid";
import type { ViewDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ViewDetailPanel } from "./ViewDetailPanel";

type SelectedView = {
  idOrName: string;
  /** List-row GUID fallback when detail payload omits stringValue (#3380). */
  catalogGuid?: string;
};

/**
 * P0.14 — CX view catalog + read-only detail (UI-07 read).
 */
export function ViewsPanel(): React.ReactElement {
  const [items, setItems] = useState<ViewDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedView | null>(null);

  useEffect(() => {
    let cancelled = false;
    listViews()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.VW_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || a.label || "").localeCompare(b.name || b.label || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  const openView = (v: ViewDef) => {
    const idOrName = v.name || resolveViewObjectGuid(v) || "";
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveViewObjectGuid(v),
    });
  };

  if (selected) {
    return (
      <ViewDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
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
  if (items.length === 0)
    return <CatalogStatus testId="developer-vw-empty">{DEV_MSG.VW_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-vw-panel">
      <CatalogHint>{DEV_MSG.VW_HINT}</CatalogHint>
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
    </div>
  );
}
