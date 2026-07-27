/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  deliveryContextId: string;
  assemblyContextId?: string;
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

export async function createEdition(
  body: EditionSummary,
): Promise<EditionSummary> {
  return (await post<unknown>(`${designRoot()}/editions`, body)) as EditionSummary;
}

export async function updateEdition(
  editionId: string | number,
  body: EditionSummary,
): Promise<EditionSummary> {
  return (await put<unknown>(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}`,
    body,
  )) as EditionSummary;
}

export async function deleteEdition(editionId: string | number): Promise<void> {
  await del(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}`,
  );
}

export async function copyEdition(
  request: CopyEditionRequest,
): Promise<EditionSummary> {
  return (await post<unknown>(
    `${designRoot()}/editions/copy`,
    request,
  )) as EditionSummary;
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

export async function associateContentList(
  editionId: string | number,
  body: EditionContentListAssoc,
): Promise<ContentListSummary> {
  return (await post<unknown>(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists`,
    body,
  )) as ContentListSummary;
}

export async function disassociateContentList(
  editionId: string | number,
  contentListId: string | number,
): Promise<void> {
  await del(
    `${designRoot()}/editions/${encodeURIComponent(String(editionId))}/contentlists/${encodeURIComponent(String(contentListId))}`,
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

export async function createContentList(
  body: ContentListSummary,
): Promise<ContentListSummary> {
  return (await post<unknown>(
    `${designRoot()}/contentlists`,
    body,
  )) as ContentListSummary;
}

export async function updateContentList(
  contentListId: string | number,
  body: ContentListSummary,
): Promise<ContentListSummary> {
  return (await put<unknown>(
    `${designRoot()}/contentlists/${encodeURIComponent(String(contentListId))}`,
    body,
  )) as ContentListSummary;
}

export async function deleteContentList(
  contentListId: string | number,
): Promise<void> {
  await del(
    `${designRoot()}/contentlists/${encodeURIComponent(String(contentListId))}`,
  );
}

// ---- Delivery types ----

export async function listDeliveryTypes(): Promise<DeliveryTypeSummary[]> {
  return normalizeArray(await get<unknown>(`${designRoot()}/deliverytypes`));
}

export async function createDeliveryType(
  body: DeliveryTypeSummary,
): Promise<DeliveryTypeSummary> {
  return (await post<unknown>(
    `${designRoot()}/deliverytypes`,
    body,
  )) as DeliveryTypeSummary;
}

export async function updateDeliveryType(
  id: string | number,
  body: DeliveryTypeSummary,
): Promise<DeliveryTypeSummary> {
  return (await put<unknown>(
    `${designRoot()}/deliverytypes/${encodeURIComponent(String(id))}`,
    body,
  )) as DeliveryTypeSummary;
}

export async function deleteDeliveryType(id: string | number): Promise<void> {
  await del(
    `${designRoot()}/deliverytypes/${encodeURIComponent(String(id))}`,
  );
}

// ---- Contexts ----

export async function listContexts(): Promise<ContextSummary[]> {
  return normalizeArray(await get<unknown>(`${designRoot()}/contexts`));
}

export async function createContext(
  body: ContextSummary,
): Promise<ContextSummary> {
  return (await post<unknown>(
    `${designRoot()}/contexts`,
    body,
  )) as ContextSummary;
}

export async function updateContext(
  contextId: string | number,
  body: ContextSummary,
): Promise<ContextSummary> {
  return (await put<unknown>(
    `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}`,
    body,
  )) as ContextSummary;
}

export async function deleteContext(contextId: string | number): Promise<void> {
  await del(
    `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}`,
  );
}

export async function listSchemesForContext(
  contextId: string | number,
): Promise<LocationSchemeSummary[]> {
  return normalizeArray(
    await get<unknown>(
      `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}/schemes`,
    ),
  );
}

export async function getScheme(
  schemeId: string | number,
): Promise<LocationSchemeSummary> {
  return (await get<unknown>(
    `${designRoot()}/schemes/${encodeURIComponent(String(schemeId))}`,
  )) as LocationSchemeSummary;
}

export async function createScheme(
  contextId: string | number,
  body: LocationSchemeSummary,
): Promise<LocationSchemeSummary> {
  return (await post<unknown>(
    `${designRoot()}/contexts/${encodeURIComponent(String(contextId))}/schemes`,
    body,
  )) as LocationSchemeSummary;
}

export async function updateScheme(
  schemeId: string | number,
  body: LocationSchemeSummary,
): Promise<LocationSchemeSummary> {
  return (await put<unknown>(
    `${designRoot()}/schemes/${encodeURIComponent(String(schemeId))}`,
    body,
  )) as LocationSchemeSummary;
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
