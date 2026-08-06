/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listServerConfigs } from "../api/developer/serverConfigsApi";
import type { ServerConfigDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { ServerConfigDetailPanel } from "./ServerConfigDetailPanel";

/**
 * P0.20 — server configuration files catalog (SY-02 read).
 */
export function ServerConfigsPanel(): React.ReactElement {
  const [items, setItems] = useState<ServerConfigDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listServerConfigs()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.CFG_ERROR));
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
        (a.displayName || a.name || "").localeCompare(
          b.displayName || b.name || "",
          undefined,
          { sensitivity: "base" },
        ),
      );
  }, [items]);

  if (selected) {
    return (
      <ServerConfigDetailPanel name={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-cfg-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-cfg-loading">{DEV_MSG.CFG_LOADING}</CatalogStatus>;
  if (sorted.length === 0)
    return <CatalogStatus testId="developer-cfg-empty">{DEV_MSG.CFG_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-cfg-panel">
      <CatalogHint>{DEV_MSG.CFG_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-cfg-table"
        rowTestId="developer-cfg-row"
        columns={[DEV_MSG.CFG_COL_DISPLAY, DEV_MSG.CFG_COL_KEY, DEV_MSG.CFG_COL_FILE]}
        rows={sorted.map((c) => {
          const openKey = (c.name || "").trim();
          return {
            key: openKey,
            onClick: () => setSelected(openKey),
            cells: [
              <button
                key="open"
                type="button"
                data-testid="developer-cfg-open"
                aria-label={`Open ${c.displayName || openKey}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  setSelected(openKey);
                }}
                style={openButtonStyle}
              >
                {c.displayName || openKey}
              </button>,
              <span key="k" style={monoCell}>
                {openKey}
              </span>,
              <span key="f" style={mutedCell}>
                {c.fileName || "—"}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
