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
 * RXCONTEXT.CONTEXTNAME is VARCHAR(50). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTEXT_NAME_LENGTH}.
 */
export const CONTEXT_NAME_MAX_LENGTH = 50;

export const CONTEXT_NAME_REQUIRED = "Name is required";

export const CONTEXT_NAME_TOO_LONG =
  "Publishing context name must be 50 characters or fewer";

export type ContextCopyNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong copy name before any create request. */
export function validateContextCopyName(raw: string): ContextCopyNameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: CONTEXT_NAME_REQUIRED };
  }
  if (name.length > CONTEXT_NAME_MAX_LENGTH) {
    return { ok: false, error: CONTEXT_NAME_TOO_LONG };
  }
  return { ok: true, name };
}

/**
 * Suggested name for the copy form. Empty when the suggestion would exceed
 * {@link CONTEXT_NAME_MAX_LENGTH}.
 */
export function suggestedContextCopyName(sourceName: string | undefined): string {
  const base = (sourceName ?? "").trim();
  if (!base) {
    return "";
  }
  const suggestion = `${base} copy`;
  if (suggestion.length > CONTEXT_NAME_MAX_LENGTH) {
    return "";
  }
  return suggestion;
}

/**
 * Body for the existing create-context API. Omits contextId and
 * defaultSchemeId so the server allocates a new context and does not point
 * it at the source context's schemes. The visible name is the only field
 * the operator supplies; the source description is kept.
 */
export function buildContextCopyBody(
  source: ContextSummary,
  newName: string,
): ContextSummary {
  const body: ContextSummary = { name: newName };
  if (source.description != null) {
    body.description = source.description;
  }
  return body;
}

/**
 * List to show after create succeeds. Prefer the refreshed rows. If the
 * refresh failed or omitted the new row, append the created context so a
 * successful copy is visible. Restore any previous row the refresh dropped.
 * Do not call this when create failed.
 */
export function contextsAfterSuccessfulCopy(
  refreshed: ContextSummary[] | null,
  created: ContextSummary,
  previous: ContextSummary[],
): ContextSummary[] {
  const rows = [...(refreshed ?? previous)];
  const createdId = created.contextId != null ? String(created.contextId) : "";
  const sourceIds = new Set(
    previous
      .map((row) => (row.contextId != null ? String(row.contextId) : ""))
      .filter((id) => !!id && id !== createdId),
  );
  for (const id of sourceIds) {
    if (!rows.some((row) => String(row.contextId ?? "") === id)) {
      const source = previous.find((row) => String(row.contextId ?? "") === id);
      if (source) {
        rows.push(source);
      }
    }
  }
  const createdName = created.name?.trim();
  const present = rows.some((row) => {
    if (createdId && String(row.contextId ?? "") === createdId) {
      return true;
    }
    return !!createdName && row.name === createdName;
  });
  if (present) {
    return rows;
  }
  return [...rows, created];
}
