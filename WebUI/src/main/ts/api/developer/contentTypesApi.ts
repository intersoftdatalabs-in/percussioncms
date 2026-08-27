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

import { normalizeDesignObjectGuid } from "../displayFormatGuid";
import { get, post, put } from "../client";
import { PATHS } from "../paths";
import {
  normalizeContentTypeControlProperties,
  normalizeContentTypeDesignGaps,
  normalizeContentTypeFields,
  normalizeContentTypeStringList,
  normalizeNamedObjectRefs,
} from "./contentTypeLists";
import type {
  ContentTypeChoiceCatalog,
  ContentTypeControlProperty,
  ContentTypeDetail,
  ContentTypeFieldControlProperties,
  ContentTypeFieldSummary,
  ContentTypeSummary,
  NamedObjectRef,
} from "./types";

export {
  normalizeContentTypeControlProperties,
  normalizeContentTypeDesignGaps,
  normalizeContentTypeFields,
  normalizeContentTypeStringList,
  normalizeNamedObjectRefs,
} from "./contentTypeLists";

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeDetail}. */
export const CONTENT_TYPE_DETAIL_ROOT = "ContentTypeDetail";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function normalizeContentTypeSummary(item: ContentTypeSummary): ContentTypeSummary {
  return normalizeDesignObjectGuid(item);
}

/** Jackson WRAP_ROOT / JAXB / ArrayList envelopes for GET /services/contenttypes. */
const CONTENT_TYPE_LIST_WRAP_KEYS = [
  "ContentTypeList",
  "contentTypeList",
  "ContentType",
  "contentType",
  "contentTypes",
  "ArrayList",
  "arrayList",
  "items",
] as const;

const MAX_CONTENT_TYPE_LIST_DEPTH = 6;

function looksLikeContentTypeSummary(obj: Record<string, unknown>): boolean {
  return (
    obj.name != null ||
    obj.label != null ||
    obj.guid != null ||
    obj.guidString != null ||
    typeof obj.hideFromMenu === "boolean"
  );
}

function isEmptyCollectionBean(obj: Record<string, unknown>): boolean {
  if (!("empty" in obj) || typeof obj.empty !== "boolean") {
    return false;
  }
  return Object.keys(obj).every((k) => k === "empty");
}

/**
 * Flatten Jackson list envelopes so the catalog never receives a non-array.
 *
 * <p>Live WRAP_ROOT_VALUE serializes {@code ContentTypeList} as
 * {@code {"ContentTypeList":[…]}} (class name) or {@code {"ContentType":[…]}}
 * ({@code @XmlRootElement}). A one-level {@code env.ContentType} read misses
 * the class-name root and can leave a truthy object for {@code [...items]} /
 * {@code .map} — DeveloperSectionErrorBoundary (#3706 / peer searches #3576).
 * Also flattens nested wraps, empty-collection beans (`{ "empty": false }`),
 * and singleton objects (#3712 catalog safety).
 */
function flattenContentTypeList(payload: unknown, depth = 0): unknown[] {
  if (payload == null || depth > MAX_CONTENT_TYPE_LIST_DEPTH) {
    return [];
  }
  if (Array.isArray(payload)) {
    const out: unknown[] = [];
    for (const item of payload) {
      if (item == null || typeof item !== "object") {
        continue;
      }
      const rec = item as Record<string, unknown>;
      const wrapped =
        !looksLikeContentTypeSummary(rec) &&
        CONTENT_TYPE_LIST_WRAP_KEYS.some((k) => rec[k] != null);
      if (wrapped || !looksLikeContentTypeSummary(rec)) {
        out.push(...flattenContentTypeList(item, depth + 1));
      } else {
        out.push(item);
      }
    }
    return out;
  }
  const obj = asRecord(payload);
  if (!obj) {
    return [];
  }
  if (isEmptyCollectionBean(obj)) {
    return [];
  }
  for (const key of CONTENT_TYPE_LIST_WRAP_KEYS) {
    if (obj[key] != null) {
      return flattenContentTypeList(obj[key], depth + 1);
    }
  }
  if (looksLikeContentTypeSummary(obj)) {
    return [obj];
  }
  return [];
}

function normalizeContentTypeDetail(detail: ContentTypeDetail): ContentTypeDetail {
  return {
    ...detail,
    fields: normalizeContentTypeFields(detail.fields),
    childFieldSets: normalizeContentTypeStringList(detail.childFieldSets),
    allowedWorkflows: normalizeNamedObjectRefs(detail.allowedWorkflows),
    allowedTemplates: normalizeNamedObjectRefs(detail.allowedTemplates),
    designGaps: normalizeContentTypeDesignGaps(detail.designGaps),
  };
}

