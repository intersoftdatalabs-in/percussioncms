/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listWorkflows } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { mutedCell } from "./catalogStyles";
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
      <div style={{ overflowX: "auto" }}>
        <table
          data-testid="developer-wf-table"
          style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.95rem" }}
        >
          <thead>
            <tr style={{ textAlign: "left", borderBottom: "2px solid #e2e8f0" }}>
              <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_NAME}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_DESC}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_DEFAULT}</th>
              <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_STEPS}</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((w, index) => {
              const openKey = (w.workflowName || "").trim();
              const stepCount = Array.isArray(w.workflowSteps) ? w.workflowSteps.length : 0;
              return (
                <tr
                  key={`${openKey}-${index}`}
                  data-testid="developer-wf-row"
                  style={{
                    borderBottom: "1px solid #edf2f7",
                    cursor: "pointer",
                  }}
                  onClick={() => setSelected(openKey)}
                >
                  <td style={{ padding: "8px" }}>
                    <button
                      type="button"
                      data-testid="developer-wf-open"
                      aria-label={`Open ${openKey}`}
                      onClick={(ev) => {
                        ev.stopPropagation();
                        setSelected(openKey);
                      }}
                      style={{
                        background: "transparent",
                        border: "none",
                        color: "#007ea8",
                        cursor: "pointer",
                        font: "inherit",
                        padding: 0,
                        fontFamily: "monospace",
                      }}
                    >
                      {openKey}
                    </button>
                  </td>
                  <td style={{ padding: "8px", ...mutedCell }}>
                    {w.workflowDescription || ""}
                  </td>
                  <td style={{ padding: "8px" }}>
                    {w.defaultWorkflow ? DEV_MSG.WF_YES : "—"}
                  </td>
                  <td style={{ padding: "8px" }}>
                    {stepCount > 0 ? String(stepCount) : "—"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
