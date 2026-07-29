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
import { getContentTypeDetail } from "../api/developer/contentTypesApi";
import type { ContentTypeDetail } from "../api/developer/types";
import { DEV_MSG } from "./messages";

function errorMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return DEV_MSG.SESSION_REDIRECT;
  }
  const api = err as ApiError;
  if (api && typeof api.status === "number") {
    return `${DEV_MSG.CT_DETAIL_ERROR} (${api.status})`;
  }
  if (err instanceof Error && err.message) {
    return `${DEV_MSG.CT_DETAIL_ERROR} ${err.message}`;
  }
  return DEV_MSG.CT_DETAIL_ERROR;
}

export function ContentTypeDetailPanel({
  idOrName,
  onBack,
}: {
  idOrName: string;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ContentTypeDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    getContentTypeDetail(idOrName)
      .then((d) => {
        if (!cancelled) setDetail(d);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        // Session redirect navigates away; still leave loading so UI does not hang
        // if navigation is delayed or blocked.
        setError(errorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  return (
    <div data-testid="developer-ct-detail">
      <button
        type="button"
        onClick={onBack}
        data-testid="developer-ct-back"
        style={{
          marginBottom: "12px",
          background: "transparent",
          border: "1px solid #cbd5e0",
          borderRadius: "4px",
          padding: "6px 12px",
          cursor: "pointer",
        }}
      >
        ← {DEV_MSG.CT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ct-detail-error" style={{ color: "#b00020" }}>
          {error}
        </div>
      ) : null}

      {!error && detail == null ? (
        <div data-testid="developer-ct-detail-loading">{DEV_MSG.CT_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ct-detail-title">
              {detail.label || detail.name || idOrName}
            </h2>
            <div style={{ fontFamily: "monospace", color: "#4a5568" }}>
              {detail.name}
              {detail.guid?.stringValue ? ` · ${detail.guid.stringValue}` : ""}
            </div>
            {detail.description ? (
              <p style={{ marginTop: "8px", color: "#2d3748" }}>{detail.description}</p>
            ) : null}
            <dl
              style={{
                display: "grid",
                gridTemplateColumns: "auto 1fr",
                gap: "4px 16px",
                marginTop: "12px",
                fontSize: "0.9rem",
              }}
            >
              <dt>{DEV_MSG.CT_META_ENABLED}</dt>
              <dd style={{ margin: 0 }}>
                {detail.enabled === false ? DEV_MSG.NO : DEV_MSG.YES}
              </dd>
              <dt>{DEV_MSG.CT_META_HIDDEN}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hideFromMenu ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.CT_META_APP}</dt>
              <dd style={{ margin: 0, fontFamily: "monospace" }}>
                {detail.appName || "—"}
              </dd>
            </dl>
          </header>

          {detail.childFieldSets && detail.childFieldSets.length > 0 ? (
            <section style={{ marginBottom: "16px" }}>
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_CHILD_SETS}</h3>
              <ul data-testid="developer-ct-child-sets">
                {detail.childFieldSets.map((n) => (
                  <li key={n} style={{ fontFamily: "monospace" }}>
                    {n}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          <section
            style={{ marginBottom: "16px" }}
            data-testid="developer-ct-workflows"
          >
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_WORKFLOWS}</h3>
            {detail.defaultWorkflow ? (
              <p style={{ fontSize: "0.9rem", marginTop: 0 }}>
                <strong>{DEV_MSG.CT_DEFAULT_WF}:</strong>{" "}
                {detail.defaultWorkflow.label || detail.defaultWorkflow.name}
              </p>
            ) : null}
            {(detail.allowedWorkflows || []).length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.CT_NONE}</p>
            ) : (
              <ul>
                {(detail.allowedWorkflows || []).map((w) => (
                  <li key={w.name || w.guid?.stringValue || w.label}>
                    {w.label || w.name}
                    {w.isDefault ? " (default)" : ""}
                    {w.name ? (
                      <span
                        style={{
                          fontFamily: "monospace",
                          color: "#718096",
                          marginLeft: "8px",
                          fontSize: "0.85rem",
                        }}
                      >
                        {w.name}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section
            style={{ marginBottom: "16px" }}
            data-testid="developer-ct-templates"
          >
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_TEMPLATES}</h3>
            {(detail.allowedTemplates || []).length === 0 ? (
              <p style={{ color: "#718096" }}>{DEV_MSG.CT_NONE}</p>
            ) : (
              <ul>
                {(detail.allowedTemplates || []).map((t) => (
                  <li key={t.name || t.guid?.stringValue || t.label}>
                    {t.label || t.name}
                    {t.name ? (
                      <span
                        style={{
                          fontFamily: "monospace",
                          color: "#718096",
                          marginLeft: "8px",
                          fontSize: "0.85rem",
                        }}
                      >
                        {t.name}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_FIELDS}</h3>
            <div style={{ overflowX: "auto" }}>
              <table
                data-testid="developer-ct-fields-table"
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                  fontSize: "0.9rem",
                }}
              >
                <thead>
                  <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELD}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_ORIGIN}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_DATATYPE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_CONTROL}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_REQUIRED}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_READONLY}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_OCCURRENCE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_RULES}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_SEARCH}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELDSET}</th>
                  </tr>
                </thead>
                <tbody>
                  {(detail.fields || []).map((f) => {
                    const rules: string[] = [];
                    if (f.hasValidation) rules.push(DEV_MSG.CT_RULE_VALIDATION);
                    if (f.hasVisibilityRules) rules.push(DEV_MSG.CT_RULE_VISIBILITY);
                    if (f.hasInputTranslation) rules.push(DEV_MSG.CT_RULE_IN_XFORM);
                    if (f.hasOutputTranslation) rules.push(DEV_MSG.CT_RULE_OUT_XFORM);
                    return (
                      <tr
                        key={`${f.fieldSet || "parent"}:${f.name}`}
                        data-testid="developer-ct-field-row"
                        style={{ borderBottom: "1px solid #edf2f7" }}
                      >
                        <td style={{ padding: "8px" }}>
                          <div>{f.label || f.name}</div>
                          <div
                            style={{
                              fontFamily: "monospace",
                              color: "#718096",
                              fontSize: "0.85rem",
                            }}
                          >
                            {f.name}
                          </div>
                        </td>
                        <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.dataType || "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.control || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.required ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.readOnly ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td
                          style={{ padding: "8px", fontFamily: "monospace" }}
                          data-testid="developer-ct-field-occurrence"
                        >
                          {f.occurrence || "—"}
                        </td>
                        <td
                          style={{ padding: "8px", fontSize: "0.85rem", color: "#4a5568" }}
                          data-testid="developer-ct-field-rules"
                        >
                          {rules.length > 0 ? rules.join(", ") : "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.searchable ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.fieldSet || "—"}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          {detail.designGaps && detail.designGaps.length > 0 ? (
            <section data-testid="developer-ct-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_GAPS}</h3>
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