/**
 * Normalize list responses: bare array, {@code ContentTypeList}/{@code ContentType}
 * envelopes, nested wraps, singleton object, or empty-collection bean (#3706).
 * Always returns an array.
 */
export function unwrapContentTypeList(payload: unknown): ContentTypeSummary[] {
  return flattenContentTypeList(payload).map((item) =>
    normalizeContentTypeSummary(item as ContentTypeSummary),
  );
}

/**
 * Normalize a content-type GET/PUT response to a flat {@link ContentTypeDetail}.
 *
 * <p>Prefers {@code { "ContentTypeDetail": { … } }} (Jackson WRAP_ROOT_VALUE);
 * also accepts a flat body. Fills {@code guid.stringValue} / {@code guidString}
 * from nested Guid parts (#3319).
 */
export function unwrapContentTypeDetail(payload: unknown): ContentTypeDetail {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root[CONTENT_TYPE_DETAIL_ROOT] ?? root.contentTypeDetail);
  let body: ContentTypeDetail;
  if (nested) {
    body = nested as ContentTypeDetail;
  } else if (
    "name" in root ||
    "guid" in root ||
    "guidString" in root ||
    "fields" in root ||
    "label" in root
  ) {
    body = root as ContentTypeDetail;
  } else {
    return {};
  }
  return normalizeDesignObjectGuid(normalizeContentTypeDetail(body));
}

/**
 * List content types available on the server.
 *
 * <p>Server: {@code GET /services/contenttypes} (ContentTypesResource).
 * Not a full design-object load — name/label/description/guid only.
 */
export async function listContentTypes(): Promise<ContentTypeSummary[]> {
  const payload = await get<unknown>(PATHS.CONTENT_TYPES);
  return unwrapContentTypeList(payload);
}

/**
 * Load design summary (fields) for one content type.
 *
 * <p>Server: {@code GET /services/contenttypes/{idOrName}} where idOrName is
 * uuid, guid string, or internal name.
 */
export async function getContentTypeDetail(
  idOrName: string,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.CONTENT_TYPES}/${key}`);
  return unwrapContentTypeDetail(payload);
}

export type ContentTypeUpdateBody = {
  label?: string;
  description?: string;
  enabled?: boolean;
  fields?: Pick<ContentTypeFieldSummary, "name" | "searchable" | "required" | "occurrence">[];
  /** Omit to leave unchanged; non-null list is a full replace. Prefer CD-08 PUT. */
  allowedWorkflows?: NamedObjectRef[];
  defaultWorkflow?: NamedObjectRef | null;
  /**
   * Omit to leave unchanged. Prefer {@link replaceContentTypeAllowedTemplates}
   * (CD-12 dedicated PUT) instead of sending this on the bulk detail PUT.
   */
  allowedTemplates?: NamedObjectRef[];
};

/** Wire body for {@code PUT .../allowedWorkflows} (Jackson root {@code ContentTypeWorkflows}). */
export type ContentTypeWorkflowsBody = {
  allowedWorkflows: NamedObjectRef[];
  defaultWorkflow?: NamedObjectRef | null;
};

/** Wire shape for {@code POST .../lock} ({@code ObjectLockSummary}). */
export type ContentTypeLockSummary = {
  session?: string;
  locker?: string;
  remainingTime?: number;
  callerAccessTime?: string;
};

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ObjectLockSummary}. */
export const OBJECT_LOCK_SUMMARY_ROOT = "ObjectLockSummary";

/**
 * Normalize a lock POST response to a flat {@link ContentTypeLockSummary}.
 */
export function unwrapObjectLockSummary(payload: unknown): ContentTypeLockSummary {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  const nested = asRecord(root[OBJECT_LOCK_SUMMARY_ROOT] ?? root.objectLockSummary);
  const body = nested ?? root;
  const out: ContentTypeLockSummary = {};
  if (typeof body.session === "string") {
    out.session = body.session;
  }
  if (typeof body.locker === "string") {
    out.locker = body.locker;
  }
  if (typeof body.remainingTime === "number") {
    out.remainingTime = body.remainingTime;
  }
  if (typeof body.callerAccessTime === "string") {
    out.callerAccessTime = body.callerAccessTime;
  }
  return out;
}

/**
 * POST /services/contenttypes/{idOrName}/lock — self-only design-session lock.
 */
export async function lockContentType(
  idOrName: string,
): Promise<ContentTypeLockSummary> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(`${PATHS.CONTENT_TYPES}/${key}/lock`);
  return unwrapObjectLockSummary(payload);
}

/**
 * POST /services/contenttypes/{idOrName}/unlock — release a held design-session lock.
 */
export async function unlockContentType(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await post(`${PATHS.CONTENT_TYPES}/${key}/unlock`);
}

/**
 * Build the wire JSON body for ContentTypesResource PUT under
 * {@link CONTENT_TYPE_DETAIL_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE
 * (same class as TemplateDetail / UserPreference).
 */
export function wrapContentTypeDetailForWire(
  body: ContentTypeUpdateBody,
): Record<string, ContentTypeUpdateBody> {
  return { [CONTENT_TYPE_DETAIL_ROOT]: body };
}

/**
 * PUT /services/contenttypes/{idOrName} — requires a held design lock; does not
 * acquire or release it. Call {@link lockContentType} first. HTTP 409 when
 * unlocked or locked by another user.
 *
 * <p>Do not send {@code enabled} here — use {@link setContentTypeEnabled} (CD-13).
 * Do not send {@code allowedWorkflows} here — use
 * {@link setContentTypeAllowedWorkflows} (CD-08).
 * Do not send {@code allowedTemplates} here — use
 * {@link replaceContentTypeAllowedTemplates} (CD-12).
 */
export async function updateContentTypeDetail(
  idOrName: string,
  body: ContentTypeUpdateBody,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}`,
    wrapContentTypeDetailForWire(body),
  );
  return unwrapContentTypeDetail(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeEnabled}. */
