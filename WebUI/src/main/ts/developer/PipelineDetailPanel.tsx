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
import { getApplicationDetail } from "../api/developer/pipelinesApi";
import type { ApplicationDetail } from "../api/developer/types";
import { backButton, errorAlert, metaGrid, monoCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export function PipelineDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ApplicationDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getApplicationDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(panelErrMsg(err, DEV_MSG.PIPE_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  return (
    <div data-testid="developer-pipe-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-pipe-back"
        style={backButton}
      >
        ← {DEV_MSG.PIPE_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-pipe-detail-error" style={errorAlert}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-pipe-detail-loading">{DEV_MSG.PIPE_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-pipe-detail-title">
              {detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: "#4a5568" }}>
              {detail.id != null ? `id ${detail.id}` : ""}
              {detail.appRoot ? ` · ${detail.appRoot}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: "#4a5568" }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.PIPE_COL_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.appType || "—"}</dd>
              <dt>{DEV_MSG.PIPE_COL_ENABLED}</dt>
              <dd style={{ margin: 0 }}>
                {detail.enabled == null
                  ? "—"
                  : detail.enabled
                    ? DEV_MSG.YES
                    : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.PIPE_META_HIDDEN}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hidden == null ? "—" : detail.hidden ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.PIPE_META_VERSION}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.version || "—"}</dd>
              <dt>{DEV_MSG.PIPE_COL_ROOT}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.appRoot || "—"}</dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-pipe-datasets">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_DATASETS}</h3>
            <p style={{ color: "#4a5568", fontSize: "0.9rem" }}>{DEV_MSG.PIPE_DATASETS_HINT}</p>
            {(detail.dataSets || []).length === 0 ? (
              <p style={{ color: "#718096" }} data-testid="developer-pipe-datasets-empty">
                {DEV_MSG.PIPE_NONE}
              </p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  data-testid="developer-pipe-datasets-table"
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.9rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_NAME}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_KIND}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DS_REQUEST}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.PIPE_COL_DESCRIPTION}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(detail.dataSets || []).map((ds, i) => (
                      <tr
                        key={ds.name || `ds-${i}`}
                        data-testid={`developer-pipe-ds-row-${i}`}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {ds.name || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>{ds.kind || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {ds.requestPage || "—"}
                        </td>
                        <td style={{ padding: "8px", color: "#4a5568" }}>
                          {ds.description || ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-pipe-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.PIPE_GAPS}</h3>
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
