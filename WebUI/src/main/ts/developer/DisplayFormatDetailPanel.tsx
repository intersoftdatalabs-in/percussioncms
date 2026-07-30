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
  getDisplayFormatDetail,
  normalizeColumns,
} from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function DisplayFormatDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<DisplayFormat | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getDisplayFormatDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.DF_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  const columns =
    detail != null ? normalizeColumns(detail.columns) : [];

  return (
    <div data-testid="developer-df-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-df-back"
        style={backButton}
      >
        ← {DEV_MSG.DF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-df-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-df-detail-loading">{DEV_MSG.DF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-df-detail-title">
              {detail.label || detail.displayName || detail.name || idOrName}
            </h2>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: "#4a5568" }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.DF_COL_NAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.name || "—"}</dd>
              <dt>{DEV_MSG.DF_COL_GUID}</dt>
              <dd style={{ margin: 0, ...monoCell }}>
                {detail.guid?.stringValue || "—"}
              </dd>
              <dt>{DEV_MSG.DF_COL_USAGE}</dt>
              <dd style={{ margin: 0 }}>
                {[
                  detail.validForFolder ? DEV_MSG.DF_USAGE_FOLDER : null,
                  detail.validForViewsAndSearches ? DEV_MSG.DF_USAGE_VIEWS : null,
                  detail.validForRelatedContent ? DEV_MSG.DF_USAGE_RELATED : null,
                ]
                  .filter(Boolean)
                  .join(", ") || "—"}
              </dd>
            </dl>
          </header>

          <section data-testid="developer-df-columns">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_COLUMNS}</h3>
            <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.DF_COLUMNS_HINT}</p>
            {columns.length === 0 ? (
              <p style={{ color: "#718096" }} data-testid="developer-df-columns-empty">
                {DEV_MSG.DF_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-df-columns-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_POS}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_SOURCE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_COL_LABEL}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_RENDER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.DF_COL_WIDTH}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {columns.map((c, i) => (
                      <tr
                        key={`${c.source ?? "col"}-${c.position ?? i}-${i}`}
                        data-testid={`developer-df-column-row-${i}`}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px" }}>
                          {c.position != null ? String(c.position) : "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {c.source || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{c.displayName || "—"}</td>
                        <td style={{ padding: "8px" }}>{c.renderType || "—"}</td>
                        <td style={{ padding: "8px" }}>
                          {c.width != null && c.width > 0 ? String(c.width) : "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginTop: "16px" }} data-testid="developer-df-gaps">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.DF_GAPS}</h3>
            <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
              <li>{DEV_MSG.DF_GAP_WRITE}</li>
              <li>{DEV_MSG.DF_GAP_COLUMNS_EDIT}</li>
              <li>{DEV_MSG.DF_GAP_COMMUNITIES}</li>
            </ul>
          </section>
        </>
      ) : null}
    </div>
  );
}
