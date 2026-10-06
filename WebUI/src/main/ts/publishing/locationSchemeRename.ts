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
import { validateLocationSchemeCopyName } from "./locationSchemeCopy";

export {
  LOCATION_SCHEME_NAME_MAX_LENGTH,
  LOCATION_SCHEME_NAME_REQUIRED,
  LOCATION_SCHEME_NAME_TOO_LONG,
} from "./locationSchemeCopy";

export type LocationSchemeRenameNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong rename before any update request. */
export function validateLocationSchemeRenameName(
  raw: string,
): LocationSchemeRenameNameResult {
  return validateLocationSchemeCopyName(raw);
}

/**
 * Name-only {@code updateScheme} body. Generator, description, content type,
 * template, context, and parameters are omitted so the server leaves them
 * stored. The scheme id is the path parameter, not a field on this body.
 */
export function buildLocationSchemeRenameBody(name: string): LocationSchemeSummary {
  return { name };
}

/**
 * List to show after a rename succeeds. Prefer the refreshed rows. If the
 * refresh failed, keep every previous row and replace only the renamed name
 * so generator, description, content type, template, and parameters stay.
 * Do not call this when the update failed.
 */
export function schemesAfterSuccessfulRename(
  refreshed: LocationSchemeSummary[] | null,
  schemeId: string,
  name: string,
  previous: LocationSchemeSummary[],
): LocationSchemeSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(schemeId);
  return previous.map((row) =>
    String(row.schemeId ?? "") === id ? { ...row, name } : row,
  );
}
