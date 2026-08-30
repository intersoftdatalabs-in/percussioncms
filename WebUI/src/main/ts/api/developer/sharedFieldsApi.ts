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
import type { SharedFieldGroupDetail, SharedFieldGroupSummary } from "./types";

/** Writable fields for POST/PUT /services/sharedfields. Fields catalog is not written here. */
export type SharedFieldGroupWriteBody = Pick<SharedFieldGroupDetail, "name" | "filename">;

/** Jackson / JAXB root for SharedFieldGroupDetail (UNWRAP_ROOT_VALUE on POST/PUT). */
export const SHARED_FIELD_GROUP_DETAIL_ROOT = "SharedFieldGroupDetail";

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
 * {@code .xml} path without spaces.
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

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapSharedFieldGroupDetailForWire(
  body: SharedFieldGroupWriteBody,
): Record<string, SharedFieldGroupWriteBody> {
  return { [SHARED_FIELD_GROUP_DETAIL_ROOT]: body };
}

/** Unwrap GET/POST/PUT payload that may be wrapped as { SharedFieldGroupDetail: {...} }. */
export function unwrapSharedFieldGroupDetail(payload: unknown): SharedFieldGroupDetail {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const raw = obj.SharedFieldGroupDetail ?? obj.sharedFieldGroupDetail;
  if (raw != null && typeof raw === "object" && !Array.isArray(raw)) {
    return raw as SharedFieldGroupDetail;
  }
  return obj as SharedFieldGroupDetail;
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
