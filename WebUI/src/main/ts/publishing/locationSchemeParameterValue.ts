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

import type {
  LocationSchemeSummary,
  SchemeParameter,
} from "../api/publishing/designApi";

export const LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH = 50;

export const LOCATION_SCHEME_PARAMETER_NAME_REQUIRED = "Parameter name is required";
export const LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG =
  "Parameter name must be 50 characters or fewer";
export const LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED = "Parameter value is required";
export const LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
  "Parameter is not on this scheme";

export type LocationSchemeParameterValueDraft = {
  name: string;
  type?: string;
  value: string;
  sequence?: number;
};

export type LocationSchemeParameterValueResult =
  | { ok: true; parameter: LocationSchemeParameterValueDraft }
  | { ok: false; error: string };

/**
 * Reject a blank name, a name that is not stored, or a blank value. A blank
 * value does not clear the stored value. Nothing is written in those cases.
 * The kept type and sequence come from the stored row. The request does not
 * rename the parameter or change its type or sequence.
 */
export function validateLocationSchemeParameterValue(
  nameRaw: string,
  valueRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeParameterValueResult {
  const name = nameRaw.trim();
  const value = valueRaw.trim();
  if (name.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED };
  }
  if (name.length > LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG };
  }
  const match = (existing ?? []).find((row) => (row.name ?? "").trim() === name);
  if (!match) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME };
  }
  if (value.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED };
  }
  const type = (match.type ?? "").trim();
  return {
    ok: true,
    parameter: {
      name,
      ...(type.length > 0 ? { type } : {}),
      value,
      ...(match.sequence != null ? { sequence: match.sequence } : {}),
    },
  };
}

/**
 * Value-one {@code updateScheme} body. Name, generator, description, content
 * type, template, and context are omitted so the server leaves them stored.
 * {@code updateParameterValue} replaces this one value and does not replace
 * the stored set. Sequence is omitted. A stored type may be sent and is not
 * applied; the server keeps the stored type and sequence. The scheme id is
 * the path parameter, not a field on this body.
 */
export function buildLocationSchemeParameterValueBody(
  parameter: LocationSchemeParameterValueDraft,
): LocationSchemeSummary {
  return {
    updateParameterValue: true,
    parameters: [
      {
        name: parameter.name,
        ...(parameter.type ? { type: parameter.type } : {}),
        value: parameter.value,
      },
    ],
  };
}

function parametersAfterValue(
  row: LocationSchemeSummary,
  updated: LocationSchemeParameterValueDraft,
  known: SchemeParameter[] | undefined,
): SchemeParameter[] {
  const refreshed = row.parameters;
  const base = refreshed && refreshed.length > 0 ? refreshed : (known ?? []);
  let found = false;
  const next = base.map((rowParameter) => {
    if ((rowParameter.name ?? "").trim() !== updated.name) {
      return rowParameter;
    }
    found = true;
    return { ...rowParameter, value: updated.value };
  });
  if (found) {
    return next;
  }
  const prior = (known ?? []).find(
    (rowParameter) => (rowParameter.name ?? "").trim() === updated.name,
  );
  if (!prior) {
    return next;
  }
  return [...next, { ...prior, value: updated.value }];
}

/**
 * List to show after a value change succeeds. Prefer the refreshed rows, then
 * replace that parameter's value when a refresh still has the previous value
 * or omits parameters. Type, sequence, and the other parameters stay. Do not
 * call this when the update failed.
 */
export function schemesAfterSuccessfulParameterValue(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  updated: LocationSchemeParameterValueDraft,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterValue(row, updated, known) };
  });
}
