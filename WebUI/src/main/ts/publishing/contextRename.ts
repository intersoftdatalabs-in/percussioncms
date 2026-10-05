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
import { validateContextCopyName } from "./contextCopy";

export {
  CONTEXT_NAME_MAX_LENGTH,
  CONTEXT_NAME_REQUIRED,
  CONTEXT_NAME_TOO_LONG,
} from "./contextCopy";

export type ContextRenameNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong rename before any update request. */
export function validateContextRenameName(raw: string): ContextRenameNameResult {
  return validateContextCopyName(raw);
}

/**
 * Name-only {@code updateContext} body. Description and the default scheme
 * are omitted so the server leaves them stored. Location schemes stay on
 * this context id because the update does not create or move schemes.
 * The id is the path parameter, not a field on this body.
 */
export function buildContextRenameBody(name: string): ContextSummary {
  return { name };
}

/**
 * List to show after a rename succeeds. Prefer the refreshed rows. If the
 * refresh failed, keep every previous row and replace only the renamed name
 * so the description and default scheme stay visible. Do not call this when
 * the update failed.
 */
export function contextsAfterSuccessfulRename(
  refreshed: ContextSummary[] | null,
  contextId: string,
  name: string,
  previous: ContextSummary[],
): ContextSummary[] {
  if (refreshed) {
    return refreshed;
  }
  const id = String(contextId);
  return previous.map((row) =>
    String(row.contextId ?? "") === id ? { ...row, name } : row,
  );
}
