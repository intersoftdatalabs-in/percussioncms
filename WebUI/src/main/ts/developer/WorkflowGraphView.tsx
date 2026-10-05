/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import React, { useCallback, useEffect, useState } from "react";
import { isApiError } from "../api/client";
import {
  createWorkflowAgingTransition,
  createWorkflowTransition,
  deleteWorkflowAgingTransition,
  deleteWorkflowStep,
  deleteWorkflowTransition,
  getWorkflowGraph,
  isNonNegativeApprovalCount,
  isPositiveMinuteInterval,
  isValidWorkflowName,
  updateTransitionApprovalsRequired,
  updateTransitionCommentRequired,
  updateWorkflowAgingInterval,
  updateWorkflowTransition,
} from "../api/developer/workflowsApi";
import type { WorkflowGraph, WorkflowGraphEdge } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { DEV_MSG } from "./messages";

/**
 * State/transition graph for one workflow (slice 32 read, slice 31 transition write,
 * slice 33 transition delete, slice 34 step delete, slice 57 absolute aging create,
 * slice 58 absolute aging interval change, slice 59 absolute aging delete,
 * slice 70 approvals required).
 * Packaged workflows stay read-only. Aging edges are not comment-required
 * and do not take an approval count.
 * Deleting an aging transition does not remove a regular transition.
 */

