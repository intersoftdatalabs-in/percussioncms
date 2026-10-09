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

import { formatApiError, isApiError } from "../api/client";
import type { SitePropertyDto } from "../api/publishing/designApi";
import { message, MSG } from "../i18n/message";
import {
  CONTEXT_VARIABLE_EXISTS,
  CONTEXT_VARIABLE_NAME_MAX_LENGTH,
  CONTEXT_VARIABLE_NAME_REQUIRED,
  CONTEXT_VARIABLE_NAME_TOO_LONG,
  contextVariableNameListed,
} from "./contextVariable";

export const CONTEXT_VARIABLE_NOT_LISTED = "Context variable is not listed";

export type ContextVariableRenameResult =
  | { ok: true; name: string; newName: string; value: string }
  | { ok: false; error: string };

/**
 * A blank, overlong, or duplicate new name does not write. The stored name
 * finds the row. The value comes from that row and is not changed by this
 * check. The same name, after trim, is not a duplicate of itself.
 */
export function validateContextVariableRename(
  rawName: string,
  rawNewName: string,
  rows: SitePropertyDto[],
): ContextVariableRenameResult {
  const name = rawName.trim();
  const newName = rawNewName.trim();
  if (name.length === 0 || newName.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_REQUIRED };
  }
  if (
    name.length > CONTEXT_VARIABLE_NAME_MAX_LENGTH ||
    newName.length > CONTEXT_VARIABLE_NAME_MAX_LENGTH
  ) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_TOO_LONG };
  }
  if (!contextVariableNameListed(rows, name)) {
    return { ok: false, error: CONTEXT_VARIABLE_NOT_LISTED };
  }
  const taken = rows.some((row) => {
    const stored = (row.name ?? "").trim();
    return stored === newName && stored !== name;
  });
  if (taken) {
    return { ok: false, error: CONTEXT_VARIABLE_EXISTS };
  }
  const match = rows.find((row) => (row.name ?? "").trim() === name);
  return { ok: true, name, newName, value: match?.value ?? "" };
}

/**
 * Rename-one site-property body. {@code renameName} replaces this one name and
 * does not create a variable or change its value. The value is omitted so a
 * draft cannot be written. Other variables are not in the body.
 */
export function buildContextVariableRenameBody(
  name: string,
  contextId: string,
  newName: string,
): SitePropertyDto {
  return {
    name,
    contextId,
    newName,
    renameName: true,
  };
}

/**
 * Map context-variable rename failures to operator-visible text. HTTP 400 →
 * invalid name; HTTP 403 → forbidden; HTTP 409 → duplicate or not listed, so
 * the old name stays. Plain {@link ApiError} objects are not {@code Error}
 * instances — do not use {@code e.message}.
 */
export function mapContextVariableRenameError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, CONTEXT_VARIABLE_EXISTS);
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

function shownValue(row: SitePropertyDto, fallback: string): string {
  const value = row.value ?? "";
  return value.length > 0 ? value : fallback;
}

/**
 * List to show after a rename succeeds. Prefer the refreshed rows, then
 * replace that variable's name when a refresh still has the previous name or
 * omits the row. The value stays. Other variables stay. Do not call this when
 * the rename failed.
 */
export function contextVariablesAfterSuccessfulRename(
  refreshed: SitePropertyDto[] | null,
  renamed: { name: string; newName: string; value: string; contextId?: string },
  previous: SitePropertyDto[],
): SitePropertyDto[] {
  const oldName = renamed.name.trim();
  const newName = renamed.newName.trim();
  const value = renamed.value;
  const rows = refreshed ?? previous;
  const hasNew = rows.some((row) => (row.name ?? "").trim() === newName);
  const hasOld = rows.some((row) => (row.name ?? "").trim() === oldName);
  if (hasNew && !hasOld) {
    return rows.map((row) => {
      if ((row.name ?? "").trim() !== newName) {
        return row;
      }
      return {
        ...row,
        name: (row.name ?? "").trim() || newName,
        contextId: row.contextId ?? renamed.contextId,
        value: shownValue(row, value),
      };
    });
  }
  if (hasNew && hasOld && oldName !== newName) {
    return rows
      .filter((row) => (row.name ?? "").trim() !== oldName)
      .map((row) => {
        if ((row.name ?? "").trim() !== newName) {
          return row;
        }
        return {
          ...row,
          name: newName,
          contextId: row.contextId ?? renamed.contextId,
          value: shownValue(row, value),
        };
      });
  }
  const base = hasOld ? rows : previous;
  return base.map((row) => {
    if ((row.name ?? "").trim() !== oldName) {
      return row;
    }
    return {
      ...row,
      name: newName,
      contextId: row.contextId ?? renamed.contextId,
      value: shownValue(row, value),
    };
  });
}
