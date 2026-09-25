/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listWorkflows } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import type { WorkflowCreateResult } from "../api/developer/workflowsApi";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, mutedCell, openButtonStyle } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { WorkflowCopyPanel } from "./WorkflowCopyPanel";
import { WorkflowCreatePanel } from "./WorkflowCreatePanel";
import { WorkflowDetailPanel } from "./WorkflowDetailPanel";

/**
 * P0.17 — workflow catalog browse (SY-04) via existing workflowmanagement API,
 * plus slice 21 catalog create (POST /services/workflows) and slice 36 copy.
 */
export function WorkflowsPanel(): React.ReactElement {
  const [items, setItems] = useState<WorkflowDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | "new" | null>(null);

  function reload(): void {
    setError(null);
    listWorkflows()
      .then((list) => {
        setItems(list);
      })
      .catch((e: unknown) => {
        setError(panelErrMsg(e, DEV_MSG.WF_ERROR));
        // Leave items null so error ≠ empty catalog (and retry can show loading).
      });
  }

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

  function handleCreated(created: WorkflowCreateResult): void {
    const name = (created.workflowName || "").trim();
    reload();
    setSelected(name ? name : null);
  }

  function handleDeleted(): void {
    setSelected(null);
    reload();
  }

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

  if (selected === "new") {
    return (
      <WorkflowCreatePanel onBack={() => setSelected(null)} onCreated={handleCreated} />
    );
  }

  if (selected != null && selected.startsWith("copy:")) {
    const sourceName = selected.slice("copy:".length);
    return (
      <WorkflowCopyPanel
        sourceName={sourceName}
        onBack={() => setSelected(null)}
        onCopied={handleCreated}
      />
    );
  }

  if (selected) {
    return (
      <WorkflowDetailPanel
        name={selected}
        onBack={() => setSelected(null)}
        onDeleted={handleDeleted}
      />
    );
  }

  if (error)
    return (
      <CatalogStatus testId="developer-wf-error" error>
        {error}
      </CatalogStatus>
    );
  if (items == null)
    return <CatalogStatus testId="developer-wf-loading">{DEV_MSG.WF_LOADING}</CatalogStatus>;
  const newWorkflowButton = (
    <button
      type="button"
      data-testid="developer-wf-new"
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
      {DEV_MSG.WF_NEW}
    </button>
  );

  if (items.length === 0) {
    return (
      <div data-testid="developer-wf-panel">
        <div style={{ marginBottom: "12px" }}>{newWorkflowButton}</div>
        <CatalogStatus testId="developer-wf-empty">{DEV_MSG.WF_EMPTY}</CatalogStatus>
      </div>
    );
  }

  return (
    <div data-testid="developer-wf-panel">
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
        <CatalogHint>{DEV_MSG.WF_HINT}</CatalogHint>
        {newWorkflowButton}
      </div>
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
              <span key="open" style={{ display: "inline-flex", gap: "8px", alignItems: "center" }}>
                <button
                  type="button"
                  data-testid="developer-wf-open"
                  data-wf-name={openKey}
                  aria-label={`Open ${openKey}`}
                  onClick={(ev) => {
                    ev.stopPropagation();
                    setSelected(openKey);
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
                    setSelected(`copy:${openKey}`);
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
        })}
      />
    </div>
  );
}
