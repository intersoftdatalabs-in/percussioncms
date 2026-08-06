/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listControls } from "../api/developer/controlsApi";
import type { ControlDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { mutedCell, openButtonStyle } from "./catalogStyles";
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
      <SimpleCatalogTable
        tableTestId="developer-ctl-table"
        rowTestId="developer-ctl-row"
        columns={[
          DEV_MSG.CTL_COL_NAME,
          DEV_MSG.CTL_COL_DISPLAY,
          DEV_MSG.CTL_COL_SCOPE,
          DEV_MSG.CTL_COL_DIM,
          DEV_MSG.CTL_COL_FLAGS,
        ]}
        rows={sorted.map((c, index) => {
          const openKey = (c.name || "").trim();
          const flags: string[] = [];
          if (c.deprecated) flags.push(DEV_MSG.CTL_FLAG_DEPRECATED);
          return {
            key: `${openKey}-${index}`,
            onClick: () => setSelected(openKey),
            cells: [
              <button
                key="open"
                type="button"
                data-testid="developer-ctl-open"
                aria-label={`Open ${openKey}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  setSelected(openKey);
                }}
                style={{ ...openButtonStyle, fontFamily: "monospace" }}
              >
                {openKey}
              </button>,
              c.displayName || "—",
              c.scope || "—",
              <span key="dim" style={mutedCell}>
                {c.dimension || "—"}
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
