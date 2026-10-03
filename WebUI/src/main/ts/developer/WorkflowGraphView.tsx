/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isApiError } from "../api/client";
import {
  createWorkflowTransition,
  deleteWorkflowStep,
  deleteWorkflowTransition,
  getWorkflowGraph,
  isValidWorkflowName,
  updateTransitionCommentRequired,
  updateWorkflowTransition,
} from "../api/developer/workflowsApi";
import type { WorkflowGraph, WorkflowGraphEdge } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { DEV_MSG } from "./messages";

/**
 * State/transition graph for one workflow (slice 32 read, slice 31 transition write,
 * slice 33 transition delete, slice 34 step delete). Packaged workflows stay read-only.
 */

type TransitionIdentity = { from: string; label: string; to: string };
export function WorkflowGraphView({
  workflowName,
}: {
  workflowName: string;
}): React.ReactElement {
  const [graph, setGraph] = useState<WorkflowGraph | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [pending, setPending] = useState<WorkflowGraphEdge | null>(null);
  const [pendingStep, setPendingStep] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [fromStep, setFromStep] = useState("");
  const [toStep, setToStep] = useState("");
  const [labelText, setLabelText] = useState("");
  const [editing, setEditing] = useState<TransitionIdentity | null>(null);

  useEffect(() => {
    let cancelled = false;
    setGraph(null);
    setError(null);
    setEditing(null);
    setLabelText("");
    setFromStep("");
    setToStep("");
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

  useEffect(() => {
    if (!graph || editing) {
      return;
    }
    const names = (graph.nodes ?? [])
      .map((node) => node.name)
      .filter((name): name is string => !!name && name.trim().length > 0);
    setFromStep((prev) => (prev && names.includes(prev) ? prev : (names[0] ?? "")));
    setToStep((prev) => (prev && names.includes(prev) ? prev : (names[1] ?? names[0] ?? "")));
  }, [graph, editing]);

  const onSaveTransition = useCallback(async () => {
    const names = (graph?.nodes ?? [])
      .map((node) => node.name)
      .filter((name): name is string => !!name && name.trim().length > 0);
    const from = editing ? editing.from : fromStep || names[0] || "";
    const to = toStep || (editing ? editing.to : names[1] || names[0] || "");
    if (!isValidWorkflowName(from) || !isValidWorkflowName(to) || !isValidWorkflowName(labelText)) {
      setError(DEV_MSG.WF_GRAPH_WRITE_INVALID);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = editing
        ? await updateWorkflowTransition(workflowName, editing.from, editing.label, editing.to, {
            to: to.trim(),
            label: labelText.trim(),
          })
        : await createWorkflowTransition(workflowName, {
            from: from.trim(),
            to: to.trim(),
            label: labelText.trim(),
          });
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_WRITE_SAVED);
      setEditing(null);
      setLabelText("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_WRITE_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_WRITE_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_WRITE_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_WRITE_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [editing, fromStep, graph, labelText, toStep, workflowName]);

  const onToggleComment = useCallback(
    async (edge: WorkflowGraphEdge, commentRequired: boolean) => {
      if (!edge.from || !edge.label) {
        return;
      }
      setBusy(true);
      setError(null);
      setNotice(null);
      try {
        const next = await updateTransitionCommentRequired(
          workflowName,
          edge.from,
          edge.label,
          commentRequired,
          edge.to,
        );
        setGraph(next);
        setNotice(DEV_MSG.WF_GRAPH_COMMENT_SAVED);
      } catch (err: unknown) {
        if (isApiError(err) && err.status === 403) {
          setError(DEV_MSG.WF_GRAPH_DELETE_FORBIDDEN);
        } else {
          setError(DEV_MSG.WF_GRAPH_COMMENT_ERROR);
        }
      } finally {
        setBusy(false);
      }
    },
    [workflowName],
  );

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

  const onConfirmDeleteStep = useCallback(async () => {
    if (!pendingStep) {
      setPendingStep(null);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const next = await deleteWorkflowStep(workflowName, pendingStep);
      setGraph(next);
      setNotice(DEV_MSG.WF_STEP_DELETED);
      setPendingStep(null);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_STEP_DELETE_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_STEP_DELETE_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_STEP_DELETE_BAD);
      } else {
        setError(DEV_MSG.WF_STEP_DELETE_ERROR);
      }
      setPendingStep(null);
    } finally {
      setBusy(false);
    }
  }, [pendingStep, workflowName]);

  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];
  const packaged = graph?.packaged === true;
  const stepNames = nodes
    .map((node) => node.name)
    .filter((name): name is string => !!name && name.trim().length > 0);
  const canWrite = !!graph && !packaged && stepNames.length > 0;
  const selectedFrom = editing ? editing.from : fromStep || stepNames[0] || "";
  const selectedTo = toStep || (editing ? editing.to : stepNames[1] || stepNames[0] || "");

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
      {canWrite ? (
        <form
          data-testid="developer-wf-transition-form"
          style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginBottom: "8px" }}
          onSubmit={(ev) => {
            ev.preventDefault();
            void onSaveTransition();
          }}
        >
          <label>
            {DEV_MSG.WF_GRAPH_WRITE_FROM}
            <select
              data-testid="developer-wf-transition-from"
              value={selectedFrom}
              disabled={busy || editing != null}
              onChange={(ev) => setFromStep(ev.target.value)}
            >
              {stepNames.map((name) => (
                <option key={`from-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_GRAPH_WRITE_LABEL}
            <input
              data-testid="developer-wf-transition-label"
              value={labelText}
              disabled={busy}
              onChange={(ev) => setLabelText(ev.target.value)}
            />
          </label>
          <label>
            {DEV_MSG.WF_GRAPH_WRITE_TO}
            <select
              data-testid="developer-wf-transition-to"
              value={selectedTo}
              disabled={busy}
              onChange={(ev) => setToStep(ev.target.value)}
            >
              {stepNames.map((name) => (
                <option key={`to-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <button
            type="submit"
            data-testid={editing ? "developer-wf-transition-save" : "developer-wf-transition-add"}
            disabled={busy}
          >
            {editing ? DEV_MSG.WF_GRAPH_WRITE_SAVE : DEV_MSG.WF_GRAPH_WRITE_ADD}
          </button>
          {editing ? (
            <button
              type="button"
              data-testid="developer-wf-transition-cancel"
              disabled={busy}
              onClick={() => {
                setEditing(null);
                setLabelText("");
                setError(null);
              }}
            >
              {DEV_MSG.WF_GRAPH_WRITE_CANCEL}
            </button>
          ) : null}
        </form>
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
              {!packaged && node.name ? (
                <button
                  type="button"
                  data-testid={`developer-wf-graph-delete-step-${i}`}
                  style={{ marginLeft: 8 }}
                  onClick={() => {
                    setNotice(null);
                    setPending(null);
                    setPendingStep(node.name as string);
                  }}
                >
                  {DEV_MSG.WF_STEP_DELETE}
                </button>
              ) : null}
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
                <label style={{ marginLeft: 8 }}>
                  <input
                    type="checkbox"
                    data-testid={`developer-wf-graph-comment-${i}`}
                    checked={edge.commentRequired === true}
                    disabled={busy}
                    onChange={(ev) => {
                      void onToggleComment(edge, ev.target.checked);
                    }}
                  />{" "}
                  {DEV_MSG.WF_GRAPH_COMMENT}
                </label>
              ) : null}
              {!packaged && edge.from && edge.label && edge.to ? (
                <button
                  type="button"
                  data-testid={`developer-wf-graph-edit-${i}`}
                  style={{ marginLeft: 8 }}
                  onClick={() => {
                    setNotice(null);
                    setError(null);
                    setPending(null);
                    setPendingStep(null);
                    setEditing({ from: edge.from as string, label: edge.label as string, to: edge.to as string });
                    setFromStep(edge.from as string);
                    setToStep(edge.to as string);
                    setLabelText(edge.label as string);
                  }}
                >
                  {DEV_MSG.WF_GRAPH_WRITE_EDIT}
                </button>
              ) : null}
              {!packaged && edge.from && edge.label ? (
                <button
                  type="button"
                  data-testid={`developer-wf-graph-delete-${i}`}
                  style={{ marginLeft: 8 }}
                  onClick={() => {
                    setNotice(null);
                    setPendingStep(null);
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
      <CatalogConfirmDialog
        open={pendingStep != null}
        busy={busy}
        message={
          pendingStep
            ? `${DEV_MSG.WF_STEP_DELETE_CONFIRM} ${pendingStep}`
            : DEV_MSG.WF_STEP_DELETE_CONFIRM
        }
        onCancel={() => {
          if (!busy) {
            setPendingStep(null);
          }
        }}
        onConfirm={() => {
          void onConfirmDeleteStep();
        }}
      />
    </section>
  );
}