export const CONTENT_TYPE_ENABLED_ROOT = "ContentTypeEnabled";

/**
 * Build the wire JSON body for {@code PUT .../enabled} under
 * {@link CONTENT_TYPE_ENABLED_ROOT}. A flat {@code { enabled }} body fails
 * server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeEnabledForWire(
  enabled: boolean,
): Record<string, { enabled: boolean }> {
  return { [CONTENT_TYPE_ENABLED_ROOT]: { enabled } };
}

/**
 * PUT /services/contenttypes/{idOrName}/enabled — CD-13 dedicated enable/disable.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. Response is {@code ContentTypeDetail}
 * with the new {@code enabled} value (lock still held).
 */
export async function setContentTypeEnabled(
  idOrName: string,
  enabled: boolean,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/enabled`,
    wrapContentTypeEnabledForWire(enabled),
  );
  return unwrapContentTypeDetail(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeWorkflows}. */
export const CONTENT_TYPE_WORKFLOWS_ROOT = "ContentTypeWorkflows";

/**
 * Build the wire JSON body for {@code PUT .../allowedWorkflows} under
 * {@link CONTENT_TYPE_WORKFLOWS_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeWorkflowsForWire(
  body: ContentTypeWorkflowsBody,
): Record<string, ContentTypeWorkflowsBody> {
  return { [CONTENT_TYPE_WORKFLOWS_ROOT]: body };
}

/**
 * PUT /services/contenttypes/{idOrName}/allowedWorkflows — CD-08 dedicated replace.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. Empty {@code allowedWorkflows}
 * clears associations. Response is {@code ContentTypeDetail} with the new set
 * (lock still held).
 */
export async function setContentTypeAllowedWorkflows(
  idOrName: string,
  body: ContentTypeWorkflowsBody,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/allowedWorkflows`,
    wrapContentTypeWorkflowsForWire(body),
  );
  return unwrapContentTypeDetail(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code NamedObjectRefList}. */
export const NAMED_OBJECT_REF_LIST_ROOT = "NamedObjectRefList";

/**
 * Build the wire JSON body for {@code PUT .../allowedTemplates} under
 * {@link NAMED_OBJECT_REF_LIST_ROOT}. A bare array fails server UNWRAP_ROOT_VALUE.
 */
export function wrapNamedObjectRefListForWire(
  items: NamedObjectRef[],
): Record<string, NamedObjectRef[]> {
  return { [NAMED_OBJECT_REF_LIST_ROOT]: items };
}

/**
 * Flatten GET/PUT {@code .../allowedTemplates} JSON to {@link NamedObjectRef}[].
 *
 * <p>Handles WRAP_ROOT {@code NamedObjectRefList}, JAXB {@code NamedObjectRef}
 * envelope, bare array, singleton object, and empty-collection beans.
 */
export function unwrapNamedObjectRefList(payload: unknown): NamedObjectRef[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return normalizeNamedObjectRefs(payload);
  }
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  if (root[NAMED_OBJECT_REF_LIST_ROOT] != null) {
    return normalizeNamedObjectRefs(root[NAMED_OBJECT_REF_LIST_ROOT]);
  }
  if (root.namedObjectRefList != null) {
    return normalizeNamedObjectRefs(root.namedObjectRefList);
  }
  return normalizeNamedObjectRefs(payload);
}

