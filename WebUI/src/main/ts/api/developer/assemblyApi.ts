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
import {
  COMMUNITY_TYPE,
  normalizeDesignObjectGuid,
  objectGuidString,
  resolveCommunityObjectGuid,
  resolveTemplateObjectGuid,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import {
  unwrapObjectLockSummary,
  type ContentTypeLockSummary,
} from "./contentTypesApi";
import {
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  normalizeSlotStringMap,
  type SlotUpdateBody,
} from "./slotLists";
import type {
  CommunityDetail,
  CommunityRoleSummary,
  CommunitySummary,
  CommunityVisibility,
  CommunityVisibleObject,
  RestGuid,
  SlotDetail,
  SlotSummary,
  TemplateDetail,
  TemplateSummary,
} from "./types";

export {
  buildSlotUpdateBody,
  slotFinderWriteRequested,
  type SlotUpdateBody,
} from "./slotLists";

export {
  asJacksonArray,
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  normalizeSlotStringMap,
} from "./slotLists";

/**
 * Jackson {@code @JsonRootName} / WRAP_ROOT_VALUE root for {@code TemplateDetail}.
 * REST {@code JacksonContextResolver} wraps single-object responses and requires
 * the same envelope on PUT (UNWRAP_ROOT_VALUE). Without client unwrap, the SPA
 * reads {@code d.templateSource} on the outer object and the Source editor is
 * always empty (#3039).
 */
export const TEMPLATE_DETAIL_ROOT = "TemplateDetail";

/** Jackson root for {@code SlotDetail} (same WRAP/UNWRAP contract as templates). */
export const SLOT_DETAIL_ROOT = "SlotDetail";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Normalize a templates GET/PUT response to a flat {@link TemplateDetail}.
 *
 * <p>Prefers {@code { "TemplateDetail": { … } }}; also accepts a flat body
 * (unit tests / proxies that already unwrapped).
 */
function normalizeTemplateDetail(body: TemplateDetail): TemplateDetail {
  const catalog = resolveTemplateObjectGuid(body);
  return normalizeDesignObjectGuid(body, catalog);
}

export function unwrapTemplateDetail(payload: unknown): TemplateDetail {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root[TEMPLATE_DETAIL_ROOT] ?? root.templateDetail);
  if (nested) {
    return normalizeTemplateDetail(nested as TemplateDetail);
  }
  // Flat body: only treat as detail when it looks like one (has template identity
  // or source/bindings fields). Bare error envelopes stay empty.
  if (
    "name" in root ||
    "templateId" in root ||
    "templateSource" in root ||
    "label" in root ||
    "bindings" in root ||
    "guid" in root ||
    "guidString" in root
  ) {
    return normalizeTemplateDetail(root as TemplateDetail);
  }
  return {};
}

/**
 * Build the wire JSON body for TemplatesResource PUT under
 * {@link TEMPLATE_DETAIL_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE
 * (same class as UserPreference #2708).
 */
export function wrapTemplateDetailForWire(
  body: Partial<
    Pick<
      TemplateDetail,
      | "name"
      | "label"
      | "description"
      | "templateSource"
      | "assembler"
      | "mimeType"
      | "bindings"
      | "slots"
    >
  >,
): Record<string, typeof body> {
  return { [TEMPLATE_DETAIL_ROOT]: body };
}

function normalizeSlotDetail(detail: SlotDetail): SlotDetail {
  return {
    ...detail,
    associations: normalizeSlotAssociations(detail.associations),
    designGaps: normalizeSlotDesignGaps(detail.designGaps),
    finderArguments: normalizeSlotStringMap(detail.finderArguments),
  };
}

/**
 * Normalize a slots GET/PUT response to a flat {@link SlotDetail}.
 */
export function unwrapSlotDetail(payload: unknown): SlotDetail {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root[SLOT_DETAIL_ROOT] ?? root.slotDetail);
  if (nested) {
    return normalizeSlotDetail(nested as SlotDetail);
  }
  if ("name" in root || "label" in root || "associations" in root || "slotLayout" in root) {
    return normalizeSlotDetail(root as SlotDetail);
  }
  return {};
}

