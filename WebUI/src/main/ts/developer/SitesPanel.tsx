/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listSites } from "../api/developer/sitesApi";
import type { SiteDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, mutedCell, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SiteDetailPanel } from "./SiteDetailPanel";

function siteName(s: SiteDef): string {
  const n = s.name;
  if (typeof n === "string") return n.trim();
  return "";
}

/**
 * P0.18 — site catalog browse (SY-04) via existing GET /services/sites.
 * Detail uses list payload (resource is list-only today).
 */
export function SitesPanel(): React.ReactElement {
  const [items, setItems] = useState<SiteDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SiteDef | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSites()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SITE_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items]
      .filter((s) => siteName(s).length > 0)
      .sort((a, b) =>
        siteName(a).localeCompare(siteName(b), undefined, { sensitivity: "base" }),
      );
  }, [items]);

  if (selected) {
    return <SiteDetailPanel site={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-site-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-site-loading">{DEV_MSG.SITE_LOADING}</CatalogStatus>
    );
  if (items.length === 0 || sorted.length === 0)
    return <CatalogStatus testId="developer-site-empty">{DEV_MSG.SITE_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-site-panel">
      <CatalogHint>{DEV_MSG.SITE_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-site-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.SITE_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SITE_COL_DESC}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SITE_COL_URL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SITE_COL_FLAGS}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((s, index) => {
              const name = siteName(s);
              const flags: string[] = [];
              if (s.pageBasedSite) flags.push(DEV_MSG.SITE_FLAG_PAGE);
              if (s.isCanonical ?? s.canonical) flags.push(DEV_MSG.SITE_FLAG_CANONICAL);
              return (
                <tr
                  key={`${name}-${index}`}
                  data-testid="developer-site-row"
                  style={{ ...tableRow, cursor: "pointer"  }}
                  onClick={() => setSelected(s)}
                >
                  <td style={{ padding: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-site-open"
                      aria-label={`Open ${name}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
                        setSelected(s);
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
                      {name}
                    </button>
                  </td>
                  <td style={{ padding: "8px", ...mutedCell }}>{s.description || ""}</td>
                  <td style={{ padding: "8px", fontFamily: "monospace", fontSize: "0.85rem" }}>
                    {s.baseUrl || "—"}
                  </td>
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
