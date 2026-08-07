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

import type { CommunityVisibleObject } from "../api/developer/types";

/**
 * Curated ObjectTypeEnum names used for community visibility filtering.
 * Matches FR inventory §5.8 object classes (not the full enum).
 */
export const COMMUNITY_VISIBILITY_TYPE_OPTIONS: ReadonlyArray<{
  value: string;
  label: string;
}> = [
  { value: "", label: "All types" },
  { value: "NODEDEF", label: "Content type (NODEDEF)" },
  { value: "TEMPLATE", label: "Template" },
  { value: "SLOT", label: "Slot" },
  { value: "DISPLAY_FORMAT", label: "Display format" },
  { value: "SEARCH_DEF", label: "Search" },
  { value: "VIEW_DEF", label: "View" },
  { value: "SITE", label: "Site" },
  { value: "WORKFLOW", label: "Workflow" },
  { value: "ACTION", label: "Menu action" },
  { value: "ITEM_FILTER", label: "Item filter" },
] as const;

/** Normalize a free-text query for case-insensitive substring match. */
export function normalizeVisibilityQuery(query: string | undefined | null): string {
  return (query ?? "").trim().toLowerCase();
}

/**
 * Client-side filter over objects already returned by the visibility API.
 * Matches name, label, or type (case-insensitive substring).
 */
export function filterVisibleObjects(
  objects: CommunityVisibleObject[],
  nameQuery: string | undefined | null,
): CommunityVisibleObject[] {
  const q = normalizeVisibilityQuery(nameQuery);
  if (!q) return objects;
  return objects.filter((o) => {
    const name = (o.name ?? "").toLowerCase();
    const label = (o.label ?? "").toLowerCase();
    const type = (o.type ?? "").toLowerCase();
    return name.includes(q) || label.includes(q) || type.includes(q);
  });
}

export type VisibilityEmptyKind = "none" | "type-filter" | "name-filter";

/**
 * Classify empty-state reason for the visibility table.
 * Returns null when there are rows to show.
 */
export function visibilityEmptyKind(
  serverCount: number,
  displayedCount: number,
  typeFilter: string | undefined | null,
  nameQuery: string | undefined | null,
): VisibilityEmptyKind | null {
  if (displayedCount > 0) return null;
  if (serverCount > 0 && normalizeVisibilityQuery(nameQuery)) {
    return "name-filter";
  }
  if (serverCount === 0 && (typeFilter ?? "").trim().length > 0) {
    return "type-filter";
  }
  if (serverCount === 0) return "none";
  return "name-filter";
}

/** Summary line counts for the filtered visibility table. */
export function visibilitySummaryCounts(
  serverCount: number,
  displayedCount: number,
): { total: number; shown: number; filtered: boolean } {
  return {
    total: serverCount,
    shown: displayedCount,
    filtered: displayedCount !== serverCount,
  };
}
