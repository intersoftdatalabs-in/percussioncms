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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Icon pipeline for the server-driven ActionToolbar (#6515, scope 1).
 *
 * <p>Classic DCE shipped a full GIF vocabulary for explorer actions
 * ({@code DesktopContentExplorer .../cx/images/: Folder, Item, Inbox,
 * CheckedIn/CheckedOut, MyContent, CustomSearch, ...} via
 * {@code PSImageIconLoader}). The modern toolbar rendered those same
 * server actions as text-only buttons. This module restores the
 * DCE-familiar <strong>icon + label</strong> affordance with modernized
 * inline SVG glyphs:</p>
 *
 * <ul>
 *   <li>Labels stay — i18n via {@code message()} and the existing
 *   {@code aria-label}/{@code title} wiring are untouched, so screen-reader
 *   and keyboard behavior do not change.</li>
 *   <li>Glyphs are {@code aria-hidden} / non-focusable decoration only.</li>
 *   <li>Glyphs use {@code stroke="currentColor"} with no hardcoded fills, so
 *   they follow the surrounding text color in light and dark themes.</li>
 *   <li>React inline SVG — no new jQuery/bridge, no bundled assets, no
 *   OS-specific paths.</li>
 *   <li>Actions with no mapping render label-only (text-first clarity for
 *   empty/error and long-tail actions is preserved).</li>
 * </ul>
 *
 * <p>DCE precedent audit for the toolbar-mapped set (retire-or-map, scope 3
 * note): every classic GIF below either has a modern glyph here or is
 * explicitly retired with a reason. GIFs outside toolbar scope (tree-node
 * states such as {@code Folder_EXPANDED}, {@code *_EXPANDED} variants,
 * {@code ContentPathSeparator}, assignee avatars {@code MySelf}/{@code Nobody},
 * design-time {@code DTItem}, {@code Missing}) keep their existing modern
 * renderings (tree CSS / text) and are NOT re-mapped here. Menu-bar and
 * context-menu icon coverage ({@code MENU_COMMAND_ICONS} +
 * {@code MenuCommandIcon}, and {@code ActionIcon} reuse in
 * {@code ContextMenu}) is the scope-2 follow-on (issue #6603).</p>
 *
 * <table>
 * <caption>DCE GIF to modern glyph mapping (toolbar scope)</caption>
 * <tr><th>DCE GIF</th><th>Modern glyph</th></tr>
 * <tr><td>Folder / Folders / FolderRef</td><td>{@code folder}</td></tr>
 * <tr><td>Item</td><td>{@code item}</td></tr>
 * <tr><td>Inbox</td><td>{@code inbox}</td></tr>
 * <tr><td>CheckedOut / CheckedOutByMe</td><td>{@code checkout}</td></tr>
 * <tr><td>CheckedIn</td><td>{@code checkin}</td></tr>
 * <tr><td>MyContent / CommunityContent</td><td>{@code user}</td></tr>
 * <tr><td>CustomSearch / NewSearch / EmptySearch</td><td>{@code search}</td></tr>
 * <tr><td>AllContent / Category</td><td>{@code grid}</td></tr>
 * <tr><td>None / Other / OtherContent (generic)</td><td>label-only (retired:
 * no distinct modern meaning on a toolbar button)</td></tr>
 * <tr><td>FolderMarked (+_Expanded)</td><td>retired: no modern toolbar
 * equivalent; selection state already conveyed by row highlight</td></tr>
 * </table>
 */

import React from "react";
import type { ExplorerMenuCommandId } from "./menuBarModel";

export type ActionIconId =
  | "open"
  | "preview"
  | "edit"
  | "properties"
  | "rename"
  | "new"
  | "folder"
  | "page"
  | "asset"
  | "item"
  | "upload"
  | "download"
  | "copy"
  | "newcopy"
  | "move"
  | "clipboard"
  | "pastelink"
  | "delete"
  | "restore"
  | "purge"
  | "empty"
  | "checkout"
  | "checkin"
  | "publish"
  | "takedown"
  | "stage"
  | "unstage"
  | "schedule"
  | "revisions"
  | "promote"
  | "audit"
  | "workflow"
  | "transition"
  | "search"
  | "refresh"
  | "flush"
  | "security"
  | "lang"
  | "relationships"
  | "dependencies"
  | "help"
  | "inbox"
  | "user"
  | "grid"
  | "site"
  | "template"
  | "slot";

function Svg({ children }: { children: React.ReactNode }): React.JSX.Element {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {children}
    </svg>
  );
}

const GLYPHS: Record<ActionIconId, React.ReactNode> = {
  open: (
    <>
      <path d="M6 2.5H3.5a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1H6" />
      <path d="M2.5 8h8.5M8.5 5.5 11 8l-2.5 2.5" />
    </>
  ),
  preview: (
    <>
      <path d="M1.5 8S4 3.8 8 3.8 14.5 8 14.5 8 12 12.2 8 12.2 1.5 8 1.5 8Z" />
      <circle cx="8" cy="8" r="1.8" />
    </>
  ),
  edit: (
    <>
      <path d="M11 2.3l2.7 2.7L5.2 13.5H2.5V10.8L11 2.3Z" />
      <path d="M9.5 3.8l2.7 2.7" />
    </>
  ),
  properties: (
    <>
      <path d="M2 5h12M2 11h12" />
      <circle cx="6" cy="5" r="1.8" />
      <circle cx="10" cy="11" r="1.8" />
    </>
  ),
  rename: (
    <>
      <path d="M2.5 2.5v3.2L10.7 14l2.8-2.8L5.7 2.5H2.5Z" />
      <circle cx="5.5" cy="5.5" r="1" />
    </>
  ),
  new: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 5v6M5 8h6" />
    </>
  ),
  folder: (
    <>
      <path d="M1.8 4.2a1 1 0 0 1 1-1h3l1.5 2h6a1 1 0 0 1 1 1V11a1 1 0 0 1-1 1h-10.5a1 1 0 0 1-1-1V4.2Z" />
    </>
  ),
  page: (
    <>
      <path d="M4 1.8h5.3L12 4.5v9.7H4V1.8Z" />
      <path d="M9.3 1.8v2.7H12" />
    </>
  ),
  asset: (
    <>
      <rect x="2" y="3" width="12" height="10" rx="1" />
      <circle cx="5.6" cy="6.4" r="1.2" />
      <path d="M2.5 11.5l3.5-3.5 2.4 2.4 2-2 3.1 3.1" />
    </>
  ),
  item: (
    <>
      <path d="M8 1.8 13.5 4.8v6.4L8 14.2 2.5 11.2V4.8L8 1.8Z" />
      <path d="M2.5 4.8 8 7.8l5.5-3M8 7.8v6.4" />
    </>
  ),
  upload: (
    <>
      <path d="M8 10.5v-9M4.8 4.7 8 1.5l3.2 3.2" />
      <path d="M2.5 11.5h11V14h-11z" />
    </>
  ),
  download: (
    <>
      <path d="M8 1.5v9M4.8 7.3 8 10.5l3.2-3.2" />
      <path d="M2.5 11.5h11V14h-11z" />
    </>
  ),
  copy: (
    <>
      <rect x="6" y="6" width="8" height="8" rx="1" />
      <path d="M10 6V3.5A1.5 1.5 0 0 0 8.5 2h-5A1.5 1.5 0 0 0 2 3.5v5A1.5 1.5 0 0 0 3.5 10H6" />
    </>
  ),
  newcopy: (
    <>
      <rect x="5" y="5" width="6.5" height="6.5" rx="1" />
      <path d="M9.5 5V3.5A1.5 1.5 0 0 0 8 2H3.5A1.5 1.5 0 0 0 2 3.5V8A1.5 1.5 0 0 0 3.5 9.5H5" />
      <path d="M13 10.5v3M11.5 12h3" />
    </>
  ),
  move: (
    <>
      <path d="M2 5.5h9M8.5 3 11 5.5 8.5 8" />
      <path d="M14 10.5H5M7.5 8 5 10.5l2.5 2.5" />
    </>
  ),
  clipboard: (
    <>
      <rect x="4" y="3.5" width="8" height="11" rx="1" />
      <rect x="6" y="1.8" width="4" height="2.8" rx="0.8" />
      <path d="M6 8.5h4M6 11h4" />
    </>
  ),
  pastelink: (
    <>
      <rect x="2.5" y="3.5" width="7.5" height="11" rx="1" />
      <rect x="4.2" y="1.8" width="4" height="2.8" rx="0.8" />
      <path d="M7.5 12.5h4a2.5 2.5 0 0 0 0-5H9.5M8.5 7.5h-2a2.5 2.5 0 0 0 0 5h2" />
    </>
  ),
  delete: (
    <>
      <path d="M2.5 4h11M6.5 4V2.5h3V4M4 4l1 10h6l1-10" />
      <path d="M6.5 7v4M9.5 7v4" />
    </>
  ),
  restore: (
    <>
      <path d="M6.5 3.5 3 7l3.5 3.5" />
      <path d="M3.5 7H10a4 4 0 0 1 0 8H7" />
    </>
  ),
  purge: (
    <>
      <path d="M2.5 4h11M6.5 4V2.5h3V4M4 4l1 10h6l1-10" />
      <path d="M3.5 13.5 12.5 3.5" />
    </>
  ),
  empty: (
    <>
      <path d="M2 11h12v2.5H2z" />
      <path d="M8 9.5v-7M5 5.5 8 2.5l3 3" />
    </>
  ),
  checkout: (
    <>
      <path d="M9.5 2.5H13a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H9.5" />
      <path d="M2.5 8h7M7 5.5 9.5 8 7 10.5" />
    </>
  ),
  checkin: (
    <>
      <path d="M6.5 2.5H3a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h3.5" />
      <path d="M13.5 8h-7M9 5.5 6.5 8 9 10.5" />
    </>
  ),
  publish: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M2 8h12M8 2c-4 4-4 8 0 12M8 2c4 4 4 8 0 12" />
    </>
  ),
  takedown: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 5v5M5.9 7.9 8 10l2.1-2.1" />
    </>
  ),
  stage: (
    <>
      <path d="M8 12.5 2.5 9 8 5.5 13.5 9 8 12.5Z" />
      <path d="M2.5 12 8 15l5.5-3" />
    </>
  ),
  unstage: (
    <>
      <path d="M8 12.5 2.5 9 8 5.5 13.5 9 8 12.5Z" />
      <path d="M3.5 13 12.5 3.5" />
    </>
  ),
  schedule: (
    <>
      <rect x="2" y="3.5" width="12" height="10.5" rx="1" />
      <path d="M2 6.5h12M5.5 2v3M10.5 2v3" />
      <circle cx="8" cy="10.3" r="1.9" />
      <path d="M8 9v1.3l.9.9" />
    </>
  ),
  revisions: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 4.5V8l2.5 2.5" />
    </>
  ),
  promote: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M8 11.5v-7M5.2 7.3 8 4.5l2.8 2.8" />
    </>
  ),
  audit: (
    <>
      <path d="M5.5 4h8M5.5 8h8M5.5 12h8" />
      <circle cx="2.9" cy="4" r="0.9" />
      <circle cx="2.9" cy="8" r="0.9" />
      <circle cx="2.9" cy="12" r="0.9" />
    </>
  ),
  workflow: (
    <>
      <circle cx="4" cy="4" r="1.8" />
      <circle cx="4" cy="12" r="1.8" />
      <circle cx="12" cy="8" r="1.8" />
      <path d="M5.7 4.6c2 .4 2.5 1.3 4.4 2.6M5.7 11.4c2-.4 2.5-1.3 4.4-2.6" />
    </>
  ),
  transition: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M5 8h6M9.3 5.8 11.5 8l-2.2 2.2" />
    </>
  ),
  search: (
    <>
      <circle cx="7" cy="7" r="4.5" />
      <path d="M10.5 10.5 14 14" />
    </>
  ),
  refresh: (
    <>
      <path d="M13.6 8A5.6 5.6 0 1 1 8 2.4c2 0 3.7 1 4.7 2.6" />
      <path d="M12.7 1.5V5h-3.5" />
    </>
  ),
  flush: (
    <>
      <path d="M8.8 1.5 3.5 9H7l-.8 5.5L11.5 7H8l.8-5.5Z" />
    </>
  ),
  security: (
    <>
      <path d="M8 1.8 13 3.8v4c0 3.2-2.1 5.4-5 6.4-2.9-1-5-3.2-5-6.4v-4L8 1.8Z" />
      <path d="M6 8.2l1.5 1.5L10.5 6.5" />
    </>
  ),
  lang: (
    <>
      <path d="M2 3.5h12V10H8.5L5.5 12.5V10H2V3.5Z" />
      <circle cx="5" cy="6.8" r="0.8" fill="currentColor" stroke="none" />
      <circle cx="8" cy="6.8" r="0.8" fill="currentColor" stroke="none" />
      <circle cx="11" cy="6.8" r="0.8" fill="currentColor" stroke="none" />
    </>
  ),
  relationships: (
    <>
      <path d="M6.5 9.5a3 3 0 0 0 4.2 0l2-2a3 3 0 0 0-4.2-4.2l-1 1" />
      <path d="M9.5 6.5a3 3 0 0 0-4.2 0l-2 2a3 3 0 0 0 4.2 4.2l1-1" />
    </>
  ),
  dependencies: (
    <>
      <circle cx="8" cy="3" r="1.6" />
      <circle cx="3.5" cy="12.5" r="1.6" />
      <circle cx="12.5" cy="12.5" r="1.6" />
      <path d="M8 4.6V8M8 8 4 11M8 8l4 3" />
    </>
  ),
  help: (
    <>
      <circle cx="8" cy="8" r="6" />
      <path d="M6.2 6.2c0-1 1-1.7 1.8-1.7s1.8.7 1.8 1.7c0 1.2-1.8 1.5-1.8 2.8" />
      <circle cx="8" cy="11.8" r="0.9" fill="currentColor" stroke="none" />
    </>
  ),
  inbox: (
    <>
      <path d="M2 9.5h12l-1 4H3l-1-4Z" />
      <path d="M2 9.5 3.2 3h9.6L14 9.5" />
    </>
  ),
  user: (
    <>
      <circle cx="8" cy="5" r="2.8" />
      <path d="M2.5 14c.5-3 2.7-4.5 5.5-4.5s5 1.5 5.5 4.5" />
    </>
  ),
  grid: (
    <>
      <rect x="2.5" y="2.5" width="4.5" height="4.5" rx="0.8" />
      <rect x="9" y="2.5" width="4.5" height="4.5" rx="0.8" />
      <rect x="2.5" y="9" width="4.5" height="4.5" rx="0.8" />
      <rect x="9" y="9" width="4.5" height="4.5" rx="0.8" />
    </>
  ),
  site: (
    <>
      <path d="M2.5 8 8 2.5 13.5 8" />
      <path d="M4.5 7v6.5h7V7" />
    </>
  ),
  template: (
    <>
      <path d="M2.5 6h8M8 3.5 10.5 6 8 8.5" />
      <path d="M13.5 10H5.5M8 7.5 5.5 10 8 12.5" />
    </>
  ),
  slot: (
    <>
      <rect
        x="2.5"
        y="2.5"
        width="11"
        height="11"
        rx="1"
        strokeDasharray="2.5 2"
      />
      <path d="M8 6v4M6 8h4" />
    </>
  ),
};

