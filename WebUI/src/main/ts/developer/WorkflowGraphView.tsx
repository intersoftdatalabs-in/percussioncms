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
  isWorkflowAgingSystemField,
  WORKFLOW_AGING_SYSTEM_FIELDS,
  addTransitionAllowedRole,
  clearTransitionAllowedRoles,
  markTransitionAsDefault,
  removeTransitionAllowedRole,
  restrictTransitionToOneRole,
  updateTransitionApprovalsRequired,
  updateTransitionCommentRequired,
  updateWorkflowAgingInterval,
  updateWorkflowAgingSystemField,
  updateWorkflowTransition,
} from "../api/developer/workflowsApi";
import type { WorkflowGraph, WorkflowGraphEdge } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { DEV_MSG } from "./messages";

/**
 * State/transition graph for one workflow (slice 32 read, slice 31 transition write,
 * slice 33 transition delete, slice 34 step delete, slice 57 absolute aging create,
 * slice 75 repeated aging create, slice 76 system-field aging create,
 * slice 58 absolute aging interval change, slice 78 repeated aging interval change,
 * slice 79 system-field date column change,
 * slice 59 absolute aging delete,
 * slice 77 repeated and system-field aging delete,
 * slice 70 approvals required, slice 71 default transition,
 * slice 72 restrict one transition to a single role,
 * slice 73 add one more role to an already-restricted transition,
 * slice 74 clear that list so every role may fire the transition again,
 * slice 80 remove one role from a transition that already lists two or more).
 * Packaged workflows stay read-only. Aging edges are not comment-required,
 * do not take an approval count, cannot be the default, and are not role-restricted.
 * Adding a role does not clear the restriction. Removing one role leaves the rest and
 * stays restricted. Clearing shows allow-all only after success.
 * Deleting an aging transition does not remove a regular transition.
 * A repeated aging row appears only after the server accepts it, and it can be
 * deleted without removing an absolute row that uses the same interval.
 * A system-field aging row appears only after the server accepts it, and it can
 * be deleted without removing absolute or repeated rows. A repeated interval
 * can be changed without moving an absolute edge that uses the same from, to,
 * and old interval. System-field rows do not offer an interval change. The
 * date column on one system-field row can be changed without moving absolute
 * or repeated rows, and the new column shows only after the server accepts it.
 */

type AgingDeleteIdentity = {
  from: string;
  to: string;
  intervalMinutes?: number;
  agingType?: string;
  systemField?: string;
};

function canDeleteAgingEdge(edge: WorkflowGraphEdge): boolean {
  if (!edge.from || !edge.to) {
    return false;
  }
  if (edge.agingType === "SYSTEM_FIELD") {
    return typeof edge.systemField === "string" && edge.systemField.trim().length > 0;
  }
  return typeof edge.intervalMinutes === "number" && edge.intervalMinutes > 0;
}

function agingDeleteConfirm(pending: AgingDeleteIdentity): string {
  if (pending.agingType === "SYSTEM_FIELD") {
    return `${DEV_MSG.WF_AGING_DELETE_CONFIRM} ${pending.from} → ${pending.to} (${pending.systemField} ${DEV_MSG.WF_SYSTEM_FIELD_KIND})`;
  }
  if (pending.agingType === "REPEATED") {
    return `${DEV_MSG.WF_AGING_DELETE_CONFIRM} ${pending.from} → ${pending.to} (${pending.intervalMinutes} minutes, ${DEV_MSG.WF_REPEATED_KIND})`;
  }
  return `${DEV_MSG.WF_AGING_DELETE_CONFIRM} ${pending.from} → ${pending.to} (${pending.intervalMinutes} minutes)`;
}

type TransitionIdentity = { from: string; label: string; to: string };

type AgingIntervalKind = "ABSOLUTE" | "REPEATED";

function agingIntervalKind(agingType: string | undefined): AgingIntervalKind | "SYSTEM_FIELD" {
  if (agingType === "REPEATED") {
    return "REPEATED";
  }
  if (agingType === "SYSTEM_FIELD") {
    return "SYSTEM_FIELD";
  }
  return "ABSOLUTE";
}

