/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listActionMenus } from "../api/developer/actionMenusApi";
import type { ActionMenu } from "../api/developer/types";
import { ActionMenuDetailPanel } from "./ActionMenuDetailPanel";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/**
 * P0.12 — action menu catalog + read-only detail (UI-02 read).
 */
export function ActionMenusPanel(): React.ReactElement {
  const [items, setItems] = useState<ActionMenu[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listActionMenus()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.AM_ERROR));
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
    return <ActionMenuDetailPanel idOrName={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-am-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-am-loading">{DEV_MSG.AM_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return <CatalogStatus testId="developer-am-empty">{DEV_MSG.AM_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-am-panel">
      <CatalogHint>{DEV_MSG.AM_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-am-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_TYPE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_HANDLER}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.AM_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((m, index) => {
              const openKey = m.name || (m.id != null ? String(m.id) : "");
              const interactive = openKey.length > 0;
              return (
                <tr
                  key={m.guid?.stringValue || m.name || `am-${index}`}
                  data-testid="developer-am-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-am-open"
                        aria-label={`Open ${m.name || openKey}`}
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
                        {m.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{m.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{m.label || ""}</td>
                  <td style={{ padding: "8px", ...monoCell }}>{m.menuType || "—"}</td>
                  <td style={{ padding: "8px" }}>{m.handler || "—"}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{m.description || ""}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
