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

import { del, get, post, put } from "../client";
import { PATHS } from "../paths";
import type { SystemDefDetail, SystemDefFieldSummary } from "./types";

/**
 * REST field-name rule (SystemDefAdaptor.validateFieldName): letter, then
 * letters/digits/underscore, max 50 characters, no spaces.
 */
export const SYSTEM_DEF_FIELD_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,49}$/;

/** Jackson / JAXB root for SystemDefDetail (UNWRAP_ROOT_VALUE on PUT). */
export const SYSTEM_DEF_DETAIL_ROOT = "SystemDefDetail";

/**
 * JAXB / Jackson WRAP_ROOT_VALUE for POST /systemdef/fields. Live CMS expects
 * {@code @XmlRootElement(name = "SystemDefField")}, not the Java class name.
 */
export const SYSTEM_DEF_FIELD_ROOT = "SystemDefField";

/** Writable field properties on PUT /services/systemdef. */
export type SystemDefFieldPatch = Pick<
  SystemDefFieldSummary,
  "name" | "searchable" | "required" | "occurrence"
>;

/** Writable add-field body on POST /services/systemdef/fields. */
export type SystemDefFieldWriteBody = Pick<
  SystemDefFieldSummary,
  "name" | "dataType" | "searchable" | "required" | "occurrence"
>;

/** PUT body — only existing field patches; empty fields leaves the catalog unchanged. */
export type SystemDefWriteBody = {
  fields?: SystemDefFieldPatch[];
};

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asFieldArray(value: unknown): SystemDefFieldSummary[] {
  if (value == null) return [];
  if (Array.isArray(value)) {
    return value.filter((item) => item != null && typeof item === "object") as SystemDefFieldSummary[];
  }
  const obj = asRecord(value);
  if (!obj) return [];
  const raw =
    obj.SystemDefFieldSummary ??
    obj.systemDefFieldSummary ??
    obj.SystemDefField ??
    obj.systemDefField ??
    obj.fields;
  if (raw == null) return [];
  if (Array.isArray(raw)) {
    return raw.filter((item) => item != null && typeof item === "object") as SystemDefFieldSummary[];
  }
  if (typeof raw === "object") {
    return [raw as SystemDefFieldSummary];
  }
  return [];
}

function asStringArray(value: unknown): string[] {
  if (value == null) return [];
  if (typeof value === "string") {
    const t = value.trim();
    return t ? [t] : [];
  }
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === "string");
  }
  return [];
}

/** True when the name matches REST create rules (letter, then word chars, max 50). */
export function isValidSystemDefFieldName(name: string | undefined | null): boolean {
  if (name == null) return false;
  return SYSTEM_DEF_FIELD_NAME_PATTERN.test(name.trim());
}

/** Add is enabled when the new field name is valid. */
export function isSystemDefFieldAddReady(name: string): boolean {
  return isValidSystemDefFieldName(name);
}

/** Wire JSON for PUT — a flat body fails JAXB root unwrap. */
export function wrapSystemDefDetailForWire(
  body: SystemDefWriteBody,
): Record<string, SystemDefWriteBody> {
  return { [SYSTEM_DEF_DETAIL_ROOT]: body };
}

/** Wire JSON for POST /fields — a flat body fails JAXB root unwrap. */
export function wrapSystemDefFieldForWire(
  body: SystemDefFieldWriteBody,
): Record<string, SystemDefFieldWriteBody> {
  return { [SYSTEM_DEF_FIELD_ROOT]: body };
}

/** Unwrap GET/PUT/POST payload that may be wrapped as { SystemDefDetail: {...} }. */
export function unwrapSystemDefDetail(payload: unknown): SystemDefDetail {
  const obj = asRecord(payload);
  if (!obj) {
    return { fields: [] };
  }
  const nested = obj.SystemDefDetail ?? obj.systemDefDetail;
  const raw = asRecord(nested) ?? obj;
  const fieldCount = raw.fieldCount;
  const cacheTimeoutMinutes = raw.cacheTimeoutMinutes;
  return {
    fieldCount: typeof fieldCount === "number" ? fieldCount : undefined,
    cacheTimeoutMinutes:
      typeof cacheTimeoutMinutes === "number" ? cacheTimeoutMinutes : undefined,
    fields: asFieldArray(raw.fields),
    designGaps: asStringArray(raw.designGaps),
  };
}

/** GET /services/systemdef */
export async function getSystemDef(): Promise<SystemDefDetail> {
  const payload = await get<unknown>(PATHS.SYSTEM_DEF);
  return unwrapSystemDefDetail(payload);
}

/**
 * PUT /services/systemdef — Admin. Patch existing field properties. Request lock
 * is acquired and released on save. Empty fields leaves the catalog unchanged.
 */
export async function updateSystemDef(body: SystemDefWriteBody): Promise<SystemDefDetail> {
  const payload = await put<unknown>(PATHS.SYSTEM_DEF, wrapSystemDefDetailForWire(body));
  return unwrapSystemDefDetail(payload);
}

/**
 * POST /services/systemdef/fields — Admin. Add a persistable system field.
 * Duplicate name is 409; invalid name is 400; lock held by another user is 409.
 */
export async function addSystemDefField(
  body: SystemDefFieldWriteBody,
): Promise<SystemDefDetail> {
  const payload = await post<unknown>(
    `${PATHS.SYSTEM_DEF}/fields`,
    wrapSystemDefFieldForWire(body),
  );
  return unwrapSystemDefDetail(payload);
}

/**
 * DELETE /services/systemdef/fields/{fieldName} — Admin. 204 on success.
 * Unknown / system-mandatory / system-internal is 400; lock is 409.
 */
export async function deleteSystemDefField(fieldName: string): Promise<void> {
  await del(`${PATHS.SYSTEM_DEF}/fields/${encodeURIComponent(fieldName)}`);
}
