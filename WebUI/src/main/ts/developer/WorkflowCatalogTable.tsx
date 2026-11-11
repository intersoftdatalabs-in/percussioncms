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
import type { WorkflowDef } from "../api/developer/types";
import { SimpleCatalogTable, type SimpleCatalogRow } from "./CatalogTable";
import { mutedCell, openButtonStyle } from "./catalogStyles";
import { DEV_MSG } from "./messages";
import { workflowOpenKey, workflowRowKey, workflowStepCount } from "./workflowCatalog";

/** Build one catalog row; kept outside the component so render stays flat. */
export function toWorkflowCatalogRow(
  w: WorkflowDef,
  index: number,
  onOpen: (name: string) => void,
  onCopy: (name: string) => void,
): SimpleCatalogRow {
  const openKey = workflowOpenKey(w);
  const stepCount = workflowStepCount(w);
  return {
    key: workflowRowKey(w, index),
    onClick: () => onOpen(openKey),
    cells: [
      <span key="open" style={{ display: "inline-flex", gap: "8px", alignItems: "center" }}>
        <button
          type="button"
          data-testid="developer-wf-open"
          data-wf-name={openKey}
          aria-label={`Open ${openKey}`}
          onClick={(ev) => {
            ev.stopPropagation();
            onOpen(openKey);
          }}
          style={{ ...openButtonStyle, fontFamily: "monospace" }}
        >
          {openKey}
        </button>
        <button
          type="button"
          data-testid="developer-wf-copy"
          data-wf-name={openKey}
          aria-label={`Copy ${openKey}`}
          onClick={(ev) => {
            ev.stopPropagation();
            onCopy(openKey);
          }}
          style={openButtonStyle}
        >
          {DEV_MSG.WF_COPY}
        </button>
      </span>,
      <span key="d" style={mutedCell}>
        {w.workflowDescription || ""}
      </span>,
      w.defaultWorkflow ? DEV_MSG.WF_YES : "—",
      stepCount > 0 ? String(stepCount) : "—",
    ],
  };
}

/**
 * Presentational workflow catalog table (extracted from WorkflowsPanel so the
 * panel orchestrator stays under the complexity budget; no behavior change).
 */
export function WorkflowCatalogTable({
  workflows,
  onOpen,
  onCopy,
}: {
  workflows: WorkflowDef[];
  onOpen: (name: string) => void;
  onCopy: (name: string) => void;
}): React.ReactElement {
  return (
    <SimpleCatalogTable
      tableTestId="developer-wf-table"
      rowTestId="developer-wf-row"
      columns={[DEV_MSG.WF_COL_NAME, DEV_MSG.WF_COL_DESC, DEV_MSG.WF_COL_DEFAULT, DEV_MSG.WF_COL_STEPS]}
      rows={workflows.map((w, index) => toWorkflowCatalogRow(w, index, onOpen, onCopy))}
    />
  );
}
