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
 * {@code RXLOCATIONSCHEME.TEMPLATEID} is a required positive id. Keep the
 * rejection text in step with
 * {@code PSPublishingDesignRestService.LOCATION_SCHEME_TEMPLATE_INVALID}.
 */
export const LOCATION_SCHEME_TEMPLATE_REQUIRED =
  "Location scheme template is required";

export const LOCATION_SCHEME_TEMPLATE_NOT_NUMERIC =
  "Location scheme template must be a number";

export type LocationSchemeTemplateResult =
  | { ok: true; templateId: number }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and must not be written, so the stored id is
 * not cleared. Anything that is not a positive integer is rejected before any
 * update request.
 */
export function validateLocationSchemeTemplate(
  raw: string,
): LocationSchemeTemplateResult {
  const text = raw.trim();
  if (text.length === 0) {
    return { ok: false, error: LOCATION_SCHEME_TEMPLATE_REQUIRED };
  }
  if (!/^[1-9][0-9]*$/.test(text)) {
    return { ok: false, error: LOCATION_SCHEME_TEMPLATE_NOT_NUMERIC };
  }
  const templateId = Number(text);
  if (!Number.isSafeInteger(templateId)) {
    return { ok: false, error: LOCATION_SCHEME_TEMPLATE_NOT_NUMERIC };
  }
  return { ok: true, templateId };
}

/**
 * Template-only {@code updateScheme} body. Name, generator, description,
 * content type, context, and parameters are omitted so the server leaves them
 * stored. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeTemplateBody(
  templateId: number,
): LocationSchemeSummary {
  return { templateId };
}

/**
 * List to show after a template save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the template
 * so the name, generator, description, content type, and parameters stay.
 * Do not call this when the update failed.
 */
export function schemesAfterSuccessfulTemplate(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  templateId: number,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(schemeId);
  return previous.map((row) =>
    String(row.schemeId ?? "") === id ? { ...row, templateId } : row,
  );
}
