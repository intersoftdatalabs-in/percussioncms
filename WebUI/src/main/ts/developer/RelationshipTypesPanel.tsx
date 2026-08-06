/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listRelationshipTypes } from "../api/developer/relationshipTypesApi";
import type { RelationshipTypeDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { RelationshipTypeDetailPanel } from "./RelationshipTypeDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.16 — relationship type catalog + read-only detail (SY-03).
 */
export function RelationshipTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<RelationshipTypeDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listRelationshipTypes()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.RT_ERROR));
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
    return (
      <RelationshipTypeDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-rt-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-rt-loading">{DEV_MSG.RT_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-rt-empty">{DEV_MSG.RT_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-rt-panel">
      <CatalogHint>{DEV_MSG.RT_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-rt-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_CATEGORY}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_TYPE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.RT_COL_FLAGS}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((t, index) => {
              const openKey = t.name || t.guid?.stringValue || "";
              const interactive = openKey.length > 0;
              const flags: string[] = [];
              if (t.allowCloning) flags.push(DEV_MSG.RT_FLAG_CLONE);
              if (t.systemType) flags.push(DEV_MSG.RT_FLAG_SYSTEM);
              if (t.userType) flags.push(DEV_MSG.RT_FLAG_USER);
              return (
                <tr
                  key={t.name || t.guid?.stringValue || `rt-${index}`}
                  data-testid="developer-rt-row"
                  style={{ ...tableRow, cursor: interactive ? "pointer" : "default"  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-rt-open"
                        aria-label={`Open ${t.name || openKey}`}
                        onClick={(ev) => {
                          ev.stopPropagation();
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
                        {t.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{t.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{t.label || "—"}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>
                    {t.categoryLabel || t.category || "—"}
                  </td>
                  <td style={{ padding: "8px" }}>{t.type || "—"}</td>
                  <td style={{ padding: "8px", fontSize: "0.85rem" }}>
                    {flags.length ? flags.join(", ") : "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