/** Exact normalized-name matches. Normalized = lowercase alphanumeric only. */
const EXACT: Record<string, ActionIconId> = {
  open: "open",
  openineditor: "open",
  openrelateditem: "open",
  openrelateditemineditor: "open",
  opentranslationvariant: "open",
  preview: "preview",
  mobilepreview: "preview",
  assemblerpreview: "preview",
  edit: "edit",
  editpromotableversion: "edit",
  editproperties: "properties",
  properties: "properties",
  viewproperties: "properties",
  itemproperties: "properties",
  rename: "rename",
  renameitem: "rename",
  renamefolder: "rename",
  new: "new",
  create: "new",
  createnewitem: "new",
  newitem: "new",
  createitem: "new",
  newfolder: "folder",
  createfolder: "folder",
  subfoldercopy: "folder",
  newpage: "page",
  createpage: "page",
  newasset: "asset",
  createasset: "asset",
  registerasset: "asset",
  upload: "upload",
  bulkupload: "upload",
  download: "download",
  export: "download",
  exportcsv: "download",
  copy: "copy",
  copyitem: "copy",
  copyfolder: "copy",
  copyfolderpath: "copy",
  copyitemguid: "copy",
  copydest: "copy",
  newcopy: "newcopy",
  newcopyitem: "newcopy",
  move: "move",
  moveitem: "move",
  movefolder: "move",
  movedest: "move",
  movetoslot: "move",
  moverelationshipslot: "move",
  paste: "clipboard",
  pasteclipboard: "clipboard",
  clipboard: "clipboard",
  copyurltoclipboard: "clipboard",
  pasteaslink: "pastelink",
  pasteaslinktoslot: "pastelink",
  pastelinktoslot: "pastelink",
  pastelink: "pastelink",
  delete: "delete",
  deleteitem: "delete",
  deletefolder: "delete",
  recycle: "delete",
  restore: "restore",
  restoreitem: "restore",
  purge: "purge",
  purgeitem: "purge",
  empty: "empty",
  emptyrecycle: "empty",
  emptyrecyclebin: "empty",
  emptytrash: "empty",
  checkout: "checkout",
  checkin: "checkin",
  forcecheckin: "checkin",
  publish: "publish",
  publishnow: "publish",
  publishfolder: "publish",
  approveincremental: "publish",
  takedown: "takedown",
  takedownfolder: "takedown",
  unpublish: "takedown",
  stage: "stage",
  stageselectedfolder: "stage",
  stagechildren: "stage",
  unstage: "unstage",
  removestaging: "unstage",
  removefromstaging: "unstage",
  unapproveincremental: "unstage",
  removeincremental: "unstage",
  schedule: "schedule",
  setschedule: "schedule",
  itemschedule: "schedule",
  revisions: "revisions",
  workflowrevisions: "revisions",
  revisionhistory: "revisions",
  revisioncompare: "revisions",
  promotableversion: "promote",
  revisionpromote: "promote",
  promote: "promote",
  audittrail: "audit",
  workflowaudittrail: "audit",
  audit: "audit",
  workflow: "workflow",
  setworkflow: "workflow",
  setfolderworkflow: "workflow",
  workflownewversion: "workflow",
  search: "search",
  savedsearch: "search",
  customsearch: "search",
  newsearch: "search",
  refresh: "refresh",
  reload: "refresh",
  flushcache: "flush",
  flush: "flush",
  security: "security",
  foldersecurity: "security",
  folderacl: "security",
  acl: "security",
  permissions: "security",
  translations: "lang",
  translate: "lang",
  translationvariant: "lang",
  setlocale: "lang",
  setfolderlocale: "lang",
  relationships: "relationships",
  addrelationship: "relationships",
  removerelationship: "relationships",
  removeallrelationships: "relationships",
  whereused: "dependencies",
  dependencies: "dependencies",
  help: "help",
  inbox: "inbox",
  mycontent: "user",
  checkoutowner: "user",
  community: "user",
  setcommunity: "user",
  setfoldercommunity: "user",
  allcontent: "grid",
  view: "preview",
  views: "preview",
  viewscatalog: "preview",
  site: "site",
  sites: "site",
  createsite: "site",
  sitecopy: "site",
  siterename: "site",
  changetemplate: "template",
  changepagetemplate: "template",
  changerelationshiptemplate: "template",
  arrangemoveupleft: "move",
  arrangemovedownright: "move",
  arrangeremove: "delete",
  slotadd: "slot",
  slotcreate: "slot",
  createiteminslot: "slot",
  linkexistingitemtoslot: "slot",
};

