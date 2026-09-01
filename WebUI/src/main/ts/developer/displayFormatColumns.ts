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

import type { DisplayFormatColumn } from "../api/developer/types";

/** Installer / perc.System catalog names — read-only in Developer chrome. */
export const PACKAGED_DISPLAY_FORMAT_NAMES: readonly string[] = [
  "Default",
  "Simple",
  "Extended",
  "By_Type",
  "By_Status",
  "By_Author",
  "Related_Content_By_Type",
  "Related_Content_By_Template",
  "Related_Content_By_Variant",
  "RankedSearchResults",
  "CM1_Default",
  "CM1_Design",
];

/**
 * Common CX field sources for the UI-08 column picker (display formats) and
 * search field-selection ({@code searchFieldCriteria.ts}).
 */
export const DISPLAY_FORMAT_FIELD_CATALOG: readonly { source: string; label: string }[] = [
  { source: "sys_title", label: "Content Title" },
  { source: "sys_checkoutstatus", label: "Checkout status" },
  { source: "sys_statename", label: "State" },
  { source: "sys_contenttypename", label: "Content type" },
  { source: "sys_contentcreatedby", label: "Created by" },
  { source: "sys_contentcreateddate", label: "Created date" },
  { source: "sys_contentlastmodifieddate", label: "Last modified" },
  { source: "sys_contentid", label: "Content id" },
  { source: "sys_workflow", label: "Workflow" },
  { source: "sys_postdate", label: "Post date" },
  { source: "sys_size", label: "Size" },
  { source: "sys_locale", label: "Locale" },
  { source: "sys_communityid", label: "Community" },
  { source: "sys_checkoutuser", label: "Checked out by" },
];

/** sys_title is required by PSDisplayFormat#setColumnList. */
export const SYS_TITLE_SOURCE = "sys_title";

/** PSDisplayColumn.SOURCE_LENGTH */
export const COLUMN_SOURCE_MAX = 128;

export function normalizeColumnSource(source: string | undefined | null): string {
  return source == null ? "" : source.trim();
}

export function columnSourceKey(source: string | undefined | null): string {
  return normalizeColumnSource(source).toLowerCase();
}

/** Packaged/system formats are not column-edited from this catalog. */
export function isPackagedDisplayFormat(name: string | undefined | null): boolean {
  const key = normalizeColumnSource(name);
  if (!key) {
    return false;
  }
  return PACKAGED_DISPLAY_FORMAT_NAMES.some((n) => n.toLowerCase() === key.toLowerCase());
}

/**
 * REST column source: non-blank, no whitespace/wildcards/path chars, max 128.
 * Shared fields may include {@code :}.
 */
export function isValidColumnSource(source: string | undefined | null): boolean {
  const key = normalizeColumnSource(source);
  if (!key) {
    return false;
  }
  if (key.length > COLUMN_SOURCE_MAX) {
    return false;
  }
  if (key !== (source ?? "").trim()) {
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

export function isSysTitleColumn(col: DisplayFormatColumn): boolean {
  return columnSourceKey(col.source) === SYS_TITLE_SOURCE;
}

/** True when {@code source} is already in the column list (case-insensitive). */
export function hasColumnSource(columns: DisplayFormatColumn[], source: string): boolean {
  const key = columnSourceKey(source);
  if (!key) {
    return false;
  }
  return columns.some((c) => columnSourceKey(c.source) === key);
}

export function catalogFieldsNotInUse(columns: DisplayFormatColumn[]): { source: string; label: string }[] {
  return DISPLAY_FORMAT_FIELD_CATALOG.filter((f) => !hasColumnSource(columns, f.source));
}

export function addDisplayFormatColumn(
  columns: DisplayFormatColumn[],
  source: string,
  displayName?: string,
): DisplayFormatColumn[] {
  const key = normalizeColumnSource(source);
  if (!isValidColumnSource(key) || hasColumnSource(columns, key)) {
    return columns;
  }
  const label =
    (displayName && displayName.trim()) ||
    DISPLAY_FORMAT_FIELD_CATALOG.find((f) => columnSourceKey(f.source) === columnSourceKey(key))
      ?.label ||
    key;
  return [
    ...columns,
    {
      source: key,
      displayName: label,
      position: columns.length,
      renderType: "Text",
      width: 0,
      ascendingSort: true,
    },
  ];
}

export function removeDisplayFormatColumn(
  columns: DisplayFormatColumn[],
  index: number,
): DisplayFormatColumn[] {
  if (index < 0 || index >= columns.length) {
    return columns;
  }
  if (isSysTitleColumn(columns[index])) {
    return columns;
  }
  return reindexColumns(columns.filter((_, i) => i !== index));
}

export function moveDisplayFormatColumn(
  columns: DisplayFormatColumn[],
  index: number,
  delta: -1 | 1,
): DisplayFormatColumn[] {
  const next = index + delta;
  if (index < 0 || index >= columns.length || next < 0 || next >= columns.length) {
    return columns;
  }
  const copy = columns.slice();
  const [row] = copy.splice(index, 1);
  copy.splice(next, 0, row);
  return reindexColumns(copy);
}

export function reindexColumns(columns: DisplayFormatColumn[]): DisplayFormatColumn[] {
  return columns.map((c, i) => ({ ...c, position: i }));
}

/** Order + source identity for dirty detection (labels/render follow the row). */
export function columnOrderSignature(columns: DisplayFormatColumn[]): string {
  return columns.map((c) => columnSourceKey(c.source)).join("\n");
}

export function columnsOrderEqual(a: DisplayFormatColumn[], b: DisplayFormatColumn[]): boolean {
  return columnOrderSignature(a) === columnOrderSignature(b);
}
