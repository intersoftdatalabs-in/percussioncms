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

import React, { useCallback, useEffect, useRef, useState } from "react";
import { resolveContentTypeObjectGuid } from "../api/displayFormatGuid";
import {
  asContentTypeText,
  contentTypeSelectionKey,
  listContentTypes,
  unwrapContentTypeList,
} from "../api/developer/contentTypesApi";
import type { ContentTypeDetail, ContentTypeSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { ContentTypeCreatePanel } from "./ContentTypeCreatePanel";
import { ContentTypeDetailPanel } from "./ContentTypeDetailPanel";
import { ContentTypeImportWizard } from "./ContentTypeImportWizard";
import { DeveloperSectionErrorBoundary } from "./DeveloperSectionErrorBoundary";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

/** Catalog cell / sort keys — JAXB wraps unwrap in {@link asContentTypeText}. */
function asCatalogText(value: unknown): string {
  return asContentTypeText(value);
}

/**
 * Catalog rows must always be a real array. listContentTypes already unwraps;
 * re-apply here so a missed envelope cannot {@code [...obj].sort} into
 * DeveloperSectionErrorBoundary (#3706).
 */
function asContentTypeCatalog(raw: unknown): ContentTypeSummary[] {
  return unwrapContentTypeList(raw);
}

function displayId(ct: ContentTypeSummary): string {
  return resolveContentTypeObjectGuid(ct) || "—";
}

function selectionKey(ct: ContentTypeSummary): string | null {
  return contentTypeSelectionKey(ct);
}

function catalogSortKey(ct: ContentTypeSummary): string {
  return asCatalogText(ct.label) || asCatalogText(ct.name);
}

type SelectedContentType = {
  idOrName: string;
  catalogGuid?: string;
};

/**
 * P0.1 list + P0.2 detail + CD-01 catalog create / lock-held delete.
 */
export function ContentTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedContentType | "new" | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback((opts?: { showLoading?: boolean }) => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    if (opts?.showLoading) {
      setItems(null);
    }
    setError(null);
    return listContentTypes()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(asContentTypeCatalog(list));
      })
      .catch((err: unknown) => {
        if (!mountedRef.current) return;
        // Session redirect navigates away; still leave an error so UI does not hang
        // if navigation is delayed or blocked.
        setError(panelErrMsg(err, DEV_MSG.CT_ERROR));
        setItems([]);
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  function openContentType(ct: ContentTypeSummary) {
    const idOrName = selectionKey(ct);
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveContentTypeObjectGuid(ct),
    });
  }

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  function handleCreated(detail: ContentTypeDetail): void {
    const idOrName = asContentTypeText(detail.name);
    void reload();
    if (idOrName) {
      setSelected({
        idOrName,
        catalogGuid: resolveContentTypeObjectGuid(detail),
      });
    } else {
      setSelected(null);
    }
  }

  if (selected === "new") {
    return (
      <ContentTypeCreatePanel
        onBack={() => setSelected(null)}
        onCreated={handleCreated}
      />
    );
  }

  if (selected) {
    return (
      <DeveloperSectionErrorBoundary
        label={DEV_MSG.TAB_CONTENT_TYPES}
        testId="developer-ct-detail-error"
      >
        <ContentTypeDetailPanel
          idOrName={selected.idOrName}
          catalogGuid={selected.catalogGuid}
          onBack={() => setSelected(null)}
          onDeleted={handleDeleted}
        />
      </DeveloperSectionErrorBoundary>
    );
  }

  const sorted =
    items && items.length > 0
      ? [...items].sort((a, b) =>
          catalogSortKey(a).localeCompare(catalogSortKey(b), undefined, {
            sensitivity: "base",
          }),
        )
      : [];

  return (
    <div data-testid="developer-ct-panel">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
          gap: "12px",
          flexWrap: "wrap",
        }}
      >
        <CatalogHint>{DEV_MSG.CT_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-ct-new"
          onClick={() => setSelected("new")}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.CT_NEW}
        </button>
      </div>
      <ContentTypeImportWizard onImported={() => void reload()} />
      {error ? (
        <CatalogStatus testId="developer-ct-error" error>
          {error}
        </CatalogStatus>
      ) : items == null ? (
        <CatalogStatus testId="developer-ct-loading">{DEV_MSG.CT_LOADING}</CatalogStatus>
      ) : items.length === 0 ? (
        <CatalogStatus testId="developer-ct-empty">{DEV_MSG.CT_EMPTY}</CatalogStatus>
      ) : (
      <SimpleCatalogTable
        tableTestId="developer-ct-table"
        rowTestId="developer-ct-row"
        columns={[
          DEV_MSG.CT_COL_LABEL,
          DEV_MSG.CT_COL_NAME,
          DEV_MSG.CT_COL_ID,
          DEV_MSG.CT_COL_DESCRIPTION,
        ]}
        rows={sorted.map((ct) => {
          const resolved = resolveContentTypeObjectGuid(ct);
          const label = asCatalogText(ct.label);
          const name = asCatalogText(ct.name);
          const key =
            resolved ||
            name ||
            `${label || "ct"}-${displayId(ct)}`;
          const openKey = selectionKey(ct);
          const interactive = !!openKey;
          return {
            key,
            dataAttrs: openKey ? { "data-ct-name": openKey } : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  style={openButtonStyle}
                  data-testid="developer-ct-open"
                  data-ct-name={openKey}
                  aria-label={`Open ${label || name || openKey}`}
                  onClick={() => openContentType(ct)}
                >
                  {label || name || "—"}
                </button>
              ) : (
                <span key="lbl" style={mutedCell}>
                  {label || "—"}
                </span>
              ),
              <span key="n" style={monoCell}>
                {name || "—"}
              </span>,
              <span key="i" style={monoCell}>
                {displayId(ct)}
              </span>,
              <span key="d" style={mutedCell}>
                {asCatalogText(ct.description)}
              </span>,
            ],
          };
        })}
      />
      )}
    </div>
  );
}
