/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listRelationshipTypes } from "../api/developer/relationshipTypesApi";
import type { RelationshipTypeDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
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
      <SimpleCatalogTable
        tableTestId="developer-rt-table"
        rowTestId="developer-rt-row"
        columns={[
          DEV_MSG.RT_COL_NAME,
          DEV_MSG.RT_COL_LABEL,
          DEV_MSG.RT_COL_CATEGORY,
          DEV_MSG.RT_COL_TYPE,
          DEV_MSG.RT_COL_FLAGS,
        ]}
        rows={sorted.map((t, index) => {
          const openKey = t.name || t.guid?.stringValue || "";
          const interactive = openKey.length > 0;
          const flags: string[] = [];
          if (t.allowCloning) flags.push(DEV_MSG.RT_FLAG_CLONE);
          if (t.systemType) flags.push(DEV_MSG.RT_FLAG_SYSTEM);
          if (t.userType) flags.push(DEV_MSG.RT_FLAG_USER);
          return {
            key: t.name || t.guid?.stringValue || `rt-${index}`,
            onClick: interactive ? () => setSelected(openKey) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  data-testid="developer-rt-open"
                  aria-label={`Open ${t.name || openKey}`}
                  onClick={(ev) => {
                    ev.stopPropagation();
                    setSelected(openKey);
                  }}
                  style={{ ...openButtonStyle, fontFamily: "monospace" }}
                >
                  {t.name || "—"}
                </button>
              ) : (
                <span key="n" style={monoCell}>
                  {t.name || "—"}
                </span>
              ),
              t.label || "—",
              <span key="cat" style={mutedCell}>
                {t.categoryLabel || t.category || "—"}
              </span>,
              t.type || "—",
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
