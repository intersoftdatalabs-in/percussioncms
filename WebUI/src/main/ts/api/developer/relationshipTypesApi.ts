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
import type { RelationshipTypeDef } from "./types";

/**
 * Catalog-level design gaps remaining after Admin write (REST-GAPS-02).
 * Server omits these on list rows; detail re-attaches or SPA falls back here.
 * Create/update/delete of user types is supported — not listed.
 */
export const RELATIONSHIP_TYPE_DESIGN_GAPS: string[] = [
  "Cloning field override editor not supported via this API",
  "Effect condition and execution-context edit not supported via this API",
];

/** Category codes/labels aligned with PSRelationshipConfig.CATEGORY_ENUM. */
export const RELATIONSHIP_TYPE_CATEGORIES: ReadonlyArray<{
  code: string;
  label: string;
}> = [
  { code: "rs_activeassembly", label: "Active Assembly" },
  { code: "rs_copy", label: "New Copy" },
  { code: "rs_folder", label: "Folder" },
  { code: "rs_generic", label: "Generic" },
  { code: "rs_widget", label: "Widget" },
  { code: "rs_promotable", label: "Promotable Version" },
  { code: "rs_translation", label: "Translation" },
  { code: "rs_recycled", label: "Recycled" },
];

/** Writable fields for POST/PUT /services/relationshiptypes. */
export type RelationshipTypeWriteBody = Pick<
  RelationshipTypeDef,
  | "name"
  | "label"
  | "description"
  | "category"
  | "copyFrom"
  | "allowCloning"
  | "useOwnerRevision"
  | "useDependentRevision"
>;

/** Jackson / JAXB root for RelationshipType (UNWRAP_ROOT_VALUE on POST/PUT). */
export const RELATIONSHIP_TYPE_ROOT = "RelationshipType";

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapRelationshipTypeForWire(
  body: RelationshipTypeWriteBody,
): Record<string, RelationshipTypeWriteBody> {
  return { [RELATIONSHIP_TYPE_ROOT]: body };
}

/** Unwrap GET/POST/PUT payload that may be wrapped as { RelationshipType: {...} }. */
export function unwrapRelationshipType(payload: unknown): RelationshipTypeDef {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const raw = obj.RelationshipType ?? obj.relationshipType;
  if (raw != null && typeof raw === "object" && !Array.isArray(raw)) {
    return raw as RelationshipTypeDef;
  }
  return obj as RelationshipTypeDef;
}

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw =
      obj.RelationshipType ??
      obj.relationshipType ??
      obj.RelationshipTypes ??
      obj.relationshipTypes;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function withGaps(t: RelationshipTypeDef): RelationshipTypeDef {
  return {
    ...t,
    designGaps:
      t.designGaps && t.designGaps.length > 0
        ? t.designGaps
        : [...RELATIONSHIP_TYPE_DESIGN_GAPS],
  };
}

/** Trim; reject blank, whitespace inside, wildcards, path separators. */
export function normalizeRelationshipTypeName(
  name: string | undefined | null,
): string {
  if (name == null) return "";
  return name.trim();
}

/** True when the name is safe for REST create (no whitespace / * / path chars). */
export function isValidRelationshipTypeName(
  name: string | undefined | null,
): boolean {
  const trimmed = normalizeRelationshipTypeName(name);
  if (!trimmed) return false;
  if (trimmed.includes("*") || trimmed.includes("..")) return false;
  if (trimmed.includes("/") || trimmed.includes("\\") || trimmed.includes("\0")) {
    return false;
  }
  for (let i = 0; i < trimmed.length; i++) {
    if (/\s/.test(trimmed.charAt(i))) return false;
  }
  return true;
}

/** Save enabled when create has name+category|copyFrom, or edit has a key. */
export function isRelationshipTypeWriteReady(opts: {
  isNew: boolean;
  name: string;
  category: string;
  copyFrom: string;
}): boolean {
  if (opts.isNew) {
    if (!isValidRelationshipTypeName(opts.name)) return false;
    const hasCopy = normalizeRelationshipTypeName(opts.copyFrom).length > 0;
    const hasCategory = opts.category.trim().length > 0;
    return hasCopy || hasCategory;
  }
  return true;
}

/** True when the type is a packaged system type (immutable in chrome). */
export function isSystemRelationshipType(
  detail: Pick<RelationshipTypeDef, "systemType" | "userType" | "type"> | null | undefined,
): boolean {
  if (detail == null) return false;
  if (detail.systemType === true) return true;
  if (detail.userType === true) return false;
  const t = (detail.type || "").trim().toLowerCase();
  return t === "system";
}

/** GET /services/relationshiptypes — list omits designGaps on the wire (REST-GAPS-02). */
export async function listRelationshipTypes(): Promise<RelationshipTypeDef[]> {
  const payload = await get<unknown>(PATHS.RELATIONSHIP_TYPES);
  return asArray<RelationshipTypeDef>(payload);
}

/** GET /services/relationshiptypes/{idOrName} */
export async function getRelationshipTypeDetail(
  idOrName: string,
): Promise<RelationshipTypeDef> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.RELATIONSHIP_TYPES}/${key}`);
  return withGaps(unwrapRelationshipType(payload));
}

/** POST /services/relationshiptypes — Admin. name + category or copyFrom. */
export async function createRelationshipType(
  body: RelationshipTypeWriteBody,
): Promise<RelationshipTypeDef> {
  const payload = await post<unknown>(
    PATHS.RELATIONSHIP_TYPES,
    wrapRelationshipTypeForWire(body),
  );
  return withGaps(unwrapRelationshipType(payload));
}

/** PUT /services/relationshiptypes/{idOrName} — Admin. Name is not renamed. */
export async function updateRelationshipType(
  idOrName: string,
  body: RelationshipTypeWriteBody,
): Promise<RelationshipTypeDef> {
  const payload = await put<unknown>(
    `${PATHS.RELATIONSHIP_TYPES}/${encodeURIComponent(idOrName)}`,
    wrapRelationshipTypeForWire(body),
  );
  return withGaps(unwrapRelationshipType(payload));
}

/** DELETE /services/relationshiptypes/{idOrName} — Admin. 204 on success. */
export async function deleteRelationshipType(idOrName: string): Promise<void> {
  await del(`${PATHS.RELATIONSHIP_TYPES}/${encodeURIComponent(idOrName)}`);
}
