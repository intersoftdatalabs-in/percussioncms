/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listExtensions } from "../api/developer/extensionsApi";
import type { ExtensionDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
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
      <SimpleCatalogTable
        tableTestId="developer-ex-table"
        rowTestId="developer-ex-row"
        columns={[
          DEV_MSG.EX_COL_NAME,
          DEV_MSG.EX_COL_HANDLER,
          DEV_MSG.EX_COL_CONTEXT,
          DEV_MSG.EX_COL_CATEGORY,
          DEV_MSG.EX_COL_FLAGS,
        ]}
        rows={sorted.map((e, index) => {
          const openKey = e.fqn || e.extensionName || "";
          const interactive = openKey.length > 0;
          const flags: string[] = [];
          if (e.jexlExtension) flags.push(DEV_MSG.EX_FLAG_JEXL);
          if (e.deprecated) flags.push(DEV_MSG.EX_FLAG_DEPRECATED);
          return {
            key: e.fqn || e.extensionName || `ex-${index}`,
            onClick: interactive ? () => setSelected(openKey) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  data-testid="developer-ex-open"
                  aria-label={`Open ${e.extensionName || openKey}`}
                  onClick={(ev) => {
                    ev.stopPropagation();
                    setSelected(openKey);
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {e.extensionName || "—"}
                </button>
              ) : (
                <span key="n" style={monoCell}>
                  {e.extensionName || "—"}
                </span>
              ),
              <span key="h" style={monoCell}>
                {e.handlerName || "—"}
              </span>,
              <span key="c" style={mutedCell}>
                {e.context || ""}
              </span>,
              e.category || "—",
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
