/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
import { CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
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
      <CatalogStatus testId="developer-kw-error" error>
        {error}
      </CatalogStatus>
    );
  }

  if (items == null) {
    return <CatalogStatus testId="developer-kw-loading">{DEV_MSG.KW_LOADING}</CatalogStatus>;
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
        <CatalogStatus testId="developer-kw-empty">{DEV_MSG.KW_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-kw-table"
          rowTestId="developer-kw-row"
          columns={[
            DEV_MSG.KW_COL_LABEL,
            DEV_MSG.KW_COL_VALUE,
            DEV_MSG.KW_COL_CHOICES,
            DEV_MSG.KW_COL_DESCRIPTION,
          ]}
          rows={items.map((kw, index) => {
            const key = kw.guid?.stringValue || kw.value || kw.label || `kw-${index}`;
            const choiceCount = kw.choices?.length ?? 0;
            return {
              key,
              onClick: () => setEditing(kw),
              cells: [
                <button
                  key="open"
                  type="button"
                  style={openButtonStyle}
                  onClick={(e) => {
                    e.stopPropagation();
                    setEditing(kw);
                  }}
                >
                  {kw.label || "—"}
                </button>,
                <span key="v" style={monoCell}>
                  {kw.value || "—"}
                </span>,
                choiceCount,
                <span key="d" style={mutedCell}>
                  {kw.description || ""}
                </span>,
              ],
            };
          })}
        />
      )}
    </div>
  );
}
