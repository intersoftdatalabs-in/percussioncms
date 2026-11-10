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
 * Per-column cell renderers for the modern Content Explorer detail list
 * (#5777).
 *
 * <p>Extracted from {@code DetailList.renderDisplayFormatCell} (cognitive
 * 25, cyclomatic 37) so each display-format column has a small, focused
 * renderer behind the existing {@link DetailColumnId} dispatch. The list
 * sort ({@code compareDetailRows}) and the rendered cell text go through
 * {@link renderDisplayFormatCell}, which is now a thin map lookup — the
 * cell-text contract is unchanged and stays pinned by
 * {@code DetailList.test.tsx} + {@code DetailList.sort.test.tsx}.</p>
 *
 * <p>Adding a new column requires (1) adding the column id to
 * {@link DetailColumnId} + {@link DETAIL_COLUMN_IDS}, (2) adding a
 * renderer here and registering it in {@link DETAIL_CELL_RENDERERS},
 * (3) adding a translation key in {@code EXPLORER_MSG}.</p>
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import {
  explorerSiteDisplayName,
  isExplorerSiteRootItem,
} from "./sitePath";

/**
 * Public display-format column contract. Maps to the live
 * `PSX_DISPLAYFORMATPROPERTY_VIEW` rows the modern list can render
 * today from {@link PSPathItem} (i.e. columns whose values are present on
 * the per-row item the `paginatedFolder` endpoint returns).
 *
 * The `modified` and `workflow` columns live on
 * {@link PSFolderProperties} rather than `PSPathItem`; they are noted
 * here for the future when the API exposes per-row modification dates
 * and workflow IDs. Until then, those column ids resolve to empty
 * strings (the test fixture asserts this behaviour).
 */
export type DetailColumnId =
  | "name"
  | "type"
  | "path"
  | "title"
  | "category"
  | "modified"
  | "workflow";

/** Every supported column id in canonical order. */
export const DETAIL_COLUMN_IDS: readonly DetailColumnId[] = [
  "name",
  "type",
  "path",
  "title",
  "category",
  "modified",
  "workflow",
];

/**
 * Prefer pathmanagement displayProperties (columnData) when a display
 * format id was requested — keys are CX system field names. Exact key
 * first, then a case-insensitive scan, then empty string.
 */
export function lookupDisplayProperty(
  item: PSPathItem,
  keys: string[],
): string {
  const props = item.displayProperties;
  if (!props || typeof props !== "object") return "";
  for (const key of keys) {
    const v = props[key];
    if (v != null && String(v).length > 0) return String(v);
  }
  const lowerKeys = keys.map((k) => k.toLowerCase());
  for (const [k, v] of Object.entries(props)) {
    if (lowerKeys.includes(k.toLowerCase()) && v != null && String(v).length > 0) {
      return String(v);
    }
  }
  return "";
}

/** Name column. Site rows expose the finder SITENAME, never a GUID sys_title (#3684). */
export function renderNameCell(item: PSPathItem): string {
  const fromSys = lookupDisplayProperty(item, ["sys_title", "name", "Name"]);
  if (isExplorerSiteRootItem(item)) {
    return explorerSiteDisplayName(item) || fromSys || item.path || "";
  }
  return fromSys || item.name || item.path || "";
}

/** Type column, falling back through content-type, item type, and category. */
export function renderTypeCell(item: PSPathItem): string {
  return (
    lookupDisplayProperty(item, [
      "sys_contenttypename",
      "sys_contenttype",
      "type",
      "Type",
    ]) ||
    item.type ||
    item.category ||
    ""
  );
}

/** Path column — the raw repository path, never blanked by display properties. */
export function renderPathCell(item: PSPathItem): string {
  return item.path ?? "";
}

/** Title column, falling back to the item name. */
export function renderTitleCell(item: PSPathItem): string {
  return (
    lookupDisplayProperty(item, ["sys_title", "title", "Title"]) ||
    item.title ||
    item.name ||
    ""
  );
}

/** Category column. */
export function renderCategoryCell(item: PSPathItem): string {
  return (
    lookupDisplayProperty(item, ["category", "Category"]) || item.category || ""
  );
}

/**
 * Modified column. `PSPathItem` does not carry this field yet (it lives
 * on `PSFolderProperties`); returns empty strings until the API exposes
 * per-row modification dates. The unit test asserts this contract.
 */
export function renderModifiedCell(item: PSPathItem): string {
  return (
    lookupDisplayProperty(item, [
      "sys_contentlastmodifieddate",
      "sys_contentmodifieddate",
      "lastModified",
      "modified",
    ]) ||
    (item as unknown as { lastModified?: string }).lastModified ||
    ""
  );
}

/**
 * Workflow column. `PSPathItem` does not carry this field yet (it lives
 * on `PSFolderProperties`); returns empty strings until the API exposes
 * per-row workflow IDs. The unit test asserts this contract.
 */
export function renderWorkflowCell(item: PSPathItem): string {
  return (
    lookupDisplayProperty(item, [
      "sys_workflow",
      "sys_workflowid",
      "workflow",
      "workflowId",
    ]) ||
    (item as unknown as { workflowId?: string }).workflowId ||
    ""
  );
}

/** Per-column renderer behind the {@link DetailColumnId} dispatch. */
export const DETAIL_CELL_RENDERERS: Record<
  DetailColumnId,
  (item: PSPathItem) => string
> = {
  name: renderNameCell,
  type: renderTypeCell,
  path: renderPathCell,
  title: renderTitleCell,
  category: renderCategoryCell,
  modified: renderModifiedCell,
  workflow: renderWorkflowCell,
};

/**
 * Per-cell renderer. Returns the cell text for the given column + item.
 * Thin dispatch over {@link DETAIL_CELL_RENDERERS} so the list sort and
 * the rendered cells share one contract. Pure so the unit test can drive
 * it without rendering. Unknown ids resolve to empty string.
 */
export function renderDisplayFormatCell(
  column: DetailColumnId,
  item: PSPathItem,
): string {
  return DETAIL_CELL_RENDERERS[column]?.(item) ?? "";
}
