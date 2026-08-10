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
 * Modern assembler catalog for Design SPA picker (#2810 / parent #2631).
 * Extension names match server plugins / IPSExtension constants where present.
 * See docs/ai-generated/tasks/template-assembler-normalization/binding-modules.md.
 */

export interface AssemblerOption {
  /** Full extension name stored on the template. */
  value: string;
  /** Short label for the select. */
  label: string;
  /** One-line guidance. */
  hint: string;
  /** Recommended for modern packages when true. */
  recommended?: boolean;
}

const PREFIX = "Java/global/percussion/assembly/";

export const ASSEMBLER_OPTIONS: readonly AssemblerOption[] = [
  {
    value: `${PREFIX}htmlAssembler`,
    label: "HTML-first",
    hint: "Simple HTML with ${path} placeholders — recommended for simple snippets.",
    recommended: true,
  },
  {
    value: `${PREFIX}markdownAssembler`,
    label: "Markdown",
    hint: "CommonMark → HTML after JEXL bindings.",
    recommended: true,
  },
  {
    value: `${PREFIX}velocityAssembler`,
    label: "Velocity",
    hint: "Macros, loops, #parse, Active Assembly macros.",
    recommended: true,
  },
  {
    value: `${PREFIX}pageAssembler`,
    label: "Page (CM1)",
    hint: "CM1 page context + regions ($perc).",
  },
  {
    value: `${PREFIX}legacyAssembler`,
    label: "Legacy / XSL",
    hint: "Compatibility only — not recommended for new templates.",
  },
  {
    value: `${PREFIX}binaryAssembler`,
    label: "Binary",
    hint: "Binary / resource output assembler.",
  },
  {
    value: `${PREFIX}dispatchAssembler`,
    label: "Dispatch",
    hint: "Dispatch to another template.",
  },
  {
    value: `${PREFIX}databaseAssembler`,
    label: "Database",
    hint: "Database result set assembler.",
  },
] as const;

/** Known full extension names from {@link ASSEMBLER_OPTIONS}. */
export const KNOWN_ASSEMBLER_VALUES: readonly string[] = ASSEMBLER_OPTIONS.map(
  (o) => o.value,
);

/**
 * Options for a select: known catalog first, then current value if custom/legacy.
 */
export function assemblerSelectOptions(
  current: string | null | undefined,
): AssemblerOption[] {
  const cur = (current || "").trim();
  const known = ASSEMBLER_OPTIONS.map((o) => ({ ...o }));
  if (cur && !known.some((o) => o.value === cur)) {
    known.unshift({
      value: cur,
      label: "Current (custom)",
      hint: "Assembler currently set on this template (not in the modern catalog).",
    });
  }
  return known;
}

/** True when value is a non-empty extension name. */
export function isValidAssemblerValue(value: string | null | undefined): boolean {
  return Boolean(value && value.trim().length > 0);
}