/** Menu-bar shell command id to glyph (#6603, scope 2). Every Content /
 * View / Help leaf maps to a DCE-familiar glyph so the menu bar and the
 * server-driven context menu share one icon vocabulary with the toolbar.
 * Toggle checkmarks stay — icons are decoration alongside the label, and
 * any future command without a row here renders label-only. */
const MENU_COMMAND_ICONS: Record<ExplorerMenuCommandId, ActionIconId> = {
  "content-search": "search",
  "content-clipboard-add": "clipboard",
  "content-multi-copy": "copy",
  "content-multi-move": "move",
  "content-multi-recycle": "delete",
  "content-multi-purge": "purge",
  "content-multi-restore": "restore",
  "content-create-site": "site",
  "content-site-copy": "site",
  "content-site-rename": "rename",
  "content-subfolder-copy": "folder",
  "content-copy-folder-path": "copy",
  "content-copy-item-guid": "copy",
  "content-set-workflow": "workflow",
  "content-set-folder-workflow": "workflow",
  "content-set-folder-community": "user",
  "content-set-folder-locale": "lang",
  "content-set-folder-display-format": "grid",
  "content-set-folder-allowed-sites": "site",
  "content-set-community": "user",
  "content-change-page-template": "template",
  "content-mobile-preview": "preview",
  "view-refresh": "refresh",
  "view-search": "search",
  "view-security": "security",
  "view-item-properties": "properties",
  "view-translations": "lang",
  "view-relationships": "relationships",
  "view-dependencies": "dependencies",
  "view-clipboard": "clipboard",
  "view-recent": "revisions",
  "help-explorer": "help",
  "help-about": "help",
};

