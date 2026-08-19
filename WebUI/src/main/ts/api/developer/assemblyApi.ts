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
  normalizeDesignObjectGuid,
  resolveTemplateObjectGuid,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import {
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  normalizeSlotStringMap,
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
 * Wire JSON body for SlotsResource PUT under {@link SLOT_DETAIL_ROOT}.
 */
export function wrapSlotDetailForWire(
  body: Partial<
    Pick<SlotDetail, "label" | "description" | "associations" | "slotLayout" | "slotStyles">
  >,
): Record<string, typeof body> {
  return { [SLOT_DETAIL_ROOT]: body };
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
 * Root-wraps request body; unwraps response.
 */
export async function updateSlotDetail(
  idOrName: string,
  body: Partial<Pick<SlotDetail, "label" | "description" | "associations" | "slotLayout" | "slotStyles">>,
): Promise<SlotDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.SLOTS}/${key}`,
    wrapSlotDetailForWire(body),
  );
  return unwrapSlotDetail(payload);
}

/** GET /services/communities/find?name=* */
export async function listCommunities(): Promise<CommunitySummary[]> {
  const payload = await get<unknown>(
    `${PATHS.COMMUNITIES}/find?name=${encodeURIComponent("*")}`,
  );
  // CommunityList extends Array — may serialize as array or envelope
  return asArray<CommunitySummary>(payload, [
    "CommunityList",
    "Community",
    "communityList",
  ]);
}

/** GET /services/communities/{idOrName} — detail with roles */
export async function getCommunityDetail(
  idOrName: string,
): Promise<CommunityDetail> {
  const key = encodeURIComponent(idOrName);
  return get<CommunityDetail>(`${PATHS.COMMUNITIES}/${key}`);
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
