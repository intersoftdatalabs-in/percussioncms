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

import type { CommunitySummary } from "../api/developer/types";
import { matchesCatalogQuery } from "./CatalogSearchFilter";

/** Detail-panel selection key for a catalog entry (name preferred, id fallback). */
export function communityOpenKey(c: CommunitySummary): string | null {
  if (c.name) return c.name;
  if (c.id != null) return String(c.id);
  if (c.guid?.stringValue) return c.guid.stringValue;
  return null;
}

/** Stable React key for a catalog row (identity fallback to position). */
export function communityRowKey(c: CommunitySummary, index: number): string {
  return String(c.id ?? c.guid?.stringValue ?? c.name ?? `comm-${index}`);
}

/** Case-insensitive label/name sort; null catalog sorts to empty. */
export function sortCommunityCatalog(items: CommunitySummary[] | null): CommunitySummary[] {
  if (!items) return [];
  return [...items].sort((a, b) =>
    (a.label || a.name || "").localeCompare(b.label || b.name || "", undefined, {
      sensitivity: "base",
    }),
  );
}

/** Client-side query filter over label/name/description/id (never mutates). */
export function filterCommunityCatalog(
  sorted: CommunitySummary[],
  query: string,
): CommunitySummary[] {
  return sorted.filter((c) =>
    matchesCatalogQuery(
      [c.label, c.name, c.description, c.id != null ? String(c.id) : undefined],
      query,
    ),
  );
}

function communityNameKey(name: string): string {
  return name.trim().toLowerCase();
}

/**
 * Optimistic catalog patch for rename / description saves: applies `patch`
 * to every entry whose name matches (case-insensitive). Returns the previous
 * reference untouched when nothing matches or the key is blank.
 */
export function patchCommunityByName(
  itemsPrev: CommunitySummary[] | null,
  name: string,
  patch: Partial<Pick<CommunitySummary, "name" | "label" | "description">>,
): CommunitySummary[] | null {
  const key = communityNameKey(name);
  if (itemsPrev == null || !key) return itemsPrev;
  if (!itemsPrev.some((c) => communityNameKey(c.name || "") === key)) return itemsPrev;
  return itemsPrev.map((c) =>
    communityNameKey(c.name || "") === key ? { ...c, ...patch } : c,
  );
}
