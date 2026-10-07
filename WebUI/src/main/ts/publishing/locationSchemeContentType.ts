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
 * {@code RXLOCATIONSCHEME.CONTENTTYPEID} is a required positive id. Keep the
 * rejection text in step with
 * {@code PSPublishingDesignRestService.LOCATION_SCHEME_CONTENT_TYPE_INVALID}.
 */
export const LOCATION_SCHEME_CONTENT_TYPE_REQUIRED =
  "Location scheme content type is required";

export const LOCATION_SCHEME_CONTENT_TYPE_NOT_NUMERIC =
  "Location scheme content type must be a number";

export type LocationSchemeContentTypeResult =
  | { ok: true; contentTypeId: number }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and must not be written, so the stored id is
 * not cleared. Anything that is not a positive integer is rejected before any
 * update request.
 */
export function validateLocationSchemeContentType(
  raw: string,
): LocationSchemeContentTypeResult {
  const text = raw.trim();
  if (text.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_CONTENT_TYPE_REQUIRED };
  }
  if (!/^[1-9][0-9]*$/.test(text)) {
    return { ok: false, error: LOCATION_SCHEME_CONTENT_TYPE_NOT_NUMERIC };
  }
  const contentTypeId = Number(text);
  if (!Number.isSafeInteger(contentTypeId)) {
    return { ok: false, error: LOCATION_SCHEME_CONTENT_TYPE_NOT_NUMERIC };
  }
  return { ok: true, contentTypeId };
}

/**
 * Content-type-only {@code updateScheme} body. Name, generator, description,
 * template, context, and parameters are omitted so the server leaves them
 * stored. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeContentTypeBody(
  contentTypeId: number,
): LocationSchemeSummary {
  return { contentTypeId };
}

/**
 * List to show after a content-type save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the content
 * type so the name, generator, description, template, and parameters stay.
 * Do not call this when the update failed.
 */
export function schemesAfterSuccessfulContentType(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  contentTypeId: number,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(schemeId);
  return previous.map((row) =>
    String(row.schemeId ?? "") === id ? { ...row, contentTypeId } : row,
  );
}
