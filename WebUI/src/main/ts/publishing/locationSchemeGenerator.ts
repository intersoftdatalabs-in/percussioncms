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
 * RXLOCATIONSCHEME.GENERATOR is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_LOCATION_SCHEME_GENERATOR_LENGTH}.
 */
export const LOCATION_SCHEME_GENERATOR_MAX_LENGTH = 255;

export const LOCATION_SCHEME_GENERATOR_REQUIRED =
  "Location scheme generator is required";

export const LOCATION_SCHEME_GENERATOR_TOO_LONG =
  "Location scheme generator must be 255 characters or fewer";

export type LocationSchemeGeneratorResult =
  | { ok: true; generator: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and must not be written. Overlong text is
 * rejected before any update request.
 */
export function validateLocationSchemeGenerator(
  raw: string,
): LocationSchemeGeneratorResult {
  const generator = raw.trim();
  if (generator.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_GENERATOR_REQUIRED };
  }
  if (generator.length > LOCATION_SCHEME_GENERATOR_MAX_LENGTH) {
    return { ok: false, error: LOCATION_SCHEME_GENERATOR_TOO_LONG };
  }
  return { ok: true, generator };
}

/**
 * Generator-only {@code updateScheme} body. Name, description, content type,
 * template, context, and parameters are omitted so the server leaves them
 * stored. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeGeneratorBody(
  generator: string,
): LocationSchemeSummary {
  return { generator };
}

/**
 * List to show after a generator save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * generator so the name, description, content type, template, and parameters
 * stay. Do not call this when the update failed.
 */
export function schemesAfterSuccessfulGenerator(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  generator: string,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(schemeId);
  return previous.map((row) =>
    String(row.schemeId ?? "") === id ? { ...row, generator } : row,
  );
}
