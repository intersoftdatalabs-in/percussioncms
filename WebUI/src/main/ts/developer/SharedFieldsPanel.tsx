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
import { listSharedFieldGroups } from "../api/developer/sharedFieldsApi";
import type { SharedFieldGroupSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedMonoCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SharedFieldGroupDetailPanel } from "./SharedFieldGroupDetailPanel";

/**
 * CD-15 — shared field group catalog with create / save / delete.
 */
export function SharedFieldsPanel(): React.ReactElement {
  const [items, setItems] = useState<SharedFieldGroupSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = catalog; "new" = create; otherwise group name. */
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
    return listSharedFieldGroups()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.SF_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
    );
  }, [items]);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected === "new") {
    return (
      <SharedFieldGroupDetailPanel
        name={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <SharedFieldGroupDetailPanel
        name={selected}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-sf-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-sf-loading">{DEV_MSG.SF_LOADING}</CatalogStatus>
    );

  return (
    <div data-testid="developer-sf-panel">
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
        <CatalogHint>{DEV_MSG.SF_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-sf-new"
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
          {DEV_MSG.SF_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-sf-empty">{DEV_MSG.SF_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-sf-table"
          rowTestId="developer-sf-row"
          columns={[DEV_MSG.SF_COL_NAME, DEV_MSG.SF_COL_FILENAME, DEV_MSG.SF_COL_FIELDS]}
          rows={sorted.map((g, index) => {
            const openKey = g.name || "";
            const interactive = openKey.length > 0;
            return {
              key: g.name || `sf-${index}`,
              onClick: interactive ? () => setSelected(openKey) : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-sf-open"
                    data-sf-name={g.name || openKey}
                    aria-label={`Open ${g.name || openKey}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelected(openKey);
                    }}
                    style={{ ...openButtonStyle, fontFamily: "monospace" }}
                  >
                    {g.name || "—"}
                  </button>
                ) : (
                  <span key="n" style={monoCell}>
                    {g.name || "—"}
                  </span>
                ),
                <span key="fn" style={mutedMonoCell}>
                  {g.filename || ""}
                </span>,
                g.fieldCount != null ? String(g.fieldCount) : "—",
              ],
            };
          })}
        />
      )}
    </div>
  );
}
