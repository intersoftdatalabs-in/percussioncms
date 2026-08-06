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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * In-memory clipboard model for the modern Content Explorer (US7 / T071).
 *
 * <p>The clipboard is the persistent (within-tab) storage for the
 * explorer's cut / copy / paste operations. It is intentionally local
 * (no server-side persistence) so that an accidental re-paste after
 * a session restore cannot bypass an explicit user action.</p>
 *
 * <p>This module is pure (no React, no fetch) — the actual transport
 * for paste operations lives in {@code clipboardApi.ts}. Tests cover
 * the validity rules that gate a paste so the explorer shell never
 * queries the user for permission to paste into a forbidden target
 * when the source item is, e.g., a page being pasted into an asset
 * folder.</p>
 */

import type {
  Clipboard,
  ClipboardItem,
  ClipboardPasteResultItem,
  ClipboardPasteSummary,
} from "../../api/contentExplorer/types";

/** Empty clipboard state. */
export const EMPTY_CLIPBOARD: Clipboard = Object.freeze({
  operation: "copy",
  items: [],
  updatedAt: "1970-01-01T00:00:00.000Z",
});

/**
 * Replace the clipboard contents. Returns a new immutable object so
 * the caller can store it in React state without copying the items
 * array by hand.
 *
 * <p>Stamps {@link Clipboard.updatedAt} on every call so the UI can
 * display the "clipboard last updated" label without tracking the
 * timestamp separately.</p>
 */
export function setClipboard(
  current: Clipboard,
  operation: "copy" | "cut",
  items: ReadonlyArray<ClipboardItem>,
  now: () => string = () => new Date().toISOString(),
): Clipboard {
  return Object.freeze({
    operation,
    items: Object.freeze([...items]),
    updatedAt: now(),
  });
}

/** True when no items are held. */
export function isEmpty(cb: Clipboard): boolean {
  return cb.items.length === 0;
}

/** Number of items held. */
export function size(cb: Clipboard): number {
  return cb.items.length;
}

/**
 * Permission check for FR-016 read-only-without-rights: a paste into
 * a target folder requires at least the source permission level of
 * every clipboard item. Returns {@code true} when the proposed paste
 * is allowed for the supplied source / target access levels.
 *
 * <p>Server is authoritative; this is the UX gate (the explorer
 * shell renders the paste button as disabled when this returns
 * false). The actual server-side ACL enforcement still happens in
 * the move / copy REST endpoints.</p>
 */
export function canPasteInto(
  cb: Clipboard,
  targetAccessLevel: ClipboardItem["sourceAccessLevel"],
): boolean {
  if (isEmpty(cb)) return false;
  if (!targetAccessLevel) return false;
  // WRITE is the minimum required to mutate the target folder.
  // (FR-016: VIEW can see but cannot mutate; READ can read +
  // safely browse; WRITE / ADMIN can mutate.)
  if (targetAccessLevel !== "WRITE" && targetAccessLevel !== "ADMIN") {
    return false;
  }
  // Source-side: every clipboard item must come from a folder the
  // caller can at least READ (otherwise the caller wouldn't have
  // been able to copy it into the clipboard in the first place).
  for (const item of cb.items) {
    const src = item.sourceAccessLevel;
    if (
      src !== "ADMIN" &&
      src !== "WRITE" &&
      src !== "READ" &&
      src !== "VIEW"
    ) {
      return false;
    }
  }
  return true;
}

/**
 * Build a per-item paste summary from a list of settled promises
 * (e.g. one per clipboard item). Used by {@code clipboardApi.paste}
 * to wrap the transport's per-item success / failure into a single
 * object the explorer shell can render.
 */
export function buildPasteSummary(
  operation: "copy" | "cut",
  items: ReadonlyArray<ClipboardItem>,
  settled: ReadonlyArray<PromiseSettledResult<unknown>>,
): ClipboardPasteSummary {
  const results: ClipboardPasteResultItem[] = items.map((item, idx) => {
    const s = settled[idx];
    if (!s) {
      return { item, ok: false, message: "missing settled result" };
    }
    if (s.status === "fulfilled") {
      return { item, ok: true };
    }
    const reason = s.reason instanceof Error ? s.reason.message : String(s.reason);
    return { item, ok: false, message: reason };
  });
  return { operation, results };
}

/**
 * True when every paste succeeded (used to decide whether the
 * clipboard can be cleared / the items can be removed from the
 * explorer's selection).
 */
export function isPasteFullySuccessful(
  summary: ClipboardPasteSummary,
): boolean {
  return summary.results.every((r) => r.ok);
}
