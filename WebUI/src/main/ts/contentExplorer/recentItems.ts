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
 * Explorer recent-items quick access (#6256 / parent #4530).
 *
 * <p>Records Explorer items as operators open them and surfaces a capped
 * Recent quick-access list in the shell chrome. Storage is client-side
 * only ({@code localStorage}, capped, per-operator) — zero new REST.
 * Folders are included with a browse affordance; pages/assets re-open in
 * the editor.</p>
 *
 * <p>Pure helper module — no DOM / fetch / React — so Vitest can lock the
 * dedupe, cap, per-operator, and stale-entry rules without mounting the
 * shell.</p>
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import { isFolder } from "./selection";

/** Maximum recent entries kept per operator (#6256). */
export const MAX_RECENT_ITEMS = 10;

/** {@code localStorage} key prefix; the operator name is appended. */
export const RECENT_STORAGE_PREFIX = "perc.explorer.recent.";

/** A recently opened Explorer row (folders included). */
export interface RecentItem {
  /** Content id when known (pages/assets opened from search may omit it). */
  id?: string | null;
  /** Display name (falls back to path when blank). */
  name: string;
  /** CMS logical path (folders keep browse semantics). */
  path: string;
  /** Folders browse in place; items re-open in the editor. */
  kind: "folder" | "item";
}

/** A stored recent entry with its open timestamp. */
export interface RecentEntry extends RecentItem {
  /** ISO-8601 timestamp of the most recent open. */
  openedAt: string;
}

/** Minimal storage seam so tests inject an in-memory map. */
export interface RecentStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

/**
 * Per-operator storage key. Operator names come from the SPA bootstrap and
 * may contain characters that are awkward in a key, so the name is encoded.
 */
export function recentStorageKey(userName: string | null | undefined): string {
  const raw = (userName ?? "").trim();
  return `${RECENT_STORAGE_PREFIX}${raw ? encodeURIComponent(raw) : "anonymous"}`;
}

/** Browser {@code localStorage} adapter; {@code null} off-browser or denied. */
export function browserRecentStorage(): RecentStorage | null {
  try {
    if (typeof window === "undefined" || window.localStorage == null) {
      return null;
    }
    const ls = window.localStorage;
    return {
      getItem: (key) => ls.getItem(key),
      setItem: (key, value) => ls.setItem(key, value),
      removeItem: (key) => ls.removeItem(key),
    };
  } catch {
    return null;
  }
}

/**
 * Stable identity for dedupe / removal. Prefers content id, then path.
 * Entries with neither are stale and cannot be keyed.
 */
export function recentItemKey(entry: RecentItem): string | null {
  const id = (entry.id ?? "").trim();
  if (id) {
    return `id:${id}`;
  }
  const path = (entry.path ?? "").trim();
  if (path) {
    return `path:${path}`;
  }
  return null;
}

function sanitizeEntry(raw: unknown): RecentEntry | null {
  if (raw == null || typeof raw !== "object") {
    return null;
  }
  const candidate = raw as Record<string, unknown>;
  const id =
    candidate.id == null || typeof candidate.id !== "string"
      ? null
      : candidate.id.trim();
  const path =
    typeof candidate.path === "string" ? candidate.path.trim() : "";
  if (!id && !path) {
    // Stale entry: nothing to open — dropped, never an error.
    return null;
  }
  const rawName = typeof candidate.name === "string" ? candidate.name.trim() : "";
  const kind = candidate.kind === "folder" ? "folder" : "item";
  const openedAt =
    typeof candidate.openedAt === "string" && candidate.openedAt
      ? candidate.openedAt
      : new Date(0).toISOString();
  return {
    id: id || null,
    name: rawName || path || id || "",
    path,
    kind,
    openedAt,
  };
}

/**
 * Load stored recents for the operator. Malformed, blank, or keyless
 * (stale) entries are dropped — a deleted or corrupt row never errors.
 */
export function loadRecentItems(
  storage: RecentStorage | null,
  userName: string | null | undefined,
): RecentEntry[] {
  if (!storage) {
    return [];
  }
  let raw: string | null = null;
  try {
    raw = storage.getItem(recentStorageKey(userName));
  } catch {
    return [];
  }
  if (!raw) {
    return [];
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return [];
  }
  if (!Array.isArray(parsed)) {
    return [];
  }
  const seen = new Set<string>();
  const entries: RecentEntry[] = [];
  for (const item of parsed) {
    const entry = sanitizeEntry(item);
    if (!entry) {
      continue;
    }
    const key = recentItemKey(entry);
    if (!key || seen.has(key)) {
      continue;
    }
    seen.add(key);
    entries.push(entry);
    if (entries.length >= MAX_RECENT_ITEMS) {
      break;
    }
  }
  return entries;
}

/** Persist recents for the operator; storage failures are swallowed. */
export function persistRecentItems(
  storage: RecentStorage | null,
  userName: string | null | undefined,
  entries: ReadonlyArray<RecentEntry>,
): void {
  if (!storage) {
    return;
  }
  try {
    storage.setItem(
      recentStorageKey(userName),
      JSON.stringify(entries.slice(0, MAX_RECENT_ITEMS)),
    );
  } catch {
    // Private-mode / quota failure must not break the Explorer.
  }
}

/**
 * Record an opened item at the front of the list. Re-opens dedupe (moved
 * to front with a fresh timestamp); the list is capped. Blank entries
 * (no id and no path) are ignored.
 */
export function recordRecentItem(
  existing: ReadonlyArray<RecentEntry>,
  item: RecentItem,
): RecentEntry[] {
  const key = recentItemKey(item);
  if (!key) {
    return [...existing];
  }
  const name = item.name.trim() || item.path.trim() || (item.id ?? "").trim();
  const entry: RecentEntry = {
    id: (item.id ?? "").trim() || null,
    name,
    path: item.path.trim(),
    kind: item.kind,
    openedAt: new Date().toISOString(),
  };
  const rest = existing.filter((prev) => recentItemKey(prev) !== key);
  return [entry, ...rest].slice(0, MAX_RECENT_ITEMS);
}

/** Remove one entry by key; unknown keys leave the list unchanged. */
export function dropRecentItem(
  existing: ReadonlyArray<RecentEntry>,
  key: string | null,
): RecentEntry[] {
  if (!key) {
    return [...existing];
  }
  return existing.filter((prev) => recentItemKey(prev) !== key);
}

/**
 * Convert an opened shell row into a recordable recent. Returns
 * {@code null} when the row has neither id nor path (nothing to re-open).
 */
export function toRecentItem(item: PSPathItem | null | undefined): RecentItem | null {
  if (!item) {
    return null;
  }
  const id = item.id == null ? "" : String(item.id).trim();
  const path = (item.path ?? "").trim();
  if (!id && !path) {
    return null;
  }
  const name = (item.name ?? "").trim() || path || id;
  return { id: id || null, name, path, kind: isFolder(item) ? "folder" : "item" };
}
