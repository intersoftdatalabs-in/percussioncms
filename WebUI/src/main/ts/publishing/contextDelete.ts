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
import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";

/** Shown when DELETE 409 has no body. Matches the publishing-design conflict text. */
export const CONTEXT_HAS_LOCATION_SCHEMES = "Publishing context has location schemes";

/**
 * Map publishing-context DELETE failures to the Design error region.
 * HTTP 400 → bad context id; HTTP 403 → forbidden; HTTP 409 → the context
 * still has location schemes. Plain {@link ApiError} objects are not
 * {@code Error} instances — do not use {@code e.message} or the shell shows
 * a generic miss.
 */
export function mapContextDeleteError(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_ERROR));
    }
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 409) {
      return formatApiError(err, CONTEXT_HAS_LOCATION_SCHEMES);
    }
  }
  return formatApiError(err, message(MSG.PUBLISH_ERROR));
}

function withoutContext(rows: ContextSummary[], deletedId: string): ContextSummary[] {
  return rows.filter((row) => String(row.contextId ?? "") !== deletedId);
}

/**
 * Contexts to show after DELETE succeeds. Drop the deleted id from the
 * refreshed list, or from the previous rows when refresh failed. Do not call
 * this when DELETE failed — the context must stay.
 */
export function contextsAfterSuccessfulDelete(
  refreshed: ContextSummary[] | null,
  deletedId: string,
  previous: ContextSummary[],
): ContextSummary[] {
  const id = deletedId.trim();
  if (!id) {
    return refreshed ?? previous;
  }
  return withoutContext(refreshed ?? previous, id);
}
