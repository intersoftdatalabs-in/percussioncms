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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Shared Display Format GUID wire helpers used by both Developer and Content
 * Explorer {@code displayFormatsApi} modules (issues #2689 / #2951 / #3200).
 *
 * <p>Keep a single implementation so synthesis / nested-Guid rules cannot drift
 * between the two REST clients.
 */

import type { DisplayFormat, RestGuid } from "./developer/types";

/**
 * {@code PSTypeEnum.DISPLAY_FORMAT} numeric type. Used only when the wire
 * omits Guid parts but still has {@code displayId}.
 */
export const DISPLAY_FORMAT_TYPE = 31;

/** {@code PSTypeEnum.NODEDEF} — content type GUID type. */
export const CONTENT_TYPE_TYPE = 2;

/** {@code PSTypeEnum.TEMPLATE} — assembly template GUID type. */
export const TEMPLATE_TYPE = 4;

/** {@code PSTypeEnum.ACTION} — menu action GUID type (#3380). */
export const ACTION_MENU_TYPE = 107;

/** {@code PSTypeEnum.VIEW_DEF} — CX view GUID type (#3380). */
export const VIEW_TYPE = 18;

function firstNonBlankString(value: unknown): string | undefined {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed || undefined;
  }
  return undefined;
}

function readOptionalLikeString(value: unknown): string | undefined {
  const direct = firstNonBlankString(value);
  if (direct) {
    return direct;
  }
  if (Array.isArray(value) && value.length > 0) {
    return readOptionalLikeString(value[0]);
  }
  if (value != null && typeof value === "object") {
    const opt = value as Record<string, unknown>;
    return (
      firstNonBlankString(opt.value) ||
      firstNonBlankString(opt.stringValue) ||
      firstNonBlankString(opt.present === true ? opt.value : undefined)
    );
  }
  return undefined;
}

function asFiniteNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
  }
  return undefined;
}

/**
 * Extract a usable object GUID string from REST Guid wire shapes.
 *
 * <p>Handles plain strings, {@code { stringValue }}, nested {@code { Guid: … }}
 * wraps, Optional-like objects, snake_case aliases, and synthesis from
 * {@code hostId-type-uuid} when {@code stringValue} is missing
 * (issues #2951 / #3200).
 */
export function objectGuidString(guid: unknown): string | undefined {
  if (guid == null) {
    return undefined;
  }
  if (typeof guid === "string") {
    const trimmed = guid.trim();
    return trimmed || undefined;
  }
  if (typeof guid !== "object" || Array.isArray(guid)) {
    return undefined;
  }

  let g = guid as Record<string, unknown>;
  const nestedGuid = g.Guid ?? g.guid;
  if (
    nestedGuid != null &&
    typeof nestedGuid === "object" &&
    !Array.isArray(nestedGuid) &&
    g.stringValue == null &&
    g.string_value == null &&
    g.hostId == null &&
    g.host_id == null &&
    g.uuid == null
  ) {
    g = nestedGuid as Record<string, unknown>;
  }

  const fromString =
    readOptionalLikeString(g.stringValue) ||
    readOptionalLikeString(g.string_value) ||
    readOptionalLikeString(g.STRING_VALUE);
  if (fromString) {
    return fromString;
  }

  const hostId = g.hostId ?? g.host_id;
  const type = g.type;
  const uuid = g.uuid;
  const hostOk = typeof hostId === "number" || typeof hostId === "string";
  const typeOk = typeof type === "number" || typeof type === "string";
  const uuidOk = typeof uuid === "number" || typeof uuid === "string";
  if (hostOk && typeOk && uuidOk) {
    return `${hostId}-${type}-${uuid}`;
  }

  return undefined;
}

/**
 * Synthesize {@code 0-31-{displayId}} when the server omitted Guid but still
 * sent the native display format id ({@link DISPLAY_FORMAT_TYPE}).
 */
export function synthesizeDisplayFormatGuidFromDisplayId(
  displayId: unknown,
): string | undefined {
  return synthesizeTypedObjectGuid(DISPLAY_FORMAT_TYPE, displayId);
}

/**
 * Synthesize {@code 0-{type}-{uuid}} when the wire omitted Guid but still
 * sent a native numeric id (content type typeId, templateId, displayId).
 */
export function synthesizeTypedObjectGuid(
  type: number,
  uuid: unknown,
): string | undefined {
  const n = asFiniteNumber(uuid);
  if (n == null || n <= 0) {
    return undefined;
  }
  return `0-${type}-${n}`;
}

