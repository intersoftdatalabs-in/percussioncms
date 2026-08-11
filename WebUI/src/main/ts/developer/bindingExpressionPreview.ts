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
 * Pure helpers for long binding-expression preview / expand UX (UI-SRC-02).
 *
 * Keeps the bindings table layout stable (maxWidth clamp) while letting users
 * expand long JEXL expressions without a permanent layout blow-up.
 */

/** Cell max width used when rendering binding expressions (px). */
export const EXPRESSION_CLAMP_MAX_WIDTH_PX = 320;

/**
 * Default character threshold above which an expression is considered "long"
 * and gets a Show more / Show less control.
 */
export const EXPRESSION_LONG_CHAR_THRESHOLD = 80;

/**
 * Line count above which an expression is considered long even if short in
 * total characters (multi-line JEXL).
 */
export const EXPRESSION_LONG_LINE_THRESHOLD = 2;

/**
 * Collapsed textarea max height (px) — roughly two monospaced lines plus
 * padding so long expressions do not stretch the table row.
 */
export const EXPRESSION_COLLAPSED_MAX_HEIGHT_PX = 56;

/**
 * Expanded textarea max height (px) before internal scroll kicks in.
 */
export const EXPRESSION_EXPANDED_MAX_HEIGHT_PX = 240;

/**
 * Normalize expression input for length / line checks.
 */
export function normalizeBindingExpression(
  expression: string | null | undefined,
): string {
  return expression == null ? "" : String(expression);
}

/**
 * Count display lines (LF / CRLF / lone CR). Empty string is one logical line.
 */
export function bindingExpressionLineCount(
  expression: string | null | undefined,
): number {
  const text = normalizeBindingExpression(expression);
  if (text.length === 0) {
    return 1;
  }
  return text.split(/\r\n|\n|\r/).length;
}

/**
 * True when the expression should offer expand / collapse chrome.
 *
 * Long by character length and/or multi-line content beyond the thresholds.
 */
export function isLongBindingExpression(
  expression: string | null | undefined,
  options?: {
    maxChars?: number;
    maxLines?: number;
  },
): boolean {
  const text = normalizeBindingExpression(expression);
  const maxChars = options?.maxChars ?? EXPRESSION_LONG_CHAR_THRESHOLD;
  const maxLines = options?.maxLines ?? EXPRESSION_LONG_LINE_THRESHOLD;
  if (text.length > maxChars) {
    return true;
  }
  return bindingExpressionLineCount(text) > maxLines;
}

/**
 * CSS style for the expression editor surface (textarea).
 * Collapsed: clamped height + hidden overflow. Expanded: taller with scroll.
 */
export function bindingExpressionEditorStyle(
  expanded: boolean,
): {
  maxWidth: number;
  maxHeight: number;
  overflow: "hidden" | "auto";
  resize: "none" | "vertical";
  whiteSpace: "pre-wrap";
  overflowWrap: "anywhere";
  wordBreak: "break-word";
  boxSizing: "border-box";
} {
  return {
    maxWidth: EXPRESSION_CLAMP_MAX_WIDTH_PX,
    maxHeight: expanded
      ? EXPRESSION_EXPANDED_MAX_HEIGHT_PX
      : EXPRESSION_COLLAPSED_MAX_HEIGHT_PX,
    overflow: expanded ? "auto" : "hidden",
    resize: expanded ? "vertical" : "none",
    whiteSpace: "pre-wrap",
    overflowWrap: "anywhere",
    wordBreak: "break-word",
    boxSizing: "border-box",
  };
}
