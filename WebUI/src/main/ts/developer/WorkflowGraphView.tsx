/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useEffect, useState } from "react";
import { isApiError } from "../api/client";
import { getWorkflowGraph } from "../api/developer/workflowsApi";
import type { WorkflowGraph } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { DEV_MSG } from "./messages";

/**
 * Read-only state/transition graph for one workflow (slice 32).
 * Packaged vs custom is a badge; edges are not editable here.
 */
export function WorkflowGraphView({
  workflowName,
}: {
  workflowName: string;
}): React.ReactElement {
  const [graph, setGraph] = useState<WorkflowGraph | null>(null);
  const [error, setError] = useState<string | null>(null);

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
  }, [workflowName]);

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
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
