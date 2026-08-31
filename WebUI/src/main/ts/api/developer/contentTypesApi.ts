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

import {
  normalizeDesignObjectGuid,
  resolveContentTypeObjectGuid,
} from "../displayFormatGuid";
import { del, get, post, put } from "../client";
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
  ContentTypeIcon,
  ContentTypeItemExits,
  ContentTypeSearchIndexing,
  ContentTypeSummary,
  NamedObjectRef,
} from "./types";
import {
  toContentTypeItemExitsPutBody,
  unwrapContentTypeItemExits,
  wrapContentTypeItemExitsForWire,
} from "./contentTypeItemExits";
import { parseChoiceCatalog } from "./contentTypeChoiceCatalog";

export {
  CONTENT_TYPE_ITEM_EXITS_ROOT,
  addItemExit,
  cloneContentTypeItemExits,
  contentTypeItemExitsEqual,
  emptyContentTypeItemExits,
  itemExitDisplay,
  listContainsExtension,
  normalizeContentTypeItemExitsList,
  removeItemExit,
  toContentTypeItemExitsPutBody,
  unwrapContentTypeItemExits,
  wrapContentTypeItemExitsForWire,
} from "./contentTypeItemExits";

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

const MAX_CONTENT_TYPE_TEXT_DEPTH = 4;

/**
 * Coerce catalog/detail string fields so JAXB wraps cannot hide Open keys
 * or throw "Objects are not valid as a React child" (#3810 / #3706).
 */
export function asContentTypeText(value: unknown, depth = 0): string {
  if (value == null || depth > MAX_CONTENT_TYPE_TEXT_DEPTH) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  if (typeof value !== "object" || Array.isArray(value)) {
    return "";
  }
  const o = value as Record<string, unknown>;
  return asContentTypeText(
    o.value ?? o.stringValue ?? o.$ ?? o._text ?? o.content,
    depth + 1,
  );
}

function optionalContentTypeText(value: unknown): string | undefined {
  const text = asContentTypeText(value);
  return text ? text : undefined;
}

function coerceContentTypeStringFields<T extends ContentTypeSummary>(item: T): T {
  return {
    ...item,
    name: optionalContentTypeText(item.name),
    label: optionalContentTypeText(item.label),
    description: optionalContentTypeText(item.description),
  };
}

function normalizeContentTypeSummary(item: ContentTypeSummary): ContentTypeSummary {
  return normalizeDesignObjectGuid(coerceContentTypeStringFields(item));
}

/**
 * Open-key for Content Types catalog → GET /services/contenttypes/{idOrName}.
 * Prefers internal name, then object GUID. Null when the row cannot be opened
 * (empty/wrapped name and no guid — no Open button, #3810).
 */
