/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React, { useEffect, useState } from "react";
import { listCommunities } from "../api/developer/assemblyApi";
import type { CommunitySummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { CommunityDetailPanel } from "./CommunityDetailPanel";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function openKey(c: CommunitySummary): string | null {
  if (c.name) return c.name;
  if (c.id != null) return String(c.id);
  if (c.guid?.stringValue) return c.guid.stringValue;
  return null;
}

export function CommunitiesPanel(): React.ReactElement {
  const [items, setItems] = useState<CommunitySummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listCommunities()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.COMM_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (selected) {
    return (
      <CommunityDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
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
  if (items.length === 0)
    return <CatalogStatus testId="developer-comm-empty">{DEV_MSG.COMM_EMPTY}</CatalogStatus>;

  const sorted = [...items].sort((a, b) =>
    (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
      sensitivity: "base",
    }),
  );

  return (
    <div data-testid="developer-comm-panel">
      <CatalogHint>{DEV_MSG.COMM_HINT}</CatalogHint>
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
    </div>
  );
}
