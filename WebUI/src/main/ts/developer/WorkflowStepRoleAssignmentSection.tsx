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
import { isApiError } from "../api/client";
import {
  addStepRole,
  listStepRoleAssignments,
  removeStepRole,
  setStepRoleAdhoc,
  setStepRoleAssignment,
  setStepRoleInbox,
  setStepRoleNotify,
} from "../api/developer/workflowsApi";
import type { WorkflowStepRoleAssignment } from "../api/developer/types";
import { CatalogConfirmDialog } from "./CatalogConfirmDialog";
import { catalogColors, errorAlert, tableHeaderRow, tableRow } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  applyAssignmentAfterReload,
  assignmentTypeLabel,
  canOfferStepRoleAssignment,
  findAssignment,
  isAssignmentChangeReady,
  mutableAssignments,
  normalizeAssignmentType,
} from "./workflowStepRoleAssignment";
import {
  applyAddedRoleAfterReload,
  assignmentStepNames,
  firstStepWithRoleToAdd,
  isAddRoleReady,
  rolesNotOnStep,
} from "./workflowStepRoleAdd";
import {
  applyRemovedRoleAfterReload,
  isRemoveRoleReady,
  removableRolesOnStep,
  removableStepNames,
} from "./workflowStepRoleRemove";
import {
  applyNotifyAfterReload,
  findNotifyRow,
  isNotifyChangeReady,
  namedRoleRows,
  notifyChoice,
  parseNotifyChoice,
  storedNotify,
} from "./workflowStepRoleNotify";
import {
  applyInboxAfterReload,
  findInboxRow,
  inboxChoice,
  inboxRoleRows,
  isInboxChangeReady,
  parseInboxChoice,
  storedInbox,
} from "./workflowStepRoleInbox";
import {
  ADHOC_TYPES,
  applyAdhocAfterReload,
  adhocRoleRows,
  findAdhocRow,
  isAdhocChangeReady,
  normalizeAdhocType,
  storedAdhoc,
  type AdhocType,
} from "./workflowStepRoleAdhoc";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

function assignmentErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_ASSIGN_FORBIDDEN;
    }
    if (err.status === 409) {
      return DEV_MSG.WF_ROLE_ASSIGN_CONFLICT;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_ASSIGN_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_ASSIGN_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_ASSIGN_ERROR;
}

function addRoleErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_ADD_FORBIDDEN;
    }
    if (err.status === 409) {
      return DEV_MSG.WF_ROLE_ADD_CONFLICT;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_ADD_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_ADD_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_ADD_ERROR;
}

function removeRoleErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_REMOVE_FORBIDDEN;
    }
    if (err.status === 409) {
      return DEV_MSG.WF_ROLE_REMOVE_CONFLICT;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_REMOVE_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_REMOVE_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_REMOVE_ERROR;
}

function notifyErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_NOTIFY_FORBIDDEN;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_NOTIFY_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_NOTIFY_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_NOTIFY_ERROR;
}

function inboxErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_INBOX_FORBIDDEN;
    }
    if (err.status === 409) {
      return DEV_MSG.WF_ROLE_INBOX_CONFLICT;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_INBOX_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_INBOX_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_INBOX_ERROR;
}

function adhocErrorFallback(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return DEV_MSG.WF_ROLE_ADHOC_FORBIDDEN;
    }
    if (err.status === 409) {
      return DEV_MSG.WF_ROLE_ADHOC_CONFLICT;
    }
    if (err.status === 400) {
      return DEV_MSG.WF_ROLE_ADHOC_BAD;
    }
    if (err.status === 404) {
      return DEV_MSG.WF_ROLE_ADHOC_MISSING;
    }
  }
  return DEV_MSG.WF_ROLE_ADHOC_ERROR;
}

function adhocLabel(value: string | undefined): string {
  const type = normalizeAdhocType(value);
  if (type === "enabled") {
    return DEV_MSG.WF_ROLE_ADHOC_ENABLED;
  }
  if (type === "anonymous") {
    return DEV_MSG.WF_ROLE_ADHOC_ANONYMOUS;
  }
  return DEV_MSG.WF_ROLE_ADHOC_DISABLED;
}

/**
 * Set Reader or Assignee on one role already assigned to one step.
 * The table shows the stored type only after a successful reload.
 */