function canChangeAgingInterval(edge: WorkflowGraphEdge): boolean {
  const kind = agingIntervalKind(edge.agingType);
  return (
    (kind === "ABSOLUTE" || kind === "REPEATED") &&
    !!edge.from &&
    !!edge.to &&
    typeof edge.intervalMinutes === "number" &&
    edge.intervalMinutes > 0
  );
}

/** One-element role lists arrive as a string on the live JSON wire (#5233). */
function roleNameList(raw: unknown): string[] {
  if (typeof raw === "string") {
    const name = raw.trim();
    return name ? [name] : [];
  }
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.filter((name): name is string => typeof name === "string" && name.trim().length > 0);
}

/** Workflow roles that are not already allowed to fire this restricted transition. */
function rolesNotYetAllowed(all: unknown, allowed: unknown): string[] {
  const taken = new Set(roleNameList(allowed).map((name) => name.toLowerCase()));
  return roleNameList(all).filter((name) => !taken.has(name.toLowerCase()));
}
type ApprovalsIdentity = TransitionIdentity & { current: number };
type AgingIntervalIdentity = {
  from: string;
  to: string;
  intervalMinutes: number;
  agingType: AgingIntervalKind;
};

function sameAgingIntervalEdit(edit: AgingIntervalIdentity, edge: WorkflowGraphEdge): boolean {
  return (
    canChangeAgingInterval(edge) &&
    edit.agingType === agingIntervalKind(edge.agingType) &&
    edit.from === edge.from &&
    edit.to === edge.to &&
    edit.intervalMinutes === edge.intervalMinutes
  );
}

type AgingFieldIdentity = {
  from: string;
  to: string;
  systemField: string;
};

function canChangeSystemField(edge: WorkflowGraphEdge): boolean {
  return (
    edge.agingType === "SYSTEM_FIELD" &&
    !!edge.from &&
    !!edge.to &&
    isWorkflowAgingSystemField(edge.systemField)
  );
}

