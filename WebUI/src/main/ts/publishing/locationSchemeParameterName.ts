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
export const LOCATION_SCHEME_PARAMETER_EXISTS =
  "Parameter name already exists on this scheme";
export const LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
  "Parameter is not on this scheme";

export type LocationSchemeParameterNameDraft = {
  name: string;
  newName: string;
  type: string;
  value?: string;
  sequence?: number;
};

export type LocationSchemeParameterNameResult =
  | { ok: true; parameter: LocationSchemeParameterNameDraft }
  | { ok: false; error: string };

/**
 * A blank, overlong, or duplicate new name does not write. The stored name
 * finds the row. Type, value, and sequence come from that row and are not
 * renamed by this check. The same name, after trim, is not a duplicate of
 * itself.
 */
export function validateLocationSchemeParameterName(
  nameRaw: string,
  newNameRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeParameterNameResult {
  const name = nameRaw.trim();
  const newName = newNameRaw.trim();
  if (name.length === 0 || newName.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED };
  }
  if (
    name.length > LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH ||
    newName.length > LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH
  ) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG };
  }
  const rows = existing ?? [];
  const match = rows.find((row) => (row.name ?? "").trim() === name);
  if (!match) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME };
  }
  const taken = rows.some((row) => {
    const stored = (row.name ?? "").trim();
    return stored === newName && stored !== name;
  });
  if (taken) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_EXISTS };
  }
  const type = (match.type ?? "").trim();
  const value = (match.value ?? "").trim();
  return {
    ok: true,
    parameter: {
      name,
      newName,
      type,
      ...(value.length > 0 ? { value } : {}),
      ...(match.sequence != null ? { sequence: match.sequence } : {}),
    },
  };
}

/**
 * Rename-one {@code updateScheme} body. Name, generator, description, content
 * type, template, and context are omitted so the server leaves them stored.
 * {@code updateParameterName} replaces this one parameter name and does not
 * replace the stored set. Type, value, and sequence are omitted; the server
 * keeps them and the other parameters. The scheme id is the path parameter.
 */
export function buildLocationSchemeParameterNameBody(
  parameter: LocationSchemeParameterNameDraft,
): LocationSchemeSummary {
  return {
    updateParameterName: true,
    parameters: [
      {
        name: parameter.name,
        newName: parameter.newName,
      },
    ],
  };
}

function parametersAfterName(
  row: LocationSchemeSummary,
  updated: LocationSchemeParameterNameDraft,
  known: SchemeParameter[] | undefined,
): SchemeParameter[] {
  const refreshed = row.parameters;
  const base = refreshed && refreshed.length > 0 ? refreshed : (known ?? []);
  const hasNew = base.some((rowParameter) => (rowParameter.name ?? "").trim() === updated.newName);
  const hasOld = base.some((rowParameter) => (rowParameter.name ?? "").trim() === updated.name);
  if (hasNew && !hasOld) {
    return base;
  }
  let found = false;
  const next = base.map((rowParameter) => {
    if ((rowParameter.name ?? "").trim() !== updated.name) {
      return rowParameter;
    }
    found = true;
    return { ...rowParameter, name: updated.newName };
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
  return [...next, { ...prior, name: updated.newName }];
}

/**
 * List to show after a rename succeeds. Prefer the refreshed rows, then
 * replace that parameter's name when a refresh still has the previous name or
 * omits parameters. Type, value, sequence, and the other parameters stay. Do
 * not call this when the update failed.
 */
export function schemesAfterSuccessfulParameterName(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  updated: LocationSchemeParameterNameDraft,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterName(row, updated, known) };
  });
}
