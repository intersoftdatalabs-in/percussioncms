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
import { getTemplateDetail } from "../api/developer/assemblyApi";
import type { TemplateDetail } from "../api/developer/types";
import { monoCell, mutedCell } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const metaGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "auto 1fr",
  gap: "4px 16px",
  marginTop: "12px",
  fontSize: "0.9rem",
};

const expressionCell: React.CSSProperties = {
  padding: "8px",
  fontFamily: "monospace",
  whiteSpace: "pre-wrap",
  wordBreak: "break-word",
  maxWidth: 320,
  overflowWrap: "anywhere",
};

const sourcePre: React.CSSProperties = {
  background: "#f7fafc",
  border: "1px solid #e2e8f0",
  borderRadius: "4px",
  padding: "12px",
  overflow: "auto",
  maxHeight: "320px",
  fontSize: "0.85rem",
};

export function TemplateDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<TemplateDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getTemplateDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        setDetail(d);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        // Keep prior detail visible when a subsequent load fails
        setError(panelErrMsg(err, DEV_MSG.TPL_DETAIL_ERROR));
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  return (
    <div data-testid="developer-tpl-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-tpl-back"
        aria-label="Back to templates list"
        style={{
          marginBottom: "12px",
          background: "transparent",
          border: "1px solid #cbd5e0",
          borderRadius: "4px",
          padding: "6px 12px",
          cursor: "pointer",
        }}
      >
        ← {DEV_MSG.TPL_BACK}
      </button>

      {error ? (
        <div
          role="alert"
          data-testid="developer-tpl-detail-error"
          style={{ color: "#b00020" }}
        >
          {error}
        </div>
      ) : null}

      {loading && detail == null ? (
        <div data-testid="developer-tpl-detail-loading">
          {DEV_MSG.TPL_DETAIL_LOADING}
        </div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-tpl-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            <div style={mutedCell}>
              <span style={monoCell}>
                {detail.name}
                {detail.templateId != null ? ` · ${detail.templateId}` : ""}
                {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
              </span>
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: "#2d3748" }}>{detail.description}</p>
            ) : null}
            <dl style={metaGrid}>
              <dt>{DEV_MSG.TPL_META_ASSEMBLER}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.assembler || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_OUTPUT}</dt>
              <dd style={{ margin: 0 }}>{detail.outputFormat || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_TYPE}</dt>
              <dd style={{ margin: 0 }}>{detail.templateType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_AA}</dt>
              <dd style={{ margin: 0 }}>{detail.aaType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_MIME}</dt>
              <dd style={{ margin: 0, ...monoCell }}>{detail.mimeType || "—"}</dd>
              <dt>{DEV_MSG.TPL_META_VARIANT}</dt>
              <dd style={{ margin: 0 }}>
                {detail.variant ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
            </dl>
          </header>

          <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-bindings">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_BINDINGS}</h3>
            {(detail.bindings || []).length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.TPL_NONE}</p>
            ) : (
              <div style={{ overflowX: "auto" }}>
                <table
                  style={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.95rem",
                  }}
                >
                  <thead>
                    <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_ORDER}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_VARIABLE}</th>
                      <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_EXPRESSION}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(detail.bindings || []).map((b, i) => (
                      <tr
                        key={`${b.variable ?? "b"}-${i}`}
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px" }}>
                          {b.executionOrder != null
                            ? Number(b.executionOrder)
                            : "—"}
                        </td>
                        <td style={{ padding: "8px", ...monoCell }}>
                          {b.variable || "—"}
                        </td>
                        <td style={expressionCell}>{b.expression || ""}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-slots">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_SLOTS}</h3>
            {(detail.slots || []).length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.TPL_NONE}</p>
            ) : (
              <ul>
                {(detail.slots || []).map((s) => (
                  <li key={s.name || s.guid?.stringValue || s.label}>
                    {s.label || s.name}
                    {s.name ? (
                      <span
                        style={{
                          ...monoCell,
                          color: "#718096",
                          marginLeft: "8px",
                          fontSize: "0.85rem",
                        }}
                      >
                        {s.name}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </section>

          {detail.templateSource ? (
            <section style={{ marginBottom: "16px" }} data-testid="developer-tpl-source">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_SOURCE}</h3>
              <pre style={sourcePre} lang="xml">
                {detail.templateSource}
              </pre>
            </section>
          ) : null}

          {(detail.designGaps || []).length > 0 ? (
            <section data-testid="developer-tpl-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.TPL_GAPS}</h3>
              <ul style={{ color: "#4a5568", fontSize: "0.9rem" }}>
                {(detail.designGaps || []).map((g) => (
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
