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
export const LOCATION_SCHEME_PARAMETER_SEQUENCE_REQUIRED =
  "Parameter sequence is required";
export const LOCATION_SCHEME_PARAMETER_SEQUENCE_INVALID =
  "Parameter sequence must be a number";
export const LOCATION_SCHEME_PARAMETER_NOT_ON_SCHEME =
  "Parameter is not on this scheme";

/** {@code RXLOCATIONSCHEMEPARAMS.SEQUENCE} is a required SQL INTEGER. */
const MIN_SEQUENCE = -2147483648;
const MAX_SEQUENCE = 2147483647;

export type LocationSchemeParameterSequenceDraft = {
  name: string;
  type: string;
  value?: string;
  sequence: number;
};

export type LocationSchemeParameterSequenceResult =
  | { ok: true; parameter: LocationSchemeParameterSequenceDraft }
  | { ok: false; error: string };

/**
 * A blank or non-integer sequence does not write. The kept name, type, and
 * value come from the stored row. The request does not rename the parameter
 * or change its type or value.
 */
export function validateLocationSchemeParameterSequence(
  nameRaw: string,
  sequenceRaw: string,
  existing: SchemeParameter[] | undefined,
): LocationSchemeParameterSequenceResult {
  const name = nameRaw.trim();
  const sequenceText = sequenceRaw.trim();
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
  if (sequenceText.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_SEQUENCE_REQUIRED };
  }
  const sequence = parseSequence(sequenceText);
  if (sequence == null) {
    return { ok: false, error: LOCATION_SCHEME_PARAMETER_SEQUENCE_INVALID };
  }
  const type = (match.type ?? "").trim();
  const value = (match.value ?? "").trim();
  return {
    ok: true,
    parameter: {
      name,
      type,
      ...(value.length > 0 ? { value } : {}),
      sequence,
    },
  };
}

/** Integer text only. A decimal, sign-only, or out-of-range value is not a sequence. */
function parseSequence(text: string): number | null {
  if (!/^-?\d+$/.test(text)) {
    return null;
  }
  const value = Number(text);
  if (!Number.isSafeInteger(value) || value < MIN_SEQUENCE || value > MAX_SEQUENCE) {
    return null;
  }
  return value;
}

/**
 * Sequence-one {@code updateScheme} body. Name, generator, description,
 * content type, template, and context are omitted so the server leaves them
 * stored. {@code updateParameterSequence} replaces this one sequence and does
 * not replace the stored set. Type and value are omitted; the server keeps
 * the stored type and value and the other parameters' sequences. The scheme
 * id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeParameterSequenceBody(
  parameter: LocationSchemeParameterSequenceDraft,
): LocationSchemeSummary {
  return {
    updateParameterSequence: true,
    parameters: [
      {
        name: parameter.name,
        sequence: parameter.sequence,
      },
    ],
  };
}

function parametersAfterSequence(
  row: LocationSchemeSummary,
  updated: LocationSchemeParameterSequenceDraft,
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
    return { ...rowParameter, sequence: updated.sequence };
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
  return [...next, { ...prior, sequence: updated.sequence }];
}

/**
 * List to show after a sequence change succeeds. Prefer the refreshed rows,
 * then replace that parameter's sequence when a refresh still has the previous
 * sequence or omits parameters. Name, type, value, and the other parameters
 * stay, including their sequences. Do not call this when the update failed.
 */
export function schemesAfterSuccessfulParameterSequence(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  updated: LocationSchemeParameterSequenceDraft,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const id = String(schemeId);
  const known = previous.find((row) => String(row.schemeId ?? "") === id)?.parameters;
  return rows.map((row) => {
    if (String(row.schemeId ?? "") !== id) {
      return row;
    }
    return { ...row, parameters: parametersAfterSequence(row, updated, known) };
  });
}
