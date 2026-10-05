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

import { isMutableAssignmentType } from "./workflowStepRoleAssignment";

export const ADHOC_TYPES = ["disabled", "enabled", "anonymous"] as const;

export type AdhocType = (typeof ADHOC_TYPES)[number];

export interface AdhocRow {
  stepName?: string;
  roleName?: string;
  assignmentType?: string;
  notify?: boolean;
  inbox?: boolean;
  adhocType?: string;
}

/** Stored adhoc type. A missing value is disabled until a reload says otherwise. */
export function storedAdhoc(row: AdhocRow | null | undefined): AdhocType {
  return normalizeAdhocType(row?.adhocType);
}

export function normalizeAdhocType(value: string | undefined | null): AdhocType {
  const n = (value ?? "").trim().toLowerCase();
  if (n === "enabled" || n === "anonymous" || n === "disabled") {
    return n;
  }
  return "disabled";
}

/** Reader or Assignee only. Admin and None stay off this confirm. */
export function adhocRoleRows(rows: AdhocRow[] | null | undefined): AdhocRow[] {
  return (rows ?? []).filter(
    (row) =>
      !!(row.stepName ?? "").trim() &&
      !!(row.roleName ?? "").trim() &&
      isMutableAssignmentType(row.assignmentType),
  );
}

export function findAdhocRow(
  rows: AdhocRow[] | null | undefined,
  stepName: string,
  roleName: string,
): AdhocRow | undefined {
  const step = stepName.trim().toLowerCase();
  const role = roleName.trim().toLowerCase();
  if (!step || !role) {
    return undefined;
  }
  return adhocRoleRows(rows).find(
    (row) =>
      (row.stepName ?? "").trim().toLowerCase() === step &&
      (row.roleName ?? "").trim().toLowerCase() === role,
  );
}

/** Confirm is enabled only when the pending type differs from the stored type. */
export function isAdhocChangeReady(current: string, next: string): boolean {
  return normalizeAdhocType(current) !== normalizeAdhocType(next);
}

/**
 * The displayed type changes only when a reload contains the requested adhoc
 * type for that Reader or Assignee. A failed or stale reload keeps the previous rows.
 */
export function applyAdhocAfterReload(
  previous: AdhocRow[],
  reloaded: AdhocRow[] | null | undefined,
  stepName: string,
  roleName: string,
  requested: string,
): { rows: AdhocRow[]; accepted: boolean } {
  if (!reloaded) {
    return { rows: previous, accepted: false };
  }
  const hit = findAdhocRow(reloaded, stepName, roleName);
  if (!hit || storedAdhoc(hit) !== normalizeAdhocType(requested)) {
    return { rows: previous, accepted: false };
  }
  return { rows: reloaded, accepted: true };
}
