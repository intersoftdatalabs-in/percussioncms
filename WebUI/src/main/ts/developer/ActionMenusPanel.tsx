/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listActionMenus } from "../api/developer/actionMenusApi";
import { resolveActionMenuObjectGuid } from "../api/displayFormatGuid";
import type { ActionMenu } from "../api/developer/types";
import { ActionMenuDetailPanel } from "./ActionMenuDetailPanel";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

type SelectedMenu = {
  idOrName: string;
  /** List-row GUID fallback when detail payload omits stringValue (#3380). */
  catalogGuid?: string;
};

/**
 * P0.12 — action menu catalog + read-only detail (UI-02 read).
 */
export function ActionMenusPanel(): React.ReactElement {
  const [items, setItems] = useState<ActionMenu[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedMenu | null>(null);

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

  const openMenu = (m: ActionMenu) => {
    const idOrName = m.name || (m.id != null ? String(m.id) : "") || resolveActionMenuObjectGuid(m) || "";
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveActionMenuObjectGuid(m),
    });
  };

  if (selected) {
    return (
      <ActionMenuDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
      />
    );
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
      <SimpleCatalogTable
        tableTestId="developer-am-table"
        rowTestId="developer-am-row"
        columns={[
          DEV_MSG.AM_COL_NAME,
          DEV_MSG.AM_COL_LABEL,
          DEV_MSG.AM_COL_TYPE,
          DEV_MSG.AM_COL_HANDLER,
          DEV_MSG.AM_COL_DESCRIPTION,
        ]}
        rows={sorted.map((m, index) => {
          const openKey =
            m.name || (m.id != null ? String(m.id) : "") || resolveActionMenuObjectGuid(m) || "";
          const interactive = openKey.length > 0;
          return {
            key: resolveActionMenuObjectGuid(m) || m.name || `am-${index}`,
            onClick: interactive ? () => openMenu(m) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  data-testid="developer-am-open"
                  aria-label={`Open ${m.name || openKey}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    openMenu(m);
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {m.name || "—"}
                </button>
              ) : (
                <span key="n" style={monoCell}>
                  {m.name || "—"}
                </span>
              ),
              m.label || "",
              <span key="t" style={monoCell}>
                {m.menuType || "—"}
              </span>,
              m.handler || "—",
              <span key="d" style={mutedCell}>
                {m.description || ""}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
