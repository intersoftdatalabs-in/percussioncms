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
import type { CommunitySummary } from "../api/developer/types";
import { SimpleCatalogTable, type SimpleCatalogRow } from "./CatalogTable";
import { monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { DEV_MSG } from "./messages";
import { communityOpenKey, communityRowKey } from "./communityCatalog";

/** Build one catalog row; kept outside the component so render stays flat. */
export function toCommunityCatalogRow(
  c: CommunitySummary,
  index: number,
  onOpen: (key: string) => void,
): SimpleCatalogRow {
  const key = communityOpenKey(c);
  const rowKey = communityRowKey(c, index);
  const display = c.label || c.name || "—";
  const ariaName = c.label || c.name || key;
  return {
    key: rowKey,
    cells: [
      key ? (
        <button
          key="open"
          type="button"
          data-testid="developer-comm-open"
          data-comm-name={c.name || key}
          style={openButtonStyle}
          aria-label={`Open ${ariaName}`}
          onClick={() => onOpen(key)}
        >
          {display}
        </button>
      ) : (
        display
      ),
      <span key="n" style={monoCell}>
        {c.name || "—"}
      </span>,
      <span key="i" style={monoCell}>
        {c.id != null ? String(c.id) : c.guid?.stringValue || "—"}
      </span>,
      <span key="d" style={mutedCell}>
        {c.description || ""}
      </span>,
    ],
  };
}

/**
 * Presentational community catalog table (extracted from CommunitiesPanel so
 * the panel orchestrator stays under the complexity budget; no behavior
 * change — same testids, columns, and open affordance).
 */
export function CommunityCatalogTable({
  communities,
  onOpen,
}: {
  communities: CommunitySummary[];
  onOpen: (key: string) => void;
}): React.ReactElement {
  return (
    <SimpleCatalogTable
      tableTestId="developer-comm-table"
      rowTestId="developer-comm-row"
      columns={[
        DEV_MSG.COMM_COL_LABEL,
        DEV_MSG.COMM_COL_NAME,
        DEV_MSG.COMM_COL_ID,
        DEV_MSG.COMM_COL_DESCRIPTION,
      ]}
      rows={communities.map((c, index) => toCommunityCatalogRow(c, index, onOpen))}
    />
  );
}
