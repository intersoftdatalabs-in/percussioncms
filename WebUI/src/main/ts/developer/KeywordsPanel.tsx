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

import React, { useCallback, useEffect, useState } from "react";
import { listKeywords } from "../api/developer/keywordsApi";
import type { KeywordSummary } from "../api/developer/types";
import { panelErrMsg } from "./errors";
import { KeywordEditorPanel } from "./KeywordEditorPanel";
import { DEV_MSG } from "./messages";

/**
 * P0.3b — keyword catalog with create / edit / delete.
 */
export function KeywordsPanel(): React.ReactElement {
  const [items, setItems] = useState<KeywordSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<KeywordSummary | null | "new">(null);

  const reload = useCallback(() => {
    setError(null);
    setItems(null);
    return listKeywords(true)
      .then((list) => setItems(list))
      .catch((err: unknown) => {
        setError(panelErrMsg(err, DEV_MSG.KW_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  if (editing !== null) {
    return (
      <KeywordEditorPanel
        initial={editing === "new" ? null : editing}
        onBack={() => setEditing(null)}
        onSaved={() => {
          setEditing(null);
          void reload();
        }}
        onDeleted={() => {
          setEditing(null);
          void reload();
        }}
      />
    );
  }

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

  return (
    <div data-testid="developer-kw-panel">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
          gap: "12px",
          flexWrap: "wrap",
        }}
      >
        <p style={{ color: "#4a5568", margin: 0, fontSize: "0.9rem" }}>{DEV_MSG.KW_HINT}</p>
        <button
          type="button"
          data-testid="developer-kw-new"
          onClick={() => setEditing("new")}
          style={{
            padding: "8px 14px",
            background: "#007ea8",
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.KW_NEW}
        </button>
      </div>

      {items.length === 0 ? (
        <div data-testid="developer-kw-empty" style={{ padding: "0.5rem 0" }}>
          {DEV_MSG.KW_EMPTY}
        </div>
      ) : (
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
              {items.map((kw, index) => {
                const key =
                  kw.guid?.stringValue || kw.value || kw.label || `kw-${index}`;
                const choiceCount = kw.choices?.length ?? 0;
                return (
                  <tr
                    key={key}
                    data-testid="developer-kw-row"
                    style={{
                      borderBottom: "1px solid #edf2f7",
                      cursor: "pointer",
                    }}
                    onClick={() => setEditing(kw)}
                  >
                    <td style={{ padding: "8px" }}>
                      <button
                        type="button"
                        style={{
                          background: "none",
                          border: "none",
                          padding: 0,
                          color: "#007ea8",
                          cursor: "pointer",
                          font: "inherit",
                          textDecoration: "underline",
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          setEditing(kw);
                        }}
                      >
                        {kw.label || "—"}
                      </button>
                    </td>
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
      )}
    </div>
  );
}
