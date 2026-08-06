/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listWorkflows } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { WorkflowDetailPanel } from "./WorkflowDetailPanel";

/**
 * P0.17 — workflow catalog browse (SY-04) via existing workflowmanagement API.
 */
export function WorkflowsPanel(): React.ReactElement {
  const [items, setItems] = useState<WorkflowDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listWorkflows()
      .then((list) => {
        if (!cancelled) setItems(list);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(e, DEV_MSG.WF_ERROR));
        // Leave items null so error ≠ empty catalog (and retry can show loading).
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const sorted = useMemo(() => {
    if (!items) return [];
    return [...items]
      .filter((w) => (w.workflowName || "").trim().length > 0)
      .sort((a, b) =>
        (a.workflowName || "").localeCompare(b.workflowName || "", undefined, {
          sensitivity: "base",
        }),
      );
  }, [items]);

  if (selected) {
    return <WorkflowDetailPanel name={selected} onBack={() => setSelected(null)} />;
  }

  if (error)
    return (
      <CatalogStatus testId="developer-wf-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-wf-loading">{DEV_MSG.WF_LOADING}</CatalogStatus>;
  if (items.length === 0)
    return <CatalogStatus testId="developer-wf-empty">{DEV_MSG.WF_EMPTY}</CatalogStatus>;

  return (
    <div data-testid="developer-wf-panel">
      <CatalogHint>{DEV_MSG.WF_HINT}</CatalogHint>
      <SimpleCatalogTable
        tableTestId="developer-wf-table"
        rowTestId="developer-wf-row"
        columns={[
          DEV_MSG.WF_COL_NAME,
          DEV_MSG.WF_COL_DESC,
          DEV_MSG.WF_COL_DEFAULT,
          DEV_MSG.WF_COL_STEPS,
        ]}
        rows={sorted.map((w, index) => {
          const openKey = (w.workflowName || "").trim();
          const stepCount = Array.isArray(w.workflowSteps) ? w.workflowSteps.length : 0;
          return {
            key: `${openKey}-${index}`,
            onClick: () => setSelected(openKey),
            cells: [
              <button
                key="open"
                type="button"
                data-testid="developer-wf-open"
                aria-label={`Open ${openKey}`}
                onClick={(ev) => {
                  ev.stopPropagation();
                  setSelected(openKey);
                }}
                style={{ ...openButtonStyle, fontFamily: "monospace" }}
              >
                {openKey}
              </button>,
              <span key="d" style={mutedCell}>
                {w.workflowDescription || ""}
              </span>,
              w.defaultWorkflow ? DEV_MSG.WF_YES : "—",
              stepCount > 0 ? String(stepCount) : "—",
            ],
          };
        })}
      />
    </div>
  );
}
