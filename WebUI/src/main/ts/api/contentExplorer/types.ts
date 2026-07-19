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

export interface PSPagedItemList {
  startIndex: number;
  maxResults: number;
  totalCount?: number;
  /** paged children; field name follows server contract. */
  children?: PSPathItem[];
  /** Some server versions return `items` instead of `children`. */
  items?: PSPathItem[];
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