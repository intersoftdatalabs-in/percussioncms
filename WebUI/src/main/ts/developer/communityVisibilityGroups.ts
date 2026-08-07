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
import { COMMUNITY_VISIBILITY_TYPE_OPTIONS } from "./communityVisibilityFilters";

/** Sentinel type key when the visibility API omits {@code type}. */
export const UNKNOWN_VISIBILITY_TYPE_KEY = "(unknown)";

/**
 * Curated ObjectTypeEnum order for SE-05 navigator groups (§5.8 object classes).
 * Types not in this list sort after, alphabetically.
 */
export const VISIBILITY_TYPE_GROUP_ORDER: readonly string[] = [
  "NODEDEF",
  "TEMPLATE",
  "SLOT",
  "DISPLAY_FORMAT",
  "SEARCH_DEF",
  "VIEW_DEF",
  "SITE",
  "WORKFLOW",
  "ACTION",
  "ITEM_FILTER",
] as const;

export interface VisibilityTypeGroup {
  /** Normalized type key (e.g. NODEDEF) or {@link UNKNOWN_VISIBILITY_TYPE_KEY}. */
  typeKey: string;
  /** Human label for the group header. */
  label: string;
  objects: CommunityVisibleObject[];
}

/** Normalize object type for grouping (trim + upper case; empty → unknown). */
export function normalizeVisibilityTypeKey(
  type: string | undefined | null,
): string {
  const t = (type ?? "").trim().toUpperCase();
  return t.length > 0 ? t : UNKNOWN_VISIBILITY_TYPE_KEY;
}

/**
 * Label for a visibility type group. Uses curated option labels when present;
 * otherwise the raw type key (or "Unknown type").
 */
export function visibilityTypeGroupLabel(typeKey: string): string {
  if (typeKey === UNKNOWN_VISIBILITY_TYPE_KEY) {
    return "Unknown type";
  }
  const opt = COMMUNITY_VISIBILITY_TYPE_OPTIONS.find(
    (o) => o.value.length > 0 && o.value.toUpperCase() === typeKey,
  );
  return opt?.label ?? typeKey;
}

function objectSortKey(o: CommunityVisibleObject): string {
  return (o.label || o.name || o.guid?.stringValue || String(o.id ?? "")).toLowerCase();
}

function compareObjects(a: CommunityVisibleObject, b: CommunityVisibleObject): number {
  return objectSortKey(a).localeCompare(objectSortKey(b), undefined, {
    sensitivity: "base",
  });
}

function typeOrderIndex(typeKey: string): number {
  if (typeKey === UNKNOWN_VISIBILITY_TYPE_KEY) {
    return Number.MAX_SAFE_INTEGER;
  }
  const idx = VISIBILITY_TYPE_GROUP_ORDER.indexOf(typeKey);
  return idx >= 0 ? idx : VISIBILITY_TYPE_GROUP_ORDER.length;
}

/**
 * Group design objects by type for the SE-05 community visibility navigator.
 * Empty input → empty array. Groups and objects are sorted stably for display.
 */
export function groupVisibleObjectsByType(
  objects: CommunityVisibleObject[],
): VisibilityTypeGroup[] {
  if (!objects || objects.length === 0) return [];

  const map = new Map<string, CommunityVisibleObject[]>();
  for (const o of objects) {
    const key = normalizeVisibilityTypeKey(o.type);
    const list = map.get(key);
    if (list) list.push(o);
    else map.set(key, [o]);
  }

  const groups: VisibilityTypeGroup[] = [];
  for (const [typeKey, objs] of map) {
    groups.push({
      typeKey,
      label: visibilityTypeGroupLabel(typeKey),
      objects: [...objs].sort(compareObjects),
    });
  }

  groups.sort((a, b) => {
    const oi = typeOrderIndex(a.typeKey) - typeOrderIndex(b.typeKey);
    if (oi !== 0) return oi;
    return a.label.localeCompare(b.label, undefined, { sensitivity: "base" });
  });

  return groups;
}

/** Stable row key for a visible object in navigator lists. */
export function visibleObjectRowKey(
  o: CommunityVisibleObject,
  index = 0,
): string {
  if (o.guid?.stringValue) return o.guid.stringValue;
  if (o.id != null) return `id:${o.id}`;
  if (o.name) return `name:${o.name}`;
  return `obj-idx:${index}`;
}
