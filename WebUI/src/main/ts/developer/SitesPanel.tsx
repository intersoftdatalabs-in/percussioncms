/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { coerceDisplayString, listSites } from "../api/developer/sitesApi";
import type { SiteDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SiteCreatePanel } from "./SiteCreatePanel";
import { SiteDetailPanel } from "./SiteDetailPanel";

function siteName(s: SiteDef): string {
  const n = coerceDisplayString(s.name);
  if (n) return n;
  const guid = s.guid;
  if (guid && typeof guid.stringValue === "string" && guid.stringValue.trim()) {
    return guid.stringValue.trim();
  }
  return "";
}

/**
 * P0.18 — site catalog browse (SY-04) via existing GET /services/sites.
 * Detail uses list payload (resource is list-only today).
 */
export function SitesPanel(): React.ReactElement {
  const [items, setItems] = useState<SiteDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SiteDef | "new" | null>(null);

  function reload(): void {
    setError(null);
    listSites()
      .then((list) => {
        setItems(list);
      })
      .catch((e: unknown) => {
        setError(panelErrMsg(e, DEV_MSG.SITE_ERROR));
      });
  }

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

  function handleCreated(created: SiteDef): void {
    reload();
    setSelected(created);
  }

  function handleDeleted(): void {
    setSelected(null);
    reload();
  }

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items]
      .filter((s) => siteName(s).length > 0)
      .sort((a, b) =>
        siteName(a).localeCompare(siteName(b), undefined, { sensitivity: "base" }),
      );
  }, [items]);

  if (selected === "new") {
    return (
      <SiteCreatePanel onBack={() => setSelected(null)} onCreated={handleCreated} />
    );
  }

  if (selected) {
    return (
      <SiteDetailPanel
        site={selected}
        onBack={() => setSelected(null)}
        onDeleted={handleDeleted}
        onUpdated={(s) => setSelected(s)}
      />
    );
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

  const newButton = (
    <button
      type="button"
      data-testid="developer-site-new"
      onClick={() => setSelected("new")}
      style={{
        padding: "8px 14px",
        background: catalogColors.accent,
        color: "#fff",
        border: "none",
        borderRadius: "4px",
        cursor: "pointer",
      }}
    >
      {DEV_MSG.SITE_NEW}
    </button>
  );

  if (items.length === 0) {
    return (
      <div data-testid="developer-site-empty">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "12px",
            flexWrap: "wrap",
            marginBottom: "12px",
          }}
        >
          <CatalogStatus testId="developer-site-empty-msg">{DEV_MSG.SITE_EMPTY}</CatalogStatus>
          {newButton}
        </div>
      </div>
    );
  }

  if (sorted.length === 0)
    return (
      <CatalogStatus testId="developer-site-error" error>
        {DEV_MSG.SITE_BIND_ERROR}
      </CatalogStatus>
    );

  return (
    <div data-testid="developer-site-panel">
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
        <CatalogHint>{DEV_MSG.SITE_HINT}</CatalogHint>
        {newButton}
      </div>
      <SimpleCatalogTable
        tableTestId="developer-site-table"
        rowTestId="developer-site-row"
        columns={[
          DEV_MSG.SITE_COL_NAME,
          DEV_MSG.SITE_COL_DESC,
          DEV_MSG.SITE_COL_URL,
          DEV_MSG.SITE_COL_FLAGS,
        ]}
        rows={sorted.map((s, index) => {
          const name = siteName(s);
          const flags: string[] = [];
          if (s.pageBasedSite) flags.push(DEV_MSG.SITE_FLAG_PAGE);
          if (s.isCanonical ?? s.canonical) flags.push(DEV_MSG.SITE_FLAG_CANONICAL);
          return {
            key: `${name}-${index}`,
            onClick: () => setSelected(s),
            cells: [
              <button
                key="open"
                type="button"
                data-testid="developer-site-open"
                aria-label={`Open ${name}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  setSelected(s);
                }}
                style={{ ...openButtonStyle, fontFamily: "monospace" }}
              >
                {name}
              </button>,
              <span key="d" style={mutedCell}>
                {s.description || ""}
              </span>,
              <span key="u" style={{ fontFamily: "monospace", fontSize: "0.85rem" }}>
                {s.baseUrl || "—"}
              </span>,
              <span key="f" style={{ fontSize: "0.85rem" }}>
                {flags.length ? flags.join(", ") : "—"}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