function sameAgingFieldEdit(edit: AgingFieldIdentity, edge: WorkflowGraphEdge): boolean {
  return (
    canChangeSystemField(edge) &&
    edit.from === edge.from &&
    edit.to === edge.to &&
    edit.systemField.trim().toUpperCase() === (edge.systemField || "").trim().toUpperCase()
  );
}
export function WorkflowGraphView({
  workflowName,
}: {
  workflowName: string;
}): React.ReactElement {
  const [graph, setGraph] = useState<WorkflowGraph | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [pending, setPending] = useState<WorkflowGraphEdge | null>(null);
  const [pendingAging, setPendingAging] = useState<AgingDeleteIdentity | null>(null);
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
  const [repeatedFrom, setRepeatedFrom] = useState("");
  const [repeatedTo, setRepeatedTo] = useState("");
  const [repeatedMinutes, setRepeatedMinutes] = useState("");
  const [systemFrom, setSystemFrom] = useState("");
  const [systemTo, setSystemTo] = useState("");
  const [systemField, setSystemField] = useState("");
  const [agingEdit, setAgingEdit] = useState<AgingIntervalIdentity | null>(null);
  const [agingNewMinutes, setAgingNewMinutes] = useState("");
  const [fieldEdit, setFieldEdit] = useState<AgingFieldIdentity | null>(null);
  const [agingNewField, setAgingNewField] = useState("");
  const [approvalsEdit, setApprovalsEdit] = useState<ApprovalsIdentity | null>(null);
  const [approvalsDraft, setApprovalsDraft] = useState("");
  const [defaultEdit, setDefaultEdit] = useState<TransitionIdentity | null>(null);
  const [rolesEdit, setRolesEdit] = useState<(TransitionIdentity & { roleName: string }) | null>(
    null,
  );
  const [addRoleEdit, setAddRoleEdit] = useState<(TransitionIdentity & { roleName: string }) | null>(
    null,
  );
  const [removeRoleEdit, setRemoveRoleEdit] = useState<
    (TransitionIdentity & { roleName: string }) | null
  >(null);
  const [clearRoleEdit, setClearRoleEdit] = useState<TransitionIdentity | null>(null);

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
    setRepeatedFrom("");
    setRepeatedTo("");
    setRepeatedMinutes("");
    setSystemFrom("");
    setSystemTo("");
    setSystemField("");
    setAgingEdit(null);
    setAgingNewMinutes("");
    setFieldEdit(null);
    setAgingNewField("");
    setPendingAging(null);
    setApprovalsEdit(null);
    setApprovalsDraft("");
    setDefaultEdit(null);
    setRolesEdit(null);
    setAddRoleEdit(null);
    setRemoveRoleEdit(null);
    setClearRoleEdit(null);
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
    setRepeatedFrom((prev) => (prev && names.includes(prev) ? prev : (names[0] ?? "")));
    setRepeatedTo((prev) => (prev && names.includes(prev) ? prev : ""));
    setSystemFrom((prev) => (prev && names.includes(prev) ? prev : (names[0] ?? "")));
    setSystemTo((prev) => (prev && names.includes(prev) ? prev : ""));
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

  const onCancelRepeated = useCallback(() => {
    setRepeatedTo("");
    setRepeatedMinutes("");
    setError(null);
  }, []);

  const onAddRepeated = useCallback(async () => {
    const names = (graph?.nodes ?? [])
      .map((node) => node.name)
      .filter((name): name is string => !!name && name.trim().length > 0);
    const from = repeatedFrom || names[0] || "";
    const to = repeatedTo.trim();
    if (!isValidWorkflowName(from) || !isValidWorkflowName(to) || !isPositiveMinuteInterval(repeatedMinutes)) {
      setError(DEV_MSG.WF_REPEATED_INVALID);
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
        intervalMinutes: Number(repeatedMinutes.trim()),
        type: "REPEATED",
      });
      setGraph(next);
      setNotice(DEV_MSG.WF_REPEATED_SAVED);
      setRepeatedMinutes("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_REPEATED_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_REPEATED_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_REPEATED_BAD);
      } else {
        setError(DEV_MSG.WF_REPEATED_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [graph, repeatedFrom, repeatedMinutes, repeatedTo, workflowName]);

  const onCancelSystemField = useCallback(() => {
    setSystemTo("");
    setSystemField("");
    setError(null);
  }, []);

  const onAddSystemField = useCallback(async () => {
    const names = (graph?.nodes ?? [])
      .map((node) => node.name)
      .filter((name): name is string => !!name && name.trim().length > 0);
    const from = systemFrom || names[0] || "";
    const to = systemTo.trim();
    const field = systemField.trim().toUpperCase();
    if (!isValidWorkflowName(from) || !isValidWorkflowName(to) || !isWorkflowAgingSystemField(field)) {
      setError(DEV_MSG.WF_SYSTEM_FIELD_INVALID);
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
        type: "SYSTEM_FIELD",
        systemField: field,
      });
      setGraph(next);
      setNotice(DEV_MSG.WF_SYSTEM_FIELD_SAVED);
      setSystemField("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_SYSTEM_FIELD_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_SYSTEM_FIELD_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_SYSTEM_FIELD_BAD);
      } else {
        setError(DEV_MSG.WF_SYSTEM_FIELD_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [graph, systemField, systemFrom, systemTo, workflowName]);

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
        ...(agingEdit.agingType === "REPEATED" ? { type: "REPEATED" as const } : {}),
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
        setError(
          agingEdit.agingType === "REPEATED"
            ? DEV_MSG.WF_REPEATED_CONFLICT
            : DEV_MSG.WF_AGING_INTERVAL_CONFLICT,
        );
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_AGING_INTERVAL_BAD);
      } else {
        setError(DEV_MSG.WF_AGING_INTERVAL_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [agingEdit, agingNewMinutes, workflowName]);

  const onCancelAgingField = useCallback(() => {
    setFieldEdit(null);
    setAgingNewField("");
    setError(null);
  }, []);

  const onSaveAgingField = useCallback(async () => {
    if (!fieldEdit) {
      return;
    }
    const nextField = agingNewField.trim().toUpperCase();
    if (
      !isWorkflowAgingSystemField(nextField) ||
      nextField === fieldEdit.systemField.trim().toUpperCase()
    ) {
      setError(DEV_MSG.WF_AGING_FIELD_INVALID);
      setNotice(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await updateWorkflowAgingSystemField(workflowName, {
        from: fieldEdit.from,
        to: fieldEdit.to,
        systemField: fieldEdit.systemField,
        newSystemField: nextField,
      });
      setGraph(next);
      setNotice(DEV_MSG.WF_AGING_FIELD_SAVED);
      setFieldEdit(null);
      setAgingNewField("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_AGING_FIELD_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_AGING_FIELD_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_AGING_FIELD_BAD);
      } else {
        setError(DEV_MSG.WF_AGING_FIELD_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [agingNewField, fieldEdit, workflowName]);

  const onConfirmDeleteAging = useCallback(async () => {
    if (!pendingAging) {
      setPendingAging(null);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next =
        pendingAging.agingType === "SYSTEM_FIELD"
          ? await deleteWorkflowAgingTransition(
              workflowName,
              pendingAging.from,
              pendingAging.to,
              undefined,
              "SYSTEM_FIELD",
              pendingAging.systemField,
            )
          : pendingAging.agingType === "REPEATED"
            ? await deleteWorkflowAgingTransition(
                workflowName,
                pendingAging.from,
                pendingAging.to,
                pendingAging.intervalMinutes,
                "REPEATED",
              )
            : await deleteWorkflowAgingTransition(
                workflowName,
                pendingAging.from,
                pendingAging.to,
                pendingAging.intervalMinutes,
              );
      setGraph(next);
      setNotice(DEV_MSG.WF_AGING_DELETED);
      setPendingAging(null);
      setAgingEdit(null);
      setFieldEdit(null);
      setAgingNewField("");
      setReloadToken((n) => n + 1);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_AGING_DELETE_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(
          pendingAging.agingType === "REPEATED" || pendingAging.agingType === "SYSTEM_FIELD"
            ? DEV_MSG.WF_AGING_DELETE_TYPED_CONFLICT
            : DEV_MSG.WF_AGING_DELETE_CONFLICT,
        );
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

  const onCancelDefault = useCallback(() => {
    setDefaultEdit(null);
    setError(null);
  }, []);

  const onSaveDefault = useCallback(async () => {
    if (!defaultEdit) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await markTransitionAsDefault(
        workflowName,
        defaultEdit.from,
        defaultEdit.label,
        defaultEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_DEFAULT_SAVED);
      setDefaultEdit(null);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_DEFAULT_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_DEFAULT_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_DEFAULT_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_DEFAULT_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [defaultEdit, workflowName]);

  const onCancelRoles = useCallback(() => {
    setRolesEdit(null);
    setError(null);
  }, []);

  const onSaveRoles = useCallback(async () => {
    if (!rolesEdit) {
      return;
    }
    const roleName = rolesEdit.roleName.trim();
    if (!roleName) {
      setError(DEV_MSG.WF_GRAPH_ROLES_INVALID);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await restrictTransitionToOneRole(
        workflowName,
        rolesEdit.from,
        rolesEdit.label,
        roleName,
        rolesEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_ROLES_SAVED);
      setRolesEdit(null);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_ROLES_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_ROLES_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_ROLES_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_ROLES_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [rolesEdit, workflowName]);

  const onCancelAddRole = useCallback(() => {
    setAddRoleEdit(null);
    setError(null);
  }, []);

  const onSaveAddRole = useCallback(async () => {
    if (!addRoleEdit) {
      return;
    }
    const roleName = addRoleEdit.roleName.trim();
    if (!roleName) {
      setError(DEV_MSG.WF_GRAPH_ROLES_ADD_INVALID);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await addTransitionAllowedRole(
        workflowName,
        addRoleEdit.from,
        addRoleEdit.label,
        roleName,
        addRoleEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_ROLES_ADD_SAVED);
      setAddRoleEdit(null);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_ROLES_ADD_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_ROLES_ADD_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_ROLES_ADD_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_ROLES_ADD_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [addRoleEdit, workflowName]);

  const onCancelRemoveRole = useCallback(() => {
    setRemoveRoleEdit(null);
    setError(null);
  }, []);

  const onSaveRemoveRole = useCallback(async () => {
    if (!removeRoleEdit) {
      return;
    }
    const roleName = removeRoleEdit.roleName.trim();
    if (!roleName) {
      setError(DEV_MSG.WF_GRAPH_ROLES_REMOVE_INVALID);
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await removeTransitionAllowedRole(
        workflowName,
        removeRoleEdit.from,
        removeRoleEdit.label,
        roleName,
        removeRoleEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_ROLES_REMOVE_SAVED);
      setRemoveRoleEdit(null);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_ROLES_REMOVE_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_ROLES_REMOVE_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_ROLES_REMOVE_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_ROLES_REMOVE_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [removeRoleEdit, workflowName]);

  const onCancelClearRoles = useCallback(() => {
    setClearRoleEdit(null);
    setError(null);
  }, []);

  const onSaveClearRoles = useCallback(async () => {
    if (!clearRoleEdit) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const next = await clearTransitionAllowedRoles(
        workflowName,
        clearRoleEdit.from,
        clearRoleEdit.label,
        clearRoleEdit.to,
      );
      setGraph(next);
      setNotice(DEV_MSG.WF_GRAPH_ROLES_CLEAR_SAVED);
      setClearRoleEdit(null);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 403) {
        setError(DEV_MSG.WF_GRAPH_ROLES_CLEAR_FORBIDDEN);
      } else if (isApiError(err) && err.status === 409) {
        setError(DEV_MSG.WF_GRAPH_ROLES_CLEAR_CONFLICT);
      } else if (isApiError(err) && (err.status === 400 || err.status === 404)) {
        setError(DEV_MSG.WF_GRAPH_ROLES_CLEAR_BAD);
      } else {
        setError(DEV_MSG.WF_GRAPH_ROLES_CLEAR_ERROR);
      }
    } finally {
      setBusy(false);
    }
  }, [clearRoleEdit, workflowName]);

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
      {canWrite ? (
        <form
          data-testid="developer-wf-repeated-aging-form"
          style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginBottom: "8px" }}
          onSubmit={(ev) => {
            ev.preventDefault();
            void onAddRepeated();
          }}
        >
          <label>
            {DEV_MSG.WF_AGING_FROM}
            <select
              data-testid="developer-wf-repeated-aging-from"
              value={repeatedFrom || stepNames[0] || ""}
              disabled={busy}
              onChange={(ev) => setRepeatedFrom(ev.target.value)}
            >
              {stepNames.map((name) => (
                <option key={`repeated-from-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_AGING_TO}
            <select
              data-testid="developer-wf-repeated-aging-to"
              value={repeatedTo}
              disabled={busy}
              onChange={(ev) => setRepeatedTo(ev.target.value)}
            >
              <option value="">{DEV_MSG.WF_AGING_TO_BLANK}</option>
              {stepNames.map((name) => (
                <option key={`repeated-to-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_AGING_MINUTES}
            <input
              data-testid="developer-wf-repeated-aging-minutes"
              inputMode="numeric"
              value={repeatedMinutes}
              disabled={busy}
              onChange={(ev) => setRepeatedMinutes(ev.target.value)}
            />
          </label>
          <button type="submit" data-testid="developer-wf-repeated-aging-add" disabled={busy}>
            {DEV_MSG.WF_REPEATED_ADD}
          </button>
          <button
            type="button"
            data-testid="developer-wf-repeated-aging-cancel"
            disabled={busy}
            onClick={() => {
              onCancelRepeated();
            }}
          >
            {DEV_MSG.WF_AGING_CANCEL}
          </button>
        </form>
      ) : null}
      {canWrite ? (
        <form
          data-testid="developer-wf-system-field-aging-form"
          style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginBottom: "8px" }}
          onSubmit={(ev) => {
            ev.preventDefault();
            void onAddSystemField();
          }}
        >
          <label>
            {DEV_MSG.WF_AGING_FROM}
            <select
              data-testid="developer-wf-system-field-aging-from"
              value={systemFrom || stepNames[0] || ""}
              disabled={busy}
              onChange={(ev) => setSystemFrom(ev.target.value)}
            >
              {stepNames.map((name) => (
                <option key={`system-from-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_AGING_TO}
            <select
              data-testid="developer-wf-system-field-aging-to"
              value={systemTo}
              disabled={busy}
              onChange={(ev) => setSystemTo(ev.target.value)}
            >
              <option value="">{DEV_MSG.WF_AGING_TO_BLANK}</option>
              {stepNames.map((name) => (
                <option key={`system-to-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <label>
            {DEV_MSG.WF_SYSTEM_FIELD_LABEL}
            <select
              data-testid="developer-wf-system-field-aging-field"
              value={systemField}
              disabled={busy}
              onChange={(ev) => setSystemField(ev.target.value)}
            >
              <option value="">{DEV_MSG.WF_SYSTEM_FIELD_BLANK}</option>
              {WORKFLOW_AGING_SYSTEM_FIELDS.map((name) => (
                <option key={`system-field-${name}`} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" data-testid="developer-wf-system-field-aging-add" disabled={busy}>
            {DEV_MSG.WF_SYSTEM_FIELD_ADD}
          </button>
          <button
            type="button"
            data-testid="developer-wf-system-field-aging-cancel"
            disabled={busy}
            onClick={() => {
              onCancelSystemField();
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
              {typeof edge.defaultTransition === "boolean" ? (
                <span
                  data-testid={`developer-wf-graph-default-${i}`}
                  data-default={edge.defaultTransition ? "true" : "false"}
                  style={{ marginLeft: 8 }}
                >
                  {edge.defaultTransition ? DEV_MSG.WF_GRAPH_DEFAULT : ""}
                </span>
              ) : null}
              {!packaged && edge.defaultTransition === false && edge.from && edge.label ? (
                defaultEdit &&
                defaultEdit.from === edge.from &&
                defaultEdit.label === edge.label &&
                defaultEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <button
                      type="button"
                      data-testid="developer-wf-default-save"
                      disabled={busy}
                      onClick={() => {
                        void onSaveDefault();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_DEFAULT_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-default-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelDefault();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_DEFAULT_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-default-edit-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setDefaultEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                      });
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_DEFAULT_MARK}
                  </button>
                )
              ) : null}
              {typeof edge.allowAllRoles === "boolean" ? (
                <span
                  data-testid={`developer-wf-graph-roles-${i}`}
                  data-allow-all={edge.allowAllRoles ? "true" : "false"}
                  data-allowed-roles={roleNameList(edge.allowedRoles).join(",")}
                  style={{ marginLeft: 8 }}
                >
                  {edge.allowAllRoles
                    ? DEV_MSG.WF_GRAPH_ROLES_ALL
                    : roleNameList(edge.allowedRoles).join(", ")}
                </span>
              ) : null}
              {!packaged &&
              edge.allowAllRoles === true &&
              edge.from &&
              edge.label &&
              roleNameList(graph?.roles).length > 0 ? (
                rolesEdit &&
                rolesEdit.from === edge.from &&
                rolesEdit.label === edge.label &&
                rolesEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <label>
                      {DEV_MSG.WF_GRAPH_ROLES_LABEL}{" "}
                      <select
                        data-testid="developer-wf-roles-value"
                        value={rolesEdit.roleName}
                        disabled={busy}
                        onChange={(ev) =>
                          setRolesEdit({ ...rolesEdit, roleName: ev.target.value })
                        }
                      >
                        {roleNameList(graph?.roles).map((roleName) => (
                          <option key={roleName} value={roleName}>
                            {roleName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-save"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        void onSaveRoles();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelRoles();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-roles-edit-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      const first = roleNameList(graph?.roles)[0] ?? "";
                      setNotice(null);
                      setError(null);
                      setRolesEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                        roleName: first,
                      });
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_ROLES_RESTRICT}
                  </button>
                )
              ) : null}
              {!packaged &&
              edge.allowAllRoles === false &&
              edge.from &&
              edge.label &&
              roleNameList(edge.allowedRoles).length > 0 &&
              rolesNotYetAllowed(graph?.roles, edge.allowedRoles).length > 0 ? (
                addRoleEdit &&
                addRoleEdit.from === edge.from &&
                addRoleEdit.label === edge.label &&
                addRoleEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <label>
                      {DEV_MSG.WF_GRAPH_ROLES_LABEL}{" "}
                      <select
                        data-testid="developer-wf-roles-add-value"
                        value={addRoleEdit.roleName}
                        disabled={busy}
                        onChange={(ev) =>
                          setAddRoleEdit({ ...addRoleEdit, roleName: ev.target.value })
                        }
                      >
                        {rolesNotYetAllowed(graph?.roles, edge.allowedRoles).map((roleName) => (
                          <option key={roleName} value={roleName}>
                            {roleName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-add-save"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        void onSaveAddRole();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_ADD_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-add-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelAddRole();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_ADD_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-roles-add-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      const first = rolesNotYetAllowed(graph?.roles, edge.allowedRoles)[0] ?? "";
                      setNotice(null);
                      setError(null);
                      setAddRoleEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                        roleName: first,
                      });
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_ROLES_ADD}
                  </button>
                )
              ) : null}
              {!packaged &&
              edge.allowAllRoles === false &&
              edge.from &&
              edge.label &&
              roleNameList(edge.allowedRoles).length >= 2 ? (
                removeRoleEdit &&
                removeRoleEdit.from === edge.from &&
                removeRoleEdit.label === edge.label &&
                removeRoleEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <label>
                      {DEV_MSG.WF_GRAPH_ROLES_LABEL}{" "}
                      <select
                        data-testid="developer-wf-roles-remove-value"
                        value={removeRoleEdit.roleName}
                        disabled={busy}
                        onChange={(ev) =>
                          setRemoveRoleEdit({ ...removeRoleEdit, roleName: ev.target.value })
                        }
                      >
                        {roleNameList(edge.allowedRoles).map((roleName) => (
                          <option key={roleName} value={roleName}>
                            {roleName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-remove-save"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        void onSaveRemoveRole();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_REMOVE_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-remove-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelRemoveRole();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_REMOVE_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-roles-remove-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      const first = roleNameList(edge.allowedRoles)[0] ?? "";
                      setNotice(null);
                      setError(null);
                      setRemoveRoleEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                        roleName: first,
                      });
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_ROLES_REMOVE}
                  </button>
                )
              ) : null}
              {!packaged && edge.allowAllRoles === false && edge.from && edge.label ? (
                clearRoleEdit &&
                clearRoleEdit.from === edge.from &&
                clearRoleEdit.label === edge.label &&
                clearRoleEdit.to === (edge.to || "") ? (
                  <span style={{ marginLeft: 8 }}>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-clear-save"
                      disabled={busy}
                      onClick={() => {
                        void onSaveClearRoles();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_CLEAR_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-roles-clear-cancel"
                      style={{ marginLeft: 8 }}
                      disabled={busy}
                      onClick={() => {
                        onCancelClearRoles();
                      }}
                    >
                      {DEV_MSG.WF_GRAPH_ROLES_CLEAR_CANCEL}
                    </button>
                  </span>
                ) : (
                  <button
                    type="button"
                    data-testid={`developer-wf-roles-clear-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setClearRoleEdit({
                        from: edge.from as string,
                        label: edge.label as string,
                        to: (edge.to as string) || "",
                      });
                    }}
                  >
                    {DEV_MSG.WF_GRAPH_ROLES_CLEAR}
                  </button>
                )
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
                data-aging-type={edge.agingType || ""}
                data-system-field={edge.systemField || ""}
              >
                {edge.from || "—"} — {edge.label || "—"} → {edge.to || "—"}
                {edge.agingType === "SYSTEM_FIELD"
                  ? ` (${edge.systemField ? `${edge.systemField} ` : ""}${DEV_MSG.WF_SYSTEM_FIELD_KIND})`
                  : typeof edge.intervalMinutes === "number"
                    ? ` (${edge.intervalMinutes} minutes)`
                    : ""}
                {edge.agingType === "REPEATED" ? ` (${DEV_MSG.WF_REPEATED_KIND})` : ""}
                {canWrite && canChangeAgingInterval(edge) ? (
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
                      setFieldEdit(null);
                      setAgingNewField("");
                      setAgingEdit({
                        from: edge.from as string,
                        to: edge.to as string,
                        intervalMinutes: edge.intervalMinutes as number,
                        agingType:
                          agingIntervalKind(edge.agingType) === "REPEATED" ? "REPEATED" : "ABSOLUTE",
                      });
                      setAgingNewMinutes("");
                    }}
                  >
                    {DEV_MSG.WF_AGING_CHANGE}
                  </button>
                ) : null}
                {canWrite && canChangeSystemField(edge) ? (
                  <button
                    type="button"
                    data-testid={`developer-wf-aging-field-change-${i}`}
                    style={{ marginLeft: 8 }}
                    disabled={busy}
                    onClick={() => {
                      setNotice(null);
                      setError(null);
                      setPending(null);
                      setPendingAging(null);
                      setPendingStep(null);
                      setAgingEdit(null);
                      setAgingNewMinutes("");
                      setFieldEdit({
                        from: edge.from as string,
                        to: edge.to as string,
                        systemField: (edge.systemField as string).trim(),
                      });
                      setAgingNewField("");
                    }}
                  >
                    {DEV_MSG.WF_AGING_FIELD_CHANGE}
                  </button>
                ) : null}
                {canWrite && canDeleteAgingEdge(edge) ? (
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
                      setFieldEdit(null);
                      setAgingNewField("");
                      setPendingAging({
                        from: edge.from as string,
                        to: edge.to as string,
                        intervalMinutes: edge.intervalMinutes,
                        agingType: edge.agingType,
                        systemField: edge.systemField,
                      });
                    }}
                  >
                    {DEV_MSG.WF_AGING_DELETE}
                  </button>
                ) : null}
                {agingEdit && sameAgingIntervalEdit(agingEdit, edge) ? (
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
                {fieldEdit && sameAgingFieldEdit(fieldEdit, edge) ? (
                  <form
                    data-testid="developer-wf-aging-field-form"
                    style={{ display: "flex", flexWrap: "wrap", gap: "8px", alignItems: "end", marginTop: 8 }}
                    onSubmit={(ev) => {
                      ev.preventDefault();
                      void onSaveAgingField();
                    }}
                  >
                    <label>
                      {DEV_MSG.WF_AGING_NEW_FIELD}
                      <select
                        data-testid="developer-wf-aging-new-field"
                        value={agingNewField}
                        disabled={busy}
                        onChange={(ev) => setAgingNewField(ev.target.value)}
                      >
                        <option value="">{DEV_MSG.WF_SYSTEM_FIELD_BLANK}</option>
                        {WORKFLOW_AGING_SYSTEM_FIELDS.map((name) => (
                          <option key={`aging-new-field-${name}`} value={name}>
                            {name}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button type="submit" data-testid="developer-wf-aging-field-save" disabled={busy}>
                      {DEV_MSG.WF_AGING_FIELD_SAVE}
                    </button>
                    <button
                      type="button"
                      data-testid="developer-wf-aging-field-cancel"
                      disabled={busy}
                      onClick={() => {
                        onCancelAgingField();
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
        message={pendingAging ? agingDeleteConfirm(pendingAging) : DEV_MSG.WF_AGING_DELETE_CONFIRM}
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
