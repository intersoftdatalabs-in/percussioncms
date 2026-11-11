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

import { resolveDisplayFormatObjectGuid } from "../api/developer/displayFormatsApi";
import type { DisplayFormat } from "../api/developer/types";
import { matchesCatalogQuery } from "./CatalogSearchFilter";

/**
 * Detail-panel selection for a catalog entry.
 * null = catalog; "new" = create; otherwise name or GUID.
 */
export type DisplayFormatSelection = {
  idOrName: string | "new";
  /** List-row GUID fallback when detail payload omits stringValue (#2951). */
  catalogGuid?: string;
};

/** Catalog key for opening detail (name preferred, internal name then GUID fallback). */
export function displayFormatOpenKey(f: DisplayFormat): string | null {
  return f.name || f.internalName || resolveDisplayFormatObjectGuid(f) || null;
}

/** Stable React key for a catalog row (identity fallback to position). */
export function displayFormatRowKey(f: DisplayFormat, index: number): string {
  return resolveDisplayFormatObjectGuid(f) || f.name || `df-${index}`;
}

/** Case-insensitive name sort; null catalog sorts to empty. */
export function sortDisplayFormatCatalog(items: DisplayFormat[] | null): DisplayFormat[] {
  if (!items) return [];
  return [...items].sort((a, b) =>
    (a.name || a.displayName || "").localeCompare(
      b.name || b.displayName || "",
      undefined,
      { sensitivity: "base" },
    ),
  );
}

/**
 * Client-side query filter over name/label/internal/display/description
 * (never mutates, never triggers a fetch).
 */
export function filterDisplayFormatCatalog(
  sorted: DisplayFormat[],
  query: string,
): DisplayFormat[] {
  return sorted.filter((f) =>
    matchesCatalogQuery(
      [f.name, f.label, f.internalName, f.displayName, f.description],
      query,
    ),
  );
}

/** Selection payload for opening detail; null when the entry has no usable key. */
export function displayFormatSelection(f: DisplayFormat): DisplayFormatSelection | null {
  const idOrName = displayFormatOpenKey(f) || "";
  if (!idOrName) return null;
  return {
    idOrName,
    catalogGuid: resolveDisplayFormatObjectGuid(f),
  };
}
