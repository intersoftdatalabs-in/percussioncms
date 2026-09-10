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
import { parseChoiceCatalog } from "./contentTypeChoiceCatalog";
import {
  normalizeContentTypeControlProperties,
  normalizeContentTypeDesignGaps,
} from "./contentTypeLists";
import { asJacksonArray } from "./slotLists";
import type {
  ContentTypeControlProperty,
  SharedFieldControlProperties,
  SharedFieldGroupDetail,
  SharedFieldGroupSummary,
  SharedFieldSummary,
} from "./types";

/** Writable fields for POST/PUT /services/sharedfields. Fields catalog is not written here. */
export type SharedFieldGroupWriteBody = Pick<SharedFieldGroupDetail, "name" | "filename">;

/**
 * REST field-name rule (SharedFieldsAdaptor.validateFieldName): letter, then
 * letters/digits/underscore, max 50 characters, no spaces or path chars.
 */
export const SHARED_FIELD_NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]{0,49}$/;

/**
 * JAXB / Jackson WRAP_ROOT_VALUE for POST /sharedfields/{name}/fields.
 * Live CMS expects {@code @XmlRootElement(name = "SharedField")}.
 */
export const SHARED_FIELD_ROOT = "SharedField";

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code SharedFieldControlProperties}. */
export const SHARED_FIELD_CONTROL_PROPERTIES_ROOT = "SharedFieldControlProperties";

/** Writable add-field body on POST /services/sharedfields/{name}/fields. */
export type SharedFieldWriteBody = Pick<
  SharedFieldSummary,
  "name" | "dataType" | "searchable" | "required" | "occurrence"
>;

export type SharedFieldControlPropertiesBody = {
  properties: ContentTypeControlProperty[];
  /** When omitted, PUT leaves the catalog unchanged. */
  choices?: SharedFieldControlProperties["choices"];
};

/** Jackson / JAXB root for SharedFieldGroupDetail (UNWRAP_ROOT_VALUE on POST/PUT). */
export const SHARED_FIELD_GROUP_DETAIL_ROOT = "SharedFieldGroupDetail";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.SharedFieldGroupSummary ?? obj.sharedFieldGroupSummary;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function asFieldArray(value: unknown): SharedFieldSummary[] {
  return asJacksonArray<SharedFieldSummary>(
    value,
    ["SharedField", "sharedField", "SharedFieldSummary", "sharedFieldSummary", "fields"],
    (o) => "name" in o || "dataType" in o || "occurrence" in o,
  );
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
}

/** True when the name is a safe REST shared-field group key (no spaces or path chars). */
export function isSafeGroupName(name: string): boolean {
  if (!name) return false;
  return !name.includes("..") && !name.includes("/") && !name.includes("\\") && !name.includes("\0");
}

/** Trim a group name for write. Empty / null becomes "". */
export function normalizeGroupName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create/update
 * ({@code SharedFieldsAdaptor.validateGroupName}).
 */
export function isValidGroupName(name: string | undefined | null): boolean {
  const key = normalizeGroupName(name);
  if (!key) return false;
  if (containsWhitespace(key)) return false;
  if (key.includes("*")) return false;
  return isSafeGroupName(key);
}

function stripXmlSuffix(filename: string): string {
  return filename.toLowerCase().endsWith(".xml") ? filename.slice(0, -4) : filename;
}

/**
 * True when filename is omitted (REST defaults to {name}.xml) or is a safe
 * {@code .xml} path without spaces. Matches
 * {@code SharedFieldsAdaptor.normalizeFilename}: blank → {name}.xml; stem
 * cannot contain spaces, {@code ..}, {@code /}, {@code \\}, or NUL; non-.xml
 * extensions are rejected; a stem with no dot is accepted (server appends
 * {@code .xml}). Group names may contain a single dot because
 * {@code isSafeGroupName} only rejects {@code ..} and path separators.
 */
