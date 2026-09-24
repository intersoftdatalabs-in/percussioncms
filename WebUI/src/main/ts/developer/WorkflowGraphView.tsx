/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isApiError } from "../api/client";
import {
  deleteWorkflowTransition,
  getWorkflowGraph,
} from "../api/developer/workflowsApi";
import type { WorkflowGraph, WorkflowGraphEdge } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { DEV_MSG } from "./messages";

/**
 * State/transition graph for one workflow (slice 32 read, slice 33 delete).
 * Packaged workflows stay read-only. Delete removes one edge and keeps steps.
 */
export function WorkflowGraphView({
  workflowName,
}: {
  workflowName: string;
}): React.ReactElement {
  const [graph, setGraph] = useState<WorkflowGraph | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [pending, setPending] = useState<WorkflowGraphEdge | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setGraph(null);
    setError(null);
    getWorkflowGraph(workflowName)
      .then((g) => {
        if (!cancelled) {
          setGraph(g);
        }
      })
      .catch((err: unknown) => {
        if (cancelled) {
          return;
        }
        if (isApiError(err) && err.status === 403) {
          setError(DEV_MSG.WF_FORBIDDEN);
          return;
        }
        if (isApiError(err) && err.status === 404) {
          setError(DEV_MSG.WF_NOT_FOUND);
          return;
        }
        setError(DEV_MSG.WF_GRAPH_ERROR);
      });
    return () => {
      cancelled = true;
    };
  }, [workflowName, reloadToken]);

  const onConfirmDelete = useCallback(async () => {
    if (!pending?.from || !pending.label) {
      setPending(null);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const next = await deleteWorkflowTransition(
        workflowName,
        pending.from,
        pending.label,
        pending.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_DELETED);
      setPending(null);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_DELETE_FORBIDDEN);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_DELETE_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_DELETE_ERROR);
      }
      setPending(null);
    } finally {
      setBusy(false);
    }
  }, [pending, workflowName]);

  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];
  const packaged = graph?.packaged === true;

  return (
    <section data-testid="developer-wf-graph" style={{ marginBottom: "16px" }}>
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_GRAPH}</h3>
      <p style={{ color: catalogColors.empty, marginTop: 0 }}>{DEV_MSG.WF_GRAPH_HINT}</p>
      {error ? (
        <div role="alert" data-testid="developer-wf-graph-error">
          {error}
        </div>
      ) : null}
      {notice ? (
        <p data-testid="developer-wf-graph-notice" style={{ color: catalogColors.accent }}>
          {notice}
        </p>
      ) : null}
      {!graph && !error ? <p data-testid="developer-wf-graph-loading">{DEV_MSG.WF_GRAPH_LOADING}</p> : null}
      {graph ? (
        <p data-testid="developer-wf-graph-kind">
          {packaged ? DEV_MSG.WF_GRAPH_PACKAGED : DEV_MSG.WF_GRAPH_CUSTOM}
        </p>
      ) : null}
      {graph && nodes.length === 0 ? (
        <p data-testid="developer-wf-graph-empty">{DEV_MSG.WF_GRAPH_EMPTY}</p>
      ) : null}
      {nodes.length > 0 ? (
        <div
          data-testid="developer-wf-graph-nodes"
          style={{ display: "flex", flexWrap: "wrap", gap: "8px", marginBottom: "8px" }}
        >
          {nodes.map((node, i) => (
            <span
              key={`${node.name ?? "n"}-${i}`}
              data-testid={`developer-wf-graph-node-${i}`}
              style={{
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "6px 10px",
                fontFamily: "monospace",
              }}
            >
              {node.name || "—"}
            </span>
          ))}
        </div>
      ) : null}
      {edges.length > 0 ? (
        <ul data-testid="developer-wf-graph-edges" style={{ margin: 0, paddingLeft: "1.2rem" }}>
          {edges.map((edge, i) => (
            <li key={`${edge.from}-${edge.label}-${edge.to}-${i}`} data-testid={`developer-wf-graph-edge-${i}`}>
              {edge.from || "—"} — {edge.label || "—"} → {edge.to || "—"}
              {!packaged && edge.from && edge.label ? (
                <button
                  type="button"
                  data-testid={`developer-wf-graph-delete-${i}`}
                  style={{ marginLeft: 8 }}
                  onClick={() => {
                    setNotice(null);
                    setPending(edge);
                  }}
                >
                  {DEV_MSG.WF_GRAPH_DELETE}
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      <CatalogConfirmDialog
        open={pending != null}
        busy={busy}
        message={
          pending
            ? `${DEV_MSG.WF_GRAPH_DELETE_CONFIRM} ${pending.from} — ${pending.label} → ${pending.to}`
            : DEV_MSG.WF_GRAPH_DELETE_CONFIRM
        }
        onCancel={() => {
          if (!busy) {
            setPending(null);
          }
        }}
        onConfirm={() => {
          void onConfirmDelete();
        }}
      />
    </section>
  );
}
