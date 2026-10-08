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
 * Client-side HTML field checks for EditorHost save.
 * Unsafe markup stays the XSS heuristic (#4680).
 * A NUL cannot be stored in item field XML / JDBC text (#5389).
 * Long-text NUL stays in {@code longTextField.ts}.
 * Single-line NUL stays in {@code singleLineTextField.ts}.
 */

import type { EditorWidgetKind } from "./controlKinds";

export interface EditorHtmlRow {
  name: string;
  kind: EditorWidgetKind;
  value: string;
}

/** Script tags, inline event handlers, or javascript: URLs. */
export function htmlLooksUnsafe(html: string): boolean {
  const s = html ?? "";
  return (
    /<\s*script\b/i.test(s) ||
    /\bon[a-z]+\s*=/i.test(s) ||
    /javascript\s*:/i.test(s)
  );
}

export function collectUnsafeHtmlFieldErrors(
  rows: readonly EditorHtmlRow[],
  unsafeMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "html") {
      continue;
    }
    if (htmlLooksUnsafe(row.value)) {
      out[row.name] = unsafeMessage;
    }
  }
  return out;
}

export function htmlContainsNul(value: string): boolean {
  return (value ?? "").includes("\u0000");
}

export function collectInvalidHtmlFieldErrors(
  rows: readonly EditorHtmlRow[],
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (row.kind !== "html") {
      continue;
    }
    if (htmlContainsNul(row.value)) {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}