/** Shared detail / list shapes that may carry a nested Guid or plain guidString. */
export type DesignObjectGuidSource = {
  guid?: unknown;
  guidString?: string | null;
};

/**
 * Resolve the GUID Object ACL / detail header should use (CT, template, peers).
 *
 * <p>Order: nested Guid ({@link objectGuidString}) → plain {@code guidString}
 * → catalog list fallback. Never returns a blank string. Issue #3319 / #3200.
 */
export function resolveDesignObjectGuid(
  obj: DesignObjectGuidSource | null | undefined,
  catalogGuid?: string | null,
): string | undefined {
  if (obj != null) {
    const fromGuid = objectGuidString(obj.guid);
    if (fromGuid) {
      return fromGuid;
    }
    const fromPlain = firstNonBlankString(obj.guidString);
    if (fromPlain) {
      return fromPlain;
    }
  }
  return firstNonBlankString(catalogGuid);
}

/**
 * Content type Object ACL GUID: detail Guid / guidString, then catalog, then
 * {@code 0-2-{uuid}} when only the type id is present (#3319).
 */
export function resolveContentTypeObjectGuid(
  ct: DesignObjectGuidSource | null | undefined,
  catalogGuid?: string | null,
): string | undefined {
  const resolved = resolveDesignObjectGuid(ct, catalogGuid);
  if (resolved) {
    return resolved;
  }
  const guid = ct?.guid;
  if (guid != null && typeof guid === "object" && !Array.isArray(guid)) {
    const uuid = (guid as RestGuid).uuid;
    return synthesizeTypedObjectGuid(CONTENT_TYPE_TYPE, uuid);
  }
  return undefined;
}

/**
 * Template Object ACL GUID: detail Guid / guidString, then catalog, then
 * {@code 0-4-{templateId}} from the list/detail native id (#3319).
 */
export function resolveTemplateObjectGuid(
  tpl:
    | (DesignObjectGuidSource & { templateId?: number | null })
    | null
    | undefined,
  catalogGuid?: string | null,
): string | undefined {
  const resolved = resolveDesignObjectGuid(tpl, catalogGuid);
  if (resolved) {
    return resolved;
  }
  return synthesizeTypedObjectGuid(TEMPLATE_TYPE, tpl?.templateId);
}

/**
 * Action menu Object ACL GUID: nested Guid / guidString, then catalog, then
 * {@code 0-107-{id}} from the native action id (#3380).
 */
export function resolveActionMenuObjectGuid(
  menu: (DesignObjectGuidSource & { id?: number | null }) | null | undefined,
  catalogGuid?: string | null,
): string | undefined {
  const resolved = resolveDesignObjectGuid(menu, catalogGuid);
  if (resolved) {
    return resolved;
  }
  return synthesizeTypedObjectGuid(ACTION_MENU_TYPE, menu?.id);
}

/**
 * View Object ACL GUID: nested Guid / guidString, then catalog, then
 * {@code 0-18-{id}} from the native view id (#3380).
 */
export function resolveViewObjectGuid(
  view: (DesignObjectGuidSource & { id?: number | null }) | null | undefined,
  catalogGuid?: string | null,
): string | undefined {
  const resolved = resolveDesignObjectGuid(view, catalogGuid);
  if (resolved) {
    return resolved;
  }
  return synthesizeTypedObjectGuid(VIEW_TYPE, view?.id);
}

/**
 * Ensure {@code guid.stringValue} / {@code guidString} are populated from
 * nested Guid parts or a catalog fallback (same contract as display format).
 */
export function normalizeDesignObjectGuid<T extends DesignObjectGuidSource>(
  obj: T,
  catalogGuid?: string | null,
): T {
  const gs = resolveDesignObjectGuid(obj, catalogGuid);
  if (!gs) {
    return obj;
  }
  const nextGuidString = firstNonBlankString(obj.guidString) || gs;
  if (obj.guid != null && typeof obj.guid === "object" && !Array.isArray(obj.guid)) {
    const existing = obj.guid as RestGuid;
    if (existing.stringValue === gs && obj.guidString === nextGuidString) {
      return obj;
    }
    return { ...obj, guidString: nextGuidString, guid: { ...existing, stringValue: gs } };
  }
  return { ...obj, guidString: nextGuidString, guid: { stringValue: gs } };
}

/**
 * Resolve the GUID the Developer detail header / Object ACL should use.
 *
 * <p>Order: nested Guid → plain {@code guidString} → catalog list fallback →
 * {@code displayId} synthesis. Never returns a blank string.
 */