/**
 * JAXB/Jackson map envelope used by slot GET/PUT for {@code finderArguments}.
 * A flat JSON object often fails to bind on UNWRAP_ROOT_VALUE + JAXB Map.
 */
export function finderArgumentsForWire(
  map: Record<string, string>,
): { entry: { key: string; value: string }[] } {
  return {
    entry: Object.entries(map).map(([key, value]) => ({ key, value })),
  };
}

/**
 * Wire JSON body for SlotsResource POST/PUT under {@link SLOT_DETAIL_ROOT}.
 */
export function wrapSlotDetailForWire(
  body: Partial<
    Pick<
      SlotDetail,
      | "name"
      | "label"
      | "description"
      | "slotType"
      | "associations"
      | "slotLayout"
      | "slotStyles"
      | "finderName"
      | "relationshipName"
      | "finderArguments"
    >
  >,
): Record<string, unknown> {
  const wire: Record<string, unknown> = { ...body };
  if (body.finderArguments != null) {
    wire.finderArguments = finderArgumentsForWire(
      normalizeSlotStringMap(body.finderArguments),
    );
  }
  return { [SLOT_DETAIL_ROOT]: wire };
}

/** Allowed {@code slotType} values on POST /services/slots (case-insensitive). */
export const SLOT_TYPES = ["REGULAR", "INLINE"] as const;
export type SlotTypeName = (typeof SLOT_TYPES)[number];

/**
 * Slot names must be non-blank, must not contain whitespace, and must not
 * contain wildcards ({@code *}) — same rules as REST create.
 */
export function isValidSlotName(name: string): boolean {
  const n = name.trim();
  return n.length > 0 && !/\s/.test(n) && !n.includes("*");
}

/** Empty slotType is valid (server defaults to REGULAR); otherwise REGULAR|INLINE. */
export function isValidSlotType(slotType: string): boolean {
  const t = slotType.trim();
  if (!t) return true;
  const upper = t.toUpperCase();
  return upper === "REGULAR" || upper === "INLINE";
}

/** Create save is enabled when the name (and optional slotType) are valid. */
export function isSlotCreateReady(opts: { name: string; slotType: string }): boolean {
  return isValidSlotName(opts.name) && isValidSlotType(opts.slotType);
}

function asArray<T>(payload: unknown, keys: string[]): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const k of keys) {
      const raw = obj[k];
      if (raw == null) continue;
      return Array.isArray(raw) ? (raw as T[]) : [raw as T];
    }
  }
  return [];
}

/** GET /services/templates */
export async function listTemplates(): Promise<TemplateSummary[]> {
  const payload = await get<unknown>(PATHS.TEMPLATES);
  return asArray<TemplateSummary>(payload, [
    "TemplateSummaryList",
    "TemplateSummary",
    "templateSummaryList",
  ]);
}

/**
 * GET /services/templates/{idOrName}
 *
 * <p>Throws {@link ApiError} / {@link SessionRedirectError} on non-2xx (including 404).
 * Unwraps Jackson {@link TEMPLATE_DETAIL_ROOT} so callers bind {@code templateSource}
 * and meta fields from a flat object (#3039).
 */
export async function getTemplateDetail(
  idOrName: string,
): Promise<TemplateDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.TEMPLATES}/${key}`);
  return unwrapTemplateDetail(payload);
}

/**
 * PUT /services/templates/{idOrName} — label, description, source, optional bindings/slots.
 * Omitted fields are left unchanged server-side; {@code bindings}/{@code slots} when present
 * fully replace the collection (including empty list). Request body is root-wrapped for
 * server UNWRAP_ROOT_VALUE; response is unwrapped the same way as GET.
 */
export async function updateTemplateDetail(
  idOrName: string,
  body: Partial<
    Pick<
      TemplateDetail,
      "label" | "description" | "templateSource" | "assembler" | "bindings" | "slots"
    >
  >,
): Promise<TemplateDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.TEMPLATES}/${key}`,
    wrapTemplateDetailForWire(body),
  );
  return unwrapTemplateDetail(payload);
}

