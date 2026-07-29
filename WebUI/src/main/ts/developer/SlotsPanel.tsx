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
import { isSessionRedirectError, type ApiError } from "../api/client";
import { listSlots } from "../api/developer/assemblyApi";
import type { SlotSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { DEV_MSG } from "./messages";

function errMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
  const api = err as ApiError;
  if (api && typeof api.status === "number") return `${fallback} (${api.status})`;
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

export function SlotsPanel(): React.ReactElement {
  const [items, setItems] = useState<SlotSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSlots()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        // Session redirect navigates away; still leave loading so UI does not hang
        // if navigation is delayed or blocked.
        setError(errMsg(e, DEV_MSG.SLOT_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) return <CatalogStatus testId="developer-slot-error" error>{error}</CatalogStatus>;
  if (items == null)
    return <CatalogStatus testId="developer-slot-loading">{DEV_MSG.SLOT_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-slot-empty">{DEV_MSG.SLOT_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-slot-panel">
      <CatalogHint>{DEV_MSG.SLOT_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-slot-table"
        rowTestId="developer-slot-row"
        columns={[
          DEV_MSG.SLOT_COL_LABEL,
          DEV_MSG.SLOT_COL_NAME,
          DEV_MSG.SLOT_COL_DESCRIPTION,
        ]}
        rows={items.map((s, index) => ({
          key: s.guid?.stringValue || s.name || `slot-${index}`,
          cells: [
            s.label || "—",
            <span key="n" style={{ fontFamily: "monospace" }}>
              {s.name || "—"}
            </span>,
            <span key="d" style={{ color: "#4a5568" }}>
              {s.description || ""}
            </span>,
          ],
        }))}
      />
    </div>
  );
}
