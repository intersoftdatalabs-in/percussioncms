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

import type { SearchFieldSummary } from "../api/developer/types";
import { DISPLAY_FORMAT_FIELD_CATALOG } from "./displayFormatColumns";

/** Installer / perc.System search names — field-criteria read-only in Developer chrome. */
export const PACKAGED_SEARCH_NAMES: readonly string[] = ["Default_Search", "RC_Search"];

/** Reuse the DF column field catalog for UI-08 search field-selection. */
export const SEARCH_FIELD_CATALOG = DISPLAY_FORMAT_FIELD_CATALOG;

/** PSSearchField.FIELDNAME_LENGTH */
export const SEARCH_FIELD_NAME_MAX = 128;

export const SEARCH_FIELD_OPERATORS: readonly { value: string; label: string }[] = [
  { value: "like", label: "like" },
  { value: "equal", label: "equals" },
  { value: "notEqual", label: "not equal" },
  { value: "lessThan", label: "less than" },
  { value: "greaterThan", label: "greater than" },
  { value: "isNull", label: "is null" },
  { value: "isNotNull", label: "is not null" },
];

export const DEFAULT_SEARCH_FIELD_OPERATOR = "like";

export function normalizeFieldName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

export function fieldNameKey(name: string | undefined | null): string {
  return normalizeFieldName(name).toLowerCase();
}

/** Packaged/system searches are not field-edited from this catalog. */
export function isPackagedSearch(name: string | undefined | null): boolean {
  const key = normalizeFieldName(name);
  if (!key) {
    return false;
  }
  return PACKAGED_SEARCH_NAMES.some((n) => n.toLowerCase() === key.toLowerCase());
}

/**
 * REST field name: non-blank, no whitespace/wildcards/path chars, max 128.
 */
export function isValidSearchFieldName(name: string | undefined | null): boolean {
  const key = normalizeFieldName(name);
  if (!key) {
    return false;
  }
  if (key.length > SEARCH_FIELD_NAME_MAX) {
    return false;
  }
  if (key !== (name ?? "").trim()) {
    return false;
  }
  if (/\s/.test(key)) {
    return false;
  }
  if (key.includes("..") || key.includes("/") || key.includes("\\") || key.includes("\0")) {
    return false;
  }
  if (key.includes("*") || key.includes("%")) {
    return false;
  }
  return true;
}

export function hasFieldName(fields: SearchFieldSummary[], name: string): boolean {
  const key = fieldNameKey(name);
  if (!key) {
    return false;
  }
  return fields.some((f) => fieldNameKey(f.fieldName) === key);
}

export function catalogFieldsNotInUse(
  fields: SearchFieldSummary[],
): { source: string; label: string }[] {
  return SEARCH_FIELD_CATALOG.filter((f) => !hasFieldName(fields, f.source));
}

export function addSearchFieldCriterion(
  fields: SearchFieldSummary[],
  fieldName: string,
  operator?: string,
  fieldValue?: string,
): SearchFieldSummary[] {
  const key = normalizeFieldName(fieldName);
  if (!isValidSearchFieldName(key) || hasFieldName(fields, key)) {
    return fields;
  }
  const label =
    SEARCH_FIELD_CATALOG.find((f) => fieldNameKey(f.source) === fieldNameKey(key))?.label || key;
  const op =
    (operator && operator.trim()) || DEFAULT_SEARCH_FIELD_OPERATOR;
  return [
    ...fields,
    {
      fieldName: key,
      displayName: label,
      operator: op,
      fieldValue: fieldValue ?? "",
      fieldType: "Text",
      position: fields.length,
    },
  ];
}

export function removeSearchFieldCriterion(
  fields: SearchFieldSummary[],
  index: number,
): SearchFieldSummary[] {
  if (index < 0 || index >= fields.length) {
    return fields;
  }
  return reindexSearchFields(fields.filter((_, i) => i !== index));
}

export function moveSearchFieldCriterion(
  fields: SearchFieldSummary[],
  index: number,
  delta: -1 | 1,
): SearchFieldSummary[] {
  const next = index + delta;
  if (index < 0 || index >= fields.length || next < 0 || next >= fields.length) {
    return fields;
  }
  const copy = fields.slice();
  const [row] = copy.splice(index, 1);
  copy.splice(next, 0, row);
  return reindexSearchFields(copy);
}

export function updateSearchFieldCriterion(
  fields: SearchFieldSummary[],
  index: number,
  patch: Partial<Pick<SearchFieldSummary, "operator" | "fieldValue">>,
): SearchFieldSummary[] {
  if (index < 0 || index >= fields.length) {
    return fields;
  }
  return fields.map((f, i) => (i === index ? { ...f, ...patch } : f));
}

export function reindexSearchFields(fields: SearchFieldSummary[]): SearchFieldSummary[] {
  return fields.map((f, i) => ({ ...f, position: i }));
}

export function normalizeSearchOperator(operator: string | undefined | null): string {
  const op = operator == null ? "" : operator.trim();
  if (!op) {
    return DEFAULT_SEARCH_FIELD_OPERATOR;
  }
  if (op === "=" || op.toLowerCase() === "eq" || op.toLowerCase() === "equals") {
    return "equal";
  }
  const known = SEARCH_FIELD_OPERATORS.find((o) => o.value.toLowerCase() === op.toLowerCase());
  return known ? known.value : op;
}

export function normalizeSearchFields(
  fields: SearchFieldSummary[] | undefined | null,
): SearchFieldSummary[] {
  if (fields == null || !Array.isArray(fields)) {
    return [];
  }
  return reindexSearchFields(
    fields.map((f) => ({ ...f, operator: normalizeSearchOperator(f.operator) })),
  );
}

export function fieldCriteriaSignature(fields: SearchFieldSummary[]): string {
  return fields
    .map(
      (f) =>
        `${fieldNameKey(f.fieldName)}\t${(f.operator || "").toLowerCase()}\t${f.fieldValue || ""}`,
    )
    .join("\n");
}

export function fieldCriteriaEqual(a: SearchFieldSummary[], b: SearchFieldSummary[]): boolean {
  return fieldCriteriaSignature(a) === fieldCriteriaSignature(b);
}
