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
import { isSessionRedirectError, type ApiError } from "../api/client";
import { listTemplates } from "../api/developer/assemblyApi";
import type { TemplateSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { DEV_MSG } from "./messages";
import { TemplateDetailPanel } from "./TemplateDetailPanel";

function errMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
  const api = err as ApiError;
  if (api && typeof api.status === "number") return `${fallback} (${api.status})`;
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

function selectionKey(t: TemplateSummary): string {
  if (t.templateName) return t.templateName;
  if (t.templateId != null) return String(t.templateId);
  return "—";
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

export function TemplatesPanel(): React.ReactElement {
  const [items, setItems] = useState<TemplateSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listTemplates()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(errMsg(e, DEV_MSG.TPL_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (selected) {
    return (
      <TemplateDetailPanel idOrName={selected} onBack={() => setSelected(null)} />
    );
  }

  if (error) return <CatalogStatus testId="developer-tpl-error" error>{error}</CatalogStatus>;
  if (items == null)
    return <CatalogStatus testId="developer-tpl-loading">{DEV_MSG.TPL_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-tpl-empty">{DEV_MSG.TPL_EMPTY}</CatalogStatus>;

  const sorted = [...items].sort((a, b) =>
    (a.templateLabel || a.templateName || "").localeCompare(
      b.templateLabel || b.templateName || "",
      undefined,
      { sensitivity: "base" },
    ),
  );

  return (
    <div data-testid="developer-tpl-panel">
      <CatalogHint>{DEV_MSG.TPL_HINT}</CatalogHint>
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-tpl-table"
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontSize: "0.95rem",
          }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_LABEL}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_ID}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.TPL_COL_DESCRIPTION}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((t, index) => {
              const openKey = selectionKey(t);
              const interactive = openKey !== "—";
              const key = String(t.templateId ?? t.templateName ?? `tpl-${index}`);
              return (
                <tr
                  key={key}
                  data-testid="developer-tpl-row"
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
                        aria-label={`Open ${t.templateLabel || t.templateName || openKey}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelected(openKey);
                        }}
                      >
                        {t.templateLabel || "—"}
                      </button>
                    ) : (
                      t.templateLabel || "—"
                    )}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {t.templateName || "—"}
                  </td>
                  <td style={{ padding: "8px", fontFamily: "monospace" }}>
                    {t.templateId != null ? String(t.templateId) : "—"}
                  </td>
                  <td style={{ padding: "8px", color: "#4a5568" }}>
                    {t.templateDescription || ""}
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
