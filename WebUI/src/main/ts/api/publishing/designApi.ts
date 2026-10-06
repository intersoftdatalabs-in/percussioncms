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
import { SERVICES_ROOT } from "../paths";

export interface EditionSummary {
  editionId?: string;
  name?: string;
  siteId?: string;
  comment?: string;
  priority?: number;
}

export interface ContentListSummary {
  contentListId?: string;
  name?: string;
  description?: string;
  listType?: string;
  generator?: string;
  url?: string;
  /**
   * Item filter uuid (or name on write). Empty string clears the filter.
   * Omitted on write leaves the stored filter unchanged.
   */
  itemFilterId?: string;
  /** Display name from the last successful load. Not sent as the source of truth. */
  itemFilterName?: string;
}

export interface DeliveryTypeSummary {
  deliveryTypeId?: string;
  name?: string;
  beanName?: string;
  description?: string;
  unpublishingRequiresAssembly?: boolean;
}

export interface ContextSummary {
  contextId?: string;
  name?: string;
  description?: string;
  defaultSchemeId?: string;
}

export interface SchemeParameter {
  name?: string;
  type?: string;
  value?: string;
  sequence?: number;
}

export interface LocationSchemeSummary {
  schemeId?: string;
  name?: string;
  description?: string;
  contextId?: string;
  generator?: string;
  contentTypeId?: number;
  templateId?: number;
  /** Create-only. Keeps a copy off an occupied context/template/content-type triple. */
  copy?: boolean;
  schemeType?: string;
  parameters?: SchemeParameter[];
}

export interface SiteDesignSummary {
  siteId?: string;
  name?: string;
  description?: string;
  folderRoot?: string;
  baseUrl?: string;
}

export interface SitePropertyDto {
  name?: string;
  contextId?: string;
  value?: string;
}

export interface EditionContentListAssoc {
  contentListId: string;
  /** Required when associating. Omitted when only the sequence is written. */
  deliveryContextId?: string;
  assemblyContextId?: string;
  /**
   * On associate, stored 1-based sequence. On reorder, the 0-based target
   * position; it must be adjacent to the current position.
   */
  sequence?: number;
}

export interface CopyEditionRequest {
  sourceEditionId: string;
  targetSiteId: string;
  newName?: string;
  copyContentLists?: boolean;
}

function designRoot(): string {
  return `${SERVICES_ROOT}/sitemanage/publishingdesign`;
}

// ---- Editions ----

export async function listEditionsBySite(
  siteId: string | number,
): Promise<EditionSummary[]> {
  return normalizeArray(
    await get<unknown>(
      `${designRoot()}/editions?siteId=${encodeURIComponent(String(siteId))}`,
    ),
  );
}

export async function getEdition(
  editionId: string | number,
): Promise<EditionSummary> {
  return (await get<unknown>(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}`,
  )) as EditionSummary;
}

function unwrapEdition(data: unknown): EditionSummary {
  if (data && typeof data === "object" && "edition" in (data as object)) {
    return (data as { edition: EditionSummary }).edition;
  }
  return data as EditionSummary;
}

/** JAXB/Jackson root wrap expected by sitemanage {@code PSEditionSummary}. */
function wrapEdition(body: EditionSummary): { edition: EditionSummary } {
  return { edition: body };
}

/** POST create. HTTP 403 non-Admin/Designer; 409 duplicate name. */
export async function createEdition(
  body: EditionSummary,
): Promise<EditionSummary> {
  return unwrapEdition(
    await post<unknown>(`${designRoot()}/editions`, wrapEdition(body)),
  );
}

/** PUT update. HTTP 403 non-Admin/Designer; 409 duplicate name. */
export async function updateEdition(
  editionId: string | number,
  body: EditionSummary,
): Promise<EditionSummary> {
  return unwrapEdition(
    await put<unknown>(
      `${designRoot()}/editions/${encodeURIComponent(String(editionId))}`,
      wrapEdition(body),
    ),
  );
}

export async function deleteEdition(editionId: string | number): Promise<void> {
  await del(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}`,
  );
}

/** JAXB root wrap expected by sitemanage {@code PSCopyEditionRequest}. */
function wrapCopyEdition(body: CopyEditionRequest): {
  copyEditionRequest: CopyEditionRequest;
} {
  return { copyEditionRequest: body };
}

export async function copyEdition(
  request: CopyEditionRequest,
): Promise<EditionSummary> {
  return unwrapEdition(
    await post<unknown>(
      `${designRoot()}/editions/copy`,
      wrapCopyEdition(request),
    ),
  );
}

