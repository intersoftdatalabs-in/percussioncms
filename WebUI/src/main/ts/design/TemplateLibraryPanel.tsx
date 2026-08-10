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

import React, { useEffect, useMemo, useState } from "react";
import { listTemplates } from "../api/developer/assemblyApi";
import type { TemplateSummary } from "../api/developer/types";
import {
  extractRestErrorMessage,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import {
  CatalogHint,
  CatalogStatus,
  SimpleCatalogTable,
} from "../developer/CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "../developer/catalogStyles";
import { DESIGN_MSG } from "./messages";
import { TemplateDetailDrawer } from "./TemplateDetailDrawer";

/** Open-key for detail drawer; null when the row is not selectable. */
export function templateSelectionKey(t: TemplateSummary): string | null {
  if (t.templateName) return t.templateName;
  if (t.templateId != null) return String(t.templateId);
  return null;
}

function listErrMsg(err: unknown, fallback: string): string {
  if (isSessionRedirectError(err)) return DESIGN_MSG.SESSION_REDIRECT;
  if (isApiError(err)) {
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) return `${fallback} ${fromBody}`;
    return `${fallback} (${err.status})`;
  }
  if (err instanceof Error && err.message) return `${fallback} ${err.message}`;
  return fallback;
}

/**
 * Design template library list (#2808): browse catalog, empty/error states,
 * read-only detail drawer. Reuses public REST {@code GET /services/templates}.
 */
export function TemplateLibraryPanel(): React.ReactElement {
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
        setError(listErrMsg(e, DESIGN_MSG.TPL_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items].sort((a, b) =>
      (a.templateLabel || a.templateName || "").localeCompare(
        b.templateLabel || b.templateName || "",
        undefined,
        { sensitivity: "base" },
      ),
    );
  }, [items]);

  if (error) {
    return (
      <CatalogStatus testId="design-tpl-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (items == null) {
    return (
      <CatalogStatus testId="design-tpl-loading">{DESIGN_MSG.TPL_LOADING}</CatalogStatus>
    );
  }
  if (items.length === 0) {
    return (
      <CatalogStatus testId="design-tpl-empty">{DESIGN_MSG.TPL_EMPTY}</CatalogStatus>
    );
  }

  return (
    <div data-testid="design-tpl-panel">
      <CatalogHint>{DESIGN_MSG.TPL_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="design-tpl-table"
        rowTestId="design-tpl-row"
        columns={[
          DESIGN_MSG.TPL_COL_LABEL,
          DESIGN_MSG.TPL_COL_NAME,
          DESIGN_MSG.TPL_COL_ID,
          DESIGN_MSG.TPL_COL_DESCRIPTION,
        ]}
        rows={sorted.map((t, index) => {
          const openKey = templateSelectionKey(t);
          const label = t.templateLabel || t.templateName || openKey || "—";
          return {
            key: String(t.templateId ?? t.templateName ?? `tpl-${index}`),
            cells: [
              openKey ? (
                <button
                  key="open"
                  type="button"
                  style={openButtonStyle}
                  aria-label={DESIGN_MSG.TPL_OPEN_ARIA.replace("{0}", label)}
                  data-testid={`design-tpl-open-${index}`}
                  onClick={() => setSelected(openKey)}
                >
                  {t.templateLabel || "—"}
                </button>
              ) : (
                t.templateLabel || "—"
              ),
              <span key="n" style={monoCell}>
                {t.templateName || "—"}
              </span>,
              <span key="i" style={monoCell}>
                {t.templateId != null ? String(t.templateId) : "—"}
              </span>,
              <span key="d" style={mutedCell}>
                {t.templateDescription || ""}
              </span>,
            ],
          };
        })}
      />
      {selected && (
        <TemplateDetailDrawer
          idOrName={selected}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}
