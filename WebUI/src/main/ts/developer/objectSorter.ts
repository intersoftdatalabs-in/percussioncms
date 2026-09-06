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
 * Developer Object Sorter — session-only list organization (#4344 / Workbench §12.3).
 *
 * <p>No user-preference REST peer exists for this catalog order, so the sort
 * mode and custom row ids stay in {@code sessionStorage}. They survive reload
 * in the same tab and are discarded when the tab closes.
 */

export const OBJECT_SORTER_STORAGE_KEY = "perc.developer.objectSorter.v1";

export const OBJECT_SORTER_PREF_VERSION = 1 as const;

export const OBJECT_SORTER_MODES = [
  "label-asc",
  "label-desc",
  "name-asc",
  "name-desc",
  "custom",
] as const;

export type ObjectSorterMode = (typeof OBJECT_SORTER_MODES)[number];

export const DEFAULT_OBJECT_SORTER_MODE: ObjectSorterMode = "label-asc";

export type ObjectSorterRow = {
  id: string;
  name: string;
  label: string;
};

export type ObjectSorterPreference = {
  version: typeof OBJECT_SORTER_PREF_VERSION;
  mode: ObjectSorterMode;
  customOrder: string[];
};

export function defaultObjectSorterPreference(): ObjectSorterPreference {
  return {
    version: OBJECT_SORTER_PREF_VERSION,
    mode: DEFAULT_OBJECT_SORTER_MODE,
    customOrder: [],
  };
}

export function isObjectSorterMode(value: unknown): value is ObjectSorterMode {
  return (
    typeof value === "string" &&
    (OBJECT_SORTER_MODES as readonly string[]).includes(value)
  );
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asIdList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  const out: string[] = [];
  const seen = new Set<string>();
  for (const item of value) {
    if (typeof item !== "string") {
      continue;
    }
    const id = item.trim();
    if (!id || seen.has(id)) {
      continue;
    }
    seen.add(id);
    out.push(id);
  }
  return out;
}

export function parseObjectSorterPreference(raw: unknown): ObjectSorterPreference {
  const rec = asRecord(raw);
  if (!rec) {
    return defaultObjectSorterPreference();
  }
  const mode = isObjectSorterMode(rec.mode) ? rec.mode : DEFAULT_OBJECT_SORTER_MODE;
  return {
    version: OBJECT_SORTER_PREF_VERSION,
    mode,
    customOrder: asIdList(rec.customOrder),
  };
}

function localeCmp(a: string, b: string): number {
  return a.localeCompare(b, undefined, { sensitivity: "base" });
}

function sortKey(row: ObjectSorterRow, field: "name" | "label"): string {
  if (field === "name") {
    return row.name || row.label;
  }
  return row.label || row.name;
}

export function compareObjectSorterRows(
  a: ObjectSorterRow,
  b: ObjectSorterRow,
  mode: ObjectSorterMode,
): number {
  switch (mode) {
    case "name-asc":
      return localeCmp(sortKey(a, "name"), sortKey(b, "name"));
    case "name-desc":
      return localeCmp(sortKey(b, "name"), sortKey(a, "name"));
    case "label-desc":
      return localeCmp(sortKey(b, "label"), sortKey(a, "label"));
    case "label-asc":
    case "custom":
    default:
      return localeCmp(sortKey(a, "label"), sortKey(b, "label"));
  }
}

export function applyObjectSorter<T>(
  items: readonly T[],
  toRow: (item: T) => ObjectSorterRow,
  pref: ObjectSorterPreference,
): T[] {
  const copy = [...items];
  if (pref.mode === "custom" && pref.customOrder.length > 0) {
    const index = new Map(pref.customOrder.map((id, i) => [id, i]));
    copy.sort((left, right) => {
      const a = toRow(left);
      const b = toRow(right);
      const ia = index.has(a.id) ? (index.get(a.id) as number) : Number.MAX_SAFE_INTEGER;
      const ib = index.has(b.id) ? (index.get(b.id) as number) : Number.MAX_SAFE_INTEGER;
      if (ia !== ib) {
        return ia - ib;
      }
      return localeCmp(sortKey(a, "label"), sortKey(b, "label"));
    });
    return copy;
  }
  const mode = pref.mode === "custom" ? DEFAULT_OBJECT_SORTER_MODE : pref.mode;
  copy.sort((left, right) => compareObjectSorterRows(toRow(left), toRow(right), mode));
  return copy;
}

export function moveObjectSorterId(
  order: readonly string[],
  id: string,
  direction: "up" | "down",
): string[] {
  const next = [...order];
  const i = next.indexOf(id);
  if (i < 0) {
    return next;
  }
  const j = direction === "up" ? i - 1 : i + 1;
  if (j < 0 || j >= next.length) {
    return next;
  }
  const swap = next[i];
  next[i] = next[j];
  next[j] = swap;
  return next;
}

export function customOrderFromRows(rows: readonly ObjectSorterRow[]): string[] {
  return rows.map((row) => row.id).filter((id) => id.length > 0);
}

function readSessionStorage(): Storage | null {
  try {
    if (typeof sessionStorage === "undefined") {
      return null;
    }
    return sessionStorage;
  } catch {
    return null;
  }
}

export function loadObjectSorterPreference(
  storage: Storage | null = readSessionStorage(),
): ObjectSorterPreference {
  if (!storage) {
    return defaultObjectSorterPreference();
  }
  try {
    const raw = storage.getItem(OBJECT_SORTER_STORAGE_KEY);
    if (!raw) {
      return defaultObjectSorterPreference();
    }
    return parseObjectSorterPreference(JSON.parse(raw) as unknown);
  } catch {
    return defaultObjectSorterPreference();
  }
}

export function saveObjectSorterPreference(
  pref: ObjectSorterPreference,
  storage: Storage | null = readSessionStorage(),
): void {
  if (!storage) {
    return;
  }
  const body: ObjectSorterPreference = {
    version: OBJECT_SORTER_PREF_VERSION,
    mode: isObjectSorterMode(pref.mode) ? pref.mode : DEFAULT_OBJECT_SORTER_MODE,
    customOrder: asIdList(pref.customOrder),
  };
  try {
    storage.setItem(OBJECT_SORTER_STORAGE_KEY, JSON.stringify(body));
  } catch {
    // Quota / private mode — keep in-memory only.
  }
}