/**
 * Resolve the icon for an Explorer menu-bar shell command id, or
 * {@code null} when the command has no mapping (caller renders label-only).
 */
export function resolveMenuCommandIcon(
  id: ExplorerMenuCommandId,
): ActionIconId | null {
  return MENU_COMMAND_ICONS[id] ?? null;
}

/** Prefix fallbacks, checked in order after exact lookup. */
const PREFIXES: Array<[string, ActionIconId]> = [
  ["workflowtransition", "transition"],
  ["transition", "transition"],
  ["arrange", "move"],
  ["slot", "slot"],
  ["workflow", "workflow"],
  ["approve", "publish"],
  ["publish", "publish"],
  ["takedown", "takedown"],
  ["unstage", "unstage"],
  ["stage", "stage"],
  ["schedule", "schedule"],
  ["checkin", "checkin"],
  ["checkout", "checkout"],
  ["restore", "restore"],
  ["purge", "purge"],
  ["recycle", "delete"],
  ["delete", "delete"],
  ["trash", "delete"],
  ["pasteaslink", "pastelink"],
  ["pastelink", "pastelink"],
  ["paste", "clipboard"],
  ["clipboard", "clipboard"],
  ["copy", "copy"],
  ["move", "move"],
  ["rename", "rename"],
  ["upload", "upload"],
  ["download", "download"],
  ["export", "download"],
  ["search", "search"],
  ["refresh", "refresh"],
  ["secur", "security"],
  ["acl", "security"],
  ["propert", "properties"],
  ["communit", "user"],
  ["translat", "lang"],
  ["locale", "lang"],
  ["relation", "relationships"],
  ["depend", "dependencies"],
  ["audit", "audit"],
  ["revision", "revisions"],
  ["version", "revisions"],
  ["history", "revisions"],
  ["template", "template"],
  ["folder", "folder"],
  ["page", "page"],
  ["asset", "asset"],
  ["site", "site"],
  ["item", "item"],
  ["inbox", "inbox"],
  ["help", "help"],
  ["creat", "new"],
  ["new", "new"],
];

