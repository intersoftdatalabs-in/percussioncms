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

/**
 * RXLOCATIONSCHEMEPARAMS.NAME and TYPE are VARCHAR(50). Keep in step with
 * {@code PSPublishingDesignRestService} parameter length constants.
 */
export const LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH = 50;
export const LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH = 50;

export const LOCATION_SCHEME_PARAMETER_NAME_REQUIRED = "Parameter name is required";
export const LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED = "Parameter value is required";
export const LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED = "Parameter type is required";
export const LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG =
  "Parameter name must be 50 characters or fewer";
export const LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG =
  "Parameter type must be 50 characters or fewer";
export const LOCATION_SCHEME_PARAMETER_EXISTS =
  "Parameter name already exists on this scheme";

export type LocationSchemeParameterDraft = {
  name: string;
  type: string;
  value: string;
};

export type LocationSchemeAddParameterResult =
  | { ok: true; parameter: LocationSchemeParameterDraft }
  | { ok: false; error: string };

/**
 * Reject a blank name, type, or value, an overlong name or type, or a name
 * already stored on the scheme. Nothing is written in those cases.
 */
export function validateLocationSchemeAddParameter(
  nameRaw: string,
  typeRaw: string,
  valueRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeAddParameterResult {
  const name = nameRaw.trim();
  const type = typeRaw.trim();
  const value = valueRaw.trim();
  if (name.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_REQUIRED };
  }
  if (name.length > LOCATION_SCHEME_PARAMETER_NAME_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_NAME_TOO_LONG };
  }
  if (type.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_TYPE_REQUIRED };
  }
  if (type.length > LOCATION_SCHEME_PARAMETER_TYPE_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_TYPE_TOO_LONG };
  }
  if (value.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_VALUE_REQUIRED };
  }
  const duplicate = (existing ?? []).some(
    (row) => (row.name ?? "").trim() === name,
  );
  if (duplicate) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_EXISTS };
  }
  return { ok: true, parameter: { name, type, value } };
}

/**
 * Add-one {@code updateScheme} body. Name, generator, description, content
 * type, template, and context are omitted so the server leaves them stored.
 * {@code addParameter} appends this one parameter and does not replace the
 * stored set. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeAddParameterBody(
  parameter: LocationSchemeParameterDraft,
): LocationSchemeSummary {
  return {
    addParameter: true,
    parameters: [
      { name: parameter.name, type: parameter.type, value: parameter.value },
    ],
  };
}

function parametersAfterAdd(
  row: LocationSchemeSummary,
  added: LocationSchemeParameterDraft,
  known: SchemeParameter[] | undefined,
): SchemeParameter[] {
  const base = row.parameters ?? known ?? [];
  if (base.some((rowParameter) => (rowParameter.name ?? "") === added.name)) {
    return base;
  }
  const sequence = base.reduce((max, rowParameter) => {
    const next = (rowParameter.sequence ?? -1) + 1;
    return next > max ? next : max;
  }, 0);
  return [...base, { ...added, sequence }];
}

/**
 * List to show after an add succeeds. Prefer the refreshed rows. Append the
 * new parameter only when that row does not already list it, so a refresh
 * that omits parameters still shows the stored set plus the one just added.
 * Do not call this when the update failed.
 */
export function schemesAfterSuccessfulAdd(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  added: LocationSchemeParameterDraft,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)
    ?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterAdd(row, added, known) };
  });
}
