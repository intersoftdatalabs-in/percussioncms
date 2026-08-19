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
import {
  createTemplate,
  deleteTemplate,
  listTemplates,
} from "../api/developer/assemblyApi";
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
import {
  catalogColors,
  monoCell,
  mutedCell,
  openButtonStyle,
} from "../developer/catalogStyles";
import { CreateTemplateDialog } from "./CreateTemplateDialog";
import { DeleteTemplateDialog } from "./DeleteTemplateDialog";
import { DESIGN_MSG } from "./messages";
import { TemplateSourceEditor } from "./TemplateSourceEditor";

/** Target for confirm-delete; {@code key} is the REST idOrName. */
export type TemplateDeleteTarget = {
  key: string;
  label: string;
};

/** Open-key for source editor; null when the row is not selectable. */
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
 * Design template library (#2808 list + #2809 source/JEXL + #2810 assembler/slots
 * + #3305 create + #3580 delete without Widget XML).
 */
export function TemplateLibraryPanel(): React.ReactElement {
  const [items, setItems] = useState<TemplateSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createBusy, setCreateBusy] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TemplateDeleteTarget | null>(
    null,
  );
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const reload = useCallback(() => {
    setReloadTick((n) => n + 1);
  }, []);

  useEffect(() => {
    let cancelled = false;
    setError(null);
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
  }, [reloadTick]);

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

  if (selected) {
    return (
      <TemplateSourceEditor
        idOrName={selected}
        onBack={() => setSelected(null)}
        onDeleted={() => {
          setSelected(null);
          reload();
        }}
      />
    );
  }

  const toolbar = (
    <div
      style={{
        display: "flex",
        justifyContent: "flex-end",
        marginBottom: 12,
      }}
    >
      <button
        type="button"
        data-testid="design-tpl-create"
        aria-label={DESIGN_MSG.TPL_CREATE_ARIA}
        style={{
          background: catalogColors.accent,
          color: "#fff",
          border: "none",
          borderRadius: 4,
          padding: "8px 14px",
          cursor: "pointer",
          font: "inherit",
        }}
        onClick={() => {
          setCreateError(null);
          setCreateOpen(true);
        }}
      >
        {DESIGN_MSG.TPL_CREATE}
      </button>
    </div>
  );

  const dialog = (
    <CreateTemplateDialog
      open={createOpen}
      busy={createBusy}
      error={createError}
      onCancel={() => {
        if (!createBusy) setCreateOpen(false);
      }}
      onSubmit={(input) => {
        setCreateBusy(true);
        setCreateError(null);
        createTemplate({
          name: input.name,
          label: input.label || input.name,
          description: input.description,
          assembler: input.assembler,
        })
          .then(() => {
            setCreateOpen(false);
            reload();
          })
          .catch((e: unknown) => {
            setCreateError(listErrMsg(e, DESIGN_MSG.TPL_CREATE_ERROR));
          })
          .finally(() => {
            setCreateBusy(false);
          });
      }}
    />
  );

  const deleteDialog = (
    <DeleteTemplateDialog
      open={deleteTarget != null}
      busy={deleteBusy}
      error={deleteError}
      label={deleteTarget?.label || ""}
      onCancel={() => {
        if (!deleteBusy) {
          setDeleteTarget(null);
          setDeleteError(null);
        }
      }}
      onConfirm={() => {
        if (!deleteTarget) return;
        setDeleteBusy(true);
        setDeleteError(null);
        deleteTemplate(deleteTarget.key)
          .then(() => {
            setDeleteTarget(null);
            reload();
          })
          .catch((e: unknown) => {
            setDeleteError(listErrMsg(e, DESIGN_MSG.TPL_DELETE_ERROR));
          })
          .finally(() => {
            setDeleteBusy(false);
          });
      }}
    />
  );

  if (error) {
    return (
      <div data-testid="design-tpl-panel">
        {toolbar}
        <CatalogStatus testId="design-tpl-error" error>
          {error}
        </CatalogStatus>
        {dialog}
        {deleteDialog}
      </div>
    );
  }
  if (items == null) {
    return (
      <CatalogStatus testId="design-tpl-loading">{DESIGN_MSG.TPL_LOADING}</CatalogStatus>
    );
  }
  if (items.length === 0) {
    return (
      <div data-testid="design-tpl-panel">
        {toolbar}
        <CatalogHint>{DESIGN_MSG.TPL_HINT}</CatalogHint>
        <CatalogStatus testId="design-tpl-empty">{DESIGN_MSG.TPL_EMPTY}</CatalogStatus>
        {dialog}
        {deleteDialog}
      </div>
    );
  }

  return (
    <div data-testid="design-tpl-panel">
      {toolbar}
      <CatalogHint>{DESIGN_MSG.TPL_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="design-tpl-table"
        rowTestId="design-tpl-row"
        columns={[
          DESIGN_MSG.TPL_COL_LABEL,
          DESIGN_MSG.TPL_COL_NAME,
          DESIGN_MSG.TPL_COL_ID,
          DESIGN_MSG.TPL_COL_DESCRIPTION,
          DESIGN_MSG.TPL_COL_ACTIONS,
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
              openKey ? (
                <button
                  key="delete"
                  type="button"
                  data-testid={`design-tpl-delete-${index}`}
                  aria-label={DESIGN_MSG.TPL_DELETE_ARIA.replace("{0}", label)}
                  style={{
                    background: "transparent",
                    border: `1px solid ${catalogColors.error}`,
                    color: catalogColors.error,
                    borderRadius: 4,
                    padding: "4px 8px",
                    cursor: "pointer",
                    font: "inherit",
                  }}
                  onClick={() => {
                    setDeleteError(null);
                    setDeleteTarget({ key: openKey, label });
                  }}
                >
                  {DESIGN_MSG.TPL_DELETE}
                </button>
              ) : (
                "—"
              ),
            ],
          };
        })}
      />
      {dialog}
      {deleteDialog}
    </div>
  );
}
