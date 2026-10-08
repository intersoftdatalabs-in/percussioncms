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

import type { SitePropertyDto } from "../api/publishing/designApi";

/**
 * RXASSEMBLERPROPERTIES.PROPERTYNAME is VARCHAR(50). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTEXT_VARIABLE_NAME_LENGTH}.
 */
export const CONTEXT_VARIABLE_NAME_MAX_LENGTH = 50;

/**
 * RXASSEMBLERPROPERTIES.PROPERTYVALUE is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTEXT_VARIABLE_VALUE_LENGTH}.
 */
export const CONTEXT_VARIABLE_VALUE_MAX_LENGTH = 255;

export const CONTEXT_VARIABLE_NAME_REQUIRED = "Context variable name is required";

export const CONTEXT_VARIABLE_NAME_TOO_LONG =
  "Context variable name must be 50 characters or fewer";

export const CONTEXT_VARIABLE_VALUE_REQUIRED = "Context variable value is required";

export const CONTEXT_VARIABLE_VALUE_TOO_LONG =
  "Context variable value must be 255 characters or fewer";

export const CONTEXT_VARIABLE_EXISTS = "Context variable already exists";

export type ContextVariableResult =
  | { ok: true; name: string; value: string }
  | { ok: false; error: string };

/**
 * Blank name or value is rejected and must not be written. Overlong text is
 * rejected before any create request. A blank value does not clear a stored
 * variable.
 */
export function validateContextVariable(
  rawName: string,
  rawValue: string,
): ContextVariableResult {
  const name = rawName.trim();
  const value = rawValue.trim();
  if (name.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_REQUIRED };
  }
  if (name.length > CONTEXT_VARIABLE_NAME_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_VARIABLE_NAME_TOO_LONG };
  }
  if (value.length === 0) {
    return { ok: false, error: CONTEXT_VARIABLE_VALUE_REQUIRED };
  }
  if (value.length > CONTEXT_VARIABLE_VALUE_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_VARIABLE_VALUE_TOO_LONG };
  }
  return { ok: true, name, value };
}

/** True when the trimmed name is already listed for this context. */
export function contextVariableNameListed(
  rows: SitePropertyDto[],
  name: string,
): boolean {
  const wanted = name.trim();
  return rows.some((row) => (row.name ?? "").trim() === wanted);
}

/**
 * List to show after a new context variable is saved. Prefer the refreshed
 * rows. If the refresh failed, keep every previous row and append the created
 * one. Do not call this when the create failed. The new row is not applied
 * until save succeeds. Other variables stay, including their values.
 */
export function contextVariablesAfterSuccessfulCreate(
  refreshed: SitePropertyDto[] | null,
  created: SitePropertyDto,
  previous: SitePropertyDto[],
): SitePropertyDto[] {
  if (refreshed) {
    return refreshed;
  }
  const name = (created.name ?? "").trim();
  const kept = previous.filter((row) => (row.name ?? "").trim() !== name);
  return [
    ...kept,
    {
      name,
      contextId: created.contextId,
      value: created.value ?? "",
    },
  ];
}