/** Fields accepted on POST /services/templates (modern create — no Widget XML). */
export type TemplateCreateBody = Partial<
  Pick<
    TemplateDetail,
    "name" | "label" | "description" | "assembler" | "templateSource" | "mimeType"
  >
> & {
  name: string;
};

/**
 * POST /services/templates — create a modern assembly template.
 * Request is root-wrapped; response is unwrapped like GET/PUT.
 */
export async function createTemplate(
  body: TemplateCreateBody,
): Promise<TemplateDetail> {
  const payload = await post<unknown>(
    PATHS.TEMPLATES,
    wrapTemplateDetailForWire(body),
  );
  return unwrapTemplateDetail(payload);
}

/**
 * DELETE /services/templates/{idOrName} — remove a modern assembly template.
 * Does not write Widget definition XML. 204 has no body.
 */
export async function deleteTemplate(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await del<void>(`${PATHS.TEMPLATES}/${key}`);
}

/** GET /services/slots */
export async function listSlots(): Promise<SlotSummary[]> {
  const payload = await get<unknown>(PATHS.SLOTS);
  return asArray<SlotSummary>(payload, ["Slot", "slot", "SlotList"]);
}

/** GET /services/slots/{idOrName} — unwraps {@link SLOT_DETAIL_ROOT}. */
export async function getSlotDetail(idOrName: string): Promise<SlotDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.SLOTS}/${key}`);
  return unwrapSlotDetail(payload);
}

/**
 * PUT /services/slots/{idOrName} — label, description, optional associations replace.
 * Non-null finderName / relationshipName / finderArguments are Admin writes that
 * require a held design lock (409 unlocked). Root-wraps request; unwraps response.
 */
export async function updateSlotDetail(
  idOrName: string,
  body: SlotUpdateBody,
): Promise<SlotDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.SLOTS}/${key}`,
    wrapSlotDetailForWire(body),
  );
  return unwrapSlotDetail(payload);
}

/** POST /services/slots/{idOrName}/lock — Admin self-only design-session lock. */
export async function lockSlot(idOrName: string): Promise<ContentTypeLockSummary> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(`${PATHS.SLOTS}/${key}/lock`);
  return unwrapObjectLockSummary(payload);
}

/** POST /services/slots/{idOrName}/unlock — release a lock owned by this session. */
export async function unlockSlot(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await post(`${PATHS.SLOTS}/${key}/unlock`);
}

/** Fields accepted on POST /services/slots (AS-01). Finder/relationship not written. */
export type SlotCreateBody = {
  name: string;
  label?: string;
  description?: string;
  slotType?: string;
};

/**
 * POST /services/slots — Admin. Unique name, no whitespace. Optional label,
 * description, and slotType (REGULAR|INLINE). Duplicate is 409. Non-Admin is 403.
 * Invalid name/slotType is 400. Request is root-wrapped; response unwrapped like GET.
 */
export async function createSlot(body: SlotCreateBody): Promise<SlotDetail> {
  const payload = await post<unknown>(PATHS.SLOTS, wrapSlotDetailForWire(body));
  return unwrapSlotDetail(payload);
}

/**
 * DELETE /services/slots/{idOrName} — Admin. 204 on success. System slot is 409.
 * Non-Admin is 403. Missing is 404.
 */
export async function deleteSlot(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await del<void>(`${PATHS.SLOTS}/${key}`);
}

/** GET /services/communities/find?name=* */
export async function listCommunities(): Promise<CommunitySummary[]> {
  const payload = await get<unknown>(
    `${PATHS.COMMUNITIES}/find?name=${encodeURIComponent("*")}`,
  );
  return unwrapCommunityList(payload);
}

/** JAXB / swagger root for POST /services/communities/bulk `List<String>` names. */
export const COMMUNITY_NAME_LIST_ROOT = "List";

/** Jackson WRAP/UNWRAP root for {@code CommunityList}. */
export const COMMUNITY_LIST_ROOT = "CommunityList";

