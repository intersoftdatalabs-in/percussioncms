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
import { listRelationshipTypes } from "../api/developer/relationshipTypesApi";
import type { RelationshipTypeDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { RelationshipTypeDetailPanel } from "./RelationshipTypeDetailPanel";
import { DEV_MSG } from "./messages";

type SelectedRt = string | "new" | null;

/**
 * SY-03 — relationship type catalog + Admin create/edit/delete for user types.
 */
export function RelationshipTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<RelationshipTypeDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedRt>(null);
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
    return listRelationshipTypes()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.RT_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || a.label || "").localeCompare(b.name || b.label || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  async function handleCreated(_detail: RelationshipTypeDef): Promise<void> {
    // Stay on the same detail instance (createdKey) so save notice remains visible.
    await reload();
  }

  async function handleDeleted(): Promise<void> {
    await reload();
    if (!mountedRef.current) return;
    setSelected(null);
    setNotice(DEV_MSG.RT_DELETED);
  }

  if (selected === "new") {
    return (
      <RelationshipTypeDetailPanel
        idOrName={null}
        catalog={sorted}
        onBack={() => setSelected(null)}
        onSaved={handleCreated}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <RelationshipTypeDetailPanel
        idOrName={selected}
        catalog={sorted}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
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

  return (
    <div data-testid="developer-rt-panel">
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
        <CatalogHint>{DEV_MSG.RT_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-rt-new"
          onClick={() => {
            setNotice(null);
            setSelected("new");
          }}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.RT_NEW}
        </button>
      </div>

      {notice ? (
        <div
          role="status"
          aria-live="polite"
          data-testid="developer-rt-panel-notice"
          style={{ color: "#276749", marginBottom: "8px" }}
        >
          {notice}
        </div>
      ) : null}

      {items.length === 0 ? (
        <CatalogStatus testId="developer-rt-empty">{DEV_MSG.RT_EMPTY}</CatalogStatus>
      ) : (
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
      )}
    </div>
  );
}
