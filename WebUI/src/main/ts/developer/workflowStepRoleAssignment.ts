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

import { canOfferWorkflowRename } from "./workflowRename";

export interface AssignmentRow {
  stepName?: string;
  roleName?: string;
  assignmentType?: string;
}

/** Reader or Assignee can be chosen here. Admin and None stay on the workflow-admin editor. */
export function normalizeAssignmentType(value: string | undefined | null): string {
  return (value ?? "").trim().toUpperCase();
}

export function isMutableAssignmentType(value: string | undefined | null): boolean {
  const n = normalizeAssignmentType(value);
  return n === "READER" || n === "ASSIGNEE";
}

export function assignmentTypeLabel(value: string | undefined | null): string {
  const n = normalizeAssignmentType(value);
  if (n === "READER") {
    return "Reader";
  }
  if (n === "ASSIGNEE") {
    return "Assignee";
  }
  if (n === "ADMIN") {
    return "Admin";
  }
  if (n === "NONE") {
    return "None";
  }
  return (value ?? "").trim();
}

/**
 * Assignment-type edits are offered only for a custom workflow that is not the
 * system default. Packaged names and the current default must not call the API.
 */
export function canOfferStepRoleAssignment(opts: {
  name: string | undefined | null;
  defaultWorkflow?: boolean;
}): boolean {
  return canOfferWorkflowRename(opts);
}

export function findAssignment(
  rows: AssignmentRow[] | null | undefined,
  stepName: string,
  roleName: string,
): AssignmentRow | undefined {
  const step = stepName.trim().toLowerCase();
  const role = roleName.trim().toLowerCase();
  if (!step || !role) {
    return undefined;
  }
  return (rows ?? []).find(
    (row) =>
      (row.stepName ?? "").trim().toLowerCase() === step &&
      (row.roleName ?? "").trim().toLowerCase() === role,
  );
}

export function mutableAssignments(rows: AssignmentRow[] | null | undefined): AssignmentRow[] {
  return (rows ?? []).filter(
    (row) =>
      !!(row.stepName ?? "").trim() &&
      !!(row.roleName ?? "").trim() &&
      isMutableAssignmentType(row.assignmentType),
  );
}

/** Confirm is enabled only when the next type is Reader or Assignee and differs. */
export function isAssignmentChangeReady(
  currentType: string | undefined | null,
  nextType: string | undefined | null,
): boolean {
  if (!isMutableAssignmentType(currentType) || !isMutableAssignmentType(nextType)) {
    return false;
  }
  return normalizeAssignmentType(currentType) !== normalizeAssignmentType(nextType);
}

/**
 * The displayed type changes only when a reload contains the requested type for
 * that step and role. A failed or stale reload keeps the previous rows.
 */
export function applyAssignmentAfterReload(
  previous: AssignmentRow[],
  reloaded: AssignmentRow[] | null | undefined,
  stepName: string,
  roleName: string,
  requestedType: string,
): { rows: AssignmentRow[]; accepted: boolean } {
  if (!reloaded) {
    return { rows: previous, accepted: false };
  }
  const hit = findAssignment(reloaded, stepName, roleName);
  if (
    !hit ||
    normalizeAssignmentType(hit.assignmentType) !== normalizeAssignmentType(requestedType)
  ) {
    return { rows: previous, accepted: false };
  }
  return { rows: reloaded, accepted: true };
}
