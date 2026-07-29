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
import { listApplications } from "../api/developer/pipelinesApi";
import type { ApplicationSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { monoCell, mutedCell, mutedMonoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { PipelineDetailPanel } from "./PipelineDetailPanel";

/**
 * P0.6 / P0.6b — classic XML Application catalog + read-only detail.
 */
export function PipelinesPanel(): React.ReactElement {
  const [items, setItems] = useState<ApplicationSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listApplications()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.PIPE_ERROR));
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
      <PipelineDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

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

  return (
    <div data-testid="developer-pipe-panel">
      <CatalogHint>{DEV_MSG.PIPE_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-pipe-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_ID}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_TYPE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_ENABLED}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_ROOT}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((app, index) => {
              const openKey = app.name || (app.id != null ? String(app.id) : "");
              const interactive = openKey.length > 0;
              return (
                <tr
                  key={String(app.id ?? app.name ?? `pipe-${index}`)}
                  data-testid="developer-pipe-row"
                  style={{
                    borderBottom: "1px solid #edf2f7",
                    cursor: interactive ? "pointer" : "default",
                  }}
                  onClick={() => {
                    if (interactive) setSelected(openKey);
                  }}
                >
                  <td style={{ padding: "8px" }}>
                    {interactive ? (
                      <button
                        type="button"
                        data-testid="developer-pipe-open"
                        aria-label={`Open ${app.name || openKey}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelected(openKey);
                        }}
                        style={{
                          background: "transparent",
                          border: "none",
                          color: "#007ea8",
                          cursor: "pointer",
                          font: "inherit",
                          padding: 0,
                          fontFamily: "monospace",
                        }}
                      >
                        {app.name || "—"}
                      </button>
                    ) : (
                      <span style={monoCell}>{app.name || "—"}</span>
                    )}
                  </td>
                  <td style={{ padding: "8px", ...monoCell }}>
                    {app.id != null ? String(app.id) : "—"}
                  </td>
                  <td style={{ padding: "8px" }}>{app.appType || "—"}</td>
                  <td style={{ padding: "8px" }}>
                    {app.enabled == null ? "—" : app.enabled ? DEV_MSG.YES : DEV_MSG.NO}
                  </td>
                  <td style={{ padding: "8px", ...mutedMonoCell }}>{app.appRoot || ""}</td>
                  <td style={{ padding: "8px", ...mutedCell }}>{app.description || ""}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
