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

import type { LocationSchemeSummary } from "../api/publishing/designApi";

/**
 * RXLOCATIONSCHEME.SCHEMENAME is VARCHAR(50). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_LOCATION_SCHEME_NAME_LENGTH}.
 */
export const LOCATION_SCHEME_NAME_MAX_LENGTH = 50;

export const LOCATION_SCHEME_NAME_REQUIRED = "Name is required";

export const LOCATION_SCHEME_NAME_TOO_LONG =
  "Location scheme name must be 50 characters or fewer";

export type LocationSchemeCopyNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong copy name before any create request. */
export function validateLocationSchemeCopyName(
  raw: string,
): LocationSchemeCopyNameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: LOCATION_SCHEME_NAME_REQUIRED };
  }
  if (name.length > LOCATION_SCHEME_NAME_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_NAME_TOO_LONG };
  }
  return { ok: true, name };
}

/**
 * Suggested name for the copy dialog. Empty when the suggestion would
 * exceed {@link LOCATION_SCHEME_NAME_MAX_LENGTH}.
 */
export function suggestedLocationSchemeCopyName(
  sourceName: string | undefined,
): string {
  const base = (sourceName ?? "").trim();
  if (!base) {
    return "";
  }
  const suggestion = `${base} copy`;
  if (suggestion.length > LOCATION_SCHEME_NAME_MAX_LENGTH) {
    return "";
  }
  return suggestion;
}

/**
 * Body for the existing create-scheme API. Omits schemeId so the server
 * allocates a new scheme. Copies generator, description, content type,
 * template, and parameters (the location path lives in those parameters).
 */
export function buildLocationSchemeCopyBody(
  source: LocationSchemeSummary,
  newName: string,
  contextId: string,
): LocationSchemeSummary {
  const body: LocationSchemeSummary = {
    name: newName,
    generator: source.generator,
    contextId,
    copy: true,
  };
  if (source.description) {
    body.description = source.description;
  }
  if (source.contentTypeId != null) {
    body.contentTypeId = source.contentTypeId;
  }
  if (source.templateId != null) {
    body.templateId = source.templateId;
  }
  if (source.parameters && source.parameters.length > 0) {
    body.parameters = source.parameters.map((p) => ({
      name: p.name,
      type: p.type,
      value: p.value,
      sequence: p.sequence,
    }));
  }
  return body;
}

/**
 * List to show after create succeeds. Prefer the refreshed rows. If the
 * refresh failed or omitted the new row, append the created scheme so a
 * successful copy is visible. Do not call this when create failed.
 */
export function schemesAfterSuccessfulCopy(
  refreshed: LocationSchemeSummary[] | null,
  created: LocationSchemeSummary,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  const rows = refreshed ?? previous;
  const createdId = created.schemeId;
  const createdName = created.name?.trim();
  const present = rows.some((row) => {
    if (createdId && row.schemeId === createdId) {
      return true;
    }
    return !!createdName && row.name === createdName;
  });
  if (present) {
    return rows;
  }
  return [...rows, created];
}
