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

/** GUID-string (or numeric id) to community name. Empty object is all communities. */
export type AllowedCommunityMap = Record<string, string>;

/** PUT/GET row for REST {@code allowedCommunities} (list of guid+name). */
export type AllowedCommunityRow = { guid?: string; name?: string };

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value == null || typeof value !== "object" || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function communityEntryKey(row: unknown): string {
  const rec = asRecord(row);
  if (!rec) {
    return typeof row === "string" ? row.trim() : "";
  }
  if (typeof rec.guid === "string" && rec.guid) {
    return rec.guid;
  }
  const guid = asRecord(rec.key) || asRecord(rec.guid);
  const fromGuid =
    (typeof guid?.stringValue === "string" && guid.stringValue) ||
    (typeof rec.stringValue === "string" && rec.stringValue) ||
    (typeof rec.key === "string" && rec.key) ||
    "";
  if (fromGuid) {
    return fromGuid;
  }
  if (typeof rec.id === "number") {
    return String(rec.id);
  }
  return "";
}

function communityEntryName(row: unknown): string {
  const rec = asRecord(row);
  if (!rec) {
    return "";
  }
  if (typeof rec.value === "string") {
    return rec.value;
  }
  if (typeof rec.name === "string") {
    return rec.name;
  }
  if (typeof rec.label === "string") {
    return rec.label;
  }
  return "";
}

/**
 * Unwrap GET {@code allowedCommunities} (JSON map, JAXB entry list, or missing).
 * Empty / omitted means all communities — the same persist state as {@code sys_community=-1}.
 */
export function normalizeAllowedCommunities(raw: unknown): AllowedCommunityMap {
  if (raw == null) {
    return {};
  }
  if (Array.isArray(raw)) {
    const out: AllowedCommunityMap = {};
    for (const row of raw) {
      const key = communityEntryKey(row);
      if (key) {
        out[key] = communityEntryName(row);
      }
    }
    return out;
  }
  const rec = asRecord(raw);
  if (!rec) {
    return {};
  }
  // Live CXF/JAXB unwraps a one-element list to a single object, not `[row]`.
  if (typeof rec.guid === "string" || typeof rec.name === "string") {
    return normalizeAllowedCommunities([rec]);
  }
  const jaxbItem = rec.DisplayFormatCommunity ?? rec.displayFormatCommunity;
  if (jaxbItem != null) {
    return normalizeAllowedCommunities(Array.isArray(jaxbItem) ? jaxbItem : [jaxbItem]);
  }
  const wrapped = rec.entry ?? rec.Entry;
  if (wrapped != null) {
    return normalizeAllowedCommunities(Array.isArray(wrapped) ? wrapped : [wrapped]);
  }
  const out: AllowedCommunityMap = {};
  for (const [key, value] of Object.entries(rec)) {
    if (!key || key === "entry" || key === "Entry") {
      continue;
    }
    out[key] = typeof value === "string" ? value : communityEntryName(value);
  }
  return out;
}

/** True when the map is empty — all communities, not a third “none” state. */
export function isAllCommunities(map: AllowedCommunityMap): boolean {
  return Object.keys(map).length === 0;
}

export function communityWireKey(c: CommunitySummary): string {
  if (c.guid?.stringValue) {
    return c.guid.stringValue;
  }
  if (c.id != null) {
    return String(c.id);
  }
  if (c.name) {
    return c.name;
  }
  return "";
}

export function communityMatchesKey(c: CommunitySummary, key: string): boolean {
  if (!key) {
    return false;
  }
  if (c.guid?.stringValue === key) {
    return true;
  }
  if (c.name === key) {
    return true;
  }
  if (c.id != null && String(c.id) === key) {
    return true;
  }
  if (c.guid?.uuid != null && String(c.guid.uuid) === key) {
    return true;
  }
  const parts = key.split("-");
  if (parts.length === 3) {
    const uuid = parts[2];
    if (c.id != null && String(c.id) === uuid) {
      return true;
    }
    if (c.guid?.uuid != null && String(c.guid.uuid) === uuid) {
      return true;
    }
  }
  return false;
}

export function selectedKeysFromMap(
  map: AllowedCommunityMap,
  catalog: CommunitySummary[],
): Set<string> {
  const keys = new Set<string>();
  for (const raw of Object.keys(map)) {
    const match = catalog.find((c) => communityMatchesKey(c, raw));
    const wire = match ? communityWireKey(match) : raw;
    if (wire) {
      keys.add(wire);
    }
  }
  return keys;
}

export function communitiesMapsEqual(a: AllowedCommunityMap, b: AllowedCommunityMap): boolean {
  const ak = Object.keys(a).sort();
  const bk = Object.keys(b).sort();
  if (ak.length !== bk.length) {
    return false;
  }
  return ak.every((k, i) => k === bk[i]);
}

/**
 * PUT body map. Empty object is all communities. Callers must not send a
 * distinct “no communities” payload.
 */
export function toAllowedCommunitiesWriteBody(
  all: boolean,
  catalog: CommunitySummary[],
  selectedKeys: Set<string>,
): AllowedCommunityRow[] {
  if (all || selectedKeys.size === 0) {
    return [];
  }
  const out: AllowedCommunityRow[] = [];
  for (const c of catalog) {
    const key = communityWireKey(c);
    if (!key) {
      continue;
    }
    if (selectedKeys.has(key) || [...selectedKeys].some((k) => communityMatchesKey(c, k))) {
      out.push({ guid: key, name: c.name || c.label || key });
    }
  }
  return out;
}
