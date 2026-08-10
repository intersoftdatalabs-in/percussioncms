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

import type { TemplateBindingSummary } from "../api/developer/types";

/** Deep-clone bindings for local editor state. */
export function cloneBindings(
  list: TemplateBindingSummary[] | undefined,
): TemplateBindingSummary[] {
  return (list || []).map((b) => ({
    executionOrder: b.executionOrder,
    variable: b.variable || "",
    expression: b.expression || "",
  }));
}

/** True when both lists have the same order/variable/expression values. */
export function bindingsEqual(
  a: TemplateBindingSummary[],
  b: TemplateBindingSummary[],
): boolean {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) {
    if (
      (a[i].executionOrder ?? null) !== (b[i].executionOrder ?? null) ||
      (a[i].variable || "") !== (b[i].variable || "") ||
      (a[i].expression || "") !== (b[i].expression || "")
    ) {
      return false;
    }
  }
  return true;
}

/**
 * Client-side validation aligned with sitemanage {@code TemplateAdaptor#toBindings}:
 * each non-null binding requires non-blank variable and expression.
 *
 * @returns null when valid; otherwise a human-readable error for the first bad row
 */
export function validateBindings(
  bindings: TemplateBindingSummary[],
): string | null {
  for (let i = 0; i < bindings.length; i++) {
    const b = bindings[i];
    if (b == null) {
      return `bindings[${i}] is null`;
    }
    if (!(b.variable || "").trim()) {
      return `bindings[${i}].variable is required`;
    }
    if (!(b.expression || "").trim()) {
      return `bindings[${i}].expression is required`;
    }
  }
  return null;
}

/** Normalize order + trim for PUT body (full replace). */
export function normalizeBindingsForSave(
  bindings: TemplateBindingSummary[],
): TemplateBindingSummary[] {
  return bindings.map((b, i) => ({
    executionOrder:
      b.executionOrder != null && b.executionOrder > 0
        ? b.executionOrder
        : i + 1,
    variable: (b.variable || "").trim(),
    expression: (b.expression || "").trim(),
  }));
}
