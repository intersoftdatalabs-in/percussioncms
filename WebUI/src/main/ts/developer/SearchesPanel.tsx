/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listSearches } from "../api/developer/searchesApi";
import type { SearchDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { monoCell, mutedCell } from "./catalogStyles";
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
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-sr-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_KIND}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_FIELDS}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SR_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((s, index) => {
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
              return (
                <tr
                  key={s.guid?.stringValue || s.name || `sr-${index}`}
                  data-testid="developer-sr-row"
                  style={{
                    borderBottom: "1px solid #edf2f7",
                    cursor: interactive ? "pointer" : "default",
                  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-sr-open"
                        aria-label={`Open ${s.name || openKey}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelected(openKey);
                        }}
                        style={{
                          background: "transparent",
                          border: "none",
                          color: "#007ea8",
                          cursor: "pointer",
                          font: "inherit",
                          padding: 0,
                          fontFamily: "monospace",
                        }}
                      >
                        {s.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{s.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{s.label || ""}</td>
                  <td style={{ padding: "8px" }}>{kind}</td>
                  <td style={{ padding: "8px" }}>{fieldCount}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{s.description || ""}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
