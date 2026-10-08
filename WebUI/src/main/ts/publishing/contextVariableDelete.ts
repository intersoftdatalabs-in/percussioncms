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
  CONTEXT_VARIABLE_NAME_MAX_LENGTH,
  CONTEXT_VARIABLE_NAME_REQUIRED,
  CONTEXT_VARIABLE_NAME_TOO_LONG,
  contextVariableNameListed,
} from "./contextVariable";

export const CONTEXT_VARIABLE_NOT_LISTED = "Context variable is not listed";

export type ContextVariableDeleteResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/**
 * Reject a blank name, an overlong name, or a name that is not already listed.
 * Nothing is deleted in those cases. The trimmed listed name is the only name
 * the delete request may send.
 */
export function validateContextVariableDelete(
  rawName: string,
  rows: SitePropertyDto[],
): ContextVariableDeleteResult {
  const name = rawName.trim();
  if (name.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_REQUIRED };
  }
  if (name.length > CONTEXT_VARIABLE_NAME_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_TOO_LONG };
  }
  if (!contextVariableNameListed(rows, name)) {
    return { ok: false, error: CONTEXT_VARIABLE_NOT_LISTED };
  }
  return { ok: true, name };
}

/**
 * Map context-variable delete failures to operator-visible text. HTTP 400 →
 * invalid name or context; HTTP 403 → forbidden; HTTP 409 → the name is not
 * listed, so another variable was not removed. Plain {@link ApiError} objects
 * are not {@code Error} instances — do not use {@code e.message}.
 */
export function mapContextVariableDeleteError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, CONTEXT_VARIABLE_NOT_LISTED);
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

/**
 * List to show after a delete succeeds. Prefer the refreshed rows, then drop
 * the removed name when a refresh still lists it. Other variables stay. Do not
 * call this when the delete failed. A failed refresh keeps every previous row
 * except the removed name.
 */
export function contextVariablesAfterSuccessfulDelete(
  refreshed: SitePropertyDto[] | null,
  removedName: string,
  previous: SitePropertyDto[],
): SitePropertyDto[] {
  const name = removedName.trim();
  const rows = refreshed ?? previous;
  return rows.filter((row) => (row.name ?? "").trim() !== name);
}
