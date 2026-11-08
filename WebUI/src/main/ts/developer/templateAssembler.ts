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
 * Normalized assembler display labels for the template editor
 * (parent #2626 slice 1; #5817).
 *
 * <p>The REST template detail carries the raw assembler extension reference
 * (full path such as {@code Java/global/percussion/assembly/htmlAssembler}
 * or a short id such as {@code velocityAssembler}). The Developer template
 * detail panel shows the canonical epic label for every registered assembler
 * so operators do not have to decode extension paths. Unknown values fall
 * back to the raw string so existing templates round-trip; blank values
 * stay blank (the panel renders an em-dash).
 *
 * <p>Catalog source: {@code modules/extensions-main/src/main/resources/Java/Extensions.xml}
 * ({@code categorystring="assembly"}) plus the #2628 HTML-first / Markdown
 * registrations and the CM1 page assemblers.
 */
export const TEMPLATE_ASSEMBLER_LABELS: Readonly<Record<string, string>> = {
  velocityAssembler: "Velocity (power path)",
  htmlAssembler: "HTML-first",
  markdownAssembler: "Markdown",
  legacyAssembler: "Legacy XSL",
  pageAssembler: "Page",
  pageVariantAssembler: "Page variant",
  pageDatabaseAssembler: "Page database",
  binaryAssembler: "Binary",
  dispatchAssembler: "Dispatch",
  databaseAssembler: "Database",
  resourceAssembler: "Resource",
  debugAssembler: "Debug",
};

/**
 * Short assembler id for a raw detail value. Full extension paths resolve to
 * the segment after the last {@code /}; short ids pass through. Blank,
 * missing, or non-string values yield {@code null}.
 */
export function templateAssemblerShortId(raw: unknown): string | null {
  if (typeof raw !== "string") {
    return null;
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return null;
  }
  const tail = trimmed.split("/").pop()?.trim() ?? "";
  return tail ? tail : null;
}

/**
 * Display label for a raw template-detail assembler value. Known catalog ids
 * map to their canonical label; unknown non-blank values fall back to the
 * raw trimmed string; blank/missing/non-string values yield {@code null}.
 */
export function templateAssemblerLabel(raw: unknown): string | null {
  const shortId = templateAssemblerShortId(raw);
  if (shortId == null) {
    return null;
  }
  const known = TEMPLATE_ASSEMBLER_LABELS[shortId];
  if (known) {
    return known;
  }
  return typeof raw === "string" ? raw.trim() : shortId;
}
