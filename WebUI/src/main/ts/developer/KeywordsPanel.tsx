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
import { listKeywords } from "../api/developer/keywordsApi";
import type { KeywordSummary } from "../api/developer/types";
import { DEV_MSG } from "./messages";

function errorMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return DEV_MSG.KW_ERROR;
  }
  const api = err as ApiError;
  if (api && typeof api.status === "number") {
    return `${DEV_MSG.KW_ERROR} (${api.status})`;
  }
  if (err instanceof Error && err.message) {
    return `${DEV_MSG.KW_ERROR} ${err.message}`;
  }
  return DEV_MSG.KW_ERROR;
}

/**
 * P0.3 — keyword catalog (read-only).
 */
export function KeywordsPanel(): React.ReactElement {
  const [items, setItems] = useState<KeywordSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setItems(null);
    listKeywords(true)
      .then((list) => {
        if (!cancelled) setItems(list);
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
      <div data-testid="developer-kw-error" role="alert" style={{ color: "#b00020" }}>
        {error}
      </div>
    );
  }

  if (items == null) {
    return (
      <div data-testid="developer-kw-loading" style={{ padding: "0.5rem 0" }}>
        {DEV_MSG.KW_LOADING}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div data-testid="developer-kw-empty" style={{ padding: "0.5rem 0" }}>
        {DEV_MSG.KW_EMPTY}
      </div>
    );
  }

  return (
    <div data-testid="developer-kw-panel">
      <p style={{ color: "#4a5568", marginBottom: "12px", fontSize: "0.9rem" }}>
        {DEV_MSG.KW_HINT}
      </p>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-kw-table"
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontSize: "0.95rem",
          }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.KW_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.KW_COL_VALUE}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.KW_COL_CHOICES}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.KW_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {items.map((kw) => {
              const key =
                kw.guid?.stringValue || kw.value || kw.label || Math.random().toString();
              const choiceCount = kw.choices?.length ?? 0;
              return (
                <tr
                  key={key}
                  data-testid="developer-kw-row"
                  style={{ borderBottom: "1px solid #edf2f7" }}
                >
                  <td style={{ padding: "8px" }}>{kw.label || "—"}</td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {kw.value || "—"}
                  </td>
                  <td style={{ padding: "8px" }}>{choiceCount}</td>
                  <td style={{ padding: "8px", color: "#4a5568" }}>
                    {kw.description || ""}
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
