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

import type { EditionSummary } from "../api/publishing/designApi";

/**
 * RXEDITION.DISPLAYTITLE is VARCHAR(100). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_EDITION_NAME_LENGTH}.
 */
export const EDITION_NAME_MAX_LENGTH = 100;

/** Matches the priority the edition editor shows when the row has none. */
export const EDITION_PRIORITY_FORM_DEFAULT = 3;

export const EDITION_NAME_REQUIRED = "Name is required";

export const EDITION_NAME_TOO_LONG =
  "Edition name must be 100 characters or fewer";

export type EditionNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or over-long edition name before any create or update. */
export function validateEditionName(raw: string): EditionNameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: EDITION_NAME_REQUIRED };
  }
  if (name.length > EDITION_NAME_MAX_LENGTH) {
    return { ok: false, error: EDITION_NAME_TOO_LONG };
  }
  return { ok: true, name };
}

/**
 * True when the open form still has the stored comment and priority.
 * A rename then omits those fields so {@code updateEdition} does not rewrite them.
 * The form default priority is not treated as an edit when the row has no priority.
 */
export function editionRenameIsNameOnly(
  edition: Pick<EditionSummary, "comment" | "priority">,
  comment: string,
  priority: number,
): boolean {
  if (comment !== (edition.comment ?? "")) {
    return false;
  }
  if (edition.priority == null || Number.isNaN(edition.priority)) {
    return priority === EDITION_PRIORITY_FORM_DEFAULT;
  }
  return priority === edition.priority;
}

/**
 * Name-only {@code updateEdition} body. Comment and priority are omitted so the
 * server leaves them stored. Content-list associations are a different resource
 * and are not included.
 */
export function editionRenameBody(
  edition: Pick<EditionSummary, "editionId">,
  name: string,
  siteId: string,
): EditionSummary {
  return {
    editionId: edition.editionId,
    name,
    siteId,
  };
}
