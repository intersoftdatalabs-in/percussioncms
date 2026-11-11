/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useMemo, useState } from "react";
import { listWorkflows } from "../api/developer/workflowsApi";
import type { WorkflowDef } from "../api/developer/types";
import type { WorkflowCreateResult } from "../api/developer/workflowsApi";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { CatalogSearchFilter } from "./CatalogSearchFilter";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { WorkflowCopyPanel } from "./WorkflowCopyPanel";
import { WorkflowCreatePanel } from "./WorkflowCreatePanel";
import { WorkflowDetailPanel } from "./WorkflowDetailPanel";
import { WorkflowCatalogTable } from "./WorkflowCatalogTable";
import {
  COPY_SELECTION_PREFIX,
  filterWorkflowCatalog,
  markDefaultWorkflowInCatalog,
  parseWorkflowSelection,
  renameWorkflowInCatalog,
  sortWorkflowCatalog,
} from "./workflowCatalog";

/** Load the catalog, mapping failures to panel error (never to empty). */
function fetchWorkflows(
  setItems: (items: WorkflowDef[] | null) => void,
  setError: (error: string | null) => void,
  isCancelled?: () => boolean,
): void {
  listWorkflows()
    .then((list) => {
      if (isCancelled?.()) return;
      setItems(list);
    })
    .catch((e: unknown) => {
      if (isCancelled?.()) return;
      setError(panelErrMsg(e, DEV_MSG.WF_ERROR));
      // Leave items null so error ≠ empty catalog (and retry can show loading).
    });
}

function NewWorkflowButton({ onNew }: { onNew: () => void }): React.ReactElement {
  return (
    <button
      type="button"
      data-testid="developer-wf-new"
      onClick={onNew}
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
}

/**
 * P0.17 — workflow catalog browse (SY-04) via existing workflowmanagement API,
 * plus slice 21 catalog create (POST /services/workflows) and slice 36 copy.
 */
export function WorkflowsPanel(): React.ReactElement {
  const [items, setItems] = useState<WorkflowDef[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** Client-side catalog filter (#5995). Never triggers a fetch; never mutates. */
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<string | "new" | null>(null);

  const reload = (): void => {
    setError(null);
    fetchWorkflows(setItems, setError);
  };

  useEffect(() => {
    let cancelled = false;
    fetchWorkflows(setItems, setError, () => cancelled);
    return () => {
      cancelled = true;
    };
  }, []);

  const handleCreated = (created: WorkflowCreateResult): void => {
    const name = (created.workflowName || "").trim();
    reload();
    setSelected(name ? name : null);
  };

  const handleDeleted = (): void => {
    setSelected(null);
    reload();
  };

  const sorted = useMemo(() => sortWorkflowCatalog(items), [items]);
  const visible = useMemo(() => filterWorkflowCatalog(sorted, query), [sorted, query]);

  const openSelected = (name: string): void => setSelected(name);
  const copySelected = (name: string): void => setSelected(`${COPY_SELECTION_PREFIX}${name}`);

  const selection = parseWorkflowSelection(selected);

  if (selection.kind === "create") {
    return <WorkflowCreatePanel onBack={() => setSelected(null)} onCreated={handleCreated} />;
  }

  if (selection.kind === "copy") {
    return (
      <WorkflowCopyPanel
        sourceName={selection.sourceName}
        onBack={() => setSelected(null)}
        onCopied={handleCreated}
      />
    );
  }

  if (selection.kind === "detail") {
    const name = selection.name;
    return (
      <WorkflowDetailPanel
        name={name}
        onBack={() => setSelected(null)}
        onDeleted={handleDeleted}
        onRenamed={(previousName, newName) => {
          const next = newName.trim();
          if (!next) {
            return;
          }
          setItems((itemsPrev) => renameWorkflowInCatalog(itemsPrev, previousName, next));
          setSelected(next);
          reload();
        }}
        onDefaultChanged={(workflowName) => {
          setItems((prev) => markDefaultWorkflowInCatalog(prev, workflowName));
        }}
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

  const newButton = <NewWorkflowButton onNew={() => setSelected("new")} />;

  if (items.length === 0) {
    return (
      <div data-testid="developer-wf-panel">
        <div style={{ marginBottom: "12px" }}>{newButton}</div>
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
        {newButton}
      </div>
      <CatalogSearchFilter
        testId="developer-wf-search"
        query={query}
        onQueryChange={setQuery}
      />
      {visible.length === 0 ? (
        <CatalogStatus testId="developer-wf-search-empty">
          {DEV_MSG.WF_SEARCH_EMPTY}
        </CatalogStatus>
      ) : (
        <WorkflowCatalogTable workflows={visible} onOpen={openSelected} onCopy={copySelected} />
      )}
    </div>
  );
}
