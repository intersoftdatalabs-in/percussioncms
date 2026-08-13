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
 * Pure grouping helpers for the Explorer Views catalog (#3116 / #3240 / #3325).
 *
 * <p>DCE {@code sys_category} / {@code PSSearch.ParentCategory} values
 * 1–4 map to My / Community / All / Other. Unknown categories fall into
 * Other so a leaf is never dropped from the tree.</p>
 *
 * <p>Inbox is a system custom-URL view at {@code //Views//MyContent/Inbox},
 * never a free-floating Explorer root (#3118 slice 2 / #3240).</p>
 *
 * <p>#3325: {@link groupViewsByParentCategory} collapses catalog rows that
 * are the same logical view (same name / guid / id). Distinct names and
 * guids stay as separate leaves; unlabeled duplicates are omitted. Shared
 * display labels (for example seven {@code All} rows) are disambiguated
 * with the internal name when the keys differ.</p>
 */

import type { ViewDef } from "../api/developer/types";
import { EXPLORER_MSG } from "./messages";

/** DCE {@code PARAM_PATH_INBOX} — Inbox lives under My Content, not CE root. */
export const PATH_MY_CONTENT_INBOX = "//Views//MyContent/Inbox";

/** Typical design {@code INTERNALNAME} / catalog {@code name} for Inbox. */
export const INBOX_VIEW_NAME = "Inbox";

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
    def.guid?.untypedString ||
    (def.id != null && Number(def.id) !== 0 ? String(def.id) : "")
  ).trim();
}

export function viewLabel(def: ViewDef): string {
  return (def.label || def.name || viewKey(def) || "—").trim();
}

/** Coerce JSON parentCategory (number or numeric string) to a finite int. */
export function coerceViewParentCategory(
  raw: unknown,
): number | undefined {
  if (typeof raw === "number" && Number.isFinite(raw)) {
    return raw;
  }
  if (typeof raw === "string" && raw.trim() !== "") {
    const n = Number.parseInt(raw, 10);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return undefined;
}

export function normalizeViewParentCategory(
  raw: number | string | undefined | null,
): ViewParentCategory {
  const n = coerceViewParentCategory(raw);
  if (n === 1 || n === 2 || n === 3 || n === 4) {
    return n;
  }
  return 4;
}

/**
 * Identity for catalog dedupe (#3325). Prefers internal name, then guid,
 * then non-zero id. Empty when the row cannot be distinguished.
 */
export function viewCatalogIdentity(def: ViewDef): string {
  const name = (def.name ?? "").trim().toLowerCase();
  if (name) {
    return `name:${name}`;
  }
  const guid = (
    def.guid?.stringValue ||
    def.guid?.untypedString ||
    ""
  )
    .trim()
    .toLowerCase();
  if (guid) {
    return `guid:${guid}`;
  }
  if (def.id != null && Number(def.id) !== 0) {
    return `id:${String(def.id).trim()}`;
  }
  return "";
}

function unlabeledStamp(def: ViewDef): string {
  const label = viewLabel(def).toLowerCase();
  const cat = normalizeViewParentCategory(def.parentCategory);
  return `unlabeled:${cat}:${label}`;
}

function preferRicherView(a: ViewDef, b: ViewDef): ViewDef {
  const aScore =
    (a.label ? 1 : 0) + (a.description ? 1 : 0) + (a.guid ? 1 : 0);
  const bScore =
    (b.label ? 1 : 0) + (b.description ? 1 : 0) + (b.guid ? 1 : 0);
  return bScore > aScore ? b : a;
}

/**
 * Collapse duplicate catalog rows that represent one logical view.
 * Distinct {@link viewCatalogIdentity} values are kept. Rows with no
 * name/guid/id are kept once per (category, label) and otherwise dropped.
 */
export function dedupeViewCatalog(
  views: readonly ViewDef[] | null | undefined,
): ViewDef[] {
  if (!Array.isArray(views) || views.length === 0) {
    return [];
  }
  const byIdentity = new Map<string, ViewDef>();
  const unlabeledSeen = new Set<string>();
  const order: string[] = [];
  for (const def of views) {
    if (def == null) {
      continue;
    }
    const identity = viewCatalogIdentity(def);
    if (!identity) {
      const stamp = unlabeledStamp(def);
      if (unlabeledSeen.has(stamp)) {
        continue;
      }
      unlabeledSeen.add(stamp);
      const key = `anon:${stamp}`;
      byIdentity.set(key, def);
      order.push(key);
      continue;
    }
    const existing = byIdentity.get(identity);
    if (existing == null) {
      byIdentity.set(identity, def);
      order.push(identity);
    } else {
      byIdentity.set(identity, preferRicherView(existing, def));
    }
  }
  return order.map((k) => byIdentity.get(k)).filter((d): d is ViewDef => d != null);
}

/**
 * Leaf label for the Views tree. When several distinct views in the same
 * group share a display label, append the internal name (or guid) so
 * operators can tell them apart (#3325).
 */
export function viewTreeLabel(def: ViewDef, group: readonly ViewDef[]): string {
  const base = viewLabel(def);
  if (!group || group.length < 2) {
    return base;
  }
  const same = group.filter(
    (d) => viewLabel(d).toLowerCase() === base.toLowerCase(),
  );
  if (same.length < 2) {
    return base;
  }
  const name = (def.name ?? "").trim();
  if (name && name.toLowerCase() !== base.toLowerCase()) {
    return `${base} (${name})`;
  }
  const guid = (def.guid?.stringValue || def.guid?.untypedString || "").trim();
  if (guid) {
    return `${base} (${guid})`;
  }
  return base;
}

/** True when the view is a custom-URL design view (Inbox family / #3118). */
export function isCustomUrlView(def: ViewDef): boolean {
  return def.customView === true;
}

function normalizeInboxToken(raw: string): string {
  return raw.trim().replace(/\\/g, "/");
}

/**
 * True when {@code def} is the system Inbox view
 * ({@code //Views//MyContent/Inbox}).
 */
export function isInboxView(def: ViewDef | null | undefined): boolean {
  if (def == null) {
    return false;
  }
  const name = normalizeInboxToken(def.name ?? "");
  const label = normalizeInboxToken(def.label ?? "");
  if (name.toLowerCase() === "inbox" || label.toLowerCase() === "inbox") {
    return true;
  }
  const key = normalizeInboxToken(viewKey(def));
  if (!key) {
    return false;
  }
  if (key.toLowerCase() === "inbox") {
    return true;
  }
  return /\/\/Views\/\/MyContent\/Inbox$/i.test(key);
}

/**
 * Standard field-criteria views and the Inbox custom-URL leaf may run via
 * {@code POST /services/views/{idOrName}/execute} (C1 / #3239). Other
 * custom-URL views stay unsupported in this slice.
 */
export function canExecuteView(def: ViewDef): boolean {
  return !isCustomUrlView(def) || isInboxView(def);
}

/** Stub used when the catalog omits Inbox so the My Content group still has the leaf. */
export function inboxViewStub(label: string = INBOX_VIEW_NAME): ViewDef {
  return {
    name: INBOX_VIEW_NAME,
    label,
    parentCategory: 1,
    customView: true,
  };
}

/**
 * Force Inbox into My Content (category 1) and inject a stub when missing.
 * Does not invent a free-floating Inbox root.
 */
export function ensureInboxInMyContent(
  views: readonly ViewDef[] | null | undefined,
): ViewDef[] {
  const list = Array.isArray(views) ? views.map((d) => ({ ...d })) : [];
  const idx = list.findIndex((d) => isInboxView(d));
  if (idx < 0) {
    list.push(inboxViewStub());
    return list;
  }
  if (list[idx].parentCategory !== 1) {
    list[idx] = { ...list[idx], parentCategory: 1 };
  }
  return list;
}

export function emptyViewCatalogGroups(): ViewCatalogGroups {
  return { 1: [], 2: [], 3: [], 4: [] };
}

/**
 * Group catalog rows by {@link ViewDef.parentCategory} (1–4).
 * Duplicate logical views are collapsed (#3325). Rows without a usable
 * key are omitted. Each group is sorted by tree label.
 */
export function groupViewsByParentCategory(
  views: readonly ViewDef[] | null | undefined,
): ViewCatalogGroups {
  const groups = emptyViewCatalogGroups();
  const list = ensureInboxInMyContent(dedupeViewCatalog(views));
  for (const def of list) {
    const key = viewKey(def);
    if (!key) {
      continue;
    }
    const cat = normalizeViewParentCategory(def.parentCategory);
    const placed = isInboxView(def)
      ? { ...def, parentCategory: 1 }
      : { ...def, parentCategory: cat };
    groups[normalizeViewParentCategory(placed.parentCategory)].push(placed);
  }
  for (const cat of VIEW_PARENT_CATEGORIES) {
    groups[cat] = dedupeViewCatalog(groups[cat]);
    groups[cat].sort((a, b) =>
      viewTreeLabel(a, groups[cat]).localeCompare(
        viewTreeLabel(b, groups[cat]),
        undefined,
        { sensitivity: "base" },
      ),
    );
  }
  return groups;
}
