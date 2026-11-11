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
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { CatalogSearchFilter } from "./CatalogSearchFilter";
import { catalogColors } from "./catalogStyles";
import { CommunityCatalogTable } from "./CommunityCatalogTable";
import { CommunityDetailPanel } from "./CommunityDetailPanel";
import {
  filterCommunityCatalog,
  patchCommunityByName,
  sortCommunityCatalog,
} from "./communityCatalog";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/**
 * SE-01 — community catalog with create / delete. Role membership stays on
 * {@link CommunityDetailPanel}.
 */
export function CommunitiesPanel(): React.ReactElement {
  const [items, setItems] = useState<CommunitySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** Client-side catalog filter (#5995). Never triggers a fetch; never mutates. */
  const [query, setQuery] = useState("");
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

  const sorted = useMemo(() => sortCommunityCatalog(items), [items]);
  const visible = useMemo(() => filterCommunityCatalog(sorted, query), [sorted, query]);

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  const handleRenamed = useCallback(
    (previousName: string, newName: string): void => {
      const next = newName.trim();
      if (!next) {
        return;
      }
      setItems((itemsPrev) =>
        patchCommunityByName(itemsPrev, previousName, { name: next, label: next }),
      );
      setSelected(next);
      void reload();
    },
    [reload],
  );

  const handleDescriptionSaved = useCallback(
    (communityName: string, description: string): void => {
      if (!communityName.trim()) {
        return;
      }
      setItems((itemsPrev) => patchCommunityByName(itemsPrev, communityName, { description }));
      void reload();
    },
    [reload],
  );

  const openSelected = useCallback((key: string): void => setSelected(key), []);

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
        onRenamed={handleRenamed}
        onDescriptionSaved={handleDescriptionSaved}
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
        <>
          <CatalogSearchFilter
            testId="developer-comm-search"
            query={query}
            onQueryChange={setQuery}
          />
          {visible.length === 0 ? (
            <CatalogStatus testId="developer-comm-search-empty">
              {DEV_MSG.COMM_SEARCH_EMPTY}
            </CatalogStatus>
          ) : (
            <CommunityCatalogTable communities={visible} onOpen={openSelected} />
          )}
        </>
      )}
    </div>
  );
}