/** Jackson WRAP/UNWRAP root for {@code GuidList} bulk delete. */
export const GUID_LIST_ROOT = "GuidList";

/** Trim a community name for create. Empty / null becomes "". */
export function normalizeCommunityName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create
 * ({@code PSSecurityDesignWs.createCommunities} — non-blank).
 * Spaces are allowed (unlike slots). Uniqueness is case-insensitive on the server.
 */
export function isValidCommunityName(name: string | undefined | null): boolean {
  return normalizeCommunityName(name).length > 0;
}

/** Create is enabled when the community name is non-blank after trim. */
export function isCommunityWriteReady(opts: { name: string }): boolean {
  return isValidCommunityName(opts.name);
}

/** Wire JSON for POST /services/communities/bulk name list. */
export function wrapCommunityNameListForWire(
  names: string[],
): Record<string, string[]> {
  return { [COMMUNITY_NAME_LIST_ROOT]: names };
}

/**
 * Wire JSON for PUT /services/communities/bulk.
 * Envelope is parsed by rest {@code CommunityListJsonReader} (String body).
 */
export function wrapCommunityListForWire(
  communities: CommunitySummary[],
): Record<string, CommunitySummary[]> {
  return { [COMMUNITY_LIST_ROOT]: communities };
}

/**
 * Wire JSON for DELETE /services/communities/bulk GuidList.
 * Envelope is parsed by rest {@code GuidListJsonReader} (String body).
 */
export function wrapGuidListForWire(ids: RestGuid[]): Record<string, RestGuid[]> {
  return { [GUID_LIST_ROOT]: ids };
}

/**
 * Flatten nested Jackson Guid wraps so {@code guid.stringValue} is usable
 * for delete / visibility (same WRAP_ROOT_VALUE as templates).
 */
export function normalizeCommunitySummary<T extends CommunitySummary>(
  item: T,
  catalogGuid?: string | null,
): T {
  const gs = resolveCommunityObjectGuid(item, catalogGuid);
  return normalizeDesignObjectGuid(item, gs);
}

/** Unwrap CommunityList array or envelope (find / create response). */
export function unwrapCommunityList(payload: unknown): CommunitySummary[] {
  return asArray<CommunitySummary>(payload, [
    COMMUNITY_LIST_ROOT,
    "Community",
    "communityList",
  ]).map((row) => normalizeCommunitySummary(row));
}

/**
 * POST /services/communities/bulk — Admin. Creates communities from names
 * ({@code ICommunityAdaptor.createCommunities}). Blank name is 400. Duplicate
 * is 409. Non-Admin is 403. Server persists (create+save); do not PUT the DTO.
 */
export async function createCommunities(
  names: string[],
): Promise<CommunitySummary[]> {
  const payload = await post<unknown>(
    `${PATHS.COMMUNITIES}/bulk`,
    wrapCommunityNameListForWire(names),
  );
  return unwrapCommunityList(payload);
}

/**
 * PUT /services/communities/bulk — Admin. Persist created/edited communities.
 * {@code release=true} releases design locks (Workbench Finish).
 */
export async function saveCommunities(
  communities: CommunitySummary[],
  release = true,
): Promise<void> {
  await put(
    `${PATHS.COMMUNITIES}/bulk`,
    wrapCommunityListForWire(communities),
    { release: String(release) },
  );
}

/**
 * Create one community by name. Server POST /bulk persists (create+save)
 * like Workbench Finish; do not PUT the DTO back (loadCommunities NPE).
 */
export async function createCommunity(name: string): Promise<CommunitySummary> {
  const trimmed = normalizeCommunityName(name);
  const created = await createCommunities([trimmed]);
  const first = created[0];
  if (first) {
    return first;
  }
  return { name: trimmed };
}

/**
 * DELETE /services/communities/bulk — Admin. GuidList of communities to remove.
 * Default {@code ignoredependencies=false}: in-use is 409 and the community remains
 * (does not steal). Missing is 404. Non-Admin is 403.
 */
