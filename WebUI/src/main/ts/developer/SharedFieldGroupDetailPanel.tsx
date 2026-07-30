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
import { getSharedFieldGroupDetail } from "../api/developer/sharedFieldsApi";
import type { SharedFieldGroupDetail } from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function SharedFieldGroupDetailPanel({
  name,
  onBack,
}: {
  name: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<SharedFieldGroupDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getSharedFieldGroupDetail(name)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.SF_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [name]);

  return (
    <div data-testid="developer-sf-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-sf-back"
        style={backButton}
      >
        ← {DEV_MSG.SF_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-sf-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-sf-detail-loading">{DEV_MSG.SF_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-sf-detail-title">
              {detail.name || name}
            </h2>
            <dl style={metaGrid}>
              <dt>{DEV_MSG.SF_COL_FILENAME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.filename || "—"}</dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-sf-fields">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SF_FIELDS}</h3>
            <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.SF_FIELDS_HINT}</p>
            {(detail.fields || []).length === 0 ? (
              <p style={{ color: "#718096" }} data-testid="developer-sf-fields-empty">
                {DEV_MSG.SF_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-sf-fields-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_FIELD}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_DATATYPE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_OCCURRENCE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_REQUIRED}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_SEARCH}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.SF_COL_READONLY}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(detail.fields || []).map((f, i) => (
                      <tr
                        key={f.name || `f-${i}`}
                        data-testid={`developer-sf-field-row-${i}`}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{f.dataType || "—"}</td>
                        <td style={{ padding: "8px" }}>{f.occurrence || "—"}</td>
                        <td style={{ padding: "8px" }}>
                          {f.required == null ? "—" : f.required ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.searchable == null
                            ? "—"
                            : f.searchable
                              ? DEV_MSG.YES
                              : DEV_MSG.NO}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.readOnly == null ? "—" : f.readOnly ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-sf-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.SF_GAPS}</h3>
              <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
                {detail.designGaps.map((g) => (
                  <li key={g}>{g}</li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