export function resolveDisplayFormatObjectGuid(
  df: DisplayFormat | null | undefined,
  catalogGuid?: string | null,
): string | undefined {
  if (df != null) {
    const fromGuid = objectGuidString(df.guid);
    if (fromGuid) {
      return fromGuid;
    }
    const fromPlain = firstNonBlankString(df.guidString);
    if (fromPlain) {
      return fromPlain;
    }
  }
  const fromCatalog = firstNonBlankString(catalogGuid);
  if (fromCatalog) {
    return fromCatalog;
  }
  return synthesizeDisplayFormatGuidFromDisplayId(df?.displayId);
}

/**
 * Ensure {@link DisplayFormat.guid}.stringValue is populated when the wire
 * Guid only carried numeric parts (or was a plain string). Also copies
 * {@code guidString} when that is the only usable form (#3200).
 */
export function normalizeDisplayFormatGuid(df: DisplayFormat): DisplayFormat {
  const gs =
    objectGuidString(df.guid) ||
    firstNonBlankString(df.guidString) ||
    synthesizeDisplayFormatGuidFromDisplayId(df.displayId);
  if (!gs) {
    return df;
  }
  const nextGuidString = firstNonBlankString(df.guidString) || gs;
  if (df.guid != null && typeof df.guid === "object" && !Array.isArray(df.guid)) {
    const existing = df.guid as RestGuid;
    if (existing.stringValue === gs && df.guidString === nextGuidString) {
      return df;
    }
    return { ...df, guidString: nextGuidString, guid: { ...existing, stringValue: gs } };
  }
  return { ...df, guidString: nextGuidString, guid: { stringValue: gs } };
}

/**
 * Unwrap Jackson {@code WRAP_ROOT_VALUE} envelopes for a single display format.
 *
 * <p>REST may wrap DTOs as {@code {"DisplayFormat":{…}}}. Without unwrapping,
 * detail panels read {@code detail.guid} as undefined. Flat payloads pass
 * through. Also normalizes {@code guid.stringValue} when only host/type/uuid
 * parts are present (#2951).
 */
export function unwrapDisplayFormat(payload: unknown): DisplayFormat {
  if (payload == null) {
    return {};
  }
  if (typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.DisplayFormat ?? root.displayFormat;
  let body: DisplayFormat;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as DisplayFormat;
  } else if (Array.isArray(nested) && nested.length > 0) {
    const first = nested[0];
    if (first != null && typeof first === "object") {
      body = first as DisplayFormat;
    } else {
      body = root as DisplayFormat;
    }
  } else {
    body = root as DisplayFormat;
  }
  return normalizeDisplayFormatGuid(body);
}

function looksLikeDisplayFormat(obj: Record<string, unknown>): boolean {
  return (
    obj.name != null ||
    obj.internalName != null ||
    obj.guid != null ||
    obj.guidString != null ||
    obj.displayId != null ||
    obj.label != null
  );
}

function flattenDisplayFormatPayload(payload: unknown): unknown[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    const out: unknown[] = [];
    for (const item of payload) {
      out.push(...flattenDisplayFormatPayload(item));
    }
    return out;
  }
  if (typeof payload !== "object") {
    return [];
  }
  const obj = payload as Record<string, unknown>;
  const listWrap = obj.DisplayFormatList ?? obj.displayFormatList;
  if (listWrap != null) {
    return flattenDisplayFormatPayload(listWrap);
  }
  const nested = obj.DisplayFormat ?? obj.displayFormat;
  if (Array.isArray(nested)) {
    return flattenDisplayFormatPayload(nested);
  }
  if (nested != null && typeof nested === "object") {
    return flattenDisplayFormatPayload(nested);
  }
  if (looksLikeDisplayFormat(obj)) {
    return [obj];
  }
  return [];
}

/**
 * Unwrap list envelopes: bare array, {@code DisplayFormat:[…]}, nested
 * {@code DisplayFormatList:{DisplayFormat:[…]}} (#3200).
 */
export function unwrapDisplayFormatList(payload: unknown): DisplayFormat[] {
  const raw = flattenDisplayFormatPayload(payload).map((item) => unwrapDisplayFormat(item));
  // Exact-name catalog: drop duplicate GUID/name rows so By_Author is unique (#3269).
  const seen = new Set<string>();
  const unique: DisplayFormat[] = [];
  for (const df of raw) {
    const key =
      objectGuidString(df.guid) ||
      (typeof df.guidString === "string" ? df.guidString.trim() : "") ||
      (typeof df.name === "string" ? df.name : "") ||
      (typeof df.internalName === "string" ? df.internalName : "");
    if (!key || seen.has(key)) {
      continue;
    }
    seen.add(key);
    unique.push(df);
  }
  return unique;
}
