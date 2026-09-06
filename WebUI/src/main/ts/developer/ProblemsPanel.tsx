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
import type { DeveloperSection } from "../app/deepLinks/allowlists";
import {
  listDesignProblems,
  type DesignProblem,
} from "../api/developer/problemsApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

export interface ProblemsPanelProps {
  /** Navigate-to-source when the problem names a peer Developer section. */
  onNavigateToSource?: (section: DeveloperSection) => void;
}

function sourceLabel(row: DesignProblem): string {
  return row.objectName?.trim() || row.objectId?.trim() || row.objectType?.trim() || "—";
}

/**
 * Developer Problems — read-only session validation list (#4345 / Workbench §12.4).
 * Distinct from pipeline application Problems on the Pipelines detail page.
 */
export function ProblemsPanel({
  onNavigateToSource,
}: ProblemsPanelProps): React.ReactElement {
  const [rows, setRows] = useState<DesignProblem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listDesignProblems()
      .then((list) => {
        if (!cancelled) setRows(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.PROB_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!rows) return [];
    return [...rows].sort((a, b) => {
      if (a.severity !== b.severity) return a.severity === "ERROR" ? -1 : 1;
      return a.message.localeCompare(b.message, undefined, { sensitivity: "base" });
    });
  }, [rows]);

  if (error) {
    return (
      <CatalogStatus testId="developer-prob-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (rows == null) {
    return (
      <CatalogStatus testId="developer-prob-loading">{DEV_MSG.PROB_LOADING}</CatalogStatus>
    );
  }

  if (sorted.length === 0) {
    return (
      <div data-testid="developer-prob-panel">
        <CatalogHint>{DEV_MSG.PROB_HINT}</CatalogHint>
        <CatalogStatus testId="developer-prob-empty">{DEV_MSG.PROB_EMPTY}</CatalogStatus>
      </div>
    );
  }

  return (
    <div data-testid="developer-prob-panel">
      <CatalogHint>{DEV_MSG.PROB_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-prob-table"
        rowTestId="developer-prob-row"
        columns={[
          DEV_MSG.PROB_COL_SEVERITY,
          DEV_MSG.PROB_COL_SOURCE,
          DEV_MSG.PROB_COL_MESSAGE,
          DEV_MSG.PROB_COL_LOCATION,
          "",
        ]}
        rows={sorted.map((row) => ({
          key: row.id,
          dataAttrs: {
            "data-prob-id": row.id,
            "data-prob-severity": row.severity,
          },
          cells: [
            <span key="sev" data-testid="developer-prob-severity" style={mutedCell}>
              {row.severity === "WARNING" ? DEV_MSG.PROB_SEV_WARNING : DEV_MSG.PROB_SEV_ERROR}
            </span>,
            <span key="src" data-testid="developer-prob-source" style={mutedCell}>
              {sourceLabel(row)}
            </span>,
            <span key="msg" data-testid="developer-prob-message">
              {row.message}
            </span>,
            <span key="loc" style={mutedCell}>
              {row.location ?? "—"}
            </span>,
            row.navigateSection && onNavigateToSource ? (
              <button
                key="nav"
                type="button"
                data-testid="developer-prob-navigate"
                data-prob-navigate={row.navigateSection}
                aria-label={`${DEV_MSG.PROB_NAVIGATE} ${sourceLabel(row)}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  onNavigateToSource(row.navigateSection as DeveloperSection);
                }}
                style={openButtonStyle}
              >
                {DEV_MSG.PROB_NAVIGATE}
              </button>
            ) : (
              <span key="nav" style={{ color: catalogColors.muted }}>
                —
              </span>
            ),
          ],
        }))}
      />
    </div>
  );
}
