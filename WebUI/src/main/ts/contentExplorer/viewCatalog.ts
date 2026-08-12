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

/**
 * Pure grouping helpers for the Explorer Views catalog (#3116).
 *
 * <p>DCE {@code sys_category} / {@code PSSearch.ParentCategory} values
 * 1–4 map to My / Community / All / Other. Unknown categories fall into
 * Other so a leaf is never dropped from the tree.</p>
 */

import type { ViewDef } from "../api/developer/types";
import { EXPLORER_MSG } from "./messages";

export const VIEW_PARENT_CATEGORIES = [1, 2, 3, 4] as const;

export type ViewParentCategory = (typeof VIEW_PARENT_CATEGORIES)[number];

export const VIEW_CATEGORY_MSG: Record<ViewParentCategory, string> = {
  1: EXPLORER_MSG.VIEWS_GROUP_MY,
  2: EXPLORER_MSG.VIEWS_GROUP_COMMUNITY,
  3: EXPLORER_MSG.VIEWS_GROUP_ALL,
  4: EXPLORER_MSG.VIEWS_GROUP_OTHER,
};

export type ViewCatalogGroups = Record<ViewParentCategory, ViewDef[]>;

export function viewKey(def: ViewDef): string {
  return (
    def.name ||
    def.guid?.stringValue ||
    (def.id != null ? String(def.id) : "")
  ).trim();
}

export function viewLabel(def: ViewDef): string {
  return (def.label || def.name || viewKey(def) || "—").trim();
}

export function normalizeViewParentCategory(
  raw: number | undefined | null,
): ViewParentCategory {
  if (raw === 1 || raw === 2 || raw === 3 || raw === 4) {
    return raw;
  }
  return 4;
}

/** True when the view is a custom-URL design view (Inbox family / #3118). */
export function isCustomUrlView(def: ViewDef): boolean {
  return def.customView === true;
}

export function emptyViewCatalogGroups(): ViewCatalogGroups {
  return { 1: [], 2: [], 3: [], 4: [] };
}

/**
 * Group catalog rows by {@link ViewDef.parentCategory} (1–4).
 * Rows without a usable key are omitted. Each group is sorted by label.
 */
export function groupViewsByParentCategory(
  views: readonly ViewDef[] | null | undefined,
): ViewCatalogGroups {
  const groups = emptyViewCatalogGroups();
  const list = Array.isArray(views) ? views : [];
  for (const def of list) {
    const key = viewKey(def);
    if (!key) {
      continue;
    }
    groups[normalizeViewParentCategory(def.parentCategory)].push(def);
  }
  for (const cat of VIEW_PARENT_CATEGORIES) {
    groups[cat].sort((a, b) =>
      viewLabel(a).localeCompare(viewLabel(b), undefined, {
        sensitivity: "base",
      }),
    );
  }
  return groups;
}
