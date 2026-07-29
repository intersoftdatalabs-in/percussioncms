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
import { listSlots } from "../api/developer/assemblyApi";
import type { SlotSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { SlotDetailPanel } from "./SlotDetailPanel";

/** Open-key for detail; null when the row is not selectable. */
function selectionKey(s: SlotSummary): string | null {
  if (s.name) return s.name;
  if (s.guid?.stringValue) return s.guid.stringValue;
  return null;
}

export function SlotsPanel(): React.ReactElement {
  const [items, setItems] = useState<SlotSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSlots()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.SLOT_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (selected) {
    return <SlotDetailPanel idOrName={selected} onBack={() => setSelected(null)} />;
  }

  if (error) return <CatalogStatus testId="developer-slot-error" error>{error}</CatalogStatus>;
  if (items == null)
    return <CatalogStatus testId="developer-slot-loading">{DEV_MSG.SLOT_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-slot-empty">{DEV_MSG.SLOT_EMPTY}</CatalogStatus>;

  const sorted = [...items].sort((a, b) =>
    (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
      sensitivity: "base",
    }),
  );

  return (
    <div data-testid="developer-slot-panel">
      <CatalogHint>{DEV_MSG.SLOT_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-slot-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.SLOT_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((s, index) => {
              const openKey = selectionKey(s);
              const key = s.guid?.stringValue || s.name || `slot-${index}`;
              return (
                <tr
                  key={key}
                  data-testid="developer-slot-row"
                  style={{
                    borderBottom: "1px solid #edf2f7",
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {openKey ? (
                      <button
                        type="button"
                        style={openButtonStyle}
                        aria-label={`Open ${s.label || s.name || openKey}`}
                        onClick={() => setSelected(openKey)}
                      >
                        {s.label || "—"}
                      </button>
                    ) : (
                      s.label || "—"
                    )}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {s.name || "—"}
                  </td>
                  <td style={{ padding: "8px", color: "#4a5568" }}>
                    {s.description || ""}
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
