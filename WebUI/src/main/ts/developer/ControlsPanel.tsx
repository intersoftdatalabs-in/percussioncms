/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listControls } from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { mutedCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { ControlDetailPanel } from "./ControlDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.19 — content editor control catalog (UI-01 read).
 */
export function ControlsPanel(): React.ReactElement {
  const [items, setItems] = useState<ControlDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listControls()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.CTL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items]
      .filter((c) => (c.name || "").trim().length > 0)
      .sort((a, b) =>
        (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
      );
  }, [items]);

  if (selected) {
    return <ControlDetailPanel name={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-ctl-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-ctl-loading">{DEV_MSG.CTL_LOADING}</CatalogStatus>;
  if (sorted.length === 0)
    return <CatalogStatus testId="developer-ctl-empty">{DEV_MSG.CTL_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-ctl-panel">
      <CatalogHint>{DEV_MSG.CTL_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-ctl-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_DISPLAY}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_SCOPE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_DIM}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CTL_COL_FLAGS}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((c, index) => {
              const openKey = (c.name || "").trim();
              const flags: string[] = [];
              if (c.deprecated) flags.push(DEV_MSG.CTL_FLAG_DEPRECATED);
              return (
                <tr
                  key={`${openKey}-${index}`}
                  data-testid="developer-ctl-row"
                  style={{ borderBottom: "1px solid #edf2f7", cursor: "pointer" }}
                  onClick={() => setSelected(openKey)}
                >
                  <td style={{ padding: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-ctl-open"
                      aria-label={`Open ${openKey}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
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
                      {openKey}
                    </button>
                  </td>
                  <td style={{ padding: "8px" }}>{c.displayName || "—"}</td>
                  <td style={{ padding: "8px" }}>{c.scope || "—"}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{c.dimension || "—"}</td>
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
