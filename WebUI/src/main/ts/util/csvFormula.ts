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
 * Shared spreadsheet-formula-injection neutralization for client-side CSV
 * exports (#6175).
 *
 * <p>RFC 4180 quoting alone does not stop formula injection: a hostile
 * content value such as {@code =cmd|' /C calc'!A0} survives quoting and
 * executes on open in Excel / Sheets. Following the OWASP CSV Injection
 * guidance, any field whose first character is {@code =}, {@code +},
 * {@code -}, {@code @}, horizontal tab, or carriage return is prefixed
 * with a single quote ({@code '}), which spreadsheet applications treat
 * as a text marker (shown in the formula bar, not the cell).</p>
 *
 * <p>Apply this <b>before</b> RFC 4180 quoting so the marker survives
 * inside quoted fields as well. Plain values (including negative numbers
 * like {@code -5}, which become the text {@code '-5}) are intentionally
 * left byte-identical only when they cannot start a formula — the text
 * coercion of leading-dash values is the documented trade-off of this
 * mitigation.</p>
 *
 * <p>Pure so Vitest can drive it without rendering. Shared by the
 * Explorer detail-list export ({@code contentExplorer/detailCsvExport})
 * and the publishing-logs export ({@code publishing/logsExport}).</p>
 */

/**
 * First characters that can start a spreadsheet formula or DDE command
 * when a CSV field opens with them.
 */
export const CSV_FORMULA_TRIGGER_CHARS: readonly string[] = [
  "=",
  "+",
  "-",
  "@",
  "\t",
  "\r",
];

/**
 * Prefix a single-quote text marker when {@code text} opens with a
 * formula-trigger character; otherwise return {@code text} unchanged.
 */
export function neutralizeCsvFormulaCell(text: string): string {
  if (text.length === 0) {
    return text;
  }
  const first = text.charAt(0);
  if (
    first === "=" ||
    first === "+" ||
    first === "-" ||
    first === "@" ||
    first === "\t" ||
    first === "\r"
  ) {
    return `'${text}`;
  }
  return text;
}
