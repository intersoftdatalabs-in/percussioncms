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

export interface InboxRow {
  stepName?: string;
  roleName?: string;
  assignmentType?: string;
  notify?: boolean;
  inbox?: boolean;
}

/** Stored SHOWININBOX. A missing flag is off until a reload says otherwise. */
export function storedInbox(row: InboxRow | null | undefined): boolean {
  return row?.inbox === true;
}

export function inboxChoice(on: boolean): "on" | "off" {
  return on ? "on" : "off";
}

export function parseInboxChoice(value: string | undefined | null): boolean {
  const n = (value ?? "").trim().toLowerCase();
  return n === "on" || n === "true";
}

/** Reader or Assignee only. Admin and None stay off this confirm. */
export function inboxRoleRows(rows: InboxRow[] | null | undefined): InboxRow[] {
  return (rows ?? []).filter(
    (row) =>
      !!(row.stepName ?? "").trim() &&
      !!(row.roleName ?? "").trim() &&
      isMutableAssignmentType(row.assignmentType),
  );
}

export function findInboxRow(
  rows: InboxRow[] | null | undefined,
  stepName: string,
  roleName: string,
): InboxRow | undefined {
  const step = stepName.trim().toLowerCase();
  const role = roleName.trim().toLowerCase();
  if (!step || !role) {
    return undefined;
  }
  return inboxRoleRows(rows).find(
    (row) =>
      (row.stepName ?? "").trim().toLowerCase() === step &&
      (row.roleName ?? "").trim().toLowerCase() === role,
  );
}

/** Confirm is enabled only when the pending flag differs from the stored flag. */
export function isInboxChangeReady(current: boolean, next: boolean): boolean {
  return current !== next;
}

/**
 * The displayed flag changes only when a reload contains the requested inbox
 * value for that Reader or Assignee. A failed or stale reload keeps the previous rows.
 */
export function applyInboxAfterReload(
  previous: InboxRow[],
  reloaded: InboxRow[] | null | undefined,
  stepName: string,
  roleName: string,
  requested: boolean,
): { rows: InboxRow[]; accepted: boolean } {
  if (!reloaded) {
    return { rows: previous, accepted: false };
  }
  const hit = findInboxRow(reloaded, stepName, roleName);
  if (!hit || storedInbox(hit) !== requested) {
    return { rows: previous, accepted: false };
  }
  return { rows: reloaded, accepted: true };
}
