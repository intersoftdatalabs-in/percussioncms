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

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { resolveTemplateObjectGuid } from "../api/displayFormatGuid";
import { listTemplates } from "../api/developer/assemblyApi";
import type { TemplateSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { DeveloperSectionErrorBoundary } from "./DeveloperSectionErrorBoundary";
import { TemplateDetailPanel } from "./TemplateDetailPanel";
import { TemplateImportWizard } from "./TemplateImportWizard";

/** Open-key for detail route; null when the row is not selectable. */
function selectionKey(t: TemplateSummary): string | null {
  if (t.templateName) return t.templateName;
  if (t.templateId != null) return String(t.templateId);
  return null;
}

type SelectedTemplate = {
  idOrName: string;
  catalogGuid?: string;
};

export function TemplatesPanel(): React.ReactElement {
  const [items, setItems] = useState<TemplateSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedTemplate | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  const reloadCatalog = useCallback(() => {
    setReloadToken((n) => n + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setItems(null);
    listTemplates()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.TPL_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

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

  function openTemplate(t: TemplateSummary) {
    const idOrName = selectionKey(t);
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveTemplateObjectGuid(t),
    });
  }

  if (selected) {
    return (
      <DeveloperSectionErrorBoundary
        label={DEV_MSG.TAB_TEMPLATES}
        testId="developer-tpl-panel-error"
      >
        <TemplateDetailPanel
          idOrName={selected.idOrName}
          catalogGuid={selected.catalogGuid}
          onBack={() => setSelected(null)}
        />
      </DeveloperSectionErrorBoundary>
    );
  }

  return (
    <div data-testid="developer-tpl-panel">
      <CatalogHint>{DEV_MSG.TPL_HINT}</CatalogHint>
      <TemplateImportWizard onImported={() => reloadCatalog()} />
      {error ? (
        <CatalogStatus testId="developer-tpl-error" error>
          {error}
        </CatalogStatus>
      ) : items == null ? (
        <CatalogStatus testId="developer-tpl-loading">{DEV_MSG.TPL_LOADING}</CatalogStatus>
      ) : items.length === 0 ? (
        <CatalogStatus testId="developer-tpl-empty">{DEV_MSG.TPL_EMPTY}</CatalogStatus>
      ) : (
        <SimpleCatalogTable
          tableTestId="developer-tpl-table"
          rowTestId="developer-tpl-row"
          columns={[
            DEV_MSG.TPL_COL_LABEL,
            DEV_MSG.TPL_COL_NAME,
            DEV_MSG.TPL_COL_ID,
            DEV_MSG.TPL_COL_DESCRIPTION,
          ]}
          rows={sorted.map((t, index) => {
            const openKey = selectionKey(t);
            return {
              key: String(t.templateId ?? t.templateName ?? `tpl-${index}`),
              cells: [
                openKey ? (
                  <button
                    key="open"
                    type="button"
                    style={openButtonStyle}
                    aria-label={`Open ${t.templateLabel || t.templateName || openKey}`}
                    onClick={() => openTemplate(t)}
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
      )}
    </div>
  );
}
