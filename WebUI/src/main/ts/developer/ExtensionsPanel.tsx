/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listExtensions } from "../api/developer/extensionsApi";
import type { ExtensionDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { monoCell, mutedCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { ExtensionDetailPanel } from "./ExtensionDetailPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.15 — server extension catalog + read-only detail.
 */
export function ExtensionsPanel(): React.ReactElement {
  const [items, setItems] = useState<ExtensionDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listExtensions()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.EX_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.extensionName || a.fqn || "").localeCompare(b.extensionName || b.fqn || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  if (selected) {
    return <ExtensionDetailPanel idOrName={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-ex-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-ex-loading">{DEV_MSG.EX_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-ex-empty">{DEV_MSG.EX_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-ex-panel">
      <CatalogHint>{DEV_MSG.EX_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-ex-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_HANDLER}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_CONTEXT}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_CATEGORY}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.EX_COL_FLAGS}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((e, index) => {
              const openKey = e.fqn || e.extensionName || "";
              const interactive = openKey.length > 0;
              const flags: string[] = [];
              if (e.jexlExtension) flags.push(DEV_MSG.EX_FLAG_JEXL);
              if (e.deprecated) flags.push(DEV_MSG.EX_FLAG_DEPRECATED);
              return (
                <tr
                  key={e.fqn || e.extensionName || `ex-${index}`}
                  data-testid="developer-ex-row"
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
                        data-testid="developer-ex-open"
                        aria-label={`Open ${e.extensionName || openKey}`}
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
                        {e.extensionName || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{e.extensionName || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px", ...monoCell }}>{e.handlerName || "—"}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{e.context || ""}</td>
                  <td style={{ padding: "8px" }}>{e.category || "—"}</td>
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
