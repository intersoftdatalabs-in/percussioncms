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

import type { ContextSummary } from "../api/publishing/designApi";

/**
 * RXCONTEXT.CONTEXTDESC is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTEXT_DESCRIPTION_LENGTH}.
 */
export const CONTEXT_DESCRIPTION_MAX_LENGTH = 255;

export const CONTEXT_DESCRIPTION_TOO_LONG =
  "Publishing context description must be 255 characters or fewer";

export type ContextDescriptionResult =
  | { ok: true; description: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is allowed and clears the stored description.
 * Overlong text is rejected before any update request.
 */
export function validateContextDescription(raw: string): ContextDescriptionResult {
  const description = raw.trim();
  if (description.length > CONTEXT_DESCRIPTION_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_DESCRIPTION_TOO_LONG };
  }
  return { ok: true, description };
}

/**
 * Description-only {@code updateContext} body. Name and the default scheme
 * are omitted so the server leaves them stored. Location schemes stay on
 * this context id because the update does not create or move schemes.
 * An empty string clears the description. The id is the path parameter.
 */
export function buildContextDescriptionBody(description: string): ContextSummary {
  return { description };
}

/**
 * List to show after a description save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * description so the name and default scheme stay visible. Do not call this
 * when the update failed. An empty description clears the stored text.
 * Location schemes are not part of this list.
 */
export function contextsAfterSuccessfulDescription(
  refreshed: ContextSummary[] | null,
  contextId: string,
  description: string,
  previous: ContextSummary[],
): ContextSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(contextId);
  return previous.map((row) =>
    String(row.contextId ?? "") === id ? { ...row, description } : row,
  );
}