export async function listEditionContentLists(
  editionId: string | number,
): Promise<ContentListSummary[]> {
  return normalizeArray(
    await get<unknown>(
      `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists`,
    ),
  );
}

/** JAXB root wrap expected by sitemanage {@code PSEditionContentListAssoc}. */
function wrapEditionContentList(body: EditionContentListAssoc): {
  editionContentList: EditionContentListAssoc;
} {
  return { editionContentList: body };
}

/**
 * POST associate. HTTP 400 missing ids; 403 non-Admin/Designer; 409 already associated.
 * Body must be the {@code editionContentList} root — a flat object does not bind.
 */
export async function associateContentList(
  editionId: string | number,
  body: EditionContentListAssoc,
): Promise<ContentListSummary> {
  return unwrapContentList(
    await post<unknown>(
      `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists`,
      wrapEditionContentList(body),
    ),
  );
}

export async function disassociateContentList(
  editionId: string | number,
  contentListId: string | number,
): Promise<void> {
  await del(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists/${encodeURIComponent(String(contentListId))}`,
  );
}

/**
 * PUT reorder. {@code sequence} is the 0-based target position and must be
 * adjacent. HTTP 400 when it is not; 403 non-Admin/Designer; 409 edition in use.
 * Stored association sequences are rewritten 1..n only after success.
 */
export async function reorderEditionContentList(
  editionId: string | number,
  contentListId: string | number,
  sequence: number,
): Promise<void> {
  await put<unknown>(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists/${encodeURIComponent(String(contentListId))}/sequence`,
    wrapEditionContentList({
      contentListId: String(contentListId),
      sequence,
    }),
  );
}

// ---- Content lists ----

export async function listContentLists(): Promise<ContentListSummary[]> {
  return normalizeArray(await get<unknown>(`${designRoot()}/contentlists`));
}

export async function getContentList(
  contentListId: string | number,
): Promise<ContentListSummary> {
  return (await get<unknown>(
    `${designRoot()}/contentlists/${encodeURIComponent(String(contentListId))}`,
  )) as ContentListSummary;
}

function contentListIdText(value: unknown): string | undefined {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed || undefined;
  }
  // Jackson sometimes emits a numeric guid for this string field (#5107).
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  return undefined;
}

function unwrapContentList(data: unknown): ContentListSummary {
  const record =
    data && typeof data === "object"
      ? (data as Record<string, unknown>)
      : undefined;
  const nested = record?.contentList;
  const source =
    nested && typeof nested === "object" && !Array.isArray(nested)
      ? (nested as Record<string, unknown>)
      : record;
  if (!source) {
    return {};
  }
  const name = source.name;
  const description = source.description;
  const listType = source.listType;
  const generator = source.generator;
  const url = source.url;
  const itemFilterName = source.itemFilterName;
  return {
    contentListId: contentListIdText(source.contentListId),
    name: typeof name === "string" ? name : undefined,
    description: typeof description === "string" ? description : undefined,
    listType: typeof listType === "string" ? listType : undefined,
    generator: typeof generator === "string" ? generator : undefined,
    url: typeof url === "string" ? url : undefined,
    itemFilterId: contentListIdText(source.itemFilterId),
    itemFilterName:
      typeof itemFilterName === "string" && itemFilterName.trim()
        ? itemFilterName
        : undefined,
  };
}

/** JAXB/Jackson root wrap expected by sitemanage {@code PSContentListSummary}. */
function wrapContentList(body: ContentListSummary): {
  contentList: ContentListSummary;
} {
  return { contentList: body };
}

/** POST create. HTTP 403 non-Admin/Designer; 409 duplicate name. */
export async function createContentList(
  body: ContentListSummary,
): Promise<ContentListSummary> {
  return unwrapContentList(
    await post<unknown>(
      `${designRoot()}/contentlists`,
      wrapContentList(body),
    ),
  );
}

/**
 * PUT update. A description change sends {@code description} only (empty
 * string clears it) so name, type, generator, legacy URL, and item filter stay
 * stored. A generator change sends {@code generator} only so name, description,
 * type, legacy URL, and item filter stay stored. HTTP 400 when the description
 * is longer than 255 characters, or when the generator is blank, longer than
 * 256 characters, or sent for a legacy list; 403 non-Admin/Designer; 409
 * duplicate name when a name is also sent.
 */
