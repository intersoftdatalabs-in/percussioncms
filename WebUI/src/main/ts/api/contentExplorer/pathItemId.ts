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
  if (typeof rec.id === "number" && Number.isFinite(rec.id)) {
    return rec.id;
  }
  if (typeof rec.id === "string" && rec.id.trim()) {
    return rec.id.trim();
  }
  const host = guidPart(rec.hostId ?? rec.host_id);
  const type = guidPart(rec.type);
  const uuid = guidPart(rec.uuid);
  if (host != null && type != null && uuid != null) {
    return `${host}-${type}-${uuid}`;
  }
  return undefined;
}

/** Non-blank GUID segment. {@code 0} is a valid host id; empty string is not. */
function guidPart(value: unknown): string | undefined {
  if (typeof value === "number") {
    return Number.isFinite(value) ? String(Math.trunc(value)) : undefined;
  }
  if (typeof value === "string") {
    const text = value.trim();
    return text.length > 0 ? text : undefined;
  }
  return undefined;
}

/**
 * CMS content ids are signed 32-bit locators. Timestamped asset names
 * such as {@code New-percSimpleTextAsset-20260820165542} have a trailing
 * digit run larger than this; treating that as a content id made
 * Relationships REST 403 and the panel show a permission error (#3811).
 */
const MAX_CMS_CONTENT_ID = 2_147_483_647;

/**
 * Percussion GUID {@code host-type-uuid} (e.g. {@code 1-101-708} or
 * {@code 16777215-101-551}). All three segments must be digits — do not
 * take the last hyphen-separated token of an asset title.
 */
const GUID_HOST_TYPE_UUID = /^(\d+)-(\d+)-(\d+)$/;

function asCmsContentId(n: number): number | null {
  if (!Number.isFinite(n) || n <= 0 || n > MAX_CMS_CONTENT_ID) {
    return null;
  }
  const truncated = Math.trunc(n);
  return truncated === n ? truncated : null;
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
    return asCmsContentId(id);
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
  if (/^\d+$/.test(s)) {
    return asCmsContentId(Number(s));
  }
  const guid = GUID_HOST_TYPE_UUID.exec(s);
  if (!guid) {
    return null;
  }
  return asCmsContentId(Number(guid[3]));
}

/**
 * Compare Explorer item ids without number/string mismatches
 * ({@code "42"} vs {@code 42}) so list {@code aria-selected} tracks
 * {@code selection.item} after a row click (#3467).
 */
