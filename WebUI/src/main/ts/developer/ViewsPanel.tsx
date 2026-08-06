/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listViews } from "../api/developer/viewsApi";
import type { ViewDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ViewDetailPanel } from "./ViewDetailPanel";

/**
 * P0.14 — CX view catalog + read-only detail (UI-07 read).
 */
export function ViewsPanel(): React.ReactElement {
  const [items, setItems] = useState<ViewDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

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

  if (selected) {
    return <ViewDetailPanel idOrName={selected} onBack={() => setSelected(null)} />;
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
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-vw-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_KIND}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_FIELDS}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.VW_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((v, index) => {
              const openKey = v.name || v.guid?.stringValue || "";
              const interactive = openKey.length > 0;
              const fieldCount = Array.isArray(v.fields) ? v.fields.length : 0;
              const kind = v.customView
                ? DEV_MSG.VW_KIND_CUSTOM
                : v.standardView
                  ? DEV_MSG.VW_KIND_STANDARD
                  : v.type || "—";
              return (
                <tr
                  key={v.guid?.stringValue || v.name || `vw-${index}`}
                  data-testid="developer-vw-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-vw-open"
                        aria-label={`Open ${v.name || openKey}`}
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
                        {v.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{v.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{v.label || ""}</td>
                  <td style={{ padding: "8px" }}>{kind}</td>
                  <td style={{ padding: "8px" }}>{fieldCount}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{v.description || ""}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
