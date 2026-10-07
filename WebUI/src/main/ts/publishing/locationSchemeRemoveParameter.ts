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

export const LOCATION_SCHEME_PARAMETER_NAME_REQUIRED = "Parameter name is required";
export const LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
  "Parameter is not on this scheme";

export type LocationSchemeRemoveParameterDraft = {
  name: string;
  type?: string;
  value?: string;
};

export type LocationSchemeRemoveParameterResult =
  | { ok: true; parameter: LocationSchemeRemoveParameterDraft }
  | { ok: false; error: string };

/**
 * Reject a blank name or a name that is not already stored. Nothing is written
 * in those cases. The kept type and value are copied from the stored row so the
 * request identifies that parameter and does not invent a replacement.
 */
export function validateLocationSchemeRemoveParameter(
  nameRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeRemoveParameterResult {
  const name = nameRaw.trim();
  if (name.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED };
  }
  const match = (existing ?? []).find((row) => (row.name ?? "").trim() === name);
  if (!match) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME };
  }
  const type = (match.type ?? "").trim();
  return {
    ok: true,
    parameter: {
      name,
      ...(type.length > 0 ? { type } : {}),
      value: match.value ?? "",
    },
  };
}

/**
 * Remove-one {@code updateScheme} body. Name, generator, description, content
 * type, template, and context are omitted so the server leaves them stored.
 * {@code removeParameter} drops this one name and does not replace the stored
 * set. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeRemoveParameterBody(
  parameter: LocationSchemeRemoveParameterDraft,
): LocationSchemeSummary {
  return {
    removeParameter: true,
    parameters: [
      {
        name: parameter.name,
        ...(parameter.type ? { type: parameter.type } : {}),
        value: parameter.value ?? "",
      },
    ],
  };
}

function parametersAfterRemove(
  row: LocationSchemeSummary,
  removedName: string,
  known: SchemeParameter[] | undefined,
): SchemeParameter[] {
  const base = row.parameters ?? known ?? [];
  return base.filter((rowParameter) => (rowParameter.name ?? "").trim() !== removedName);
}

/**
 * List to show after a remove succeeds. Prefer the refreshed rows, then drop
 * the removed name when a refresh still lists it or omits parameters. Do not
 * call this when the update failed. An empty parameter list does not drop the
 * scheme row.
 */
export function schemesAfterSuccessfulRemove(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  removedName: string,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterRemove(row, removedName, known) };
  });
}
