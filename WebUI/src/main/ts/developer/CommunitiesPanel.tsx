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
import { listCommunities } from "../api/developer/assemblyApi";
import type { CommunitySummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { CommunityDetailPanel } from "./CommunityDetailPanel";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function openKey(c: CommunitySummary): string | null {
  if (c.name) return c.name;
  if (c.id != null) return String(c.id);
  if (c.guid?.stringValue) return c.guid.stringValue;
  return null;
}

/**
 * SE-01 — community catalog with create / delete. Role membership stays on
 * {@link CommunityDetailPanel}.
 */
export function CommunitiesPanel(): React.ReactElement {
  const [items, setItems] = useState<CommunitySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** null = catalog; "new" = create; otherwise name or id. */
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
    return listCommunities()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.COMM_ERROR));
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
      <CommunityDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <CommunityDetailPanel
        idOrName={selected}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-comm-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-comm-loading">{DEV_MSG.COMM_LOADING}</CatalogStatus>;

  return (
    <div data-testid="developer-comm-panel">
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
        <CatalogHint>{DEV_MSG.COMM_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-comm-new"
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
          {DEV_MSG.COMM_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-comm-empty">{DEV_MSG.COMM_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-comm-table"
          rowTestId="developer-comm-row"
          columns={[
            DEV_MSG.COMM_COL_LABEL,
            DEV_MSG.COMM_COL_NAME,
            DEV_MSG.COMM_COL_ID,
            DEV_MSG.COMM_COL_DESCRIPTION,
          ]}
          rows={sorted.map((c, index) => {
            const key = openKey(c);
            return {
              key: String(c.id ?? c.guid?.stringValue ?? c.name ?? `comm-${index}`),
              cells: [
                key ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-comm-open"
                    data-comm-name={c.name || key}
                    style={openButtonStyle}
                    aria-label={`Open ${c.label || c.name || key}`}
                    onClick={() => setSelected(key)}
                  >
                    {c.label || c.name || "—"}
                  </button>
                ) : (
                  c.label || "—"
                ),
                <span key="n" style={monoCell}>
                  {c.name || "—"}
                </span>,
                <span key="i" style={monoCell}>
                  {c.id != null ? String(c.id) : c.guid?.stringValue || "—"}
                </span>,
                <span key="d" style={mutedCell}>
                  {c.description || ""}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}
