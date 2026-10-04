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

import {
  findAssignment,
  isMutableAssignmentType,
  normalizeAssignmentType,
  type AssignmentRow,
} from "./workflowStepRoleAssignment";

function distinctTrimmed(
  rows: AssignmentRow[] | null | undefined,
  read: (row: AssignmentRow) => string | undefined,
): string[] {
  const names: string[] = [];
  for (const row of rows ?? []) {
    const name = (read(row) ?? "").trim();
    if (!name) {
      continue;
    }
    if (!names.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
      names.push(name);
    }
  }
  return names;
}

/** Step names that already have at least one assigned role. */
export function assignmentStepNames(rows: AssignmentRow[] | null | undefined): string[] {
  return distinctTrimmed(rows, (row) => row.stepName);
}

/** Workflow roles that appear on some step but not on {@code stepName}. */
export function rolesNotOnStep(
  rows: AssignmentRow[] | null | undefined,
  stepName: string,
): string[] {
  const step = stepName.trim();
  if (!step) {
    return [];
  }
  return distinctTrimmed(rows, (row) => row.roleName).filter(
    (role) => !findAssignment(rows, step, role),
  );
}

/** First step that still has a workflow role which is not assigned to it. */
export function firstStepWithRoleToAdd(rows: AssignmentRow[] | null | undefined): string {
  const steps = assignmentStepNames(rows);
  return steps.find((step) => rolesNotOnStep(rows, step).length > 0) ?? steps[0] ?? "";
}

/** Confirm is enabled only for Reader or Assignee on a role that is not already on the step. */
export function isAddRoleReady(
  rows: AssignmentRow[] | null | undefined,
  stepName: string,
  roleName: string,
  assignmentType: string | undefined | null,
): boolean {
  const step = stepName.trim();
  const role = roleName.trim();
  if (!step || !role || !isMutableAssignmentType(assignmentType)) {
    return false;
  }
  return !findAssignment(rows, step, role);
}

/**
 * The table gains the role only when a reload contains that step, role, and type.
 * A failed or stale reload keeps the previous rows, so the role is not shown.
 */
export function applyAddedRoleAfterReload(
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
