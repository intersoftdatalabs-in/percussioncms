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
import {
  isImmutableExtension,
  listExtensions,
} from "../api/developer/extensionsApi";
import type { ExtensionDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { ExtensionDetailPanel } from "./ExtensionDetailPanel";
import { DEV_MSG } from "./messages";

type SelectedExtension = {
  idOrName: string | "new";
};

/**
 * SY-01 — server extension catalog with create / save / delete for user extensions.
 */
export function ExtensionsPanel(): React.ReactElement {
  const [items, setItems] = useState<ExtensionDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedExtension | null>(null);
  const [listNotice, setListNotice] = useState<string | null>(null);
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
    return listExtensions()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.EX_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.extensionName || a.fqn || "").localeCompare(b.extensionName || b.fqn || "", undefined, {
        sensitivity: "base",
      }),
    );
  }, [items]);

  function handleDeleted(): void {
    setListNotice(DEV_MSG.EX_DELETED);
    setSelected(null);
    void reload();
  }

  const openExtension = (e: ExtensionDef) => {
    const openKey = e.fqn || e.extensionName || "";
    if (!openKey) return;
    setSelected({ idOrName: openKey });
  };

  if (selected?.idOrName === "new") {
    return (
      <ExtensionDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (selected) {
    return (
      <ExtensionDetailPanel
        idOrName={selected.idOrName}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-ex-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-ex-loading">{DEV_MSG.EX_LOADING}</CatalogStatus>;

  return (
    <div data-testid="developer-ex-panel">
      {listNotice ? (
        <div
          data-testid="developer-ex-list-notice"
          style={{ color: "#276749", marginBottom: "12px" }}
        >
          {listNotice}
        </div>
      ) : null}
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
        <CatalogHint>{DEV_MSG.EX_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-ex-new"
          onClick={() => {
            setListNotice(null);
            setSelected({ idOrName: "new" });
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
          {DEV_MSG.EX_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-ex-empty">{DEV_MSG.EX_EMPTY}</CatalogStatus>
      ) : (
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
            if (isImmutableExtension(e)) flags.push(DEV_MSG.EX_FLAG_SYSTEM);
            if (e.jexlExtension) flags.push(DEV_MSG.EX_FLAG_JEXL);
            if (e.deprecated) flags.push(DEV_MSG.EX_FLAG_DEPRECATED);
            return {
              key: e.fqn || e.extensionName || `ex-${index}`,
              onClick: interactive ? () => openExtension(e) : undefined,
              cells: [
                interactive ? (
                  <button
                    key="open"
                    type="button"
                    data-testid="developer-ex-open"
                    data-ex-name={e.extensionName || openKey}
                    data-ex-context={e.context || ""}
                    data-immutable={isImmutableExtension(e) ? "true" : "false"}
                    aria-label={`Open ${e.extensionName || openKey}`}
                    onClick={(ev) => {
                      ev.stopPropagation();
                      openExtension(e);
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
      )}
    </div>
  );
}