export function contentTypeSelectionKey(ct: ContentTypeSummary): string | null {
  const name = asContentTypeText(ct.name);
  if (name && name !== "—") {
    return name;
  }
  const guid = resolveContentTypeObjectGuid(ct);
  return guid || null;
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
    ...coerceContentTypeStringFields(detail),
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
  const preferred = asRecord(root[CONTENT_TYPE_DETAIL_ROOT] ?? root.contentTypeDetail);
  let body: ContentTypeDetail;
  if (preferred) {
    body = preferred as ContentTypeDetail;
  } else {
    const alt = asRecord(root.ContentType ?? root.contentType);
    if (
      alt &&
      (alt.name != null ||
        alt.fields != null ||
        alt.guid != null ||
        alt.label != null ||
        alt.allowedTemplates != null)
    ) {
      body = alt as ContentTypeDetail;
    } else if (
      "name" in root ||
      "guid" in root ||
      "guidString" in root ||
      "fields" in root ||
      "label" in root ||
      "allowedTemplates" in root
    ) {
      body = root as ContentTypeDetail;
    } else {
      return {};
    }
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

/**
 * Writable fields for {@code POST /services/contenttypes} (CD-01 create).
 * Name is required; unique (case-insensitive); letters, digits, underscore, period;
 * no spaces or wildcards. Omitted {@code enabled} defaults to true on the server.
 */
export type ContentTypeCreateBody = {
  name: string;
  label?: string;
  description?: string;
  enabled?: boolean;
};

/** Internal names: letters, digits, underscore, and period (REST/PSStringUtils). */
export const CONTENT_TYPE_NAME_PATTERN = /^[A-Za-z0-9_.]+$/;

/** Trim; empty when missing. */
export function normalizeContentTypeName(name: string | undefined | null): string {
  if (name == null) {
    return "";
  }
  return name.trim();
}

/**
 * True when the name is a legal REST create/rename key: non-blank, no whitespace,
 * no wildcards, and only content-type name characters.
 */
export function isValidContentTypeName(name: string | undefined | null): boolean {
  const n = normalizeContentTypeName(name);
  if (!n) {
    return false;
  }
  if (/\s/.test(n) || n.includes("*") || n.includes("%")) {
    return false;
  }
  return CONTENT_TYPE_NAME_PATTERN.test(n);
}

/** Create Save is enabled when the internal name is valid. Label is optional. */
export function isContentTypeCreateReady(opts: { name: string }): boolean {
  return isValidContentTypeName(opts.name);
}

/**
 * Build the wire JSON body for ContentTypesResource POST under
 * {@link CONTENT_TYPE_DETAIL_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeCreateForWire(
  body: ContentTypeCreateBody,
): Record<string, ContentTypeCreateBody> {
  return { [CONTENT_TYPE_DETAIL_ROOT]: body };
}

/**
 * POST /services/contenttypes — Admin. Creates and persists a type (Workbench Finish).
 * Duplicate name is 409; blank/whitespace/wildcard name is 400; non-Admin is 403.
 */
export async function createContentType(
  body: ContentTypeCreateBody,
): Promise<ContentTypeDetail> {
  const payload = await post<unknown>(
    PATHS.CONTENT_TYPES,
    wrapContentTypeCreateForWire(body),
  );
  return unwrapContentTypeDetail(payload);
}

/**
 * DELETE /services/contenttypes/{idOrName} — Admin. Requires a design-session lock
 * already held by the current user ({@link lockContentType}). Does not steal locks.
 * HTTP 204 on success; 409 when unlocked or locked by another user; 403 non-Admin.
 */
export async function deleteContentType(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await del(`${PATHS.CONTENT_TYPES}/${key}`);
}

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
 * Do not send type-level search indexing here — use
 * {@link setContentTypeSearchIndexing} (CD-10).
 * Do not send {@code allowedWorkflows} here — use
 * {@link setContentTypeAllowedWorkflows} (CD-08).
 * Do not send {@code allowedTemplates} here — use
 * {@link replaceContentTypeAllowedTemplates} (CD-12).
 * Do not send icon strategy here — use {@link setContentTypeIcon} (CD-11).
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

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeSearchIndexing}. */
export const CONTENT_TYPE_SEARCH_INDEXING_ROOT = "ContentTypeSearchIndexing";

/**
 * Normalize GET/PUT {@code .../searchIndexing} to a flat flag.
 *
 * <p>Prefers {@code { "ContentTypeSearchIndexing": { searchIndexing } }}
 * (Jackson WRAP_ROOT_VALUE); also accepts a flat body. Missing flag defaults
 * to on (Workbench / CD-10).
 */
export function unwrapContentTypeSearchIndexing(
  payload: unknown,
): ContentTypeSearchIndexing {
  const root = asRecord(payload);
  if (!root) {
    return { searchIndexing: true };
  }
  const nested =
    asRecord(root[CONTENT_TYPE_SEARCH_INDEXING_ROOT]) ??
    asRecord(root.contentTypeSearchIndexing);
  const body = nested ?? root;
  return { searchIndexing: body.searchIndexing !== false };
}

/**
 * Build the wire JSON body for {@code PUT .../searchIndexing} under
 * {@link CONTENT_TYPE_SEARCH_INDEXING_ROOT}. A flat {@code { searchIndexing }}
 * body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeSearchIndexingForWire(
  searchIndexing: boolean,
): Record<string, { searchIndexing: boolean }> {
  return { [CONTENT_TYPE_SEARCH_INDEXING_ROOT]: { searchIndexing } };
}

/**
 * GET /services/contenttypes/{idOrName}/searchIndexing — CD-10 type-level flag.
 *
 * <p>No design lock required. Distinct from per-field {@code searchable}.
 * Default is on.
 */
export async function getContentTypeSearchIndexing(
  idOrName: string,
): Promise<ContentTypeSearchIndexing> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/searchIndexing`,
  );
  return unwrapContentTypeSearchIndexing(payload);
}

/**
 * PUT /services/contenttypes/{idOrName}/searchIndexing — CD-10 dedicated write.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. Missing {@code searchIndexing} is
 * HTTP 400 (this client always sends the boolean). Response is
 * {@code ContentTypeSearchIndexing} (lock still held).
 */
export async function setContentTypeSearchIndexing(
  idOrName: string,
  searchIndexing: boolean,
): Promise<ContentTypeSearchIndexing> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/searchIndexing`,
    wrapContentTypeSearchIndexingForWire(searchIndexing),
  );
  return unwrapContentTypeSearchIndexing(payload);
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

