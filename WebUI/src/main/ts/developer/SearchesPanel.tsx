/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listSearches } from "../api/developer/searchesApi";
import type { SearchDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SearchDetailPanel } from "./SearchDetailPanel";

/**
 * P0.13 — CX search catalog + read-only detail (UI-06 read).
 */
export function SearchesPanel(): React.ReactElement {
  const [items, setItems] = useState<SearchDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSearches()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SR_ERROR));
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

  if (selected) {
    return <SearchDetailPanel idOrName={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-sr-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-sr-loading">{DEV_MSG.SR_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-sr-empty">{DEV_MSG.SR_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-sr-panel">
      <CatalogHint>{DEV_MSG.SR_HINT}</CatalogHint>
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
    </div>
  );
}