/**
 * GET /services/contenttypes/{idOrName}/allowedTemplates — CD-12 read.
 * No design lock required. Empty list means none.
 */
export async function getContentTypeAllowedTemplates(
  idOrName: string,
): Promise<NamedObjectRef[]> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.CONTENT_TYPES}/${key}/allowedTemplates`);
  return unwrapNamedObjectRefList(payload);
}

/**
 * PUT /services/contenttypes/{idOrName}/allowedTemplates — CD-12 full replace.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. HTTP 400 when a template name/guid
 * cannot be resolved. Empty list clears associations.
 */
export async function replaceContentTypeAllowedTemplates(
  idOrName: string,
  templates: NamedObjectRef[],
): Promise<NamedObjectRef[]> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/allowedTemplates`,
    wrapNamedObjectRefListForWire(templates),
  );
  return unwrapNamedObjectRefList(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeFieldControlProperties}. */
export const CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT =
  "ContentTypeFieldControlProperties";

export type ContentTypeFieldControlPropertiesBody = {
  properties: ContentTypeControlProperty[];
};

function asChoiceCatalog(raw: unknown): ContentTypeChoiceCatalog | null {
  const rec = asRecord(raw);
  if (!rec) {
    return null;
  }
  const out: ContentTypeChoiceCatalog = {};
  if (typeof rec.type === "string") {
    out.type = rec.type;
  }
  if (typeof rec.globalId === "string") {
    out.globalId = rec.globalId;
  }
  if (typeof rec.sortOrder === "string") {
    out.sortOrder = rec.sortOrder;
  }
  if (typeof rec.lookupHref === "string") {
    out.lookupHref = rec.lookupHref;
  }
  return out;
}

/**
 * Flatten GET/PUT {@code .../fields/{field}/controlProperties} JSON.
 *
 * <p>Handles WRAP_ROOT {@code ContentTypeFieldControlProperties}, a flat body,
 * JAXB property envelopes, and empty-collection beans.
 */
export function unwrapFieldControlProperties(
  payload: unknown,
): ContentTypeFieldControlProperties {
  const root = asRecord(payload);
  if (!root) {
    return { properties: [] };
  }
  const nested = asRecord(
    root[CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT] ??
      root.contentTypeFieldControlProperties,
  );
  const body = nested ?? root;
  const out: ContentTypeFieldControlProperties = {
    properties: normalizeContentTypeControlProperties(body.properties),
  };
  if (typeof body.fieldName === "string") {
    out.fieldName = body.fieldName;
  }
  if (typeof body.control === "string") {
    out.control = body.control;
  }
  if (body.choices != null) {
    out.choices = asChoiceCatalog(body.choices);
  }
  if (body.designGaps != null) {
    out.designGaps = normalizeContentTypeDesignGaps(body.designGaps);
  }
  return out;
}

/**
 * Build the wire JSON body for {@code PUT .../controlProperties} under
 * {@link CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT}. A flat body fails
 * server UNWRAP_ROOT_VALUE. {@code choices} is omitted so the catalog is
 * unchanged (choice filter / null-entry / default-selected are not written).
 */
export function wrapFieldControlPropertiesForWire(
  body: ContentTypeFieldControlPropertiesBody,
): Record<string, ContentTypeFieldControlPropertiesBody> {
  return { [CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT]: body };
}

/**
 * GET /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties
 * — CD-07 read. No design lock required. Empty properties means none.
 */
export async function getFieldControlProperties(
  idOrName: string,
  fieldName: string,
): Promise<ContentTypeFieldControlProperties> {
  const typeKey = encodeURIComponent(idOrName);
  const fieldKey = encodeURIComponent(fieldName);
  const payload = await get<unknown>(
    `${PATHS.CONTENT_TYPES}/${typeKey}/fields/${fieldKey}/controlProperties`,
  );
  return unwrapFieldControlProperties(payload);
}

/**
 * PUT /services/contenttypes/{idOrName}/fields/{fieldName}/controlProperties
 * — CD-07 full replace of property values.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. Empty {@code properties} clears
 * parameters. Does not send {@code choices}.
 */
export async function replaceFieldControlProperties(
  idOrName: string,
  fieldName: string,
  properties: ContentTypeControlProperty[],
): Promise<ContentTypeFieldControlProperties> {
  const typeKey = encodeURIComponent(idOrName);
  const fieldKey = encodeURIComponent(fieldName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${typeKey}/fields/${fieldKey}/controlProperties`,
    wrapFieldControlPropertiesForWire({ properties }),
  );
  return unwrapFieldControlProperties(payload);
}
