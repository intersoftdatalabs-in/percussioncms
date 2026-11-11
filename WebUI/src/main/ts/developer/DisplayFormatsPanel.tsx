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
import { listDisplayFormats } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { CatalogSearchFilter } from "./CatalogSearchFilter";
import { catalogColors } from "./catalogStyles";
import { DisplayFormatCatalogTable } from "./DisplayFormatCatalogTable";
import {
  type DisplayFormatSelection,
  displayFormatSelection,
  filterDisplayFormatCatalog,
  sortDisplayFormatCatalog,
} from "./displayFormatCatalog";
import { DisplayFormatDetailPanel } from "./DisplayFormatDetailPanel";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/**
 * UI-05 / UI-08 — display format catalog with create / save / delete,
 * user-format column add/remove/reorder, and default sort.
 */
export function DisplayFormatsPanel(): React.ReactElement {
  const [items, setItems] = useState<DisplayFormat[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** Client-side catalog filter (#5995). Never triggers a fetch; never mutates. */
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<DisplayFormatSelection | null>(null);
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
    return listDisplayFormats()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(list);
      })
      .catch((e: unknown) => {
        if (!mountedRef.current) return;
        setError(panelErrMsg(e, DEV_MSG.DF_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  const sorted = useMemo(() => sortDisplayFormatCatalog(items), [items]);
  const visible = useMemo(() => filterDisplayFormatCatalog(sorted, query), [sorted, query]);

  const openSelected = useCallback((f: DisplayFormat): void => {
    const selection = displayFormatSelection(f);
    if (selection) setSelected(selection);
  }, []);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  if (selected?.idOrName === "new") {
    return (
      <DisplayFormatDetailPanel
        idOrName={null}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
        onColumnsSaved={() => void reload()}
      />
    );
  }

  if (selected) {
    return (
      <DisplayFormatDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
        onSaved={() => void reload()}
        onDeleted={handleDeleted}
        onColumnsSaved={() => void reload()}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-df-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-df-loading">{DEV_MSG.DF_LOADING}</CatalogStatus>
    );

  return (
    <div data-testid="developer-df-panel">
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
        <CatalogHint>{DEV_MSG.DF_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-df-new"
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
          {DEV_MSG.DF_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <CatalogStatus testId="developer-df-empty">{DEV_MSG.DF_EMPTY}</CatalogStatus>
      ) : (
        <>
          <CatalogSearchFilter
            testId="developer-df-search"
            query={query}
            onQueryChange={setQuery}
          />
          {visible.length === 0 ? (
            <CatalogStatus testId="developer-df-search-empty">
              {DEV_MSG.DF_SEARCH_EMPTY}
            </CatalogStatus>
          ) : (
            <DisplayFormatCatalogTable formats={visible} onOpen={openSelected} />
          )}
        </>
      )}
    </div>
  );
}
