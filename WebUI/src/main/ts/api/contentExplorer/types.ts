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

// ---------- US7 P-Adv: clipboard / dependency / wizard DTO mirrors ----------
//
// All US7 types are derived from the existing sitemanage + rest DTOs
// already in scope (pathApi: pathmanagement; PSSearchCriteria;
// IPSWidgetAssetRelationshipService). Per constitution II (Evidence
// Over Invention) there are no invented fields — when a new server
// field is required, add it via a `rest` change with a
// service-contract test (T052a) and threat-model note (T052b).
//
// See `specs/992-react-content-explorer/research/relationship-rest-gaps.md`
// for the T074 spike result that drives the gap policy for the
// DependencyViewer + RelationshipsView rows.

/**
 * One clipboard entry. The clipboard is an in-memory store of the
 * items the explorer user has cut or copied; paste operations
 * resolve through the typed {@link ClipboardItem.kind}-switched
 * transport in {@code clipboardApi.ts}.
 */
export interface ClipboardItem {
  /** Stable item id (pathmanagement / itemmanagement id space). */
  id: string;
  /** Item path (e.g. {@code /Sites/Foo/Bar}). */
  path: string;
  /** Item type discriminator (page / asset / folder). */
  kind: "page" | "asset" | "folder";
  /** Display name (title for page/asset, name for folder). */
  name?: string;
  /** Permission snapshot from the source folder; surfaced for the
   *  paste-warning UX (FR-016 read-only without rights). */
  sourceAccessLevel?: FolderAccessLevel;
}

/**
 * The clipboard state. Empty by default; populated by the explorer's
 * cut / copy actions; cleared after a successful paste. The state
 * is intentionally local (not persisted across page reloads) so the
 * accidental re-paste after a session restore can't bypass an
 * explicit user action.
 */
export interface Clipboard {
  /** Operation mode — copy is non-destructive, cut moves the items. */
  operation: "copy" | "cut";
  /** The items in the clipboard (preserves source order). */
  items: ReadonlyArray<ClipboardItem>;
  /** Timestamp the clipboard was last mutated. */
  updatedAt: string;
}

/**
 * Wire body for `POST /Rhythmyx/rest/sitemanage/site/copy` — mirrors
 * {@code com.percussion.sitemanage.data.PSSiteCopyRequest}. The modern
 * SiteCopyWizard UIs this body and submits.
 */
export interface PSSiteCopyRequest {
  sourceSite?: string;
  targetSite?: string;
  targetFolder?: string;
  workflows?: string;
  templates?: string;
  autoCleanup?: boolean;
}

/**
 * Wire body for `POST /Rhythmyx/rest/pathmanagement/path/moveItem` —
 * mirrors {@code com.percussion.pathmanagement.data.PSMoveFolderItem}
 * (the existing pathApi.moveItem DTO). Re-used by the
 * SubfolderCopyWizard (copy:true) and the ReducedActions move / copy
 * wiring (US1).
 */
export interface PSCopyRequest {
  sourcePath: string;
  targetPath: string;
  copy: boolean;
}

/**
 * Result of one clipboard paste operation. Reports the per-item
 * outcome so the explorer shell can refresh the tree / list and
 * surface partial-failure messages.
 */
export interface ClipboardPasteResultItem {
  item: ClipboardItem;
  /** True when the underlying REST call returned 2xx. */
  ok: boolean;
  /** Error message from the failed REST call, present when {@link ok} is false. */
  message?: string;
}

export interface ClipboardPasteSummary {
  operation: "copy" | "cut";
  results: ClipboardPasteResultItem[];
}

export type RelationshipDimension =
  | "outgoing"
  | "incoming"
  | "aa"
  | "taxonomy"
  | "local"
  | "reverse";

export interface RelationshipSummary {
  dimension: RelationshipDimension;
  count: number;
  label?: string;
  unknown?: boolean;
}

export interface NodeRelationshipSummary {
  nodeId: string;
  nodePath?: string;
  dimensions: RelationshipSummary[];
  /**
   * True when this summary was assembled client-side from item
   * metadata only (the full server-side graph query is gated). The
   * UI uses this to label the panel "Client-side preview".
   * See `specs/992-react-content-explorer/research/relationship-rest-gaps.md`.
   */
  clientSideOnly: boolean;
}