type TransitionIdentity = { from: string; label: string; to: string };
type ApprovalsIdentity = TransitionIdentity & { current: number };
type AgingIntervalIdentity = { from: string; to: string; intervalMinutes: number };
export function WorkflowGraphView({
  workflowName,
}: {
  workflowName: string;
}): React.ReactElement {
  const [graph, setGraph] = useState<WorkflowGraph | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [pending, setPending] = useState<WorkflowGraphEdge | null>(null);
  const [pendingAging, setPendingAging] = useState<AgingIntervalIdentity | null>(null);
  const [pendingStep, setPendingStep] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [fromStep, setFromStep] = useState("");
  const [toStep, setToStep] = useState("");
  const [labelText, setLabelText] = useState("");
  const [editing, setEditing] = useState<TransitionIdentity | null>(null);
  const [agingFrom, setAgingFrom] = useState("");
  const [agingTo, setAgingTo] = useState("");
  const [agingMinutes, setAgingMinutes] = useState("");
  const [agingEdit, setAgingEdit] = useState<AgingIntervalIdentity | null>(null);
  const [agingNewMinutes, setAgingNewMinutes] = useState("");
  const [approvalsEdit, setApprovalsEdit] = useState<ApprovalsIdentity | null>(null);
  const [approvalsDraft, setApprovalsDraft] = useState("");

  useEffect(() => {
    let cancelled = false;
    setGraph(null);
    setError(null);
    setEditing(null);
    setLabelText("");
    setFromStep("");
    setToStep("");
    setAgingFrom("");
    setAgingTo("");
    setAgingMinutes("");
    setAgingEdit(null);
    setAgingNewMinutes("");
    setPendingAging(null);
    setApprovalsEdit(null);
    setApprovalsDraft("");
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
    setAgingFrom((prev) => (prev && names.includes(prev) ? prev : (names[0] ?? "")));
    setAgingTo((prev) => (prev && names.includes(prev) ? prev : ""));
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

  const onCancelAging = useCallback(() => {
    setAgingTo("");
    setAgingMinutes("");
    setError(null);
  }, []);

  const onAddAging = useCallback(async () => {
    const names = (graph?.nodes ?? [])
      .map((node) => node.name)
      .filter((name): name is string => !!name && name.trim().length > 0);
    const from = agingFrom || names[0] || "";
    const to = agingTo.trim();
    if (!isValidWorkflowName(from) || !isValidWorkflowName(to) || !isPositiveMinuteInterval(agingMinutes)) {
      setError(DEV_MSG.WF_AGING_INVALID);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await createWorkflowAgingTransition(workflowName, {
        from: from.trim(),
        to,
        intervalMinutes: Number(agingMinutes.trim()),
      });
      setGraph(next);
      setNotice(DEV_MSG.WF_AGING_SAVED);
      setAgingMinutes("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_AGING_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_AGING_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_AGING_BAD);
      } else {
        setError(DEV_MSG.WF_AGING_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [agingFrom, agingMinutes, agingTo, graph, workflowName]);

  const onCancelAgingInterval = useCallback(() => {
    setAgingEdit(null);
    setAgingNewMinutes("");
    setError(null);
  }, []);

  const onSaveAgingInterval = useCallback(async () => {
    if (!agingEdit) {
      return;
    }
    const nextMinutes = agingNewMinutes.trim();
    if (
      !isPositiveMinuteInterval(nextMinutes) ||
      Number(nextMinutes) === agingEdit.intervalMinutes
    ) {
      setError(DEV_MSG.WF_AGING_INTERVAL_INVALID);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await updateWorkflowAgingInterval(workflowName, {
        from: agingEdit.from,
        to: agingEdit.to,
        intervalMinutes: agingEdit.intervalMinutes,
        newIntervalMinutes: Number(nextMinutes),
      });
      setGraph(next);
      setNotice(DEV_MSG.WF_AGING_INTERVAL_SAVED);
      setAgingEdit(null);
      setAgingNewMinutes("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_AGING_INTERVAL_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_AGING_INTERVAL_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_AGING_INTERVAL_BAD);
      } else {
        setError(DEV_MSG.WF_AGING_INTERVAL_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [agingEdit, agingNewMinutes, workflowName]);

  const onConfirmDeleteAging = useCallback(async () => {
    if (!pendingAging) {
      setPendingAging(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await deleteWorkflowAgingTransition(
        workflowName,
        pendingAging.from,
        pendingAging.to,
        pendingAging.intervalMinutes,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_AGING_DELETED);
      setPendingAging(null);
      setAgingEdit(null);
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_AGING_DELETE_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_AGING_DELETE_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_AGING_DELETE_BAD);
      } else {
        setError(DEV_MSG.WF_AGING_DELETE_ERROR);
      }
      setPendingAging(null);
    } finally {
      setBusy(false);
    }
  }, [pendingAging, workflowName]);

  const onCancelApprovals = useCallback(() => {
    setApprovalsEdit(null);
    setApprovalsDraft("");
    setError(null);
  }, []);

  const onSaveApprovals = useCallback(async () => {
    if (!approvalsEdit) {
      return;
    }
    const nextCount = approvalsDraft.trim();
    if (
      !isNonNegativeApprovalCount(nextCount) ||
      Number(nextCount) === approvalsEdit.current
    ) {
      setError(DEV_MSG.WF_GRAPH_APPROVALS_INVALID);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await updateTransitionApprovalsRequired(
        workflowName,
        approvalsEdit.from,
        approvalsEdit.label,
        Number(nextCount),
        approvalsEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_APPROVALS_SAVED);
      setApprovalsEdit(null);
      setApprovalsDraft("");
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_APPROVALS_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_APPROVALS_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_APPROVALS_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_APPROVALS_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [approvalsDraft, approvalsEdit, workflowName]);

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
  const edges = (graph?.edges ?? []).filter((edge) => edge.aging !== true);
  const agingEdges = (graph?.edges ?? []).filter((edge) => edge.aging === true);
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
      {canWrite ? (
        <form
          data-testid="developer-wf-aging-form"
          style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginBottom: "8px" }}
          onSubmit={(ev) => {
            ev.preventDefault();
            void onAddAging();
          }}
        >
          <label>
            {DEV_MSG.WF_AGING_FROM}
            <select
              data-testid="developer-wf-aging-from"
              value={agingFrom || stepNames[0] || ""}
              disabled={busy}
              onChange={(ev) => setAgingFrom(ev.target.value)}
            >
              {stepNames.map((name) => (
                <option key={`aging-from-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_AGING_TO}
            <select
              data-testid="developer-wf-aging-to"
              value={agingTo}
              disabled={busy}
              onChange={(ev) => setAgingTo(ev.target.value)}
            >
              <option value="">{DEV_MSG.WF_AGING_TO_BLANK}</option>
              {stepNames.map((name) => (
                <option key={`aging-to-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_AGING_MINUTES}
            <input
              data-testid="developer-wf-aging-minutes"
              inputMode="numeric"
              value={agingMinutes}
              disabled={busy}
              onChange={(ev) => setAgingMinutes(ev.target.value)}
            />
          </label>
          <button type="submit" data-testid="developer-wf-aging-add" disabled={busy}>
            {DEV_MSG.WF_AGING_ADD}
          </button>
          <button
            type="button"
            data-testid="developer-wf-aging-cancel"
            disabled={busy}
            onClick={() => {
              onCancelAging();
            }}
          >
            {DEV_MSG.WF_AGING_CANCEL}
          </button>
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
                    setPendingAging(null);
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
              {typeof edge.approvalsRequired === "number" ? (
                <span
                  data-testid={`developer-wf-graph-approvals-${i}`}
                  data-approvals={edge.approvalsRequired}
                  style={{ marginLeft: 8 }}
                >
                  {DEV_MSG.WF_GRAPH_APPROVALS}: {edge.approvalsRequired}
                </span>
              ) : null}
              {!packaged &&
              edge.from &&
              edge.label &&
              typeof edge.approvalsRequired === "number" ? (
                approvalsEdit &&
                approvalsEdit.from === edge.from &&
                approvalsEdit.label === edge.label &&
                approvalsEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <label>
                      {DEV_MSG.WF_GRAPH_APPROVALS}{" "}
                      <input
                        data-testid="developer-wf-approvals-value"
                        inputMode="numeric"
                        value={approvalsDraft}
                        disabled={busy}
                        onChange={(ev) => setApprovalsDraft(ev.target.value)}
                      />
                    </label>
                    <button
                      type="button"
                      data-testid="developer-wf-approvals-save"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        void onSaveApprovals();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_APPROVALS_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-approvals-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelApprovals();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_APPROVALS_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-approvals-edit-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setApprovalsEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                        current: edge.approvalsRequired as number,
                      });
                      setApprovalsDraft(String(edge.approvalsRequired));
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_APPROVALS_SET}
                  </button>
                )
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
                    setPendingAging(null);
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
                    setPendingAging(null);
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
      {agingEdges.length > 0 ? (
        <div data-testid="developer-wf-aging">
          <h4 style={{ fontSize: "0.95rem", marginBottom: "4px" }}>{DEV_MSG.WF_AGING_TITLE}</h4>
          <ul data-testid="developer-wf-aging-edges" style={{ margin: 0, paddingLeft: "1.2rem" }}>
            {agingEdges.map((edge, i) => (
              <li
                key={`aging-${edge.from}-${edge.label}-${edge.to}-${i}`}
                data-testid={`developer-wf-aging-edge-${i}`}
                data-from={edge.from || ""}
                data-to={edge.to || ""}
                data-interval={edge.intervalMinutes ?? ""}
              >
                {edge.from || "—"} — {edge.label || "—"} → {edge.to || "—"}
                {typeof edge.intervalMinutes === "number" ? ` (${edge.intervalMinutes} minutes)` : ""}
                {canWrite &&
                edge.from &&
                edge.to &&
                typeof edge.intervalMinutes === "number" ? (
                  <button
                    type="button"
                    data-testid={`developer-wf-aging-change-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setPending(null);
                      setPendingAging(null);
                      setPendingStep(null);
                      setAgingEdit({
                        from: edge.from as string,
                        to: edge.to as string,
                        intervalMinutes: edge.intervalMinutes as number,
                      });
                      setAgingNewMinutes("");
                    }}
                  >
                    {DEV_MSG.WF_AGING_CHANGE}
                  </button>
                ) : null}
                {canWrite &&
                edge.from &&
                edge.to &&
                typeof edge.intervalMinutes === "number" &&
                edge.intervalMinutes > 0 ? (
                  <button
                    type="button"
                    data-testid={`developer-wf-aging-delete-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setPending(null);
                      setPendingStep(null);
                      setAgingEdit(null);
                      setPendingAging({
                        from: edge.from as string,
                        to: edge.to as string,
                        intervalMinutes: edge.intervalMinutes as number,
                      });
                    }}
                  >
                    {DEV_MSG.WF_AGING_DELETE}
                  </button>
                ) : null}
                {agingEdit &&
                agingEdit.from === edge.from &&
                agingEdit.to === edge.to &&
                agingEdit.intervalMinutes === edge.intervalMinutes ? (
                  <form
                    data-testid="developer-wf-aging-interval-form"
                    style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginTop: 8 }}
                    onSubmit={(ev) => {
                      ev.preventDefault();
                      void onSaveAgingInterval();
                    }}
                  >
                    <label>
                      {DEV_MSG.WF_AGING_NEW_MINUTES}
                      <input
                        data-testid="developer-wf-aging-new-minutes"
                        inputMode="numeric"
                        value={agingNewMinutes}
                        disabled={busy}
                        onChange={(ev) => setAgingNewMinutes(ev.target.value)}
                      />
                    </label>
                    <button type="submit" data-testid="developer-wf-aging-interval-save" disabled={busy}>
                      {DEV_MSG.WF_AGING_INTERVAL_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-aging-interval-cancel"
                      disabled={busy}
                      onClick={() => {
                        onCancelAgingInterval();
                      }}
                    >
                      {DEV_MSG.WF_AGING_CANCEL}
                    </button>
                  </form>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
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
        open={pendingAging != null}
        busy={busy}
        message={
          pendingAging
            ? `${DEV_MSG.WF_AGING_DELETE_CONFIRM} ${pendingAging.from} → ${pendingAging.to} (${pendingAging.intervalMinutes} minutes)`
            : DEV_MSG.WF_AGING_DELETE_CONFIRM
        }
        onCancel={() => {
          if (!busy) {
            setPendingAging(null);
          }
        }}
        onConfirm={() => {
          void onConfirmDeleteAging();
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
