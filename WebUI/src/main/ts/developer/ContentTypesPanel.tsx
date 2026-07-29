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
import { ContentTypeDetailPanel } from "./ContentTypeDetailPanel";
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
    return DEV_MSG.SESSION_REDIRECT;
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

function selectionKey(ct: ContentTypeSummary): string {
  return ct.name || ct.guid?.stringValue || displayId(ct);
}

const openButtonStyle: React.CSSProperties = {
  background: "none",
  border: "none",
  padding: 0,
  color: "#007ea8",
  cursor: "pointer",
  font: "inherit",
  textAlign: "left",
  textDecoration: "underline",
};

/**
 * P0.1 list + P0.2 read-only field catalog detail.
 */
export function ContentTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

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
        if (cancelled) return;
        // Session redirect navigates away; still leave loading so UI does not hang
        // if navigation is delayed or blocked.
        setError(errorMessage(err));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (selected) {
    return (
      <ContentTypeDetailPanel
        idOrName={selected}
        onBack={() => setSelected(null)}
      />
    );
  }

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
              const openKey = selectionKey(ct);
              const interactive = openKey !== "—";
              return (
                <tr
                  key={key}
                  data-testid="developer-ct-row"
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
                        style={openButtonStyle}
                        aria-label={`Open ${ct.label || ct.name || openKey}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelected(openKey);
                        }}
                      >
                        {ct.label || "—"}
                      </button>
                    ) : (
                      <span style={{ color: "#4a5568" }}>{ct.label || "—"}</span>
                    )}
                  </td>
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
