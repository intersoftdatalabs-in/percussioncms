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
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { DEV_MSG } from "./messages";

function errMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DEV_MSG.SESSION_REDIRECT;
  const api = err as ApiError;
  if (api && typeof api.status === "number") return `${fallback} (${api.status})`;
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

export function TemplatesPanel(): React.ReactElement {
  const [items, setItems] = useState<TemplateSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listTemplates()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        // Session redirect navigates away; still leave loading so UI does not hang
        // if navigation is delayed or blocked.
        setError(errMsg(e, DEV_MSG.TPL_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

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
      <SimpleCatalogTable
        tableTestId="developer-tpl-table"
        rowTestId="developer-tpl-row"
        columns={[
          DEV_MSG.TPL_COL_LABEL,
          DEV_MSG.TPL_COL_NAME,
          DEV_MSG.TPL_COL_ID,
          DEV_MSG.TPL_COL_DESCRIPTION,
        ]}
        rows={sorted.map((t, index) => ({
          key: String(t.templateId ?? t.templateName ?? `tpl-${index}`),
          cells: [
            t.templateLabel || "—",
            <span key="n" style={{ fontFamily: "monospace" }}>
              {t.templateName || "—"}
            </span>,
            <span key="i" style={{ fontFamily: "monospace" }}>
              {t.templateId != null ? String(t.templateId) : "—"}
            </span>,
            <span key="d" style={{ color: "#4a5568" }}>
              {t.templateDescription || ""}
            </span>,
          ],
        }))}
      />
    </div>
  );
}