// ---------- US3 P-Menu: action-menu REST DTO mirrors ----------
//
// Mirrors `rest/src/main/java/com/percussion/rest/actions/*`. Align to live
// DTOs (`ActionMenu`, `ActionMenuList`, `ActionMenuParameter`,
// `ActionMenuProperty`, `ActionMenuVisibilityContext`,
// `ActionMenuModeUIContext`, `AllowedContentTypeMenusRequest`,
// `AllowedWorkflowTransitionsRequest`) per constitution II
// (Evidence Over Invention). Do not invent fields; if a field is missing
// on the server, add it via a rest change with a service-contract test
// (T052a) and threat-model note (T052b).
//
// See `specs/992-react-content-explorer/contracts/action-menu-api.md` for the
// surface contract and current REST gaps (US3 maturity notes).

/**
 * Mirrors {@code PSAction.TYPE_*} constants on the server:
 * {@code TYPE_MENU}, {@code TYPE_CONTEXTMENU}, {@code TYPE_MENUITEM},
 * {@code DYNAMICMENU}.
 */
export type ActionMenuType = "MENU" | "CONTEXTMENU" | "MENUITEM" | "DYNAMICMENU";

export interface ActionMenuParameter {
  name?: string;
  value?: string;
  description?: string;
}

export interface ActionMenuProperty {
  actionId: number;
  name?: string;
  value?: string;
  description?: string;
}

export interface ActionMenuVisibilityContext {
  // Field shape per server DTO; the typed mapper + Vitest tests pin
  // the until-then-permissive surface until the SPEC exposes the
  // field set explicitly.
  [key: string]: unknown;
}

export interface ActionMenuModeUIContext {
  [key: string]: unknown;
}

/**
 * Server children envelope. Server's {@code ActionMenu.children} is
 * the {@code ActionMenuList extends ArrayList<ActionMenu>} type with
 * {@code @XmlRootElement(name = "ActionMenuList")}, so the wire is
 * {@code {"ActionMenuList": ActionMenu[]}}.
 */
export interface ActionMenuListEnvelope {
  ActionMenuList?: ActionMenu[];
}

/**
 * Mirrors {@code com.percussion.rest.actions.ActionMenu}. See the
 * {@code @Schema} / Javadoc on the server DTO for per-field meaning;
 * fields with non-nullable semantics on the server
 * ({@code name}) are required here.
 */
export interface ActionMenu {
  id: number;
  guid?: { raw?: string };
  name: string;
  label?: string;
  description?: string;
  /** Action URL relative to document base for the page hosting the menu. */
  url?: string;
  sortRank: number;
  menuType: ActionMenuType;
  /** Marker for client-handled vs server-handled actions. */
  handler?: string;
  children?: ActionMenuListEnvelope;
  parameters?: ActionMenuParameter[];
  visibilityContexts?: ActionMenuVisibilityContext[];
  uiContexts?: ActionMenuModeUIContext[];
  properties?: ActionMenuProperty[];
}

/**
 * Wire body for {@code POST /actions/find/types}. Mirrors
 * {@code AllowedContentTypeMenusRequest}.
 */
export interface AllowedContentTypeMenusRequest {
  contentIds: number[];
}

/**
 * Client-facing flattened menu model used by {@code ContextMenu} /
 * {@code ActionToolbar} (US3 T053 / T054). Decoupled from the wire
 * shape so the UI does not depend on server naming and so unit tests
 * can build menus without mocking the REST envelope.
 */
export interface MenuAction {
  /** Stable identifier (server `name`); used as the React `key`. */
  name: string;
  /** Display label (server `label`); falls back to `name` if absent. */
  label: string;
  description?: string;
  url?: string;
  handler?: string;
  sortRank: number;
  menuType: ActionMenuType;
  parameters?: ActionMenuParameter[];
  /** Empty unless the parent has cascading children. */
  children?: MenuAction[];
}
