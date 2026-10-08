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
export const LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH = 50;

export const LOCATION_SCHEME_PARAMETER_NAME_REQUIRED = "Parameter name is required";
export const LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG =
  "Parameter name must be 50 characters or fewer";
export const LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED = "Parameter type is required";
export const LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG =
  "Parameter type must be 50 characters or fewer";
export const LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
  "Parameter is not on this scheme";

/** Types the Design form offers. A stored type outside this list stays selectable. */
export const LOCATION_SCHEME_PARAMETER_TYPE_CHOICES = ["String", "BackendColumn"] as const;

/** Select options for one stored parameter. An unknown stored type stays first. */
export function locationSchemeParameterTypeChoices(storedType: string): string[] {
  const known: string[] = [...LOCATION_SCHEME_PARAMETER_TYPE_CHOICES];
  const current = storedType.trim();
  if (current.length > 0 && !known.includes(current)) {
    return [current, ...known];
  }
  return known;
}

export type LocationSchemeParameterTypeDraft = {
  name: string;
  type: string;
  value?: string;
  sequence?: number;
};

export type LocationSchemeParameterTypeResult =
  | { ok: true; parameter: LocationSchemeParameterTypeDraft }
  | { ok: false; error: string };

/**
 * Reject a blank name, a name that is not stored, a blank type, or a type
 * longer than its column. A blank type does not clear the stored type.
 * Nothing is written in those cases. The kept value and sequence come from
 * the stored row. The request does not rename the parameter or change its
 * value or sequence.
 */
export function validateLocationSchemeParameterType(
  nameRaw: string,
  typeRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeParameterTypeResult {
  const name = nameRaw.trim();
  const type = typeRaw.trim();
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
  if (type.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED };
  }
  if (type.length > LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG };
  }
  const value = (match.value ?? "").trim();
  return {
    ok: true,
    parameter: {
      name,
      type,
      ...(value.length > 0 ? { value } : {}),
      ...(match.sequence != null ? { sequence: match.sequence } : {}),
    },
  };
}

/**
 * Type-one {@code updateScheme} body. Name, generator, description, content
 * type, template, and context are omitted so the server leaves them stored.
 * {@code updateParameterType} replaces this one type and does not replace
 * the stored set. Sequence is omitted. A stored value may be sent and is not
 * applied; the server keeps the stored value and sequence. The scheme id is
 * the path parameter, not a field on this body.
 */
export function buildLocationSchemeParameterTypeBody(
  parameter: LocationSchemeParameterTypeDraft,
): LocationSchemeSummary {
  return {
    updateParameterType: true,
    parameters: [
      {
        name: parameter.name,
        type: parameter.type,
        ...(parameter.value ? { value: parameter.value } : {}),
      },
    ],
  };
}

function parametersAfterType(
  row: LocationSchemeSummary,
  updated: LocationSchemeParameterTypeDraft,
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
    return { ...rowParameter, type: updated.type };
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
  return [...next, { ...prior, type: updated.type }];
}

/**
 * List to show after a type change succeeds. Prefer the refreshed rows, then
 * replace that parameter's type when a refresh still has the previous type
 * or omits parameters. Value, sequence, and the other parameters stay. Do not
 * call this when the update failed.
 */
export function schemesAfterSuccessfulParameterType(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  updated: LocationSchemeParameterTypeDraft,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterType(row, updated, known) };
  });
}
