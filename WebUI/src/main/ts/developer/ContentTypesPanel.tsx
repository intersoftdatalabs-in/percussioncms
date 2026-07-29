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
import {
  isSessionRedirectError,
  type ApiError,
} from "../api/client";
import { listContentTypes } from "../api/developer/contentTypesApi";
import type { ContentTypeSummary } from "../api/developer/types";
import { DEV_MSG } from "./messages";

function displayId(ct: ContentTypeSummary): string {
  const g = ct.guid;
  if (!g) return "—";
  if (g.stringValue) return g.stringValue;
  if (g.uuid != null) return String(g.uuid);
  if (g.longValue != null) return String(g.longValue);
  return "—";
}

function errorMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return DEV_MSG.CT_ERROR;
  }
  const api = err as ApiError;
  if (api && typeof api.status === "number") {
    return `${DEV_MSG.CT_ERROR} (${api.status} ${api.statusText || ""})`.trim();
  }
  if (err instanceof Error && err.message) {
    return `${DEV_MSG.CT_ERROR} ${err.message}`;
  }
  return DEV_MSG.CT_ERROR;
}

/**
 * P0.1 — catalog content types from public REST.
 */
export function ContentTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setItems(null);
    listContentTypes()
      .then((list) => {
        if (!cancelled) {
          setItems(list);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled && !isSessionRedirectError(err)) {
          setError(errorMessage(err));
          setItems([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <div data-testid="developer-ct-error" role="alert" style={{ color: "#b00020" }}>
        {error}
      </div>
    );
  }

  if (items == null) {
    return (
      <div data-testid="developer-ct-loading" style={{ padding: "0.5rem 0" }}>
        {DEV_MSG.CT_LOADING}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div data-testid="developer-ct-empty" style={{ padding: "0.5rem 0" }}>
        {DEV_MSG.CT_EMPTY}
      </div>
    );
  }

  const sorted = [...items].sort((a, b) =>
    (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
      sensitivity: "base",
    }),
  );

  return (
    <div data-testid="developer-ct-panel">
      <p style={{ color: "#4a5568", marginBottom: "12px", fontSize: "0.9rem" }}>
        {DEV_MSG.CT_HINT}
      </p>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-ct-table"
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontSize: "0.95rem",
          }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_ID}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((ct) => {
              const key =
                ct.guid?.stringValue ||
                ct.name ||
                `${ct.label ?? "ct"}-${displayId(ct)}`;
              return (
                <tr
                  key={key}
                  data-testid="developer-ct-row"
                  style={{ borderBottom: "1px solid #edf2f7" }}
                >
                  <td style={{ padding: "8px" }}>{ct.label || "—"}</td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {ct.name || "—"}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {displayId(ct)}
                  </td>
                  <td style={{ padding: "8px", color: "#4a5568" }}>
                    {ct.description || ""}
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
