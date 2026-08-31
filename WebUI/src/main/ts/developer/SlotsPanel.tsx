/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { listSlots } from "../api/developer/assemblyApi";
import type { SlotSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { DeveloperSectionErrorBoundary } from "./DeveloperSectionErrorBoundary";
import { SlotDetailPanel } from "./SlotDetailPanel";

/** Open-key for detail; null when the row is not selectable. */
function selectionKey(s: SlotSummary): string | null {
  if (s.name) return s.name;
  if (s.guid?.stringValue) return s.guid.stringValue;
  return null;
}

/**
 * AS-01 — assembly slot catalog with create / save / delete (REST POST/DELETE).
 */
export function SlotsPanel(): React.ReactElement {
  const [items, setItems] = useState<SlotSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = catalog; "new" = create; otherwise slot name or guid. */
  const [selected, setSelected] = useState<string | "new" | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback((opts?: { showLoading?: boolean }) => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    if (opts?.showLoading) {
      setItems(null);
    }
    setError(null);
    return listSlots()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.SLOT_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected === "new") {
    return (
      <DeveloperSectionErrorBoundary
        label={DEV_MSG.TAB_SLOTS}
        testId="developer-slot-detail-error"
      >
        <SlotDetailPanel
          idOrName={null}
          onBack={() => setSelected(null)}
          onSaved={() => void reload()}
          onDeleted={handleDeleted}
        />
      </DeveloperSectionErrorBoundary>
    );
  }

  if (selected) {
    return (
      <DeveloperSectionErrorBoundary
        label={DEV_MSG.TAB_SLOTS}
        testId="developer-slot-detail-error"
      >
        <SlotDetailPanel
          idOrName={selected}
          onBack={() => setSelected(null)}
          onSaved={() => void reload()}
          onDeleted={handleDeleted}
        />
      </DeveloperSectionErrorBoundary>
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-slot-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-slot-loading">{DEV_MSG.SLOT_LOADING}</CatalogStatus>
    );

  return (
    <div data-testid="developer-slot-panel">
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
        <CatalogHint>{DEV_MSG.SLOT_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-slot-new"
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
          {DEV_MSG.SLOT_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-slot-empty">{DEV_MSG.SLOT_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-slot-table"
          rowTestId="developer-slot-row"
          columns={[DEV_MSG.SLOT_COL_LABEL, DEV_MSG.SLOT_COL_NAME, DEV_MSG.SLOT_COL_DESCRIPTION]}
          rows={sorted.map((s, index) => {
            const openKey = selectionKey(s);
            const interactive = openKey != null && openKey.length > 0;
            return {
              key: s.guid?.stringValue || s.name || `slot-${index}`,
              onClick: interactive && openKey ? () => setSelected(openKey) : undefined,
              cells: [
                interactive && openKey ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-slot-open"
                    data-slot-name={s.name || openKey}
                    style={openButtonStyle}
                    aria-label={`Open ${s.label || s.name || openKey}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelected(openKey);
                    }}
                  >
                    {s.label || "—"}
                  </button>
                ) : (
                  s.label || "—"
                ),
                <span key="n" style={monoCell}>
                  {s.name || "—"}
                </span>,
                <span key="d" style={mutedCell}>
                  {s.description || ""}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}
