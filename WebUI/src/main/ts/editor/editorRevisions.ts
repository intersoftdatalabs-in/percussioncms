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

/**
 * Restore prior revision from the React Content Editor host (#4604).
 *
 * <p>Reuses itemmanagement {@code GET /services/itemmanagement/item/revisions/{id}}
 * to list revisions and {@code GET /services/itemmanagement/item/restoreRevision/{guid}}
 * to promote the chosen revision. HTTP 403 (not authorized) and 404 (item or revision
 * not found) are failures and are <strong>not</strong> treated as success.</p>
 */

import { isApiError } from "../api/client";
import type { ItemRevision } from "../api/contentExplorer/itemRevisionsApi";
import type { EditorHostMode } from "./editorHostUrl";

export type EditorRevisionErrorReason = "forbidden" | "not_found" | "failed";

/** Restore prior revision is available in edit mode when an item is open. */
export function canRestoreFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit";
}

/**
 * Display string for a revision row in the dropdown (e.g. {@code "5 — Live — admin — 2025-04-12"}).
 *
 * <p>Falls back to whatever fields are non-empty so an older revision payload still
 * produces a usable label.</p>
 */
export function summarizeRevisionRow(rev: ItemRevision, fallback = ""): string {
  const parts: string[] = [];
  if (Number.isFinite(rev.revId) && rev.revId > 0) {
    parts.push(`#${rev.revId}`);
  }
  if (rev.status) {
    parts.push(rev.status);
  }
  if (rev.lastModifier) {
    parts.push(rev.lastModifier);
  }
  if (rev.lastModifiedDate) {
    parts.push(rev.lastModifiedDate);
  }
  const summary = parts.filter(Boolean).join(" — ");
  return summary || fallback;
}

/**
 * Build the confirm prompt body authors see before the restore fires. The host
 * passes the localized message + the selected revision row; the host calls
 * {@code window.confirm} (or the test seam) with this string.
 */
export function restoreRevisionConfirmBody(
  row: string,
  itemName = "",
): string {
  const name = itemName.trim();
  return name ? `${row} (${name})` : row;
}

/** Map REST failures so 403/404 are not treated as a successful restore. */
export function editorRevisionErrorReason(
  err: unknown,
): EditorRevisionErrorReason {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}

/**
 * Normalize a revision id coming back from form state. Returns {@code null} for
 * a sentinel / non-numeric value so the host can disable the action.
 */
export function parseRevisionId(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value) && value > 0) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 10);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }
  return null;
}
