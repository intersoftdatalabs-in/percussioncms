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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { listActionMenus } from "../api/developer/actionMenusApi";
import { resolveActionMenuObjectGuid } from "../api/displayFormatGuid";
import type { ActionMenu } from "../api/developer/types";
import { ActionMenuDetailPanel } from "./ActionMenuDetailPanel";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

type SelectedMenu = {
  idOrName: string | "new";
  /** List-row GUID fallback when detail payload omits stringValue (#3380). */
  catalogGuid?: string;
};

/**
 * UI-02 — action menu catalog with create / save / delete.
 */
export function ActionMenusPanel(): React.ReactElement {
  const [items, setItems] = useState<ActionMenu[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedMenu | null>(null);
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
    return listActionMenus()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.AM_ERROR));
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

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  const openMenu = (m: ActionMenu) => {
    const idOrName = m.name || (m.id != null ? String(m.id) : "") || resolveActionMenuObjectGuid(m) || "";
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveActionMenuObjectGuid(m),
    });
  };

  if (selected?.idOrName === "new") {
    return (
      <ActionMenuDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <ActionMenuDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
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

  return (
    <div data-testid="developer-am-panel">
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
        <CatalogHint>{DEV_MSG.AM_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-am-new"
          onClick={() => setSelected({ idOrName: "new" })}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.AM_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-am-empty">{DEV_MSG.AM_EMPTY}</CatalogStatus>
      ) : (
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
                    data-am-name={m.name || openKey}
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
      )}
    </div>
  );
}
