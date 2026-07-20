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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Client-side TypeScript types mirroring sitemanage server DTOs.
 *
 * <p>Align to live {@code PSPathItem}, {@code PSPagedItemList},
 * {@code PSFolderProperties}, {@code PSRenameFolderItem},
 * {@code PSMoveFolderItem}, {@code PSFolderPermission} per constitution II
 * (Evidence Over Invention). Do not invent fields; if a field is missing on
 * the server, add it via a sitemanage change with a service-contract test
 * (T052a) and threat-model note (T052b).</p>
 *
 * <p>Mirror of {@code specs/992-react-content-explorer/data-model.md}.</p>
 */

export type FolderAccessLevel = "ADMIN" | "WRITE" | "READ" | "VIEW";

export interface PSPathItem {
  id?: string;
  name: string;
  title?: string;
  path: string;
  /** Item type / category (e.g. "folder", "page", "asset"). */
  type?: string;
  /** CMS-side category discriminator; broader than {@link type}. */
  category?: string;
  leaf?: boolean;
  /** Hint flags for tree expansion; may be false on paginated children. */
  hasFolderChildren?: boolean;
  hasItemChildren?: boolean;
  hasSectionChildren?: boolean;
  accessLevel?: FolderAccessLevel;
  displayProperties?: Record<string, unknown>;
  folderPath?: string;
  folderPaths?: string[];
}

/**
 * Wire shape of the sitemanage `PSPagedItemList` response.
 *
 * <p>Mirrors {@code projects/sitemanage/src/main/java/com/percussion/share/data/PSPagedItemList.java}:
 * the server-side DTO has {@code @JsonRootName("PagedItemList")} and
 * fields {@code childrenCount} (Integer), {@code startIndex} (Integer),
 * {@code firstItemId} (String), {@code childrenInPage} (List<PSPathItem>).
 * The root wrapper key is {@code PagedItemList}.</p>
 *
 * <p>Use {@link paginatedFolder} in {@code pathApi.ts} to get a normalized
 * client-facing shape ({@code children}, {@code totalCount}, {@code startIndex})
 * — the pathApi unwraps this wire shape.</p>
 */
export interface PSPagedItemList {
  childrenCount?: number;
  startIndex?: number;
  firstItemId?: string;
  childrenInPage?: PSPathItem[];
}

/**
 * Client-facing paginated result returned by {@link paginatedFolder}.
 *
 * <p>This is a normalized shape (independent of the server wire format).
 * Per-page array is under {@code children}; total under {@code totalCount}.</p>
 */
export interface PSPagedResult {
  children: PSPathItem[];
  totalCount?: number;
  startIndex: number;
}

export interface PSFolderPermission {
  accessLevel: FolderAccessLevel;
  adminPrincipals?: PSPrincipal[];
  writePrincipals?: PSPrincipal[];
  readPrincipals?: PSPrincipal[];
  viewPrincipals?: PSPrincipal[];
}

export interface PSPrincipal {
  /** "USER" or "ROLE". */
  type: "USER" | "ROLE";
  name: string;
}

export interface PSFolderProperties {
  id: string;
  name: string;
  permission?: PSFolderPermission;
  acl?: unknown;
  communityId?: string;
  communityName?: string;
  locale?: string;
  displayFormatName?: string;
  workflowId?: string;
  allowedSites?: string[];
}

export interface PSRenameFolderItem {
  path: string;
  newName: string;
}

export interface PSMoveFolderItem {
  sourcePath: string;
  targetPath: string;
  /** When true, server performs copy rather than move. */
  copy?: boolean;
}

/**
 * ReducedAction enum (FR-010a, data-model.md). The intermediate hard-cut set
 * for the Finder / Desktop CE retirement; expanded (not redefined) by the
 * full P-Menu phase in US3.
 */
export enum ReducedAction {
  Open = "open",
  Preview = "preview",
  CreateFolder = "createFolder",
  Rename = "rename",
  Move = "move",
  Copy = "copy",
  Delete = "delete",
}