export async function deleteCommunities(
  ids: RestGuid[],
  ignoreDependencies = false,
): Promise<void> {
  await del(
    `${PATHS.COMMUNITIES}/bulk`,
    { ignoredependencies: String(ignoreDependencies) },
    wrapGuidListForWire(ids),
  );
}

/** Delete one community by GUID. Never sends ignoredependencies unless asked. */
export async function deleteCommunity(
  guid: RestGuid,
  ignoreDependencies = false,
): Promise<void> {
  await deleteCommunities([guid], ignoreDependencies);
}

/** Unwrap GET `{ Community: {…} }` or a flat Community body. */
export function unwrapCommunityDetail(payload: unknown): CommunityDetail {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root.Community ?? root.community);
  const body = (nested ?? root) as CommunityDetail;
  return normalizeCommunitySummary(body);
}

/**
 * GUID for community delete / visibility. Reads nested Guid wraps and
 * synthesizes {@code 0-13-{id}} when only the numeric id is present.
 */
export function communityGuidForWrite(
  detail: CommunitySummary | null | undefined,
  fallback?: RestGuid | null,
): RestGuid | null {
  const gs = resolveCommunityObjectGuid(detail, objectGuidString(fallback));
  if (!gs) {
    return fallback ?? null;
  }
  const existing =
    detail?.guid != null && typeof detail.guid === "object" && !Array.isArray(detail.guid)
      ? (detail.guid as RestGuid)
      : fallback;
  if (existing && existing.stringValue === gs) {
    return existing;
  }
  return { ...(existing || {}), stringValue: gs, type: existing?.type ?? COMMUNITY_TYPE };
}

/** GET /services/communities/{idOrName} — detail with roles */
export async function getCommunityDetail(
  idOrName: string,
): Promise<CommunityDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.COMMUNITIES}/${key}`);
  return unwrapCommunityDetail(payload);
}

/** GET /services/communities/roles — all roles for membership picker */
export async function listAvailableRoles(): Promise<CommunityRoleSummary[]> {
  const payload = await get<unknown>(`${PATHS.COMMUNITIES}/roles`);
  return asArray<CommunityRoleSummary>(payload, [
    "CommunityRoleList",
    "CommunityRole",
    "roleList",
  ]);
}

/** PUT /services/communities/{idOrName}/roles — replace memberships */
export async function updateCommunityRoles(
  idOrName: string,
  roles: CommunityRoleSummary[],
): Promise<CommunityDetail> {
  const key = encodeURIComponent(idOrName);
  return put<CommunityDetail>(`${PATHS.COMMUNITIES}/${key}/roles`, roles);
}

/** Preferred filter header for community visibility (server also accepts legacy {@code type}). */
export const COMMUNITY_VISIBILITY_TYPE_HEADER = "X-Object-Type";

/**
 * POST /services/communities/visibility — objects visible to the given community GUID.
 *
 * <p>Body is a GuidList (JSON array of Guid). Optional object-type filter is sent as
 * {@link COMMUNITY_VISIBILITY_TYPE_HEADER} (trimmed non-empty only).
 */
export async function getCommunityVisibility(
  communityGuid: RestGuid,
  objectType?: string,
): Promise<CommunityVisibleObject[]> {
  const trimmedType = objectType?.trim();
  const headers: HeadersInit | undefined =
    trimmedType && trimmedType.length > 0
      ? { [COMMUNITY_VISIBILITY_TYPE_HEADER]: trimmedType }
      : undefined;
  const payload = await post<unknown>(
    `${PATHS.COMMUNITIES}/visibility`,
    [communityGuid],
    headers,
  );
  // Response: CommunityVisibilityList array or envelope
  const list = asArray<CommunityVisibility>(payload, [
    "CommunityVisibilityList",
    "CommunityVisibility",
  ]);
  const first = list[0];
  if (!first?.visibleObjects) return [];
  if (Array.isArray(first.visibleObjects)) return first.visibleObjects;
  const env = first.visibleObjects as { ObjectSummary?: CommunityVisibleObject[] };
  return Array.isArray(env.ObjectSummary) ? env.ObjectSummary : [];
}
