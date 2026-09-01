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

import type { ViewFieldSummary } from "../api/developer/types";
import {
  DISPLAY_FORMAT_FIELD_CATALOG,
  isValidColumnSource,
  normalizeColumnSource,
} from "./displayFormatColumns";

/** Same CX field catalog as display-format columns (UI-08 shared picker). */
export const VIEW_FIELD_CATALOG = DISPLAY_FORMAT_FIELD_CATALOG;

export const VIEW_FIELD_OPERATORS: readonly { value: string; label: string }[] = [
  { value: "equal", label: "equals" },
  { value: "notEqual", label: "not equal" },
  { value: "like", label: "like" },
  { value: "notLike", label: "not like" },
  { value: "greaterThan", label: "greater than" },
  { value: "lessThan", label: "less than" },
  { value: "isNull", label: "is null" },
  { value: "isNotNull", label: "is not null" },
];

export const VIEW_FIELD_TYPES: readonly string[] = ["Text", "Number", "Date"];

export const DEFAULT_VIEW_FIELD_OPERATOR = "equal";
export const DEFAULT_VIEW_FIELD_TYPE = "Text";

export function normalizeViewFieldName(name: string | undefined | null): string {
  if (name == null) {
    return "";
  }
  if (typeof name !== "string") {
    return String(name).trim();
  }
  return normalizeColumnSource(name);
}

export function viewFieldNameKey(name: string | undefined | null): string {
  return normalizeViewFieldName(name).toLowerCase();
}

export function isValidViewFieldName(name: string | undefined | null): boolean {
  return isValidColumnSource(name);
}

export function isKnownViewFieldName(name: string | undefined | null): boolean {
  const key = viewFieldNameKey(name);
  if (!key) {
    return false;
  }
  return VIEW_FIELD_CATALOG.some((f) => viewFieldNameKey(f.source) === key);
}

export function hasViewField(fields: ViewFieldSummary[], name: string): boolean {
  const key = viewFieldNameKey(name);
  if (!key) {
    return false;
  }
  return fields.some((f) => viewFieldNameKey(f.fieldName) === key);
}

export function catalogViewFieldsNotInUse(
  fields: ViewFieldSummary[],
): { source: string; label: string }[] {
  return VIEW_FIELD_CATALOG.filter((f) => !hasViewField(fields, f.source));
}

export function addViewFieldCriterion(
  fields: ViewFieldSummary[],
  source: string,
  operator = DEFAULT_VIEW_FIELD_OPERATOR,
  fieldValue = "",
  fieldType = DEFAULT_VIEW_FIELD_TYPE,
): ViewFieldSummary[] {
  const key = normalizeViewFieldName(source);
  if (!isValidViewFieldName(key) || !isKnownViewFieldName(key) || hasViewField(fields, key)) {
    return fields;
  }
  const label = VIEW_FIELD_CATALOG.find((f) => viewFieldNameKey(f.source) === viewFieldNameKey(key))
    ?.label || key;
  return reindexViewFields([
    ...fields,
    {
      fieldName: key,
      displayName: label,
      operator: operator || DEFAULT_VIEW_FIELD_OPERATOR,
      fieldValue: fieldValue ?? "",
      fieldType: fieldType || DEFAULT_VIEW_FIELD_TYPE,
      position: fields.length,
    },
  ]);
}

export function removeViewFieldCriterion(fields: ViewFieldSummary[], index: number): ViewFieldSummary[] {
  if (index < 0 || index >= fields.length) {
    return fields;
  }
  return reindexViewFields(fields.filter((_, i) => i !== index));
}

export function moveViewFieldCriterion(
  fields: ViewFieldSummary[],
  index: number,
  delta: -1 | 1,
): ViewFieldSummary[] {
  const next = index + delta;
  if (index < 0 || index >= fields.length || next < 0 || next >= fields.length) {
    return fields;
  }
  const copy = fields.slice();
  const [row] = copy.splice(index, 1);
  copy.splice(next, 0, row);
  return reindexViewFields(copy);
}

export function patchViewFieldCriterion(
  fields: ViewFieldSummary[],
  index: number,
  patch: Partial<ViewFieldSummary>,
): ViewFieldSummary[] {
  if (index < 0 || index >= fields.length) {
    return fields;
  }
  const copy = fields.slice();
  copy[index] = { ...copy[index], ...patch };
  return copy;
}

export function reindexViewFields(fields: ViewFieldSummary[]): ViewFieldSummary[] {
  return fields.map((f, i) => ({ ...f, position: i }));
}

export function viewFieldsSignature(fields: ViewFieldSummary[]): string {
  return fields
    .map(
      (f) =>
        `${viewFieldNameKey(f.fieldName)}|${(f.operator || "").trim()}|${f.fieldValue ?? ""}|${
          f.fieldType || ""
        }`,
    )
    .join("\n");
}

export function viewFieldsEqual(a: ViewFieldSummary[], b: ViewFieldSummary[]): boolean {
  return viewFieldsSignature(a) === viewFieldsSignature(b);
}

function asFieldRows(fields: unknown): ViewFieldSummary[] {
  if (fields == null) {
    return [];
  }
  if (Array.isArray(fields)) {
    return fields.filter((row): row is ViewFieldSummary => row != null && typeof row === "object");
  }
  if (typeof fields === "object") {
    const rec = fields as Record<string, unknown>;
    const inner = rec.ViewFieldSummary ?? rec.viewFieldSummary ?? rec.fields;
    if (inner != null && inner !== fields) {
      return asFieldRows(inner);
    }
  }
  return [];
}

export function normalizeViewFields(fields: ViewFieldSummary[] | undefined | null): ViewFieldSummary[] {
  return reindexViewFields(
    asFieldRows(fields).map((f) => ({
      ...f,
      fieldName: normalizeViewFieldName(f.fieldName),
      displayName: typeof f.displayName === "string" ? f.displayName : f.displayName == null ? "" : String(f.displayName),
      operator: typeof f.operator === "string" ? f.operator : f.operator == null ? "" : String(f.operator),
      fieldValue: typeof f.fieldValue === "string" ? f.fieldValue : f.fieldValue == null ? "" : String(f.fieldValue),
      fieldType: typeof f.fieldType === "string" ? f.fieldType : f.fieldType == null ? "" : String(f.fieldType),
    })),
  );
}
