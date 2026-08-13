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

import React, { useEffect, useState } from "react";
import { resolveContentTypeObjectGuid } from "../api/displayFormatGuid";
import { listContentTypes } from "../api/developer/contentTypesApi";
import type { ContentTypeSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { ContentTypeDetailPanel } from "./ContentTypeDetailPanel";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

function displayId(ct: ContentTypeSummary): string {
  return resolveContentTypeObjectGuid(ct) || "—";
}

function selectionKey(ct: ContentTypeSummary): string {
  return ct.name || resolveContentTypeObjectGuid(ct) || displayId(ct);
}

type SelectedContentType = {
  idOrName: string;
  catalogGuid?: string;
};

/**
 * P0.1 list + P0.2 read-only field catalog detail.
 */
export function ContentTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedContentType | null>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    setItems(null);
    listContentTypes()
      .then((list) => {
        if (!cancelled) {
          setItems(list);
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        // Session redirect navigates away; still leave an error so UI does not hang
        // if navigation is delayed or blocked.
        setError(panelErrMsg(err, DEV_MSG.CT_ERROR));
        setItems([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function openContentType(ct: ContentTypeSummary) {
    const idOrName = selectionKey(ct);
    if (!idOrName || idOrName === "—") return;
    setSelected({
      idOrName,
      catalogGuid: resolveContentTypeObjectGuid(ct),
    });
  }

  if (selected) {
    return (
      <ContentTypeDetailPanel
        idOrName={selected.idOrName}
        catalogGuid={selected.catalogGuid}
        onBack={() => setSelected(null)}
      />
    );
  }

  if (error) {
    return (
      <CatalogStatus testId="developer-ct-error" error>
        {error}
      </CatalogStatus>
    );
  }

  if (items == null) {
    return <CatalogStatus testId="developer-ct-loading">{DEV_MSG.CT_LOADING}</CatalogStatus>;
  }

  if (items.length === 0) {
    return <CatalogStatus testId="developer-ct-empty">{DEV_MSG.CT_EMPTY}</CatalogStatus>;
  }

  const sorted = [...items].sort((a, b) =>
    (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
      sensitivity: "base",
    }),
  );

  return (
    <div data-testid="developer-ct-panel">
      <CatalogHint>{DEV_MSG.CT_HINT}</CatalogHint>
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
          const key =
            resolved ||
            ct.name ||
            `${ct.label ?? "ct"}-${displayId(ct)}`;
          const openKey = selectionKey(ct);
          const interactive = openKey !== "—";
          return {
            key,
            onClick: interactive ? () => openContentType(ct) : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  style={openButtonStyle}
                  aria-label={`Open ${ct.label || ct.name || openKey}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    openContentType(ct);
                  }}
                >
                  {ct.label || "—"}
                </button>
              ) : (
                <span key="lbl" style={mutedCell}>
                  {ct.label || "—"}
                </span>
              ),
              <span key="n" style={monoCell}>
                {ct.name || "—"}
              </span>,
              <span key="i" style={monoCell}>
                {displayId(ct)}
              </span>,
              <span key="d" style={mutedCell}>
                {ct.description || ""}
              </span>,
            ],
          };
        })}
      />
    </div>
  );
}
