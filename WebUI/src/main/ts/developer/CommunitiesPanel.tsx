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
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, openButtonStyle, tableHeaderRow, tableRow } from "./catalogStyles";
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
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-comm-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={tableHeaderRow}>
              <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_ID}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.COMM_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((c, index) => {
              const key = openKey(c);
              const rowKey = String(c.id ?? c.guid?.stringValue ?? c.name ?? `comm-${index}`);
              return (
                <tr
                  key={rowKey}
                  data-testid="developer-comm-row"
                  style={tableRow}
                >
                  <td style={{ padding: "8px" }}>
                    {key ? (
                      <button
                        type="button"
                        style={openButtonStyle}
                        aria-label={`Open ${c.label || c.name || key}`}
                        onClick={() => setSelected(key)}
                      >
                        {c.label || c.name || "—"}
                      </button>
                    ) : (
                      c.label || "—"
                    )}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {c.name || "—"}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {c.id != null ? String(c.id) : c.guid?.stringValue || "—"}
                  </td>
                  <td style={{ padding: "8px", color: catalogColors.muted }}>
                    {c.description || ""}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