export async function updateContentList(
  contentListId: string | number,
  body: ContentListSummary,
): Promise<ContentListSummary> {
  return unwrapContentList(
    await put<unknown>(
      `${designRoot()}/contentlists/${encodeURIComponent(String(contentListId))}`,
      wrapContentList(body),
    ),
  );
}

export async function deleteContentList(
  contentListId: string | number,
): Promise<void> {
  await del(
    `${designRoot()}/contentlists/${encodeURIComponent(String(contentListId))}`,
  );
}

export interface CopyContentListRequest {
  sourceContentListId: string;
  newName: string;
}

/** JAXB root wrap expected by sitemanage {@code PSCopyContentListRequest}. */
function wrapCopyContentList(body: CopyContentListRequest): {
  copyContentListRequest: CopyContentListRequest;
} {
  return { copyContentListRequest: body };
}

/**
 * POST copy. Allocates a new content list id and copies the source definition.
 * HTTP 400 blank or overlong name; 403 non-Admin/Designer; 404 missing source;
 * 409 duplicate name. Does not modify the source list.
 */
export async function copyContentList(
  request: CopyContentListRequest,
): Promise<ContentListSummary> {
  return unwrapContentList(
    await post<unknown>(
      `${designRoot()}/contentlists/copy`,
      wrapCopyContentList(request),
    ),
  );
}

// ---- Delivery types ----

export async function listDeliveryTypes(): Promise<DeliveryTypeSummary[]> {
  return normalizeArray(await get<unknown>(`${designRoot()}/deliverytypes`));
}

function unwrapDeliveryType(data: unknown): DeliveryTypeSummary {
  if (data && typeof data === "object" && "deliveryType" in (data as object)) {
    return (data as { deliveryType: DeliveryTypeSummary }).deliveryType;
  }
  return data as DeliveryTypeSummary;
}

/** JAXB/Jackson root wrap expected by sitemanage {@code PSDeliveryTypeSummary}. */
function wrapDeliveryType(body: DeliveryTypeSummary): {
  deliveryType: DeliveryTypeSummary;
} {
  return { deliveryType: body };
}

/**
 * POST create. Copy reuses this with a new name and the source bean,
 * description, and unpublishing flag (no deliveryTypeId).
 * HTTP 400 blank or overlong name, or missing bean; 403 non-Admin/Designer;
 * 409 duplicate name.
 */
export async function createDeliveryType(
  body: DeliveryTypeSummary,
): Promise<DeliveryTypeSummary> {
  return unwrapDeliveryType(
    await post<unknown>(
      `${designRoot()}/deliverytypes`,
      wrapDeliveryType(body),
    ),
  );
}

/**
 * PUT update. A rename sends {@code name} only so bean name, description, and
 * the unpublishing-requires-assembly flag stay stored. A description change
 * sends {@code description} only (empty string clears it) so name and bean
 * stay stored. A bean-name change sends {@code beanName} only so name and
 * description stay stored. A blank bean name is not a clear.
 * HTTP 400 invalid name, blank or overlong bean, or overlong description;
 * 403 non-Admin/Designer; 409 duplicate name.
 */
export async function updateDeliveryType(
  id: string | number,
  body: DeliveryTypeSummary,
): Promise<DeliveryTypeSummary> {
  return unwrapDeliveryType(
    await put<unknown>(
      `${designRoot()}/deliverytypes/${encodeURIComponent(String(id))}`,
      wrapDeliveryType(body),
    ),
  );
}

export async function deleteDeliveryType(id: string | number): Promise<void> {
  await del(
    `${designRoot()}/deliverytypes/${encodeURIComponent(String(id))}`,
  );
}

// ---- Contexts ----

export async function listContexts(): Promise<ContextSummary[]> {
  // List JSON uses numeric context ids. The select stores string values.
  return normalizeArray(await get<unknown>(`${designRoot()}/contexts`)).map((row) =>
    unwrapContext(row),
  );
}

/**
 * JAXB/Jackson root wrap expected by sitemanage {@code PSContextSummary}.
 * A flat name root does not bind under UNWRAP_ROOT_VALUE. Omits id and
 * default scheme unless the caller set them. Copy does not set them, so
 * create allocates a new context and leaves location schemes on the source.
 */