export function isValidFilename(filename: string | undefined | null): boolean {
  if (filename == null) return true;
  const raw = filename.trim();
  if (!raw) return true;
  if (containsWhitespace(raw)) return false;
  const stem = stripXmlSuffix(raw);
  if (!isSafeGroupName(stem) || stem.includes("*")) return false;
  if (raw.toLowerCase().endsWith(".xml")) return true;
  return !raw.includes(".");
}

/** Save is enabled when the group name is valid and filename is blank or valid. */
export function isSharedFieldGroupWriteReady(opts: {
  name: string;
  filename?: string;
}): boolean {
  return isValidGroupName(opts.name) && isValidFilename(opts.filename);
}

/** True when the name matches REST nested-field create rules. */
export function isValidSharedFieldName(name: string | undefined | null): boolean {
  if (name == null) return false;
  if (name !== name.trim()) return false;
  if (!SHARED_FIELD_NAME_PATTERN.test(name)) return false;
  return isSafeGroupName(name);
}

/** Add is enabled when the new nested field name is valid. */
export function isSharedFieldAddReady(name: string): boolean {
  return isValidSharedFieldName(name);
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapSharedFieldGroupDetailForWire(
  body: SharedFieldGroupWriteBody,
): Record<string, SharedFieldGroupWriteBody> {
  return { [SHARED_FIELD_GROUP_DETAIL_ROOT]: body };
}

/** Unwrap GET/POST/PUT payload that may be wrapped as { SharedFieldGroupDetail: {...} }. */
export function unwrapSharedFieldGroupDetail(payload: unknown): SharedFieldGroupDetail {
  const obj = asRecord(payload);
  if (!obj) {
    return {};
  }
  const nested = asRecord(obj.SharedFieldGroupDetail ?? obj.sharedFieldGroupDetail);
  const raw = nested ?? obj;
  const out: SharedFieldGroupDetail = { ...raw };
  if (raw.fields !== undefined) {
    out.fields = asFieldArray(raw.fields);
  }
  return out;
}

/** Wire JSON for POST /fields — a flat body fails JAXB root unwrap. */
export function wrapSharedFieldForWire(
  body: SharedFieldWriteBody,
): Record<string, SharedFieldWriteBody> {
  return { [SHARED_FIELD_ROOT]: body };
}

/**
 * Flatten GET/PUT {@code .../fields/{field}/controlProperties} JSON.
 *
 * <p>Handles WRAP_ROOT {@code SharedFieldControlProperties}, a flat body,
 * JAXB property envelopes, and empty-collection beans.
 */
export function unwrapSharedFieldControlProperties(
  payload: unknown,
): SharedFieldControlProperties {
  const root = asRecord(payload);
  if (!root) {
    return { properties: [] };
  }
  const nested = asRecord(
    root[SHARED_FIELD_CONTROL_PROPERTIES_ROOT] ?? root.sharedFieldControlProperties,
  );
  const body = nested ?? root;
  const out: SharedFieldControlProperties = {
    properties: normalizeContentTypeControlProperties(body.properties),
  };
  if (typeof body.fieldName === "string") {
    out.fieldName = body.fieldName;
  }
  if (typeof body.control === "string") {
    out.control = body.control;
  }
  if (body.choices != null) {
    const choices = parseChoiceCatalog(body.choices);
    if (choices) {
      out.choices = choices;
    }
  }
  if (body.designGaps != null) {
    out.designGaps = normalizeContentTypeDesignGaps(body.designGaps);
  }
  return out;
}

/**
 * Build the wire JSON body for {@code PUT .../controlProperties} under
 * {@link SHARED_FIELD_CONTROL_PROPERTIES_ROOT}. A flat body fails server
 * UNWRAP_ROOT_VALUE. Omit {@code choices} to leave the catalog unchanged.
 */
export function wrapSharedFieldControlPropertiesForWire(
  body: SharedFieldControlPropertiesBody,
): Record<string, SharedFieldControlPropertiesBody> {
  const wrapped: SharedFieldControlPropertiesBody = {
    properties: body.properties,
  };
  if (body.choices !== undefined) {
    wrapped.choices = body.choices;
  }
  return { [SHARED_FIELD_CONTROL_PROPERTIES_ROOT]: wrapped };
}

function nestedFieldUrl(groupName: string, suffix: string): string {
  return `${PATHS.SHARED_FIELDS}/${encodeURIComponent(groupName)}/fields/${suffix}`;
}

/** GET /services/sharedfields */
export async function listSharedFieldGroups(): Promise<SharedFieldGroupSummary[]> {
  const payload = await get<unknown>(PATHS.SHARED_FIELDS);
  return asArray<SharedFieldGroupSummary>(payload);
}

/** GET /services/sharedfields/{name} */
export async function getSharedFieldGroupDetail(
  name: string,
): Promise<SharedFieldGroupDetail> {
  const key = encodeURIComponent(name);
  const payload = await get<unknown>(`${PATHS.SHARED_FIELDS}/${key}`);
  return unwrapSharedFieldGroupDetail(payload);
}

/** POST /services/sharedfields — Admin. Name required. Duplicate is 409. */
export async function createSharedFieldGroup(
  body: SharedFieldGroupWriteBody,
): Promise<SharedFieldGroupDetail> {
  const payload = await post<unknown>(
    PATHS.SHARED_FIELDS,
    wrapSharedFieldGroupDetailForWire(body),
  );
  return unwrapSharedFieldGroupDetail(payload);
}

/** PUT /services/sharedfields/{name} — Admin. Optional rename via body.name. */
export async function updateSharedFieldGroup(
  name: string,
  body: SharedFieldGroupWriteBody,
): Promise<SharedFieldGroupDetail> {
  const payload = await put<unknown>(
    `${PATHS.SHARED_FIELDS}/${encodeURIComponent(name)}`,
    wrapSharedFieldGroupDetailForWire(body),
  );
  return unwrapSharedFieldGroupDetail(payload);
}

/** DELETE /services/sharedfields/{name} — Admin. 204 on success; missing is 404. */
export async function deleteSharedFieldGroup(name: string): Promise<void> {
  await del(`${PATHS.SHARED_FIELDS}/${encodeURIComponent(name)}`);
}

/**
 * POST /services/sharedfields/{name}/fields — Admin. Add a persistable nested
 * field. Duplicate name is 409; invalid name is 400; lock held by another user
 * is 409.
 */
export async function addSharedField(
  groupName: string,
  body: SharedFieldWriteBody,
): Promise<SharedFieldGroupDetail> {
  const payload = await post<unknown>(
    `${PATHS.SHARED_FIELDS}/${encodeURIComponent(groupName)}/fields`,
    wrapSharedFieldForWire(body),
  );
  return unwrapSharedFieldGroupDetail(payload);
}

/**
 * DELETE /services/sharedfields/{name}/fields/{fieldName} — Admin. 204 on
 * success. Unknown group/field is 404; lock is 409.
 */
export async function deleteSharedField(groupName: string, fieldName: string): Promise<void> {
  await del(nestedFieldUrl(groupName, encodeURIComponent(fieldName)));
}

/**
 * GET /services/sharedfields/{name}/fields/{fieldName}/controlProperties —
 * Admin. No lock. Empty properties means none.
 */
export async function getSharedFieldControlProperties(
  groupName: string,
  fieldName: string,
): Promise<SharedFieldControlProperties> {
  const payload = await get<unknown>(
    nestedFieldUrl(groupName, `${encodeURIComponent(fieldName)}/controlProperties`),
  );
  return unwrapSharedFieldControlProperties(payload);
}

/**
 * PUT /services/sharedfields/{name}/fields/{fieldName}/controlProperties —
 * Admin. Request lock is acquired and released on save. Empty properties
 * clears. Omit {@code choices} to leave the catalog unchanged.
 */
export async function replaceSharedFieldControlProperties(
  groupName: string,
  fieldName: string,
  body: SharedFieldControlPropertiesBody,
): Promise<SharedFieldControlProperties> {
  const payload = await put<unknown>(
    nestedFieldUrl(groupName, `${encodeURIComponent(fieldName)}/controlProperties`),
    wrapSharedFieldControlPropertiesForWire(body),
  );
  return unwrapSharedFieldControlProperties(payload);
}
