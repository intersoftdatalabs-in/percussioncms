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

/** Steps that have at least one Reader or Assignee role. */
export function removableStepNames(rows: AssignmentRow[] | null | undefined): string[] {
  const mutable = (rows ?? []).filter(
    (row) =>
      !!(row.stepName ?? "").trim() &&
      !!(row.roleName ?? "").trim() &&
      isMutableAssignmentType(row.assignmentType),
  );
  return distinctTrimmed(mutable, (row) => row.stepName);
}

/** Reader or Assignee roles already assigned to {@code stepName}. Admin and None are omitted. */
export function removableRolesOnStep(
  rows: AssignmentRow[] | null | undefined,
  stepName: string,
): string[] {
  const step = stepName.trim().toLowerCase();
  if (!step) {
    return [];
  }
  return distinctTrimmed(
    (rows ?? []).filter(
      (row) =>
        (row.stepName ?? "").trim().toLowerCase() === step &&
        isMutableAssignmentType(row.assignmentType),
    ),
    (row) => row.roleName,
  );
}

/** Confirm is enabled only for a Reader or Assignee role already on the step. */
export function isRemoveRoleReady(
  rows: AssignmentRow[] | null | undefined,
  stepName: string,
  roleName: string,
): boolean {
  const step = stepName.trim();
  const role = roleName.trim();
  if (!step || !role) {
    return false;
  }
  const hit = findAssignment(rows, step, role);
  return !!hit && isMutableAssignmentType(hit.assignmentType);
}

/**
 * The table drops the role only when a reload no longer contains that step and role.
 * A failed or stale reload keeps the previous rows.
 */
export function applyRemovedRoleAfterReload(
  previous: AssignmentRow[],
  reloaded: AssignmentRow[] | null | undefined,
  stepName: string,
  roleName: string,
): { rows: AssignmentRow[]; accepted: boolean } {
  if (!reloaded) {
    return { rows: previous, accepted: false };
  }
  if (findAssignment(reloaded, stepName, roleName)) {
    return { rows: previous, accepted: false };
  }
  return { rows: reloaded, accepted: true };
}
