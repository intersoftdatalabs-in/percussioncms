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
import { listApplications } from "../api/developer/pipelinesApi";
import type { ApplicationSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { DEV_MSG } from "./messages";

function errMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
  const api = err as ApiError;
  if (api && typeof api.status === "number") return `${fallback} (${api.status})`;
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

/**
 * P0.6 — pipeline / XML application catalog (read-only).
 */
export function PipelinesPanel(): React.ReactElement {
  const [items, setItems] = useState<ApplicationSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listApplications()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        // Session redirect navigates away; still leave loading so UI does not hang
        // if navigation is delayed or blocked.
        setError(errMsg(e, DEV_MSG.PIPE_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error)
    return (
      <CatalogStatus testId="developer-pipe-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return (
      <CatalogStatus testId="developer-pipe-loading">{DEV_MSG.PIPE_LOADING}</CatalogStatus>
    );
  if (items.length === 0)
    return <CatalogStatus testId="developer-pipe-empty">{DEV_MSG.PIPE_EMPTY}</CatalogStatus>;

  const sorted = [...items].sort((a, b) =>
    (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" }),
  );

  return (
    <div data-testid="developer-pipe-panel">
      <CatalogHint>{DEV_MSG.PIPE_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-pipe-table"
        rowTestId="developer-pipe-row"
        columns={[
          DEV_MSG.PIPE_COL_NAME,
          DEV_MSG.PIPE_COL_ID,
          DEV_MSG.PIPE_COL_TYPE,
          DEV_MSG.PIPE_COL_ENABLED,
          DEV_MSG.PIPE_COL_ROOT,
          DEV_MSG.PIPE_COL_DESCRIPTION,
        ]}
        rows={sorted.map((app, index) => ({
          key: String(app.id ?? app.name ?? `pipe-${index}`),
          cells: [
            <span key="n" style={{ fontFamily: "monospace" }}>
              {app.name || "—"}
            </span>,
            <span key="i" style={{ fontFamily: "monospace" }}>
              {app.id != null ? String(app.id) : "—"}
            </span>,
            app.appType || "—",
            app.enabled == null ? "—" : app.enabled ? DEV_MSG.YES : DEV_MSG.NO,
            <span key="r" style={{ fontFamily: "monospace", color: "#4a5568" }}>
              {app.appRoot || ""}
            </span>,
            <span key="d" style={{ color: "#4a5568" }}>
              {app.description || ""}
            </span>,
          ],
        }))}
      />
    </div>
  );
}