export function WorkflowStepRoleAssignmentSection({
  workflowName,
  defaultWorkflow,
}: {
  workflowName: string;
  defaultWorkflow?: boolean;
}): React.ReactElement {
  const [rows, setRows] = useState<WorkflowStepRoleAssignment[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [stepName, setStepName] = useState("");
  const [roleName, setRoleName] = useState("");
  const [nextType, setNextType] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [addStep, setAddStep] = useState("");
  const [addRole, setAddRole] = useState("");
  const [addType, setAddType] = useState("READER");
  const [addBusy, setAddBusy] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);
  const [addNotice, setAddNotice] = useState<string | null>(null);
  const [removeStep, setRemoveStep] = useState("");
  const [removeRole, setRemoveRole] = useState("");
  const [removeBusy, setRemoveBusy] = useState(false);
  const [removeError, setRemoveError] = useState<string | null>(null);
  const [removeNotice, setRemoveNotice] = useState<string | null>(null);
  const [removeConfirmOpen, setRemoveConfirmOpen] = useState(false);
  const [notifyStep, setNotifyStep] = useState("");
  const [notifyRole, setNotifyRole] = useState("");
  const [nextNotify, setNextNotify] = useState(false);
  const [notifyBusy, setNotifyBusy] = useState(false);
  const [notifyError, setNotifyError] = useState<string | null>(null);
  const [notifyNotice, setNotifyNotice] = useState<string | null>(null);
  const [inboxStep, setInboxStep] = useState("");
  const [inboxRole, setInboxRole] = useState("");
  const [nextInbox, setNextInbox] = useState(false);
  const [inboxBusy, setInboxBusy] = useState(false);
  const [inboxError, setInboxError] = useState<string | null>(null);
  const [inboxNotice, setInboxNotice] = useState<string | null>(null);
  const [adhocStep, setAdhocStep] = useState("");
  const [adhocRole, setAdhocRole] = useState("");
  const [nextAdhoc, setNextAdhoc] = useState<AdhocType>("disabled");
  const [adhocBusy, setAdhocBusy] = useState(false);
  const [adhocError, setAdhocError] = useState<string | null>(null);
  const [adhocNotice, setAdhocNotice] = useState<string | null>(null);

  const canOffer = canOfferStepRoleAssignment({
    name: workflowName,
    defaultWorkflow,
  });

  useEffect(() => {
    let cancelled = false;
    setRows([]);
    setLoadError(null);
    setError(null);
    setNotice(null);
    setStepName("");
    setRoleName("");
    setNextType("");
    setAddStep("");
    setAddRole("");
    setAddType("READER");
    setAddError(null);
    setAddNotice(null);
    setRemoveStep("");
    setRemoveRole("");
    setRemoveError(null);
    setRemoveNotice(null);
    setRemoveConfirmOpen(false);
    setNotifyStep("");
    setNotifyRole("");
    setNextNotify(false);
    setNotifyError(null);
    setNotifyNotice(null);
    setInboxStep("");
    setInboxRole("");
    setNextInbox(false);
    setInboxError(null);
    setInboxNotice(null);
    setAdhocStep("");
    setAdhocRole("");
    setNextAdhoc("disabled");
    setAdhocError(null);
    setAdhocNotice(null);
    listStepRoleAssignments(workflowName)
      .then((loaded) => {
        if (cancelled) {
          return;
        }
        const next = Array.isArray(loaded) ? loaded : [];
        setRows(next);
        const first = mutableAssignments(next)[0];
        if (first?.stepName && first.roleName) {
          setStepName(first.stepName);
          setRoleName(first.roleName);
          setNextType(normalizeAssignmentType(first.assignmentType));
        }
        const stepToAdd = firstStepWithRoleToAdd(next);
        setAddStep(stepToAdd);
        setAddRole(rolesNotOnStep(next, stepToAdd)[0] ?? "");
        setAddType("READER");
        const stepToRemove = removableStepNames(next)[0] ?? "";
        setRemoveStep(stepToRemove);
        setRemoveRole(removableRolesOnStep(next, stepToRemove)[0] ?? "");
        const notifyFirst = namedRoleRows(next)[0];
        if (notifyFirst?.stepName && notifyFirst.roleName) {
          setNotifyStep(notifyFirst.stepName);
          setNotifyRole(notifyFirst.roleName);
          setNextNotify(storedNotify(notifyFirst));
        }
        const inboxFirst = inboxRoleRows(next)[0];
        if (inboxFirst?.stepName && inboxFirst.roleName) {
          setInboxStep(inboxFirst.stepName);
          setInboxRole(inboxFirst.roleName);
          setNextInbox(storedInbox(inboxFirst));
        }
        const adhocFirst = adhocRoleRows(next)[0];
        if (adhocFirst?.stepName && adhocFirst.roleName) {
          setAdhocStep(adhocFirst.stepName);
          setAdhocRole(adhocFirst.roleName);
          setNextAdhoc(storedAdhoc(adhocFirst));
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setLoadError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [workflowName]);

  const editable = useMemo(() => mutableAssignments(rows), [rows]);
  const steps = useMemo(() => {
    const names: string[] = [];
    for (const row of editable) {
      const name = (row.stepName ?? "").trim();
      if (name && !names.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
        names.push(name);
      }
    }
    return names;
  }, [editable]);
  const rolesForStep = useMemo(
    () =>
      editable.filter(
        (row) => (row.stepName ?? "").trim().toLowerCase() === stepName.trim().toLowerCase(),
      ),
    [editable, stepName],
  );
  const current = findAssignment(rows, stepName, roleName);
  const currentType = current?.assignmentType;
  const ready = canOffer && isAssignmentChangeReady(currentType, nextType);
  const addSteps = useMemo(() => assignmentStepNames(rows), [rows]);
  const rolesToAdd = useMemo(() => rolesNotOnStep(rows, addStep), [rows, addStep]);
  const addReady = canOffer && isAddRoleReady(rows, addStep, addRole, addType);
  const removeSteps = useMemo(() => removableStepNames(rows), [rows]);
  const rolesToRemove = useMemo(
    () => removableRolesOnStep(rows, removeStep),
    [rows, removeStep],
  );
  const removeReady = canOffer && isRemoveRoleReady(rows, removeStep, removeRole);
  const notifyRows = useMemo(() => namedRoleRows(rows), [rows]);
  const notifySteps = useMemo(() => {
    const names: string[] = [];
    for (const row of notifyRows) {
      const name = (row.stepName ?? "").trim();
      if (name && !names.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
        names.push(name);
      }
    }
    return names;
  }, [notifyRows]);
  const notifyRolesForStep = useMemo(
    () =>
      notifyRows.filter(
        (row) => (row.stepName ?? "").trim().toLowerCase() === notifyStep.trim().toLowerCase(),
      ),
    [notifyRows, notifyStep],
  );
  const currentNotify = findNotifyRow(rows, notifyStep, notifyRole);
  const currentNotifyOn = storedNotify(currentNotify);
  const notifyReady =
    canOffer &&
    !!currentNotify &&
    isNotifyChangeReady(currentNotifyOn, nextNotify);
  const inboxRows = useMemo(() => inboxRoleRows(rows), [rows]);
  const inboxSteps = useMemo(() => {
    const names: string[] = [];
    for (const row of inboxRows) {
      const name = (row.stepName ?? "").trim();
      if (name && !names.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
        names.push(name);
      }
    }
    return names;
  }, [inboxRows]);
  const inboxRolesForStep = useMemo(
    () =>
      inboxRows.filter(
        (row) => (row.stepName ?? "").trim().toLowerCase() === inboxStep.trim().toLowerCase(),
      ),
    [inboxRows, inboxStep],
  );
  const currentInbox = findInboxRow(rows, inboxStep, inboxRole);
  const currentInboxOn = storedInbox(currentInbox);
  const inboxReady =
    canOffer && !!currentInbox && isInboxChangeReady(currentInboxOn, nextInbox);
  const adhocRows = useMemo(() => adhocRoleRows(rows), [rows]);
  const adhocSteps = useMemo(() => {
    const names: string[] = [];
    for (const row of adhocRows) {
      const name = (row.stepName ?? "").trim();
      if (name && !names.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
        names.push(name);
      }
    }
    return names;
  }, [adhocRows]);
  const adhocRolesForStep = useMemo(
    () =>
      adhocRows.filter(
        (row) => (row.stepName ?? "").trim().toLowerCase() === adhocStep.trim().toLowerCase(),
      ),
    [adhocRows, adhocStep],
  );
  const currentAdhoc = findAdhocRow(rows, adhocStep, adhocRole);
  const currentAdhocType = storedAdhoc(currentAdhoc);
  const adhocReady =
    canOffer && !!currentAdhoc && isAdhocChangeReady(currentAdhocType, nextAdhoc);

  function chooseStep(nextStep: string): void {
    setStepName(nextStep);
    setError(null);
    setNotice(null);
    const role = editable.find(
      (row) => (row.stepName ?? "").trim().toLowerCase() === nextStep.trim().toLowerCase(),
    );
    const name = role?.roleName ?? "";
    setRoleName(name);
    setNextType(normalizeAssignmentType(role?.assignmentType));
  }

  function chooseRole(nextRole: string): void {
    setRoleName(nextRole);
    setError(null);
    setNotice(null);
    const hit = findAssignment(rows, stepName, nextRole);
    setNextType(normalizeAssignmentType(hit?.assignmentType));
  }

  function cancel(): void {
    setNextType(normalizeAssignmentType(currentType));
    setError(null);
    setNotice(null);
  }

  function chooseNotifyStep(nextStep: string): void {
    setNotifyStep(nextStep);
    setNotifyError(null);
    setNotifyNotice(null);
    const role = notifyRows.find(
      (row) => (row.stepName ?? "").trim().toLowerCase() === nextStep.trim().toLowerCase(),
    );
    const name = role?.roleName ?? "";
    setNotifyRole(name);
    setNextNotify(storedNotify(role));
  }

  function chooseNotifyRole(nextRole: string): void {
    setNotifyRole(nextRole);
    setNotifyError(null);
    setNotifyNotice(null);
    setNextNotify(storedNotify(findNotifyRow(rows, notifyStep, nextRole)));
  }

  function cancelNotify(): void {
    setNextNotify(currentNotifyOn);
    setNotifyError(null);
    setNotifyNotice(null);
  }

  function chooseInboxStep(nextStep: string): void {
    setInboxStep(nextStep);
    setInboxError(null);
    setInboxNotice(null);
    const role = inboxRows.find(
      (row) => (row.stepName ?? "").trim().toLowerCase() === nextStep.trim().toLowerCase(),
    );
    const name = role?.roleName ?? "";
    setInboxRole(name);
    setNextInbox(storedInbox(role));
  }

  function chooseInboxRole(nextRole: string): void {
    setInboxRole(nextRole);
    setInboxError(null);
    setInboxNotice(null);
    setNextInbox(storedInbox(findInboxRow(rows, inboxStep, nextRole)));
  }

  function cancelInbox(): void {
    setNextInbox(currentInboxOn);
    setInboxError(null);
    setInboxNotice(null);
  }

  function chooseAdhocStep(nextStep: string): void {
    setAdhocStep(nextStep);
    setAdhocError(null);
    setAdhocNotice(null);
    const role = adhocRows.find(
      (row) => (row.stepName ?? "").trim().toLowerCase() === nextStep.trim().toLowerCase(),
    );
    const name = role?.roleName ?? "";
    setAdhocRole(name);
    setNextAdhoc(storedAdhoc(role));
  }

  function chooseAdhocRole(nextRole: string): void {
    setAdhocRole(nextRole);
    setAdhocError(null);
    setAdhocNotice(null);
    setNextAdhoc(storedAdhoc(findAdhocRow(rows, adhocStep, nextRole)));
  }

  function cancelAdhoc(): void {
    setNextAdhoc(currentAdhocType);
    setAdhocError(null);
    setAdhocNotice(null);
  }

  function chooseAddStep(nextStep: string): void {
    setAddStep(nextStep);
    setAddError(null);
    setAddNotice(null);
    setAddRole(rolesNotOnStep(rows, nextStep)[0] ?? "");
    setAddType("READER");
  }

  function cancelAdd(): void {
    setAddType("READER");
    setAddRole(rolesNotOnStep(rows, addStep)[0] ?? "");
    setAddError(null);
    setAddNotice(null);
  }

  function chooseRemoveStep(nextStep: string): void {
    setRemoveStep(nextStep);
    setRemoveError(null);
    setRemoveNotice(null);
    setRemoveConfirmOpen(false);
    setRemoveRole(removableRolesOnStep(rows, nextStep)[0] ?? "");
  }

  function requestRemove(): void {
    if (!canOffer || removeBusy || !removeReady) {
      return;
    }
    setRemoveError(null);
    setRemoveNotice(null);
    setRemoveConfirmOpen(true);
  }

  function cancelRemove(): void {
    if (removeBusy) {
      return;
    }
    setRemoveConfirmOpen(false);
    setRemoveError(null);
  }

  async function confirmAdd(): Promise<void> {
    if (!canOffer || addBusy || !addReady) {
      return;
    }
    const step = addStep.trim();
    const role = addRole.trim();
    const requested = normalizeAssignmentType(addType);
    setAddBusy(true);
    setAddError(null);
    setAddNotice(null);
    try {
      await addStepRole(workflowName, step, {
        roleName: role,
        assignmentType: requested,
      });
    } catch (err: unknown) {
      setAddError(panelErrMsg(err, addRoleErrorFallback(err)));
      setAddBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyAddedRoleAfterReload(rows, reloaded, step, role, requested);
      if (!applied.accepted) {
        setAddError(DEV_MSG.WF_ROLE_ADD_ERROR);
        return;
      }
      setRows(applied.rows);
      setAddNotice(DEV_MSG.WF_ROLE_ADD_SAVED);
      const nextRole = rolesNotOnStep(applied.rows, step)[0] ?? "";
      setAddRole(nextRole);
      setAddType("READER");
    } catch (err: unknown) {
      setAddError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
    } finally {
      setAddBusy(false);
    }
  }

  async function confirmRemove(): Promise<void> {
    if (!canOffer || removeBusy || !removeReady) {
      return;
    }
    const step = removeStep.trim();
    const role = removeRole.trim();
    setRemoveBusy(true);
    setRemoveError(null);
    setRemoveNotice(null);
    try {
      await removeStepRole(workflowName, step, role);
    } catch (err: unknown) {
      setRemoveError(panelErrMsg(err, removeRoleErrorFallback(err)));
      setRemoveConfirmOpen(false);
      setRemoveBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyRemovedRoleAfterReload(rows, reloaded, step, role);
      if (!applied.accepted) {
        setRemoveError(DEV_MSG.WF_ROLE_REMOVE_ERROR);
        setRemoveConfirmOpen(false);
        return;
      }
      setRows(applied.rows);
      setRemoveNotice(DEV_MSG.WF_ROLE_REMOVE_SAVED);
      setRemoveConfirmOpen(false);
      const still = removableRolesOnStep(applied.rows, step);
      if (still.length > 0) {
        setRemoveRole(still[0]);
      } else {
        const nextStep = removableStepNames(applied.rows)[0] ?? "";
        setRemoveStep(nextStep);
        setRemoveRole(removableRolesOnStep(applied.rows, nextStep)[0] ?? "");
      }
    } catch (err: unknown) {
      setRemoveError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
      setRemoveConfirmOpen(false);
    } finally {
      setRemoveBusy(false);
    }
  }

  async function confirmNotify(): Promise<void> {
    if (!canOffer || notifyBusy || !notifyReady) {
      return;
    }
    const step = notifyStep.trim();
    const role = notifyRole.trim();
    const requested = nextNotify;
    setNotifyBusy(true);
    setNotifyError(null);
    setNotifyNotice(null);
    try {
      await setStepRoleNotify(workflowName, step, {
        roleName: role,
        notify: requested,
      });
    } catch (err: unknown) {
      setNotifyError(panelErrMsg(err, notifyErrorFallback(err)));
      setNotifyBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyNotifyAfterReload(rows, reloaded, step, role, requested);
      if (!applied.accepted) {
        setNotifyError(DEV_MSG.WF_ROLE_NOTIFY_ERROR);
        return;
      }
      setRows(applied.rows);
      setNextNotify(requested);
      setNotifyNotice(DEV_MSG.WF_ROLE_NOTIFY_SAVED);
    } catch (err: unknown) {
      setNotifyError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
    } finally {
      setNotifyBusy(false);
    }
  }

  async function confirmInbox(): Promise<void> {
    if (!canOffer || inboxBusy || !inboxReady) {
      return;
    }
    const step = inboxStep.trim();
    const role = inboxRole.trim();
    const requested = nextInbox;
    setInboxBusy(true);
    setInboxError(null);
    setInboxNotice(null);
    try {
      await setStepRoleInbox(workflowName, step, {
        roleName: role,
        inbox: requested,
      });
    } catch (err: unknown) {
      setInboxError(panelErrMsg(err, inboxErrorFallback(err)));
      setInboxBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyInboxAfterReload(rows, reloaded, step, role, requested);
      if (!applied.accepted) {
        setInboxError(DEV_MSG.WF_ROLE_INBOX_ERROR);
        return;
      }
      setRows(applied.rows);
      setNextInbox(requested);
      setInboxNotice(DEV_MSG.WF_ROLE_INBOX_SAVED);
    } catch (err: unknown) {
      setInboxError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
    } finally {
      setInboxBusy(false);
    }
  }

  async function confirmAdhoc(): Promise<void> {
    if (!canOffer || adhocBusy || !adhocReady) {
      return;
    }
    const step = adhocStep.trim();
    const role = adhocRole.trim();
    const requested = normalizeAdhocType(nextAdhoc);
    setAdhocBusy(true);
    setAdhocError(null);
    setAdhocNotice(null);
    try {
      await setStepRoleAdhoc(workflowName, step, {
        roleName: role,
        adhocType: requested,
      });
    } catch (err: unknown) {
      setAdhocError(panelErrMsg(err, adhocErrorFallback(err)));
      setAdhocBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyAdhocAfterReload(rows, reloaded, step, role, requested);
      if (!applied.accepted) {
        setAdhocError(DEV_MSG.WF_ROLE_ADHOC_ERROR);
        return;
      }
      setRows(applied.rows);
      setNextAdhoc(requested);
      setAdhocNotice(DEV_MSG.WF_ROLE_ADHOC_SAVED);
    } catch (err: unknown) {
      setAdhocError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
    } finally {
      setAdhocBusy(false);
    }
  }

  async function confirm(): Promise<void> {
    if (!canOffer || busy || !ready) {
      return;
    }
    const step = stepName.trim();
    const role = roleName.trim();
    const requested = normalizeAssignmentType(nextType);
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await setStepRoleAssignment(workflowName, step, {
        roleName: role,
        assignmentType: requested,
      });
    } catch (err: unknown) {
      setError(panelErrMsg(err, assignmentErrorFallback(err)));
      setBusy(false);
      return;
    }
    try {
      const reloaded = await listStepRoleAssignments(workflowName);
      const applied = applyAssignmentAfterReload(rows, reloaded, step, role, requested);
      if (!applied.accepted) {
        setError(DEV_MSG.WF_ROLE_ASSIGN_ERROR);
        return;
      }
      setRows(applied.rows);
      setNextType(requested);
      setNotice(DEV_MSG.WF_ROLE_ASSIGN_SAVED);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.WF_ROLE_ASSIGN_LOAD_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section data-testid="developer-wf-role-assign" style={{ marginTop: "16px" }}>
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_ASSIGN_TITLE}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
        {canOffer ? DEV_MSG.WF_ROLE_ASSIGN_HINT : DEV_MSG.WF_ROLE_ASSIGN_PACKAGED}
      </p>
      {loadError ? (
        <div
          role="alert"
          data-testid="developer-wf-role-assign-load-error"
          style={{ ...errorAlert, marginBottom: "8px" }}
        >
          {loadError}
        </div>
      ) : null}
      {rows.length === 0 ? (
        <p data-testid="developer-wf-role-assign-empty" style={{ color: catalogColors.empty }}>
          {DEV_MSG.WF_ROLE_ASSIGN_EMPTY}
        </p>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table
            data-testid="developer-wf-role-assign-table"
            style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.9rem" }}
          >
            <thead>
              <tr style={tableHeaderRow}>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_STEP}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_COL_ROLES}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_ROLE_ASSIGN_TYPE}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_ROLE_NOTIFY_FLAG}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_ROLE_INBOX_FLAG}</th>
                <th style={{ padding: "8px" }}>{DEV_MSG.WF_ROLE_ADHOC_FLAG}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, i) => (
                <tr
                  key={`${row.stepName ?? "s"}-${row.roleName ?? "r"}-${i}`}
                  data-testid={`developer-wf-role-assign-row-${i}`}
                  style={tableRow}
                >
                  <td style={{ padding: "8px" }}>{row.stepName || "—"}</td>
                  <td style={{ padding: "8px" }}>{row.roleName || "—"}</td>
                  <td
                    style={{ padding: "8px" }}
                    data-testid={`developer-wf-role-assign-type-${i}`}
                    data-assignment-type={normalizeAssignmentType(row.assignmentType)}
                  >
                    {assignmentTypeLabel(row.assignmentType) || "—"}
                  </td>
                  <td
                    style={{ padding: "8px" }}
                    data-testid={`developer-wf-role-notify-${i}`}
                    data-notify={storedNotify(row) ? "true" : "false"}
                  >
                    {storedNotify(row) ? DEV_MSG.WF_ROLE_NOTIFY_ON : DEV_MSG.WF_ROLE_NOTIFY_OFF}
                  </td>
                  <td
                    style={{ padding: "8px" }}
                    data-testid={`developer-wf-role-inbox-${i}`}
                    data-inbox={storedInbox(row) ? "true" : "false"}
                  >
                    {storedInbox(row) ? DEV_MSG.WF_ROLE_INBOX_ON : DEV_MSG.WF_ROLE_INBOX_OFF}
                  </td>
                  <td
                    style={{ padding: "8px" }}
                    data-testid={`developer-wf-role-adhoc-${i}`}
                    data-adhoc={storedAdhoc(row)}
                  >
                    {adhocLabel(row.adhocType)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {canOffer && editable.length > 0 ? (
        <div style={{ marginTop: "12px" }}>
          {error ? (
            <div
              role="alert"
              data-testid="developer-wf-role-assign-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {error}
            </div>
          ) : null}
          {notice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-assign-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {notice}
            </div>
          ) : null}
          <label htmlFor="wf-role-assign-step" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.WF_COL_STEP}
          </label>
          <select
            id="wf-role-assign-step"
            data-testid="developer-wf-role-assign-step"
            style={inputStyle}
            value={stepName}
            disabled={busy}
            onChange={(e) => chooseStep(e.target.value)}
            aria-label={DEV_MSG.WF_COL_STEP}
          >
            {steps.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <label
            htmlFor="wf-role-assign-role"
            style={{ display: "block", margin: "8px 0 4px" }}
          >
            {DEV_MSG.WF_COL_ROLES}
          </label>
          <select
            id="wf-role-assign-role"
            data-testid="developer-wf-role-assign-role"
            style={inputStyle}
            value={roleName}
            disabled={busy}
            onChange={(e) => chooseRole(e.target.value)}
            aria-label={DEV_MSG.WF_COL_ROLES}
          >
            {rolesForStep.map((row) => (
              <option key={row.roleName} value={row.roleName}>
                {row.roleName}
              </option>
            ))}
          </select>
          <label
            htmlFor="wf-role-assign-type"
            style={{ display: "block", margin: "8px 0 4px" }}
          >
            {DEV_MSG.WF_ROLE_ASSIGN_TYPE}
          </label>
          <select
            id="wf-role-assign-type"
            data-testid="developer-wf-role-assign-type"
            style={inputStyle}
            value={normalizeAssignmentType(nextType)}
            disabled={busy}
            onChange={(e) => {
              setNextType(e.target.value);
              if (error) {
                setError(null);
              }
            }}
            aria-label={DEV_MSG.WF_ROLE_ASSIGN_TYPE}
          >
            <option value="READER">{DEV_MSG.WF_ROLE_ASSIGN_READER}</option>
            <option value="ASSIGNEE">{DEV_MSG.WF_ROLE_ASSIGN_ASSIGNEE}</option>
          </select>
          <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
            <button
              type="button"
              data-testid="developer-wf-role-assign-confirm"
              disabled={busy || !ready}
              onClick={() => void confirm()}
              style={{
                background: busy || !ready ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
                cursor: busy || !ready ? "not-allowed" : "pointer",
              }}
            >
              {busy ? DEV_MSG.WF_ROLE_ASSIGN_BUSY : DEV_MSG.WF_ROLE_ASSIGN_CONFIRM}
            </button>
            <button
              type="button"
              data-testid="developer-wf-role-assign-cancel"
              disabled={busy}
              onClick={cancel}
              style={{
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
              }}
            >
              {DEV_MSG.WF_ROLE_ASSIGN_CANCEL}
            </button>
          </div>
        </div>
      ) : null}
      {canOffer && notifyRows.length > 0 ? (
        <div data-testid="developer-wf-role-notify" style={{ marginTop: "16px" }}>
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_NOTIFY_TITLE}</h3>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
            {DEV_MSG.WF_ROLE_NOTIFY_HINT}
          </p>
          {notifyError ? (
            <div
              role="alert"
              data-testid="developer-wf-role-notify-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {notifyError}
            </div>
          ) : null}
          {notifyNotice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-notify-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {notifyNotice}
            </div>
          ) : null}
          <label htmlFor="wf-role-notify-step" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.WF_COL_STEP}
          </label>
          <select
            id="wf-role-notify-step"
            data-testid="developer-wf-role-notify-step"
            style={inputStyle}
            value={notifyStep}
            disabled={notifyBusy}
            onChange={(e) => chooseNotifyStep(e.target.value)}
            aria-label={DEV_MSG.WF_COL_STEP}
          >
            {notifySteps.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-notify-role" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_COL_ROLES}
          </label>
          <select
            id="wf-role-notify-role"
            data-testid="developer-wf-role-notify-role"
            style={inputStyle}
            value={notifyRole}
            disabled={notifyBusy}
            onChange={(e) => chooseNotifyRole(e.target.value)}
            aria-label={DEV_MSG.WF_COL_ROLES}
          >
            {notifyRolesForStep.map((row) => (
              <option key={row.roleName} value={row.roleName}>
                {row.roleName}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-notify-value" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_ROLE_NOTIFY_FLAG}
          </label>
          <select
            id="wf-role-notify-value"
            data-testid="developer-wf-role-notify-value"
            style={inputStyle}
            value={notifyChoice(nextNotify)}
            disabled={notifyBusy}
            onChange={(e) => {
              setNextNotify(parseNotifyChoice(e.target.value));
              if (notifyError) {
                setNotifyError(null);
              }
            }}
            aria-label={DEV_MSG.WF_ROLE_NOTIFY_FLAG}
          >
            <option value="on">{DEV_MSG.WF_ROLE_NOTIFY_ON}</option>
            <option value="off">{DEV_MSG.WF_ROLE_NOTIFY_OFF}</option>
          </select>
          <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
            <button
              type="button"
              data-testid="developer-wf-role-notify-confirm"
              disabled={notifyBusy || !notifyReady}
              onClick={() => void confirmNotify()}
              style={{
                background:
                  notifyBusy || !notifyReady ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
                cursor: notifyBusy || !notifyReady ? "not-allowed" : "pointer",
              }}
            >
              {notifyBusy ? DEV_MSG.WF_ROLE_NOTIFY_BUSY : DEV_MSG.WF_ROLE_NOTIFY_CONFIRM}
            </button>
            <button
              type="button"
              data-testid="developer-wf-role-notify-cancel"
              disabled={notifyBusy}
              onClick={cancelNotify}
              style={{
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
              }}
            >
              {DEV_MSG.WF_ROLE_NOTIFY_CANCEL}
            </button>
          </div>
        </div>
      ) : null}
      {canOffer && inboxRows.length > 0 ? (
        <div data-testid="developer-wf-role-inbox" style={{ marginTop: "16px" }}>
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_INBOX_TITLE}</h3>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
            {DEV_MSG.WF_ROLE_INBOX_HINT}
          </p>
          {inboxError ? (
            <div
              role="alert"
              data-testid="developer-wf-role-inbox-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {inboxError}
            </div>
          ) : null}
          {inboxNotice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-inbox-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {inboxNotice}
            </div>
          ) : null}
          <label htmlFor="wf-role-inbox-step" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.WF_COL_STEP}
          </label>
          <select
            id="wf-role-inbox-step"
            data-testid="developer-wf-role-inbox-step"
            style={inputStyle}
            value={inboxStep}
            disabled={inboxBusy}
            onChange={(e) => chooseInboxStep(e.target.value)}
            aria-label={DEV_MSG.WF_COL_STEP}
          >
            {inboxSteps.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-inbox-role" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_COL_ROLES}
          </label>
          <select
            id="wf-role-inbox-role"
            data-testid="developer-wf-role-inbox-role"
            style={inputStyle}
            value={inboxRole}
            disabled={inboxBusy}
            onChange={(e) => chooseInboxRole(e.target.value)}
            aria-label={DEV_MSG.WF_COL_ROLES}
          >
            {inboxRolesForStep.map((row) => (
              <option key={row.roleName} value={row.roleName}>
                {row.roleName}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-inbox-value" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_ROLE_INBOX_FLAG}
          </label>
          <select
            id="wf-role-inbox-value"
            data-testid="developer-wf-role-inbox-value"
            style={inputStyle}
            value={inboxChoice(nextInbox)}
            disabled={inboxBusy}
            onChange={(e) => {
              setNextInbox(parseInboxChoice(e.target.value));
              if (inboxError) {
                setInboxError(null);
              }
            }}
            aria-label={DEV_MSG.WF_ROLE_INBOX_FLAG}
          >
            <option value="on">{DEV_MSG.WF_ROLE_INBOX_ON}</option>
            <option value="off">{DEV_MSG.WF_ROLE_INBOX_OFF}</option>
          </select>
          <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
            <button
              type="button"
              data-testid="developer-wf-role-inbox-confirm"
              disabled={inboxBusy || !inboxReady}
              onClick={() => void confirmInbox()}
              style={{
                background:
                  inboxBusy || !inboxReady ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
                cursor: inboxBusy || !inboxReady ? "not-allowed" : "pointer",
              }}
            >
              {inboxBusy ? DEV_MSG.WF_ROLE_INBOX_BUSY : DEV_MSG.WF_ROLE_INBOX_CONFIRM}
            </button>
            <button
              type="button"
              data-testid="developer-wf-role-inbox-cancel"
              disabled={inboxBusy}
              onClick={cancelInbox}
              style={{
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
              }}
            >
              {DEV_MSG.WF_ROLE_INBOX_CANCEL}
            </button>
          </div>
        </div>
      ) : null}
      {canOffer && adhocRows.length > 0 ? (
        <div data-testid="developer-wf-role-adhoc" style={{ marginTop: "16px" }}>
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_ADHOC_TITLE}</h3>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
            {DEV_MSG.WF_ROLE_ADHOC_HINT}
          </p>
          {adhocError ? (
            <div
              role="alert"
              data-testid="developer-wf-role-adhoc-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {adhocError}
            </div>
          ) : null}
          {adhocNotice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-adhoc-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {adhocNotice}
            </div>
          ) : null}
          <label htmlFor="wf-role-adhoc-step" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.WF_COL_STEP}
          </label>
          <select
            id="wf-role-adhoc-step"
            data-testid="developer-wf-role-adhoc-step"
            style={inputStyle}
            value={adhocStep}
            disabled={adhocBusy}
            onChange={(e) => chooseAdhocStep(e.target.value)}
            aria-label={DEV_MSG.WF_COL_STEP}
          >
            {adhocSteps.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-adhoc-role" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_COL_ROLES}
          </label>
          <select
            id="wf-role-adhoc-role"
            data-testid="developer-wf-role-adhoc-role"
            style={inputStyle}
            value={adhocRole}
            disabled={adhocBusy}
            onChange={(e) => chooseAdhocRole(e.target.value)}
            aria-label={DEV_MSG.WF_COL_ROLES}
          >
            {adhocRolesForStep.map((row) => (
              <option key={row.roleName} value={row.roleName}>
                {row.roleName}
              </option>
            ))}
          </select>
          <label htmlFor="wf-role-adhoc-value" style={{ display: "block", margin: "8px 0 4px" }}>
            {DEV_MSG.WF_ROLE_ADHOC_FLAG}
          </label>
          <select
            id="wf-role-adhoc-value"
            data-testid="developer-wf-role-adhoc-value"
            style={inputStyle}
            value={normalizeAdhocType(nextAdhoc)}
            disabled={adhocBusy}
            onChange={(e) => {
              setNextAdhoc(normalizeAdhocType(e.target.value));
              if (adhocError) {
                setAdhocError(null);
              }
            }}
            aria-label={DEV_MSG.WF_ROLE_ADHOC_FLAG}
          >
            {ADHOC_TYPES.map((type) => (
              <option key={type} value={type}>
                {adhocLabel(type)}
              </option>
            ))}
          </select>
          <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
            <button
              type="button"
              data-testid="developer-wf-role-adhoc-confirm"
              disabled={adhocBusy || !adhocReady}
              onClick={() => void confirmAdhoc()}
              style={{
                background:
                  adhocBusy || !adhocReady ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
                cursor: adhocBusy || !adhocReady ? "not-allowed" : "pointer",
              }}
            >
              {adhocBusy ? DEV_MSG.WF_ROLE_ADHOC_BUSY : DEV_MSG.WF_ROLE_ADHOC_CONFIRM}
            </button>
            <button
              type="button"
              data-testid="developer-wf-role-adhoc-cancel"
              disabled={adhocBusy}
              onClick={cancelAdhoc}
              style={{
                background: "transparent",
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "8px 14px",
                font: "inherit",
              }}
            >
              {DEV_MSG.WF_ROLE_ADHOC_CANCEL}
            </button>
          </div>
        </div>
      ) : null}
      {canOffer && addSteps.length > 0 ? (
        <div data-testid="developer-wf-role-add" style={{ marginTop: "16px" }}>
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_ADD_TITLE}</h3>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
            {DEV_MSG.WF_ROLE_ADD_HINT}
          </p>
          {addError ? (
            <div
              role="alert"
              data-testid="developer-wf-role-add-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {addError}
            </div>
          ) : null}
          {addNotice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-add-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {addNotice}
            </div>
          ) : null}
          <label htmlFor="wf-role-add-step" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.WF_COL_STEP}
          </label>
          <select
            id="wf-role-add-step"
            data-testid="developer-wf-role-add-step"
            style={inputStyle}
            value={addStep}
            disabled={addBusy}
            onChange={(e) => chooseAddStep(e.target.value)}
            aria-label={DEV_MSG.WF_COL_STEP}
          >
            {addSteps.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          {rolesToAdd.length === 0 ? (
            <p data-testid="developer-wf-role-add-empty" style={{ color: catalogColors.empty }}>
              {DEV_MSG.WF_ROLE_ADD_EMPTY}
            </p>
          ) : (
            <>
              <label htmlFor="wf-role-add-role" style={{ display: "block", margin: "8px 0 4px" }}>
                {DEV_MSG.WF_ROLE_ADD_ROLE}
              </label>
              <select
                id="wf-role-add-role"
                data-testid="developer-wf-role-add-role"
                style={inputStyle}
                value={addRole}
                disabled={addBusy}
                onChange={(e) => {
                  setAddRole(e.target.value);
                  setAddError(null);
                  setAddNotice(null);
                }}
                aria-label={DEV_MSG.WF_ROLE_ADD_ROLE}
              >
                {rolesToAdd.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
              <label htmlFor="wf-role-add-type" style={{ display: "block", margin: "8px 0 4px" }}>
                {DEV_MSG.WF_ROLE_ASSIGN_TYPE}
              </label>
              <select
                id="wf-role-add-type"
                data-testid="developer-wf-role-add-type"
                style={inputStyle}
                value={normalizeAssignmentType(addType) || "READER"}
                disabled={addBusy}
                onChange={(e) => {
                  setAddType(e.target.value);
                  if (addError) {
                    setAddError(null);
                  }
                }}
                aria-label={DEV_MSG.WF_ROLE_ASSIGN_TYPE}
              >
                <option value="READER">{DEV_MSG.WF_ROLE_ASSIGN_READER}</option>
                <option value="ASSIGNEE">{DEV_MSG.WF_ROLE_ASSIGN_ASSIGNEE}</option>
              </select>
              <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-wf-role-add-confirm"
                  disabled={addBusy || !addReady}
                  onClick={() => void confirmAdd()}
                  style={{
                    background: addBusy || !addReady ? catalogColors.disabled : catalogColors.accent,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    padding: "8px 14px",
                    font: "inherit",
                    cursor: addBusy || !addReady ? "not-allowed" : "pointer",
                  }}
                >
                  {addBusy ? DEV_MSG.WF_ROLE_ADD_BUSY : DEV_MSG.WF_ROLE_ADD_CONFIRM}
                </button>
                <button
                  type="button"
                  data-testid="developer-wf-role-add-cancel"
                  disabled={addBusy}
                  onClick={cancelAdd}
                  style={{
                    background: "transparent",
                    border: `1px solid ${catalogColors.softBorder}`,
                    borderRadius: "4px",
                    padding: "8px 14px",
                    font: "inherit",
                  }}
                >
                  {DEV_MSG.WF_ROLE_ADD_CANCEL}
                </button>
              </div>
            </>
          )}
        </div>
      ) : null}
      {canOffer && rows.length > 0 ? (
        <div data-testid="developer-wf-role-remove" style={{ marginTop: "16px" }}>
          <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.WF_ROLE_REMOVE_TITLE}</h3>
          <p style={{ color: catalogColors.muted, fontSize: "0.9rem", margin: "0 0 8px" }}>
            {DEV_MSG.WF_ROLE_REMOVE_HINT}
          </p>
          {removeError ? (
            <div
              role="alert"
              data-testid="developer-wf-role-remove-error"
              style={{ ...errorAlert, marginBottom: "8px" }}
            >
              {removeError}
            </div>
          ) : null}
          {removeNotice ? (
            <div
              role="status"
              aria-live="polite"
              data-testid="developer-wf-role-remove-notice"
              style={{ color: catalogColors.accent, marginBottom: "8px" }}
            >
              {removeNotice}
            </div>
          ) : null}
          {removeSteps.length === 0 ? (
            <p data-testid="developer-wf-role-remove-empty" style={{ color: catalogColors.empty }}>
              {DEV_MSG.WF_ROLE_REMOVE_EMPTY}
            </p>
          ) : (
            <>
              <label htmlFor="wf-role-remove-step" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.WF_COL_STEP}
              </label>
              <select
                id="wf-role-remove-step"
                data-testid="developer-wf-role-remove-step"
                style={inputStyle}
                value={removeStep}
                disabled={removeBusy}
                onChange={(e) => chooseRemoveStep(e.target.value)}
                aria-label={DEV_MSG.WF_COL_STEP}
              >
                {removeSteps.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
              <label
                htmlFor="wf-role-remove-role"
                style={{ display: "block", margin: "8px 0 4px" }}
              >
                {DEV_MSG.WF_ROLE_REMOVE_ROLE}
              </label>
              <select
                id="wf-role-remove-role"
                data-testid="developer-wf-role-remove-role"
                style={inputStyle}
                value={removeRole}
                disabled={removeBusy}
                onChange={(e) => {
                  setRemoveRole(e.target.value);
                  setRemoveError(null);
                  setRemoveNotice(null);
                  setRemoveConfirmOpen(false);
                }}
                aria-label={DEV_MSG.WF_ROLE_REMOVE_ROLE}
              >
                {rolesToRemove.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
              <div style={{ marginTop: "8px" }}>
                <button
                  type="button"
                  data-testid="developer-wf-role-remove-request"
                  disabled={removeBusy || !removeReady}
                  onClick={requestRemove}
                  style={{
                    background:
                      removeBusy || !removeReady ? catalogColors.disabled : catalogColors.error,
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    padding: "8px 14px",
                    font: "inherit",
                    cursor: removeBusy || !removeReady ? "not-allowed" : "pointer",
                  }}
                >
                  {removeBusy ? DEV_MSG.WF_ROLE_REMOVE_BUSY : DEV_MSG.WF_ROLE_REMOVE_REQUEST}
                </button>
              </div>
            </>
          )}
          <CatalogConfirmDialog
            open={removeConfirmOpen}
            busy={removeBusy}
            message={`${DEV_MSG.WF_ROLE_REMOVE_CONFIRM} ${removeStep.trim()} / ${removeRole.trim()}`}
            onCancel={cancelRemove}
            onConfirm={() => {
              void confirmRemove();
            }}
          />
        </div>
      ) : null}
    </section>
  );
}