/**
 * GET /services/contenttypes/{idOrName}/itemExits — CD-09 read.
 * No design lock required. Empty lists mean none.
 */
export async function getContentTypeItemExits(
  idOrName: string,
): Promise<ContentTypeItemExits> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.CONTENT_TYPES}/${key}/itemExits`);
  return unwrapContentTypeItemExits(payload);
}

/**
 * PUT /services/contenttypes/{idOrName}/itemExits — CD-09 full replace.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. HTTP 400 when required lists are
 * missing or an extension FQN cannot be resolved. Empty lists clear.
 */
export async function replaceContentTypeItemExits(
  idOrName: string,
  body: ContentTypeItemExits,
  includePipeExits = false,
  includeMaxErrors = false,
): Promise<ContentTypeItemExits> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/itemExits`,
    wrapContentTypeItemExitsForWire(
      toContentTypeItemExitsPutBody(body, includePipeExits, includeMaxErrors),
    ),
  );
  return unwrapContentTypeItemExits(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeFieldControlProperties}. */
export const CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT =
  "ContentTypeFieldControlProperties";

export type ContentTypeFieldControlPropertiesBody = {
  properties: ContentTypeControlProperty[];
  /** When omitted, PUT leaves the catalog unchanged. */
  choices?: ContentTypeChoiceCatalog;
};

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
 * {@link CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT}. A flat body fails
 * server UNWRAP_ROOT_VALUE. Omit {@code choices} to leave the catalog
 * unchanged; send {@code type: none} to clear it.
 */
export function wrapFieldControlPropertiesForWire(
  body: ContentTypeFieldControlPropertiesBody,
): Record<string, ContentTypeFieldControlPropertiesBody> {
  const wrapped: ContentTypeFieldControlPropertiesBody = {
    properties: body.properties,
  };
  if (body.choices !== undefined) {
    wrapped.choices = body.choices;
  }
  return { [CONTENT_TYPE_FIELD_CONTROL_PROPERTIES_ROOT]: wrapped };
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
 * — CD-07 full replace of property values, optional choice catalog.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. Empty {@code properties} clears
 * parameters. Omit {@code choices} to leave the catalog unchanged; pass
 * {@code { type: "none" }} to clear it.
 */
export async function replaceFieldControlProperties(
  idOrName: string,
  fieldName: string,
  properties: ContentTypeControlProperty[],
  choices?: ContentTypeChoiceCatalog,
): Promise<ContentTypeFieldControlProperties> {
  const typeKey = encodeURIComponent(idOrName);
  const fieldKey = encodeURIComponent(fieldName);
  const body: ContentTypeFieldControlPropertiesBody = { properties };
  if (choices !== undefined) {
    body.choices = choices;
  }
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${typeKey}/fields/${fieldKey}/controlProperties`,
    wrapFieldControlPropertiesForWire(body),
  );
  return unwrapFieldControlProperties(payload);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeField}. */
export const CONTENT_TYPE_FIELD_ROOT = "ContentTypeField";

export type ContentTypeIncludeFieldBody = {
  name: string;
  fieldType: "system" | "shared";
};

/**
 * Wire body for {@code POST .../fields} (CD-03). Origin is always local;
 * include of system/shared fields is {@code POST .../fields/include} (CD-04).
 */
export type ContentTypeLocalFieldCreateBody = {
  name: string;
  label?: string;
  dataType?: string;
  control?: string;
  searchable?: boolean;
  required?: boolean;
  occurrence?: string;
  fieldSet?: string | null;
};

/**
 * Build the wire JSON body for {@code POST .../fields} or {@code .../include}
 * under {@link CONTENT_TYPE_FIELD_ROOT}. A flat body fails server
 * UNWRAP_ROOT_VALUE. Include bodies keep {@code system}/{@code shared};
 * local create always sets {@code fieldType} {@code local}.
 */
export function wrapContentTypeFieldForWire(
  body: ContentTypeIncludeFieldBody,
): Record<string, ContentTypeIncludeFieldBody>;
export function wrapContentTypeFieldForWire(
  body: ContentTypeLocalFieldCreateBody,
): Record<string, ContentTypeLocalFieldCreateBody & { fieldType: "local" }>;
export function wrapContentTypeFieldForWire(
  body: ContentTypeIncludeFieldBody | ContentTypeLocalFieldCreateBody,
): Record<string, unknown> {
  if ("fieldType" in body && (body.fieldType === "system" || body.fieldType === "shared")) {
    return { [CONTENT_TYPE_FIELD_ROOT]: body };
  }
  return {
    [CONTENT_TYPE_FIELD_ROOT]: {
      ...body,
      fieldType: "local" as const,
    },
  };
}

/**
 * POST /services/contenttypes/{idOrName}/fields/include — CD-04 include an
 * existing system or shared field (origin is not copied as local).
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked, locked by another user, or the field is already included.
 * HTTP 404 when the catalog field is unknown. HTTP 400 when {@code fieldType}
 * is not system or shared (including {@code local}).
 */
export async function includeContentTypeField(
  idOrName: string,
  body: ContentTypeIncludeFieldBody,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/fields/include`,
    wrapContentTypeFieldForWire(body),
  );
  return unwrapContentTypeDetail(payload);
}