export function wrapContext(body: ContextSummary): { context: ContextSummary } {
  const wire: ContextSummary = {};
  if (body.name != null) {
    wire.name = body.name;
  }
  if (body.description != null) {
    wire.description = body.description;
  }
  if (body.contextId != null && body.contextId !== "") {
    wire.contextId = String(body.contextId);
  }
  if (body.defaultSchemeId != null && body.defaultSchemeId !== "") {
    wire.defaultSchemeId = String(body.defaultSchemeId);
  }
  return { context: wire };
}

/** Accept a wrapped {@code context} document or an already-flat summary. */
export function unwrapContext(data: unknown): ContextSummary {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return {};
  }
  const record = data as Record<string, unknown>;
  const nested = record.context;
  const source =
    nested && typeof nested === "object" && !Array.isArray(nested)
      ? (nested as Record<string, unknown>)
      : record;
  const summary: ContextSummary = {};
  if (source.contextId != null && source.contextId !== "") {
    summary.contextId = String(source.contextId);
  }
  if (typeof source.name === "string") {
    summary.name = source.name;
  }
  if (typeof source.description === "string") {
    summary.description = source.description;
  }
  if (source.defaultSchemeId != null && source.defaultSchemeId !== "") {
    summary.defaultSchemeId = String(source.defaultSchemeId);
  }
  return summary;
}

/**
 * POST create. Copy reuses this with a new name and the source description.
 * The source id and default scheme are not sent, so location schemes stay
 * on the source context. HTTP 400 blank or overlong name; 403 non-Admin/Designer;
 * 409 duplicate name.
 */
export async function createContext(
  body: ContextSummary,
): Promise<ContextSummary> {
  return unwrapContext(
    await post<unknown>(`${designRoot()}/contexts`, wrapContext(body)),
  );
}

/**
 * PUT update. A rename sends {@code name} only so the description and default
 * scheme stay stored. A description change sends {@code description} only so
 * the name and default scheme stay stored. Location schemes stay on this
 * context id; this request does not create or move them. An empty description
 * clears the stored text. HTTP 400 overlong name or description; 403
 * non-Admin/Designer; 409 duplicate name.
 */
export async function updateContext(
  contextId: string | number,
  body: ContextSummary,
): Promise<ContextSummary> {
  return unwrapContext(
    await put<unknown>(
      `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}`,
      wrapContext(body),
    ),
  );
}

export async function deleteContext(contextId: string | number): Promise<void> {
  await del(
    `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}`,
  );
}

export async function listSchemesForContext(
  contextId: string | number,
): Promise<LocationSchemeSummary[]> {
  return unwrapLocationSchemeList(
    await get<unknown>(
      `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}/schemes`,
    ),
  );
}

/** List payload: a raw array, one scheme, or a root-wrapped collection. */
export function unwrapLocationSchemeList(data: unknown): LocationSchemeSummary[] {
  if (Array.isArray(data)) {
    return data.map((row) => unwrapLocationScheme(row));
  }
  if (!data || typeof data !== "object") {
    return [];
  }
  const record = data as Record<string, unknown>;
  const nested = record.locationScheme;
  if (Array.isArray(nested)) {
    return nested.map((row) => unwrapLocationScheme(row));
  }
  if (nested && typeof nested === "object") {
    return [unwrapLocationScheme(nested)];
  }
  for (const value of Object.values(record)) {
    if (Array.isArray(value)) {
      return value.map((row) => unwrapLocationScheme(row));
    }
  }
  if (record.name || record.schemeId || record.generator) {
    return [unwrapLocationScheme(record)];
  }
  return [];
}

export async function getScheme(
  schemeId: string | number,
): Promise<LocationSchemeSummary> {
  return unwrapLocationScheme(
    await get<unknown>(
      `${designRoot()}/schemes/${encodeURIComponent(String(schemeId))}`,
    ),
  );
}

export async function createScheme(
  contextId: string | number,
  body: LocationSchemeSummary,
): Promise<LocationSchemeSummary> {
  return unwrapLocationScheme(
    await post<unknown>(
      `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}/schemes`,
      wrapLocationScheme(body),
    ),
  );
}

export async function updateScheme(
  schemeId: string | number,
  body: LocationSchemeSummary,
): Promise<LocationSchemeSummary> {
  return unwrapLocationScheme(
    await put<unknown>(
      `${designRoot()}/schemes/${encodeURIComponent(String(schemeId))}`,
      wrapLocationScheme(body),
    ),
  );
}

/**
 * JAXB/Jackson root wrap expected by {@code PSLocationSchemeSummary}.
 * Parameters are the {@code schemeParameter} array, not a {@code parameters} field.
 */