/** Selection payload returned from ContentBrowser to its host (US2). */
export interface SelectionItem {
  id: string;
  path: string;
  name?: string;
  type?: string;
  category?: string;
  /** Content-type ids the item is associated with; preview selector uses these. */
  contentTypeIds?: string[];
}

export interface SelectionResult {
  /** Single-select returns one-element array; multi-select returns full set. */
  items: SelectionItem[];
}

/**
 * Preview information for the currently-focused item in ContentBrowser.
 * Surface added per PR review on `specs/992-react-content-explorer/contracts/content-browser-host.md`.
 */
export interface PreviewInfo {
  item: SelectionItem;
  templateId: string;
  /** Server-rendered preview URL or rendered HTML — implementation-defined. */
  url: string;
}

// ---------- US5 P-Search: searchmanagement REST DTO mirrors ----------
//
// Mirrors `projects/sitemanage/src/main/java/com/percussion/searchmanagement/data/*`
// (PSSearchCriteria, PSPagedItemPropertiesList, PSItemProperties). Align to
// live DTOs per constitution II (Evidence Over Invention). Do not invent
// fields; if a field is missing on the server, add it via a sitemanage
// change with a service-contract test (T052a) and threat-model note (T052b).
//
// See `specs/992-react-content-explorer/contracts/search-api.md` (TODO if
// not present; US5 contract path is the sitemanage OpenAPI generator for
// PSSearchRestService at `/Rhythmyx/services/searchmanagement/search/get/extendedresults`).

/**
 * Wire body for `POST /searchmanagement/search/get/extendedresults`.
 *
 * <p>Mirrors {@code com.percussion.searchmanagement.data.PSSearchCriteria}.
 * The wire is wrapped under `SearchCriteria` (the DTO's
 * {@code @XmlRootElement(name = "SearchCriteria")} triggers Jackson's
 * root-name wrapping); the helper {@code searchExtended} unwraps the
 * envelope before sending the request.</p>
 */
export interface PSSearchCriteria {
  query?: string;
  searchType?: string;
  startIndex?: number;
  maxResults?: number;
  sortColumn?: string;
  sortOrder?: string;
  formatId?: number;
  /** Per-field constraint map; matches `PSSearchCriteria.searchFields`. */
  searchFields?: Record<string, string>;
  folderPath?: string;
  caseSensitive?: boolean;
}

/**
 * Per-result row from {@code PSPagedItemPropertiesList}. Mirrors
 * {@code com.percussion.share.data.PSItemProperties}.
 */
export interface PSItemProperties {
  /** Item id (pathmanagement / itemmanagement id space). */
  id?: string;
  /** Display title, typically the workflow title field. */
  title?: string;
  name?: string;
  /** Folder path of the item (e.g. {@code /Sites/Foo/Bar}). */
  folderPath?: string;
  /** Item type discriminator (page / asset / folder). */
  type?: string;
  /** Display-format column values keyed by column id. */
  displayProperties?: Record<string, unknown>;
  /** Workflow / state hints surfaced for the list view. */
  workflowState?: string;
  /** Last modified timestamp (ISO 8601 string per server). */
  lastModified?: string;
  /** Locale of the result. */
  locale?: string;
}

/**
 * Wire shape of {@code PSPagedItemPropertiesList} — list wrapper with
 * paged metadata. The server carries
 * {@code @JsonRootName(value = "PagedItemPropertiesList")} (verified
 * against the live CMS on 2026-07-20) and the array of results is
 * under {@code childrenInPage}.
 */
export interface PSPagedItemPropertiesList {
  childrenCount?: number;
  startIndex?: number;
  /** Per-page rows. */
  childrenInPage?: PSItemProperties[];
}

/** Wire envelope for the {@code /search/get/extendedresults} response. */
export interface PSPagedItemPropertiesListEnvelope {
  PagedItemPropertiesList?: PSPagedItemPropertiesList;
}

/**
 * Client-facing search result set (normalized across wire envelopes).
 * Used by the {@code SearchPanel} component and any downstream
 * explorer open / reveal flows.
 */
export interface PSSearchResults {
  children: PSItemProperties[];
  totalCount?: number;
  startIndex: number;
}
