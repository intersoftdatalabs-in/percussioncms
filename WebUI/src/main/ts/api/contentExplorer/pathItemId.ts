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
 * Explorer list / selection content-id bind (#3546 / parent #2778).
 *
 * <p>Sample-site and display-format rows sometimes omit {@code id}, send a
 * slug, or wrap a GUID as {@code {stringValue}} / host-type-uuid parts.
 * Relationships (and other item tools) only mount when
 * {@link parseExplorerContentId} succeeds — bind a parseable id onto the
 * path item before the list hands it to selection.</p>
 */

import type { PSPathItem } from "./types";

/** Display-format / type-property keys that carry a CMS content id. */
const CONTENT_ID_KEYS = [
  "sys_contentid",
  "sys_contentId",
  "contentId",
  "contentid",
  "sys_id",
] as const;

/**
 * Flatten a wire id (string, number, or Jackson GUID object) to a scalar.
 */
export function unwrapExplorerWireId(
  raw: unknown,
): string | number | undefined {
  if (raw == null || raw === "") {
    return undefined;
  }
  if (typeof raw === "number" || typeof raw === "string") {
    return raw;
  }
  if (typeof raw !== "object" || Array.isArray(raw)) {
    return undefined;
  }
  const rec = raw as Record<string, unknown>;
  const stringValue = rec.stringValue ?? rec.string_value;
  if (typeof stringValue === "string" && stringValue.trim()) {
    return stringValue.trim();
  }
  if (typeof rec.id === "string" || typeof rec.id === "number") {
    return rec.id;
  }
  const host = rec.hostId ?? rec.host_id;
  const type = rec.type;
  const uuid = rec.uuid;
  if (host != null && type != null && uuid != null) {
    return `${host}-${type}-${uuid}`;
  }
  return undefined;
}

/**
 * Parse a content id from a numeric string, GUID {@code host-type-uuid}
 * (last segment), or Jackson GUID object.
 *
 * @returns positive integer content id, or {@code null}
 */
export function parseExplorerContentId(
  id: string | number | undefined | unknown,
): number | null {
  if (id == null || id === "") {
    return null;
  }
  if (typeof id === "number") {
    return Number.isFinite(id) && id > 0 ? Math.trunc(id) : null;
  }
  if (typeof id === "object") {
    const unwrapped = unwrapExplorerWireId(id);
    if (unwrapped == null) {
      return null;
    }
    return parseExplorerContentId(unwrapped);
  }
  const s = String(id).trim();
  if (!s) {
    return null;
  }
  const whole = Number(s);
  if (Number.isFinite(whole) && whole > 0) {
    return Math.trunc(whole);
  }
  // Percussion GUID host-type-uuid (e.g. 1-101-708) — content id is last segment.
  const last = s.split("-").pop();
  if (!last) {
    return null;
  }
  const n = Number(last);
  return Number.isFinite(n) && n > 0 ? Math.trunc(n) : null;
}

function collectTypePropertyMap(
  typeProperties: unknown,
): Record<string, unknown> | null {
  if (typeProperties == null || typeof typeProperties !== "object") {
    return null;
  }
  const rec = typeProperties as Record<string, unknown>;
  const entries = rec.entries;
  if (entries != null && typeof entries === "object" && !Array.isArray(entries)) {
    return entries as Record<string, unknown>;
  }
  return rec;
}

/**
 * Copy of {@code item} with {@code id} set to a parseable content/GUID
 * string when the wire omitted it or used a non-GUID shape that still
 * carries {@code sys_contentid} / a GUID object.
 *
 * Folders keep their original id when nothing parseable is found (site
 * name slugs stay slugs).
 */
export function bindExplorerPathItemId(item: PSPathItem): PSPathItem {
  const extras = item as PSPathItem & {
    typeProperties?: unknown;
    guid?: unknown;
    Guid?: unknown;
  };
  const candidates: unknown[] = [item.id, extras.guid, extras.Guid];
  const dp = item.displayProperties;
  if (dp) {
    for (const key of CONTENT_ID_KEYS) {
      candidates.push(dp[key]);
    }
  }
  const tp = collectTypePropertyMap(extras.typeProperties);
  if (tp) {
    for (const key of CONTENT_ID_KEYS) {
      candidates.push(tp[key]);
    }
  }

  for (const raw of candidates) {
    const unwrapped = unwrapExplorerWireId(raw);
    if (unwrapped == null) {
      continue;
    }
    if (parseExplorerContentId(unwrapped) == null) {
      continue;
    }
    const nextId =
      typeof unwrapped === "string" ? unwrapped.trim() : String(unwrapped);
    if (item.id === nextId) {
      return item;
    }
    return { ...item, id: nextId };
  }

  const unwrappedId = unwrapExplorerWireId(item.id);
  if (typeof unwrappedId === "string" && unwrappedId !== item.id) {
    return { ...item, id: unwrappedId };
  }
  if (typeof unwrappedId === "number") {
    const nextId = String(unwrappedId);
    if (item.id === nextId) {
      return item;
    }
    return { ...item, id: nextId };
  }
  return item;
}