/**
 * POST /services/contenttypes/{idOrName}/fields — CD-03 add a persistable local
 * field (backend column + display mapping).
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked, locked by another user, or the field name already exists.
 * HTTP 400 for invalid name / dataType. Response is {@code ContentTypeDetail}
 * with the new catalog (lock still held).
 */
export async function addLocalContentTypeField(
  idOrName: string,
  body: ContentTypeLocalFieldCreateBody,
): Promise<ContentTypeDetail> {
  const key = encodeURIComponent(idOrName);
  const payload = await post<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/fields`,
    wrapContentTypeFieldForWire(body),
  );
  return unwrapContentTypeDetail(payload);
}

/**
 * DELETE /services/contenttypes/{idOrName}/fields/{fieldName} — CD-03 remove a
 * local field and its display mapping.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. HTTP 400 when the field is
 * system/shared. HTTP 404 when the type or field is missing. HTTP 204 on
 * success (lock still held).
 */
export async function deleteLocalContentTypeField(
  idOrName: string,
  fieldName: string,
): Promise<void> {
  const typeKey = encodeURIComponent(idOrName);
  const fieldKey = encodeURIComponent(fieldName);
  await del(`${PATHS.CONTENT_TYPES}/${typeKey}/fields/${fieldKey}`);
}

/** Jackson {@code WRAP_ROOT_VALUE} root for {@code ContentTypeIcon}. */
export const CONTENT_TYPE_ICON_ROOT = "ContentTypeIcon";

/** REST icon source: no icon; value is cleared. */
export const CONTENT_TYPE_ICON_NONE = "none";

/** REST icon source: specified file path or name. */
export const CONTENT_TYPE_ICON_SPECIFIED = "specified";

/** REST icon source: derive from a file field name. */
export const CONTENT_TYPE_ICON_FROM_FILE_FIELD = "fromFileField";

/** Allowed CD-11 {@code source} values. */
export const CONTENT_TYPE_ICON_SOURCES = [
  CONTENT_TYPE_ICON_NONE,
  CONTENT_TYPE_ICON_SPECIFIED,
  CONTENT_TYPE_ICON_FROM_FILE_FIELD,
] as const;

export type ContentTypeIconSource = (typeof CONTENT_TYPE_ICON_SOURCES)[number];

/**
 * Normalize a REST icon source (case-insensitive). Unknown or blank is
 * {@link CONTENT_TYPE_ICON_NONE}.
 */
export function normalizeContentTypeIconSource(source: unknown): ContentTypeIconSource {
  const trimmed = typeof source === "string" ? source.trim() : "";
  if (trimmed.toLowerCase() === CONTENT_TYPE_ICON_SPECIFIED.toLowerCase()) {
    return CONTENT_TYPE_ICON_SPECIFIED;
  }
  if (trimmed.toLowerCase() === CONTENT_TYPE_ICON_FROM_FILE_FIELD.toLowerCase()) {
    return CONTENT_TYPE_ICON_FROM_FILE_FIELD;
  }
  return CONTENT_TYPE_ICON_NONE;
}

/**
 * True when {@code source} is {@code none} (case-insensitive).
 */
export function isContentTypeIconNone(source: unknown): boolean {
  return normalizeContentTypeIconSource(source) === CONTENT_TYPE_ICON_NONE;
}

/**
 * True when {@code source} is one of the REST icon sources (case-insensitive).
 */
export function isKnownContentTypeIconSource(source: unknown): boolean {
  if (typeof source !== "string") {
    return false;
  }
  const trimmed = source.trim();
  if (!trimmed) {
    return false;
  }
  const lower = trimmed.toLowerCase();
  return CONTENT_TYPE_ICON_SOURCES.some((s) => s.toLowerCase() === lower);
}

function contentTypeIconClientError(message: string): never {
  throw { status: 400, statusText: "Bad Request", body: message };
}

/**
 * Build the inner PUT body for {@code PUT .../icon}. {@code none} omits value
 * (REST clears it). Non-{@code none} requires a non-blank value (400).
 */
export function contentTypeIconPutBody(
  source: string,
  value?: string | null,
): { source: ContentTypeIconSource; value?: string } {
  if (!isKnownContentTypeIconSource(source)) {
    contentTypeIconClientError("source must be none, specified, or fromFileField");
  }
  const normalized = normalizeContentTypeIconSource(source);
  if (normalized === CONTENT_TYPE_ICON_NONE) {
    return { source: CONTENT_TYPE_ICON_NONE };
  }
  const trimmed = typeof value === "string" ? value.trim() : "";
  if (!trimmed) {
    contentTypeIconClientError("value is required when source is not none");
  }
  return { source: normalized, value: trimmed };
}

/**
 * Build the wire JSON body for {@code PUT .../icon} under
 * {@link CONTENT_TYPE_ICON_ROOT}. A flat body fails server UNWRAP_ROOT_VALUE.
 */
export function wrapContentTypeIconForWire(
  source: string,
  value?: string | null,
): Record<string, { source: ContentTypeIconSource; value?: string }> {
  return { [CONTENT_TYPE_ICON_ROOT]: contentTypeIconPutBody(source, value) };
}

/**
 * Flatten GET/PUT {@code .../icon} JSON to {@link ContentTypeIcon}.
 *
 * <p>Handles WRAP_ROOT {@code ContentTypeIcon} and a flat body. {@code none}
 * clears value.
 */
export function unwrapContentTypeIcon(payload: unknown): ContentTypeIcon {
  const root = asRecord(payload);
  if (!root) {
    return { source: CONTENT_TYPE_ICON_NONE };
  }
  const nested = asRecord(root[CONTENT_TYPE_ICON_ROOT] ?? root.contentTypeIcon);
  const body = nested ?? root;
  const source = normalizeContentTypeIconSource(body.source);
  if (source === CONTENT_TYPE_ICON_NONE) {
    return { source: CONTENT_TYPE_ICON_NONE };
  }
  const raw = body.value;
  const value = typeof raw === "string" ? raw.trim() : raw == null ? "" : String(raw).trim();
  return value ? { source, value } : { source };
}

/**
 * GET /services/contenttypes/{idOrName}/icon — CD-11 read. No design lock
 * required. {@code none} has no value. Does not return icon binaries.
 */
export async function getContentTypeIcon(idOrName: string): Promise<ContentTypeIcon> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.CONTENT_TYPES}/${key}/icon`);
  return unwrapContentTypeIcon(payload);
}

/**
 * PUT /services/contenttypes/{idOrName}/icon — CD-11 dedicated icon strategy.
 *
 * <p>Requires a design-session lock already held by the current user
 * ({@link lockContentType}). Does not acquire or release the lock. HTTP 409
 * when unlocked or locked by another user. {@code none} clears value.
 * Non-{@code none} with a blank value is HTTP 400 (client-side before fetch,
 * matching REST). Does not upload icon binaries.
 */
export async function setContentTypeIcon(
  idOrName: string,
  source: string,
  value?: string | null,
): Promise<ContentTypeIcon> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.CONTENT_TYPES}/${key}/icon`,
    wrapContentTypeIconForWire(source, value),
  );
  return unwrapContentTypeIcon(payload);
}
