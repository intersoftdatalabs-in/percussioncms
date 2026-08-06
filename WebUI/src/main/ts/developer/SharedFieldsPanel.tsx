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

import React, { useEffect, useMemo, useState } from "react";
import { listSharedFieldGroups } from "../api/developer/sharedFieldsApi";
import type { SharedFieldGroupSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedMonoCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SharedFieldGroupDetailPanel } from "./SharedFieldGroupDetailPanel";

/**
 * P0.7 — shared field group catalog + read-only detail (CD-15 read).
 */
export function SharedFieldsPanel(): React.ReactElement {
  const [items, setItems] = useState<SharedFieldGroupSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSharedFieldGroups()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SF_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
    );
  }, [items]);

  if (selected) {
    return (
      <SharedFieldGroupDetailPanel name={selected} onBack={() => setSelected(null)} />
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
  if (items.length === 0)
    return <CatalogStatus testId="developer-sf-empty">{DEV_MSG.SF_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-sf-panel">
      <CatalogHint>{DEV_MSG.SF_HINT}</CatalogHint>
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
    </div>
  );
}
