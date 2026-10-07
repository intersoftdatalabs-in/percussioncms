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
 * RXLOCATIONSCHEME.DESCRIPTION is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_LOCATION_SCHEME_DESCRIPTION_LENGTH}.
 */
export const LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH = 255;

export const LOCATION_SCHEME_DESCRIPTION_TOO_LONG =
  "Location scheme description must be 255 characters or fewer";

export type LocationSchemeDescriptionResult =
  | { ok: true; description: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is allowed and clears the stored description.
 * Overlong text is rejected before any update request.
 */
export function validateLocationSchemeDescription(
  raw: string,
): LocationSchemeDescriptionResult {
  const description = raw.trim();
  if (description.length > LOCATION_SCHEME_DESCRIPTION_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_DESCRIPTION_TOO_LONG };
  }
  return { ok: true, description };
}

/**
 * Description-only {@code updateScheme} body. Name, generator, content type,
 * template, context, and parameters are omitted so the server leaves them
 * stored. An empty string clears the description. The scheme id is the path
 * parameter, not a field on this body.
 */
export function buildLocationSchemeDescriptionBody(
  description: string,
): LocationSchemeSummary {
  return { description };
}

/**
 * List to show after a description save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * description so the name, generator, content type, template, and parameters
 * stay. Do not call this when the update failed. An empty description clears
 * the stored text.
 */
export function schemesAfterSuccessfulDescription(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  description: string,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(schemeId);
  return previous.map((row) =>
    String(row.schemeId ?? "") === id ? { ...row, description } : row,
  );
}
