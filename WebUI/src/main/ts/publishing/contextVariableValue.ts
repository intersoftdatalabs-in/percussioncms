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
  CONTEXT_VARIABLE_VALUE_MAX_LENGTH,
  CONTEXT_VARIABLE_VALUE_REQUIRED,
  CONTEXT_VARIABLE_VALUE_TOO_LONG,
  contextVariableNameListed,
  type ContextVariableResult,
} from "./contextVariable";

export const CONTEXT_VARIABLE_NOT_LISTED = "Context variable is not listed";

/**
 * Reject a blank value, an overlong value, or a name that is not already
 * listed. A blank value does not clear the stored variable. The name is not
 * renamed. Nothing is written in those cases.
 */
export function validateContextVariableValue(
  rawName: string,
  rawValue: string,
  rows: SitePropertyDto[],
): ContextVariableResult {
  const name = rawName.trim();
  const value = rawValue.trim();
  if (name.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_REQUIRED };
  }
  if (name.length > CONTEXT_VARIABLE_NAME_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_TOO_LONG };
  }
  if (!contextVariableNameListed(rows, name)) {
    return { ok: false, error: CONTEXT_VARIABLE_NOT_LISTED };
  }
  if (value.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_VALUE_REQUIRED };
  }
  if (value.length > CONTEXT_VARIABLE_VALUE_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_VARIABLE_VALUE_TOO_LONG };
  }
  return { ok: true, name, value };
}

/**
 * Value-one site-property body. {@code updateValue} replaces this one value
 * and does not create a name. The same name is sent. Other variables are not
 * in the body.
 */
export function buildContextVariableValueBody(
  name: string,
  contextId: string,
  value: string,
): SitePropertyDto {
  return {
    name,
    contextId,
    value,
    updateValue: true,
  };
}

/**
 * Map context-variable value failures to operator-visible text. HTTP 400 →
 * invalid value; HTTP 403 → forbidden; HTTP 409 → the name is not listed, so
 * the request did not create it. Plain {@link ApiError} objects are not
 * {@code Error} instances — do not use {@code e.message}.
 */
export function mapContextVariableValueSaveError(err: unknown): string {
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
 * List to show after a value change succeeds. Prefer the refreshed rows, then
 * replace that variable's value when a refresh still has the previous value or
 * omits the row. The name stays. Other variables stay. Do not call this when
 * the update failed.
 */
export function contextVariablesAfterSuccessfulValueChange(
  refreshed: SitePropertyDto[] | null,
  updated: SitePropertyDto,
  previous: SitePropertyDto[],
): SitePropertyDto[] {
  const name = (updated.name ?? "").trim();
  const value = updated.value ?? "";
  const rows = refreshed ?? previous;
  let found = false;
  const next = rows.map((row) => {
    if ((row.name ?? "").trim() !== name) {
      return row;
    }
    found = true;
    return {
      ...row,
      name: (row.name ?? "").trim() || name,
      contextId: row.contextId ?? updated.contextId,
      value,
    };
  });
  if (found) {
    return next;
  }
  return previous.map((row) => {
    if ((row.name ?? "").trim() !== name) {
      return row;
    }
    return { ...row, value };
  });
}
