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

import React from "react";
import { normalizeColumns } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { SimpleCatalogTable, type SimpleCatalogRow } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { displayFormatOpenKey, displayFormatRowKey } from "./displayFormatCatalog";
import { DEV_MSG } from "./messages";

/** Build one catalog row; kept outside the component so render stays flat. */
export function toDisplayFormatCatalogRow(
  f: DisplayFormat,
  index: number,
  onOpen: (f: DisplayFormat) => void,
): SimpleCatalogRow {
  const openKey = displayFormatOpenKey(f) ?? "";
  const interactive = openKey.length > 0;
  const colCount = normalizeColumns(f.columns).length;
  const usage: string[] = [];
  if (f.validForFolder) usage.push(DEV_MSG.DF_USAGE_FOLDER);
  if (f.validForViewsAndSearches) usage.push(DEV_MSG.DF_USAGE_VIEWS);
  if (f.validForRelatedContent) usage.push(DEV_MSG.DF_USAGE_RELATED);
  return {
    key: displayFormatRowKey(f, index),
    onClick: interactive ? () => onOpen(f) : undefined,
    // Exact name on the row — Playwright must not use hasText substring (#3269).
    dataAttrs: openKey ? { "data-df-name": openKey } : undefined,
    cells: [
      interactive ? (
        <button
          key="open"
          type="button"
          data-testid="developer-df-open"
          data-df-name={openKey}
          aria-label={`Open ${f.name || openKey}`}
          onClick={(e) => {
            e.stopPropagation();
            onOpen(f);
          }}
          style={{ ...openButtonStyle, fontFamily: "monospace" }}
        >
          {f.name || "—"}
        </button>
      ) : (
        <span key="n" style={monoCell}>
          {f.name || "—"}
        </span>
      ),
      <span
        key="l"
        data-testid="developer-df-catalog-label"
        data-df-name={openKey}
        data-df-label={f.label || f.displayName || ""}
      >
        {f.label || f.displayName || ""}
      </span>,
      colCount,
      <span key="u" style={{ fontSize: "0.85rem" }}>
        {usage.length ? usage.join(", ") : "—"}
      </span>,
      <span
        key="d"
        style={mutedCell}
        data-testid="developer-df-catalog-description"
        data-df-name={openKey}
        data-df-description={f.description || ""}
      >
        {f.description || ""}
      </span>,
    ],
  };
}

/**
 * Presentational display-format catalog table (extracted from
 * DisplayFormatsPanel so the panel orchestrator stays under the complexity
 * budget; no behavior change — same testids, columns, and open affordance).
 */
export function DisplayFormatCatalogTable({
  formats,
  onOpen,
}: {
  formats: DisplayFormat[];
  onOpen: (f: DisplayFormat) => void;
}): React.ReactElement {
  return (
    <SimpleCatalogTable
      tableTestId="developer-df-table"
      rowTestId="developer-df-row"
      columns={[
        DEV_MSG.DF_COL_NAME,
        DEV_MSG.DF_COL_LABEL,
        DEV_MSG.DF_COL_COLUMNS,
        DEV_MSG.DF_COL_USAGE,
        DEV_MSG.DF_COL_DESCRIPTION,
      ]}
      rows={formats.map((f, index) => toDisplayFormatCatalogRow(f, index, onOpen))}
    />
  );
}
