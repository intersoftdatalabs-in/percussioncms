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

export interface NotifyRow {
  stepName?: string;
  roleName?: string;
  assignmentType?: string;
  notify?: boolean;
}

/** Stored ISNOTIFYON. A missing flag is off until a reload says otherwise. */
export function storedNotify(row: NotifyRow | null | undefined): boolean {
  return row?.notify === true;
}

export function notifyChoice(on: boolean): "on" | "off" {
  return on ? "on" : "off";
}

export function parseNotifyChoice(value: string | undefined | null): boolean {
  const n = (value ?? "").trim().toLowerCase();
  return n === "on" || n === "true";
}

export function namedRoleRows(rows: NotifyRow[] | null | undefined): NotifyRow[] {
  return (rows ?? []).filter(
    (row) => !!(row.stepName ?? "").trim() && !!(row.roleName ?? "").trim(),
  );
}

export function findNotifyRow(
  rows: NotifyRow[] | null | undefined,
  stepName: string,
  roleName: string,
): NotifyRow | undefined {
  const step = stepName.trim().toLowerCase();
  const role = roleName.trim().toLowerCase();
  if (!step || !role) {
    return undefined;
  }
  return namedRoleRows(rows).find(
    (row) =>
      (row.stepName ?? "").trim().toLowerCase() === step &&
      (row.roleName ?? "").trim().toLowerCase() === role,
  );
}

/** Confirm is enabled only when the pending flag differs from the stored flag. */
export function isNotifyChangeReady(current: boolean, next: boolean): boolean {
  return current !== next;
}

/**
 * The displayed flag changes only when a reload contains the requested notify
 * value for that step and role. A failed or stale reload keeps the previous rows.
 */
export function applyNotifyAfterReload(
  previous: NotifyRow[],
  reloaded: NotifyRow[] | null | undefined,
  stepName: string,
  roleName: string,
  requested: boolean,
): { rows: NotifyRow[]; accepted: boolean } {
  if (!reloaded) {
    return { rows: previous, accepted: false };
  }
  const hit = findNotifyRow(reloaded, stepName, roleName);
  if (!hit || storedNotify(hit) !== requested) {
    return { rows: previous, accepted: false };
  }
  return { rows: reloaded, accepted: true };
}