/**
 * Normalize a server action name for icon lookup: lowercase, strip every
 * non-alphanumeric (handles {@code workflow-transition:Submit},
 * {@code Paste_As_Link}, {@code Arrange_MoveUpLeft}, ...).
 */
export function normalizeActionKey(name: string): string {
  return name.toLowerCase().replace(/[^a-z0-9]/g, "");
}

/**
 * Resolve the icon for a server action name, or {@code null} when the
 * action has no mapping (caller renders label-only).
 */
export function resolveActionIcon(name: string): ActionIconId | null {
  const key = normalizeActionKey(name);
  if (!key) {
    return null;
  }
  const exact = EXACT[key];
  if (exact) {
    return exact;
  }
  for (const [prefix, icon] of PREFIXES) {
    if (key.startsWith(prefix) || key.includes(prefix)) {
      return icon;
    }
  }
  return null;
}

const iconWrapStyle: React.CSSProperties = {
  display: "inline-flex",
  flexShrink: 0,
  marginRight: 6,
  verticalAlign: "-3px",
};

export interface ActionIconProps {
  /** Server action {@code name} (not the label). */
  name: string;
  /**
   * Test id for the decorative wrapper. Defaults to the toolbar chrome;
   * the menu bar passes {@code menu-bar-icon} and the context menu passes
   * {@code context-menu-icon} so surface specs can scope icon assertions.
   */
  testId?: string;
}

/**
 * Decorative icon for a toolbar action. Returns {@code null} for unmapped
 * actions so the caller falls back to a label-only button.
 */
export function ActionIcon(props: ActionIconProps): React.JSX.Element | null {
  const icon = resolveActionIcon(props.name);
  if (!icon) {
    return null;
  }
  return (
    <span
      data-testid={props.testId ?? "action-toolbar-icon"}
      aria-hidden="true"
      style={iconWrapStyle}
    >
      <Svg>{GLYPHS[icon]}</Svg>
    </span>
  );
}

/** Decorative icon for a menu-bar shell command id ({@code null} → label-only). */
export function MenuCommandIcon(props: {
  id: ExplorerMenuCommandId;
}): React.JSX.Element | null {
  const icon = resolveMenuCommandIcon(props.id);
  if (!icon) {
    return null;
  }
  return (
    <span
      data-testid="menu-bar-icon"
      aria-hidden="true"
      style={iconWrapStyle}
    >
      <Svg>{GLYPHS[icon]}</Svg>
    </span>
  );
}