export function wrapLocationScheme(body: LocationSchemeSummary): {
  locationScheme: Record<string, unknown>;
} {
  const wire: Record<string, unknown> = {
    name: body.name,
    description: body.description,
    contextId: body.contextId,
    generator: body.generator,
    contentTypeId: body.contentTypeId,
    templateId: body.templateId,
  };
  if (body.schemeId) {
    wire.schemeId = body.schemeId;
  }
  if (body.copy) {
    wire.copy = true;
  }
  if (body.parameters && body.parameters.length > 0) {
    wire.schemeParameter = body.parameters;
  }
  return { locationScheme: wire };
}

/** Accept a wrapped {@code locationScheme} document or an already-flat summary. */
export function unwrapLocationScheme(data: unknown): LocationSchemeSummary {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return {};
  }
  const record = data as Record<string, unknown>;
  const nested = record.locationScheme;
  const source =
    nested && typeof nested === "object" && !Array.isArray(nested)
      ? (nested as Record<string, unknown>)
      : record;
  const parameters = normalizeSchemeParameters(
    source.parameters ?? source.schemeParameter,
  );
  return {
    schemeId: idText(source.schemeId),
    name: textField(source.name),
    description: textField(source.description),
    contextId: idText(source.contextId),
    generator: textField(source.generator),
    contentTypeId: longField(source.contentTypeId),
    templateId: longField(source.templateId),
    schemeType: textField(source.schemeType),
    parameters,
  };
}

function textField(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function idText(value: unknown): string | undefined {
  if (typeof value === "string" && value.trim()) {
    return value.trim();
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  return undefined;
}

function longField(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim() && !Number.isNaN(Number(value))) {
    return Number(value);
  }
  return undefined;
}

function normalizeSchemeParameters(raw: unknown): SchemeParameter[] | undefined {
  const rows = parameterRows(raw);
  if (!rows) {
    return undefined;
  }
  return rows.map((row) => ({
    name: textField(row.name),
    type: textField(row.type),
    value: textField(row.value),
    sequence: longField(row.sequence),
  }));
}

function parameterRows(raw: unknown): Array<Record<string, unknown>> | undefined {
  if (Array.isArray(raw)) {
    return raw.filter((row) => row && typeof row === "object") as Array<
      Record<string, unknown>
    >;
  }
  if (!raw || typeof raw !== "object") {
    return undefined;
  }
  const inner = (raw as Record<string, unknown>).schemeParameter;
  if (Array.isArray(inner)) {
    return inner.filter((row) => row && typeof row === "object") as Array<
      Record<string, unknown>
    >;
  }
  if (inner && typeof inner === "object") {
    return [inner as Record<string, unknown>];
  }
  return undefined;
}

export async function deleteScheme(schemeId: string | number): Promise<void> {
  await del(
    `${designRoot()}/schemes/${encodeURIComponent(String(schemeId))}`,
  );
}

// ---- Design sites + properties ----

export async function listDesignSites(): Promise<SiteDesignSummary[]> {
  return normalizeArray(await get<unknown>(`${designRoot()}/sites`));
}

export async function listSiteProperties(
  siteId: string | number,
  contextId: string | number,
): Promise<SitePropertyDto[]> {
  return normalizeArray(
    await get<unknown>(
      `${designRoot()}/sites/${encodeURIComponent(String(siteId))}/properties?contextId=${encodeURIComponent(String(contextId))}`,
    ),
  );
}

export async function putSiteProperty(
  siteId: string | number,
  body: SitePropertyDto,
): Promise<SitePropertyDto> {
  return (await put<unknown>(
    `${designRoot()}/sites/${encodeURIComponent(String(siteId))}/properties`,
    body,
  )) as SitePropertyDto;
}

export async function deleteSiteProperty(
  siteId: string | number,
  name: string,
  contextId: string | number,
): Promise<void> {
  await del(
    `${designRoot()}/sites/${encodeURIComponent(String(siteId))}/properties?name=${encodeURIComponent(name)}&contextId=${encodeURIComponent(String(contextId))}`,
  );
}

function normalizeArray<T>(data: unknown): T[] {
  if (Array.isArray(data)) {
    return data as T[];
  }
  if (data && typeof data === "object") {
    for (const key of Object.keys(data as object)) {
      const v = (data as Record<string, unknown>)[key];
      if (Array.isArray(v)) {
        return v as T[];
      }
    }
  }
  return [];
}
