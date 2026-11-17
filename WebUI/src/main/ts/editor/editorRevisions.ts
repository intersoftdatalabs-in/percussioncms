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

export type RevisionCompareBlock =
  | { ok: true; left: number; right: number }
  | { ok: false; reason: "need_two" | "same" };

/**
 * Two distinct positive revision ids are required before a compare request.
 * The same id, or a missing id, must not be sent (that would look like a diff).
 */
export function revisionCompareSelection(
  left: unknown,
  right: unknown,
): RevisionCompareBlock {
  const a = parseRevisionId(left);
  const b = parseRevisionId(right);
  if (a == null || b == null) {
    return { ok: false, reason: "need_two" };
  }
  if (a === b) {
    return { ok: false, reason: "same" };
  }
  return { ok: true, left: a, right: b };
}

/**
 * A field value copied from a compare row into the draft (#6475).
 *
 * <p>The notice names the source revision, not just the field, so authors can
 * tell after the click whether the older or the newer value landed in the
 * draft. Nothing is written until the author saves.</p>
 */
export interface RestoredCompareField {
  field: string;
  revId: number;
}

/**
 * Display string for a field-level restore notice (e.g.
 * {@code "sys_title (revision #4)"}). Falls back to the bare field name when
 * the revision id is not a positive number.
 */
export function describeRestoredCompareField(
  field: string,
  revId: number,
): string {
  const name = field.trim();
  if (Number.isFinite(revId) && revId > 0) {
    return `${name} (revision #${revId})`;
  }
  return name;
}

/** Server returned a compare with no field rows — show empty, not a fabricated diff. */
export function isEmptyRevisionCompare(
  fields: { length: number } | null | undefined,
): boolean {
  return fields == null || fields.length === 0;
}

/**
 * One word-level segment of a revision compare cell (#6533).
 *
 * <p>{@code same} text appears in both revisions; {@code removed} text appears
 * only in the older value; {@code added} text appears only in the newer value.
 * Callers render the older cell from {@code same + removed} segments and the
 * newer cell from {@code same + added} segments so long text fields read as an
 * inline diff instead of two full values to eyeball.</p>
 */
export interface WordDiffSegment {
  text: string;
  kind: "same" | "removed" | "added";
}

/** Token cap so pathological multi-megabyte fields fall back to plain text. */
const WORD_DIFF_TOKEN_CAP = 2000;

/** Split into word/whitespace tokens so spacing survives the render. */
export function splitWordDiffTokens(value: string): string[] {
  return value.split(/(\s+)/).filter((part) => part.length > 0);
}

/**
 * Word-level diff of two compare cell values (#6533).
 *
 * <p>Tokenizes on whitespace (keeping the whitespace) and aligns tokens with a
 * longest-common-subsequence pass. Adjacent same-kind tokens are merged so the
 * render is a short run of {@code <span>}/{@code <ins>}/{@code <del>} nodes.
 * Returns a single {@code same} segment when the values are equal, and falls
 * back to whole-value removed+added when either side exceeds the token cap.</p>
 */
export function computeWordDiff(
  left: string,
  right: string,
): WordDiffSegment[] {
  const older = left ?? "";
  const newer = right ?? "";
  if (older === newer) {
    return older ? [{ text: older, kind: "same" }] : [];
  }
  const leftTokens = splitWordDiffTokens(older);
  const rightTokens = splitWordDiffTokens(newer);
  if (leftTokens.length + rightTokens.length > WORD_DIFF_TOKEN_CAP) {
    return wholeValueWordDiff(older, newer);
  }
  const table = buildWordDiffTable(leftTokens, rightTokens);
  return mergeWordDiffSegments(
    traceWordDiff(table, leftTokens, rightTokens),
  );
}

/** Whole-value fallback when tokenized fields exceed the diff cap. */
function wholeValueWordDiff(
  older: string,
  newer: string,
): WordDiffSegment[] {
  const fallback: WordDiffSegment[] = [];
  if (older) {
    fallback.push({ text: older, kind: "removed" });
  }
  if (newer) {
    fallback.push({ text: newer, kind: "added" });
  }
  return fallback;
}

/** Suffix-length LCS table over the two token lists. */
function buildWordDiffTable(
  leftTokens: string[],
  rightTokens: string[],
): { table: Uint32Array; cols: number } {
  const cols = rightTokens.length + 1;
  const table = new Uint32Array((leftTokens.length + 1) * cols);
  for (let i = leftTokens.length - 1; i >= 0; i -= 1) {
    for (let j = rightTokens.length - 1; j >= 0; j -= 1) {
      table[i * cols + j] =
        leftTokens[i] === rightTokens[j]
          ? table[(i + 1) * cols + (j + 1)] + 1
          : Math.max(table[(i + 1) * cols + j], table[i * cols + (j + 1)]);
    }
  }
  return { table, cols };
}

/** Walk the LCS table back into unmerged same/removed/added segments. */
function traceWordDiff(
  { table, cols }: { table: Uint32Array; cols: number },
  leftTokens: string[],
  rightTokens: string[],
): WordDiffSegment[] {
  const raw: WordDiffSegment[] = [];
  let i = 0;
  let j = 0;
  while (i < leftTokens.length && j < rightTokens.length) {
    if (leftTokens[i] === rightTokens[j]) {
      raw.push({ text: leftTokens[i], kind: "same" });
      i += 1;
      j += 1;
    } else if (table[(i + 1) * cols + j] >= table[i * cols + (j + 1)]) {
      raw.push({ text: leftTokens[i], kind: "removed" });
      i += 1;
    } else {
      raw.push({ text: rightTokens[j], kind: "added" });
      j += 1;
    }
  }
  while (i < leftTokens.length) {
    raw.push({ text: leftTokens[i], kind: "removed" });
    i += 1;
  }
  while (j < rightTokens.length) {
    raw.push({ text: rightTokens[j], kind: "added" });
    j += 1;
  }
  return raw;
}

/** Merge adjacent same-kind segments so the render stays a short run. */
function mergeWordDiffSegments(
  raw: WordDiffSegment[],
): WordDiffSegment[] {
  const merged: WordDiffSegment[] = [];
  for (const segment of raw) {
    const last = merged[merged.length - 1];
    if (last != null && last.kind === segment.kind) {
      last.text += segment.text;
    } else {
      merged.push({ ...segment });
    }
  }
  return merged;
}

/**
 * Copy one revision field value into the editor draft (#6080).
 *
 * <p>Field-level restore is a local draft edit: the caller saves through the
 * normal save path, so no server call happens here. A blank name is ignored
 * and the incoming draft is returned unchanged; otherwise the named field is
 * set to {@code value} (possibly empty) and the other entries are kept.</p>
 */
export function applyRevisionFieldValue(
  draft: Record<string, string>,
  name: string,
  value: string,
): Record<string, string> {
  const field = name.trim();
  if (!field) {
    return draft;
  }
  return { ...draft, [field]: value };
}
