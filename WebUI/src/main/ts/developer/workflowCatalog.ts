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

import type { WorkflowDef } from "../api/developer/types";
import { matchesCatalogQuery } from "./CatalogSearchFilter";

/**
 * Selection prefix for the workflow copy flow (`copy:<sourceName>`).
 * Kept beside {@link parseWorkflowSelection} so the panel and the table
 * agree on the encoding without string surgery in JSX.
 */
export const COPY_SELECTION_PREFIX = "copy:";

/** Discriminated view state for {@link parseWorkflowSelection}. */
export type WorkflowSelection =
  | { kind: "create" }
  | { kind: "copy"; sourceName: string }
  | { kind: "detail"; name: string }
  | { kind: "catalog" };

/**
 * Decode the panel `selected` state into a view branch. Centralizes the
 * `"new"` / `"copy:<name>"` / detail-string protocol so the panel render
 * is a flat switch instead of nested `startsWith`/`slice` surgery.
 */
export function parseWorkflowSelection(selected: string | "new" | null): WorkflowSelection {
  if (selected === "new") {
    return { kind: "create" };
  }
  if (selected != null && selected.startsWith(COPY_SELECTION_PREFIX)) {
    return { kind: "copy", sourceName: selected.slice(COPY_SELECTION_PREFIX.length) };
  }
  if (selected) {
    return { kind: "detail", name: selected };
  }
  return { kind: "catalog" };
}

/** Catalog rows sorted by workflow name (base sensitivity); null-safe. */
export function sortWorkflowCatalog(items: WorkflowDef[] | null): WorkflowDef[] {
  if (!items) return [];
  return [...items]
    .filter((w) => (w.workflowName || "").trim().length > 0)
    .sort((a, b) =>
      (a.workflowName || "").localeCompare(b.workflowName || "", undefined, {
        sensitivity: "base",
      }),
    );
}

/** Client-side catalog filter; never triggers a fetch, never mutates. */
export function filterWorkflowCatalog(sorted: WorkflowDef[], query: string): WorkflowDef[] {
  return sorted.filter((w) =>
    matchesCatalogQuery([w.workflowName, w.workflowDescription], query),
  );
}

function workflowNameKey(name: string | undefined | null): string {
  return (name || "").trim().toLowerCase();
}

/**
 * Apply a successful detail-panel rename to the cached catalog list.
 * Returns the previous list reference when there is nothing to apply
 * (null list), so `setItems` callers stay a one-liner.
 */
export function renameWorkflowInCatalog(
  items: WorkflowDef[] | null,
  previousName: string,
  nextName: string,
): WorkflowDef[] | null {
  if (items == null) return items;
  const prev = workflowNameKey(previousName);
  return items.map((w) =>
    workflowNameKey(w.workflowName) === prev ? { ...w, workflowName: nextName } : w,
  );
}

/**
 * Clear the previous default flag when another workflow is set default
 * (detail-panel `onDefaultChanged`). Null-safe; returns the previous
 * reference when there is nothing to apply.
 */
export function markDefaultWorkflowInCatalog(
  items: WorkflowDef[] | null,
  workflowName: string,
): WorkflowDef[] | null {
  if (items == null) return items;
  const key = workflowNameKey(workflowName);
  return items.map((w) => ({
    ...w,
    defaultWorkflow: workflowNameKey(w.workflowName) === key,
  }));
}

/** Trimmed workflow name used as row identity and selection key. */
export function workflowOpenKey(w: WorkflowDef): string {
  return (w.workflowName || "").trim();
}

/** Stable row key for the catalog table. */
export function workflowRowKey(w: WorkflowDef, index: number): string {
  return `${workflowOpenKey(w)}-${index}`;
}

/** Step count for the catalog Steps column (non-array → 0). */
export function workflowStepCount(w: WorkflowDef): number {
  return Array.isArray(w.workflowSteps) ? w.workflowSteps.length : 0;
}
