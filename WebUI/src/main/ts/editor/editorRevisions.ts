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
 * Names of the changed rows in a revision compare, in table order (#6597).
 *
 * <p>Diff navigation (Previous/Next change) steps through exactly these rows
 * so the live status can announce {@code Change X of Y}. Unchanged rows are
 * never navigation stops, whether or not the changed-only filter hides them.</p>
 */
export function changedCompareFieldNames(
  fields: readonly { name: string; changed: boolean }[] | null | undefined,
): string[] {
  if (fields == null) {
    return [];
  }
  return fields.filter((field) => field.changed).map((field) => field.name);
}

/**
 * Fill the compare navigation status template (e.g.
 * {@code "Change {0} of {1}: {2}"} with a 0-based index, the changed-row
 * total, and the current field name). Mirrors
 * {@link formatDelegationNotice} in {@code editorWorkflow.ts}: the template
 * comes from {@code message()}, the substitution is pure and unit-tested.
 */
export function formatCompareNavStatus(
  template: string,
  index: number,
  total: number,
  field: string,
): string {
  return String(template ?? "")
    .replace("{0}", String(index + 1))
    .replace("{1}", String(total))
    .replace("{2}", field);
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

/** Longest single token eligible for intra-word refinement (#6533). */
const WORD_SUBSTITUTION_CHAR_CAP = 64;

/**
 * Word-level diff of two compare cell values (#6533).
 *
 * <p>Tokenizes on whitespace (keeping the whitespace) and aligns tokens with a
 * longest-common-subsequence pass. Adjacent same-kind tokens are merged so the
 * render is a short run of {@code <span>}/{@code <ins>}/{@code <del>} nodes.
 * Single-word substitutions are refined to the changed characters
 * ({@link refineWordSubstitution}) so typo fixes read as one marked letter
 * instead of a whole marked word. Returns a single {@code same} segment when
 * the values are equal, and falls back to whole-value removed+added when
 * either side exceeds the token cap.</p>
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
    refineSubstitutionPairs(traceWordDiff(table, leftTokens, rightTokens)),
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
 * Refine adjacent removed+added pairs to the changed characters (#6533).
 *
 * <p>Each pair is one word-level substitution (for example {@code "old"} vs
 * {@code "new"}); pairs that qualify as single words are re-diffed at
 * character level so {@code "grey"} vs {@code "gray"} marks just the
 * {@code "e"}/{@code "a"}. A second merge pass coalesces the boundary runs
 * the refinement can split (for example the {@code same} tail).</p>
 */
function refineSubstitutionPairs(
  segments: WordDiffSegment[],
): WordDiffSegment[] {
  const refined: WordDiffSegment[] = [];
  let i = 0;
  while (i < segments.length) {
    const current = segments[i];
    const next = segments[i + 1];
    if (current.kind === "removed" && next?.kind === "added") {
      refined.push(...refineWordSubstitution(current.text, next.text));
      i += 2;
    } else {
      refined.push(current);
      i += 1;
    }
  }
  return refined;
}

/** A substitution pair refines only when both sides are one short word. */
function isRefinableSubstitution(removed: string, added: string): boolean {
  if (!removed || !added) {
    return false;
  }
  if (
    removed.length > WORD_SUBSTITUTION_CHAR_CAP ||
    added.length > WORD_SUBSTITUTION_CHAR_CAP
  ) {
    return false;
  }
  return !/\s/.test(removed) && !/\s/.test(added);
}

/**
 * Character-level diff of one word-level substitution pair (#6533).
 *
 * <p>Reuses the token LCS over code points (`Array.from`, so astral symbols
 * stay intact). Multi-word pairs and over-long single tokens keep their
 * word-level removed+added marks instead of a noisy character scatter.</p>
 */
export function refineWordSubstitution(
  removed: string,
  added: string,
): WordDiffSegment[] {
  if (!isRefinableSubstitution(removed, added)) {
    return [
      { text: removed, kind: "removed" },
      { text: added, kind: "added" },
    ];
  }
  const leftChars = Array.from(removed);
  const rightChars = Array.from(added);
  const { table, cols } = buildWordDiffTable(leftChars, rightChars);
  return mergeWordDiffSegments(traceWordDiff({ table, cols }, leftChars, rightChars));
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

/** Side of a compare cell a collapsed run renders for (#6672). */
export type WordDiffCellSide = "older" | "newer";

/**
 * Context words kept at each edge of a collapsed unchanged run (#6672).
 *
 * <p>Classic unified-diff context: a middle run keeps the trailing words
 * before the next change and the leading words after the previous one, so
 * the changed words stay readable without scrolling through the field.</p>
 */
export const WORD_DIFF_COLLAPSE_CONTEXT_WORDS = 8;

/**
 * Minimum hidden words before an unchanged run collapses (#6672).
 *
 * <p>A run collapses only when hiding it removes at least this many words
 * beyond the kept context, so short fields never gain a toggle.</p>
 */
export const WORD_DIFF_COLLAPSE_MIN_HIDDEN_WORDS = 10;

/**
 * One renderable piece of a compare cell with collapsed runs (#6672).
 *
 * <p>A {@code collapsed: false} piece renders as before (a plain span for
 * {@code same} text, {@code <del>} for the older side, {@code <ins>} for
 * the newer side). A {@code collapsed: true} piece renders as an ellipsis
 * for {@code hiddenWords} unchanged words; the cell toggle expands all of
 * them at once.</p>
 */
export type CollapsedWordDiffPart =
  | { collapsed: false; text: string; kind: WordDiffSegment["kind"] }
  | { collapsed: true; hiddenWords: number };

/** Split text into words; interior whitespace is not preserved (#6672). */
function splitCollapseWords(text: string): string[] {
  return text.split(/\s+/).filter((word) => word.length > 0);
}

/**
 * Split one unchanged run into kept edges plus a collapsed gap (#6672).
 *
 * <p>Returns {@code null} when the run is short enough to render whole.
 * {@code keepHead} keeps the run's leading context words (a change precedes
 * it) and {@code keepTail} keeps its trailing context words (a change
 * follows it), so a middle run keeps both edges in reading order while an
 * edge run keeps only the side facing its change.</p>
 */
function splitUnchangedRun(
  words: string[],
  keepHead: boolean,
  keepTail: boolean,
  context: number,
  minimum: number,
): CollapsedWordDiffPart[] | null {
  const keepCount = (keepHead ? context : 0) + (keepTail ? context : 0);
  if (words.length <= keepCount + minimum) {
    return null;
  }
  const parts: CollapsedWordDiffPart[] = [];
  if (keepHead) {
    parts.push({
      collapsed: false,
      text: words.slice(0, context).join(" "),
      kind: "same",
    });
  }
  parts.push({ collapsed: true, hiddenWords: words.length - keepCount });
  if (keepTail) {
    parts.push({
      collapsed: false,
      text: words.slice(-context).join(" "),
      kind: "same",
    });
  }
  return parts;
}

/**
 * Collapse long unchanged runs of a compare cell (#6672).
 *
 * <p>Keeps only the segments the given side renders ({@code same} plus the
 * side's own marks) and replaces an unchanged run longer than the kept
 * context plus {@code minHiddenWords} with a collapsed gap carrying the
 * hidden word count. A leading run keeps its trailing context words, a
 * trailing run keeps its leading context words, and a middle run keeps
 * both edges. Runs collapse only when the cell also has a change to anchor
 * the context to; an all-unchanged or empty cell renders as before so
 * there is never a toggle with nothing to show.</p>
 */
export function collapseWordDiffCell(
  segments: WordDiffSegment[],
  side: WordDiffCellSide,
  contextWords = WORD_DIFF_COLLAPSE_CONTEXT_WORDS,
  minHiddenWords = WORD_DIFF_COLLAPSE_MIN_HIDDEN_WORDS,
): CollapsedWordDiffPart[] {
  const kept = (segments ?? []).filter(
    (segment) =>
      segment.kind === "same" ||
      (side === "older"
        ? segment.kind === "removed"
        : segment.kind === "added"),
  );
  if (!kept.some((segment) => segment.kind !== "same")) {
    return kept.map((segment) => ({
      collapsed: false as const,
      text: segment.text,
      kind: segment.kind,
    }));
  }
  const context = Math.max(0, Math.floor(contextWords));
  const minimum = Math.max(0, Math.floor(minHiddenWords));
  if (context === 0) {
    return kept.map((segment) => ({
      collapsed: false as const,
      text: segment.text,
      kind: segment.kind,
    }));
  }
  const parts: CollapsedWordDiffPart[] = [];
  kept.forEach((segment, index) => {
    if (segment.kind !== "same") {
      parts.push({ collapsed: false, text: segment.text, kind: segment.kind });
      return;
    }
    const split = splitUnchangedRun(
      splitCollapseWords(segment.text),
      index > 0,
      index < kept.length - 1,
      context,
      minimum,
    );
    if (split == null) {
      parts.push({ collapsed: false, text: segment.text, kind: segment.kind });
    } else {
      parts.push(...split);
    }
  });
  return parts;
}

/**
 * Total hidden unchanged words across collapsed gaps (#6672).
 *
 * <p>The cell toggle names this count so authors know how much text one
 * click reveals.</p>
 */
export function collapsedWordDiffHiddenWords(
  parts: readonly CollapsedWordDiffPart[],
): number {
  return (parts ?? []).reduce(
    (total, part) => total + (part.collapsed ? part.hiddenWords : 0),
    0,
  );
}

/**
 * Fill the collapsed-cell toggle template (e.g.
 * {@code "Show {0} unchanged words"} with the hidden word count). Mirrors
 * {@link formatCompareNavStatus}: the template comes from
 * {@code message()}, the substitution is pure and unit-tested.
 */
export function formatCollapsedWordDiffCount(
  template: string,
  hiddenWords: number,
): string {
  return String(template ?? "").replace("{0}", String(hiddenWords));
}