export function sameExplorerItemId(
  a: string | number | null | undefined,
  b: string | number | null | undefined,
): boolean {
  if (a == null || b == null) {
    return false;
  }
  const left = String(a).trim();
  const right = String(b).trim();
  return left.length > 0 && left === right;
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
 * True when {@code id} is a Percussion {@code host-type-uuid} GUID
 * ({@code 16777215-101-551}). Used by Explorer row test ids and Translations
 * GET (#3871 / parent #2649).
 */
export function isExplorerGuidShapedId(id: unknown): boolean {
  const unwrapped = unwrapExplorerWireId(id);
  if (unwrapped == null) {
    return false;
  }
  return GUID_HOST_TYPE_UUID.test(String(unwrapped).trim());
}

/**
 * Flatten JAXB {@code columnData} / display-format maps into a string map.
 *
 * <p>Pathmanagement JSON may send {@code displayProperties} as a plain object
 * or JAXB {@code columnData} as {@code {displayProperty:[{name,value}]}} /
 * {@code {column:[{name,value}]}}.
 */
export function flattenDisplayPropertyMap(
  raw: unknown,
): Record<string, unknown> | null {
  if (raw == null || typeof raw !== "object") {
    return null;
  }
  if (Array.isArray(raw)) {
    return flattenDisplayPropertyEntries(raw);
  }
  const rec = raw as Record<string, unknown>;
  const list = rec.displayProperty ?? rec.column;
  if (Array.isArray(list)) {
    return flattenDisplayPropertyEntries(list);
  }
  if (rec.entries != null && typeof rec.entries === "object" && !Array.isArray(rec.entries)) {
    return rec.entries as Record<string, unknown>;
  }
  return rec;
}

function flattenDisplayPropertyEntries(
  list: unknown[],
): Record<string, unknown> | null {
  const out: Record<string, unknown> = {};
  for (const entry of list) {
    if (entry == null || typeof entry !== "object") {
      continue;
    }
    const rec = entry as Record<string, unknown>;
    const name = rec.name ?? rec.key;
    if (typeof name === "string" && name.trim()) {
      out[name.trim()] = rec.value ?? rec.Value;
    }
  }
  return Object.keys(out).length > 0 ? out : null;
}

function collectIdCandidates(item: PSPathItem): unknown[] {
  const extras = item as PSPathItem & {
    typeProperties?: unknown;
    guid?: unknown;
    Guid?: unknown;
    columnData?: unknown;
  };
  const candidates: unknown[] = [item.id, extras.guid, extras.Guid];
  const dp =
    flattenDisplayPropertyMap(item.displayProperties) ??
    flattenDisplayPropertyMap(extras.columnData);
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
  return candidates;
}

function asBoundIdString(unwrapped: string | number): string {
  return typeof unwrapped === "string" ? unwrapped.trim() : String(unwrapped);
}

/**
 * Copy of {@code item} with {@code id} set to a parseable content/GUID
 * string when the wire omitted it or used a non-GUID shape that still
 * carries {@code sys_contentid} / a GUID object.
 *
 * <p>Prefer a full {@code host-type-uuid} GUID over a bare numeric
 * {@code sys_contentid}. Translations GET 404s on the last segment
 * ({@code 551}) while the GUID ({@code 16777215-101-551}) returns 200
 * (#3871 / #3703 / parent #2649).</p>
 *
 * Folders keep their original id when nothing parseable is found (site
 * name slugs stay slugs).
 */
export function bindExplorerPathItemId(item: PSPathItem): PSPathItem {
  const candidates = collectIdCandidates(item);
  let guidId: string | undefined;
  let numericId: string | undefined;
  for (const raw of candidates) {
    const unwrapped = unwrapExplorerWireId(raw);
    if (unwrapped == null) {
      continue;
    }
    if (parseExplorerContentId(unwrapped) == null) {
      continue;
    }
    const nextId = asBoundIdString(unwrapped);
    if (!nextId) {
      continue;
    }
    if (GUID_HOST_TYPE_UUID.test(nextId)) {
      if (guidId == null) {
        guidId = nextId;
      }
    } else if (numericId == null) {
      numericId = nextId;
    }
  }
  const nextId = guidId ?? numericId;
  if (nextId == null) {
    // Nothing parseable — keep the original id (folder slugs, unparseable
    // objects). Do not assign an unwrapped string that
    // {@link parseExplorerContentId} rejected.
    return item;
  }
  if (sameExplorerItemId(item.id, nextId)) {
    return item;
  }
  return { ...item, id: nextId };
}

/**
 * Normalize one path-list child: unwrap nested {@code PathItem} roots,
 * alias JAXB {@code columnData} onto {@code displayProperties}, then bind
 * a Translations-usable id (#3871).
 */
export function normalizeListedPathItem(raw: unknown): PSPathItem {
  if (raw == null || typeof raw !== "object") {
    return { name: "", path: "" };
  }
  let rec = raw as Record<string, unknown>;
  const nested = rec.PathItem;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    rec = nested as Record<string, unknown>;
  }
  const item = { ...(rec as unknown as PSPathItem) };
  if (item.displayProperties == null) {
    const flat = flattenDisplayPropertyMap(rec.columnData);
    if (flat) {
      item.displayProperties = flat;
    }
  }
  return bindExplorerPathItemId(item);
}

/**
 * Stable list-row identity for {@code data-testid="detail-row-…"} and
 * {@code data-item-id}. Prefers the bound GUID (or numeric content id)
 * so Translations GET uses the same key the adaptor accepts (#3871).
 */
export function explorerDetailRowIdKey(item: PSPathItem): string {
  const bound = bindExplorerPathItemId(item);
  const fromId = unwrapExplorerWireId(bound.id);
  if (fromId != null) {
    const text = asBoundIdString(fromId);
    if (text) {
      return text;
    }
  }
  const path = (bound.path ?? "").trim();
  if (path) {
    return path;
  }
  return (bound.name ?? "").trim();
}
