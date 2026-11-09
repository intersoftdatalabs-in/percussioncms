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
 * Read-only workflow history/audit trail for the React Content Editor
 * host (#5829, parent #4532).
 *
 * <p>Reuses itemmanagement {@code GET /services/itemmanagement/item/revisions/{id}}:
 * the revisions summary already carries the recorded transition comments
 * ({@code ItemAuditComment} rows with date, user, transition type, and
 * comment). This slice only reads those rows — it never restores a revision
 * and never runs a transition. HTTP 403 (not authorized) and 404 (item not
 * found) are failures and are <strong>not</strong> treated as an empty
 * history.</p>
 */

import {
  isRecordedAuditComment,
  type ItemAuditComment,
  type ItemRevisionsSummary,
} from "../api/contentExplorer/itemRevisionsApi";
import type { EditorHostMode } from "./editorHostUrl";

/** Workflow history is read-only, so it is available in edit and view. */
export function canViewWorkflowHistoryFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit" || mode === "view";
}

/**
 * Recorded transition comments for the history panel. A row that carries a
 * date, user, or type is still listed when the comment text itself is empty;
 * fully blank rows are dropped so the panel never renders a blank row.
 */
export function workflowHistoryEntries(
  summary: ItemRevisionsSummary | null | undefined,
): ItemAuditComment[] {
  if (summary == null) {
    return [];
  }
  return (summary.comments ?? []).filter(isRecordedAuditComment);
}

/**
 * Display string for one history row (e.g. {@code "2026-04-12 — admin — Approve — Looks good"}).
 *
 * <p>Falls back to whatever fields are non-empty so an older audit payload
 * still produces a usable label.</p>
 */
export function summarizeWorkflowHistoryRow(
  comment: ItemAuditComment,
  fallback = "",
): string {
  const parts = [
    comment.commentDate.trim(),
    comment.commenter.trim(),
    comment.commentType.trim(),
    comment.comment.trim(),
  ].filter((part) => part.length > 0);
  const summary = parts.join(" — ");
  return summary || fallback;
}
