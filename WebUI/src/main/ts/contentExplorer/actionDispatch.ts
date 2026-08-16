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

/**
 * Explorer server-action dispatcher. Presentation never navigates to Data
 * Flow (legacy XML application) URLs — those 404 from the SPA.
 *
 * @see specs/992-react-content-explorer/contracts/action-execution.md
 */

import {
  fetchPreviewLocation,
  flushAssemblerCache,
  resetNavigation,
} from "../api/contentExplorer/assemblyApi";
import {
  createNewCopy,
  createPromotableVersion,
} from "../api/contentExplorer/itemCopyApi";
import { del } from "../api/client";
import { PATHS } from "../api/paths";
import { publishSelectedItem } from "./itemPublish";
import type { MenuAction, PSPathItem } from "../api/contentExplorer/types";
import { classifyUrl, safeNavigate } from "../util/safeNavigate";
import {
  ASSEMBLY_WINDOW_FEATURES,
  assemblyWindowName,
  buildAssemblyHostUrl,
} from "../assembly/assemblyHostUrl";
import {
  EDITOR_WINDOW_FEATURES,
  buildEditorHostUrl,
  editorWindowName,
} from "../editor/editorHostUrl";
import { createEditorItem } from "../editor/itemCreateApi";
import {
  isExplorerPageType,
  loadPageTemplates,
  type PageTemplateChoice,
} from "../editor/pageTemplates";
import {
  isNewItemHostName,
  parseExplorerContentId,
} from "./menuCatalogLoad";
import { resolveFolderPathFromSelection } from "./folderPath";
import { EXPLORER_MSG } from "./messages";
import {
  buildSitePathPreviewUrl,
  normalizeCmsPath,
  openPreviewItem,
  resolvePreviewKind,
} from "./previewItem";
import { isFolder } from "./selection";
import { parseWorkflowTransitionTrigger } from "./workflowMenuActions";
import {
  type AssemblySlotContext,
  isSlotActionName,
  slotContextHasRelationship,
  slotContextHasSlot,
} from "../assembly/slotContext";
import {
  addSlotRelationship,
  changeSlotTemplateSlot,
  fetchSlotAllowedTemplates,
  moveSlotRelationship,
  removeSlotRelationship,
} from "../api/contentExplorer/slotRelationshipApi";

export type ActionKind =
  | "client"
  | "rest"
  | "editor"
  | "unavailable"
  | "legacy-file"
  | "workflow";

const EDITOR_NAMES = new Set([
  "edit",
  "edit_content",
  "edit_properties",
  "quick_edit",
  "view_content",
  "view_properties",
  "revision_viewcontent",
  "revision_viewproperties",
  "revision_promote",
]);

const PREVIEW_PARENT_NAMES = new Set([
  "item_preview",
  "enterprise_preview",
  "corporate_preview",
  "slot_item_preview",
  "slot_item_enterprise_preview",
  "slot_item_corporate_preview",
]);

/** Explorer Active Assembly parents — open the preview-first host (996). */
export const AA_PREVIEW_PARENT_NAMES = new Set([
  "item_activeassembly",
  "enterpriseitem_activeassembly",
  "corporateitem_activeassembly",
  "item_assembly",
]);

const AA_UNAVAILABLE_NAMES = new Set(["aa_table_editor"]);

const P1_PANEL_NAMES = new Set([
  "translate",
  "item_viewdependents",
  "workflow_revisions",
  "workflow_audittrail",
]);

const P1_REST_NAMES = new Set([
  "flush_cache",
  "navreset",
  "workflow_newversion",
  "edit_promotableversion",
]);

const DATA_FLOW_PATH_MARKERS = [
  "sys_cxsupport",
  "sys_action",
  "sys_cesupport",
  "sys_rcsupport",
  "sys_cx/",
  "sys_cx?",
  "sys_compare",
  "sys_cxitemassembly",
  "sys_cxdependencytree",
  "sys_uisupport",
  "sys_actiontranslate",
  "rxs_navsupport",
  "sys_cmp",
];

/** Content Editor XML apps used as New Item / Edit URLs (P3 — do not navigate). */
const CONTENT_EDITOR_PATH_MARKERS = [
  "rx_ce",
  "psx_ce",
  "sys_ce/",
  "contenteditorurls",
  "checkoutedit",
  "checkoutaapage",
  "checkoutaadoc",
];

export interface ActionDispatchContext {
  item: PSPathItem | null;
  folderPath?: string | null;
  onOpen?: (item: PSPathItem) => void;
  onPreview?: (item: PSPathItem) => void | Promise<void>;
  onPurge?: (item: PSPathItem) => Promise<void>;
  onShowTranslations?: () => void;
  onShowDependencies?: () => void;
  onShowRevisions?: (tab: "revisions" | "audit") => void;
  flushCache?: () => Promise<void>;
  resetNav?: () => Promise<void>;
  createCopy?: (itemId: string) => Promise<void>;
  createPromotable?: (itemId: string) => Promise<void>;
  createItem?: typeof createEditorItem;
  loadPageTemplates?: typeof loadPageTemplates;
  pickPageTemplate?: (
    templates: PageTemplateChoice[],
  ) => Promise<string | null>;
  onPublish?: (item: PSPathItem) => Promise<void>;
  /** Parent menu name when the user activated a child (AA vs Preview). */
  parentName?: string;
  writeClipboard?: (text: string) => Promise<void>;
  confirm?: (body: string) => boolean;
  openWindow?: (url: string, target?: string, features?: string) => Window | null;
  fetchPreview?: typeof fetchPreviewLocation;
  runWorkflow?: (itemId: string, trigger: string) => Promise<void>;
  /**
   * Selected AA slot (and optional relationship). Folder browse has no
   * slot — dispatch must not invent Arrange_* from a folder.
   */
  slot?: AssemblySlotContext | null;
  addToSlot?: typeof addSlotRelationship;
  removeSlotRel?: typeof removeSlotRelationship;
  moveSlotRel?: typeof moveSlotRelationship;
  changeSlotTemplate?: typeof changeSlotTemplateSlot;
  pickSlotDependent?: (
    slot: AssemblySlotContext,
  ) => Promise<{ contentId: number; templateId: number; folderId?: number } | null>;
  pickSlotCreate?: (
    slot: AssemblySlotContext,
  ) => Promise<{
    contentType: string;
    folderPath: string;
    templateId?: string;
    snippetTemplateId: number;
  } | null>;
  pickSlotTemplateSlot?: (
    slot: AssemblySlotContext,
  ) => Promise<{ slotId: number; templateId: number } | null>;
}

export interface ActionDispatchResult {
  kind: ActionKind;
  messageKey?: string;
  refresh?: boolean;
}

export function normalizeActionName(name: string | undefined | null): string {
  return (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
}

export function isDataFlowActionUrl(url: string | undefined | null): boolean {
  if (url == null) {
    return false;
  }
  const u = url.trim().toLowerCase().replace(/\\/g, "/");
  if (!u) {
    return false;
  }
  return DATA_FLOW_PATH_MARKERS.some((m) => u.includes(m));
}

export function isContentEditorActionUrl(url: string | undefined | null): boolean {
  if (url == null) {
    return false;
  }
  const u = url.trim().toLowerCase().replace(/\\/g, "/");
  if (!u) {
    return false;
  }
  return CONTENT_EDITOR_PATH_MARKERS.some((m) => u.includes(m));
}

export function isAssemblerPreviewUrl(url: string | undefined | null): boolean {
  if (url == null) {
    return false;
  }
  const u = url.trim().toLowerCase().replace(/\\/g, "/");
  return u.includes("assembler/render") || u.includes("previewslotvariant");
}

export function classifyAction(action: MenuAction): ActionKind {
  const name = normalizeActionName(action.name);
  if (parseWorkflowTransitionTrigger(action.name) != null) {
    return "workflow";
  }
  if (P1_PANEL_NAMES.has(name) || name === "copy_url_to_clipboard") {
    return "client";
  }
  if (P1_REST_NAMES.has(name)) {
    return "rest";
  }
  if (EDITOR_NAMES.has(name) || isContentEditorActionUrl(action.url)) {
    return "editor";
  }
  if (AA_PREVIEW_PARENT_NAMES.has(name)) {
    return "rest";
  }
  if (isSlotActionName(name)) {
    return "rest";
  }
  if (AA_UNAVAILABLE_NAMES.has(name)) {
    return "unavailable";
  }
  if (name === "lifecycle_analysis") {
    return "legacy-file";
  }
  if (isAssemblerPreviewUrl(action.url) || PREVIEW_PARENT_NAMES.has(name)) {
    return "rest";
  }
  if (name === "purge" || name === "publish_now") {
    return "rest";
  }
  if (name === "create_new_item" || isNewItemHostName(action.parentName)) {
    return "rest";
  }
  if (isDataFlowActionUrl(action.url)) {
    return "unavailable";
  }
  if (name === "open" || name === "refresh" || name === "delete") {
    return "client";
  }
  if (action.url && !isDataFlowActionUrl(action.url) && !isAssemblerPreviewUrl(action.url)) {
    return "legacy-file";
  }
  return "client";
}

/** Walk a menu tree and return the parent name that owns {@code childName}. */
export function findMenuParentName(
  actions: MenuAction[],
  childName: string,
): string | undefined {
  for (const action of actions) {
    if (action.children?.some((c) => c.name === childName)) {
      return action.name;
    }
    const nested = findMenuParentName(action.children ?? [], childName);
    if (nested) {
      return nested;
    }
  }
  return undefined;
}

export function isNewItemAction(
  action: MenuAction,
  parentName?: string,
): boolean {
  const name = normalizeActionName(action.name);
  if (name === "create_new_item") {
    return true;
  }
  return isNewItemHostName(parentName) || isNewItemHostName(action.parentName);
}

export function isAaPreviewAction(
  actionName: string | undefined,
  parentName?: string,
): boolean {
  return (
    AA_PREVIEW_PARENT_NAMES.has(normalizeActionName(actionName)) ||
    AA_PREVIEW_PARENT_NAMES.has(normalizeActionName(parentName))
  );
}

export function parseTemplateIdFromAction(action: MenuAction): number | null {
  const params = action.parameters ?? [];
  for (const p of params) {
    const n = (p.name ?? "").toLowerCase();
    if (n === "sys_template" || n === "templateid" || n === "sys_variantid") {
      const id = Number(p.value);
      if (Number.isFinite(id) && id > 0) {
        return id;
      }
    }
  }
  const url = action.url ?? "";
  try {
    const q = new URL(url, "http://localhost/").searchParams;
    const raw = q.get("sys_template") ?? q.get("templateId") ?? q.get("sys_variantid");
    const id = raw != null ? Number(raw) : NaN;
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  } catch {
    /* ignore */
  }
  return null;
}

/** CMS path or site-preview URL suitable for Copy URL to Clipboard. */
export function resolveCopyableItemUrl(item: PSPathItem): string {
  const site = buildSitePathPreviewUrl(item.path);
  if (site) {
    return site;
  }
  return normalizeCmsPath(item.path);
}

function resolvePreviewHref(previewUrl: string): string {
  const trimmed = previewUrl.trim();
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  const path = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  if (typeof window === "undefined") {
    return path;
  }
  const locPath = window.location?.pathname ?? "";
  if (
    (locPath === "/Rhythmyx" || locPath.startsWith("/Rhythmyx/")) &&
    !path.startsWith("/Rhythmyx/")
  ) {
    return `/Rhythmyx${path}`;
  }
  return path;
}

/**
 * Permanent purge for a page or asset. Other types return false so the
 * dispatcher can show {@link EXPLORER_MSG.ACTION_UNAVAILABLE}.
 */
export async function purgeSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePreviewKind(item);
  if (kind === "page") {
    await del<void>(`${PATHS.PAGE_PURGE}/${encodeURIComponent(id)}`);
    return true;
  }
  if (kind === "asset") {
    await del<void>(`${PATHS.ASSET_PURGE}/${encodeURIComponent(id)}`);
    return true;
  }
  return false;
}

function defaultOpenWindow(
  url: string,
  target?: string,
  features?: string,
): Window | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.open(url, target ?? "_blank", features ?? "noopener,noreferrer");
}

export async function dispatchAction(
  action: MenuAction,
  ctx: ActionDispatchContext,
): Promise<ActionDispatchResult> {
  const kind = classifyAction(action);
  const item = ctx.item;

  if (kind === "workflow") {
    const trigger = parseWorkflowTransitionTrigger(action.name);
    if (trigger == null || !item?.id) {
      return { kind, messageKey: EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED };
    }
    if (ctx.runWorkflow) {
      await ctx.runWorkflow(String(item.id), trigger);
    }
    return { kind, refresh: true };
  }

  if (isNewItemAction(action, ctx.parentName)) {
    const typeName = normalizeActionName(action.name);
    if (typeName === "create_new_item" || isNewItemHostName(action.name)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TYPE };
    }
    const folder = resolveFolderPathFromSelection(
      ctx.folderPath,
      item?.path,
      item?.type,
    );
    if (!folder) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_FOLDER };
    }
    let templateId: string | undefined;
    if (isExplorerPageType(action.name)) {
      const load = ctx.loadPageTemplates ?? loadPageTemplates;
      const templates = await load(folder, action.name);
      if (templates.length === 0) {
        return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TEMPLATE };
      }
      templateId = templates[0]?.id;
      if (templates.length > 1) {
        if (!ctx.pickPageTemplate) {
          return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TEMPLATE };
        }
        const picked = await ctx.pickPageTemplate(templates);
        if (!picked) {
          return { kind: "rest" };
        }
        templateId = picked;
      }
    }
    const create = ctx.createItem ?? createEditorItem;
    const created = await create({
      contentType: action.name,
      folderPath: folder,
      templateId,
    });
    const contentId = parseExplorerContentId(created.itemId);
    if (contentId == null) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
    }
    const open = ctx.openWindow ?? defaultOpenWindow;
    open(
      buildEditorHostUrl(contentId, "edit"),
      editorWindowName(contentId),
      EDITOR_WINDOW_FEATURES,
    );
    return { kind: "rest", refresh: true };
  }

  if (kind === "editor") {
    const editorName = normalizeActionName(action.name);
    if (!EDITOR_NAMES.has(editorName)) {
      return { kind, messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
    }
    if (!item || isFolder(item)) {
      return { kind, messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const contentId = parseExplorerContentId(item.id);
    if (contentId == null) {
      return { kind, messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
    }
    const view =
      editorName.startsWith("view_") || editorName.startsWith("revision_view");
    const href = buildEditorHostUrl(contentId, view ? "view" : "edit");
    const open = ctx.openWindow ?? defaultOpenWindow;
    open(href, editorWindowName(contentId), EDITOR_WINDOW_FEATURES);
    return { kind };
  }

  if (isSlotActionName(normalizeActionName(action.name))) {
    return dispatchSlotAction(action, ctx);
  }

  if (kind === "unavailable") {
    return { kind, messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
  }

  if (kind === "legacy-file" && action.url) {
    const base =
      typeof window !== "undefined" ? window.location.href : "http://localhost/";
    if (isDataFlowActionUrl(action.url) || isAssemblerPreviewUrl(action.url)) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    const classified = classifyUrl(action.url, base);
    if (isContentEditorActionUrl(action.url)) {
      return { kind: "editor", messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
    }
    if (classified.ok) {
      safeNavigate(action.url, base);
    }
    return { kind };
  }

  const name = normalizeActionName(action.name);

  if (name === "open" && item && ctx.onOpen) {
    ctx.onOpen(item);
    return { kind: "client" };
  }

  if (name === "translate") {
    if (!item || isFolder(item) || parseExplorerContentId(item.id) == null) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    ctx.onShowTranslations?.();
    return { kind: "client" };
  }

  if (name === "item_viewdependents") {
    if (!item || isFolder(item) || parseExplorerContentId(item.id) == null) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    ctx.onShowDependencies?.();
    return { kind: "client" };
  }

  if (name === "workflow_revisions") {
    ctx.onShowRevisions?.("revisions");
    if (!item || isFolder(item) || parseExplorerContentId(item.id) == null) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    return { kind: "client" };
  }

  if (name === "workflow_audittrail") {
    ctx.onShowRevisions?.("audit");
    if (!item || isFolder(item) || parseExplorerContentId(item.id) == null) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    return { kind: "client" };
  }

  if (name === "flush_cache") {
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_FLUSH_CACHE,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.flushCache) {
      await ctx.flushCache();
    } else {
      await flushAssemblerCache();
    }
    return { kind: "rest" };
  }

  if (name === "navreset") {
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_NAV_RESET,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.resetNav) {
      await ctx.resetNav();
    } else {
      await resetNavigation();
    }
    return { kind: "rest" };
  }

  if (name === "workflow_newversion") {
    if (!item || isFolder(item) || !item.id) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_NEW_COPY,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.createCopy) {
      await ctx.createCopy(String(item.id));
    } else {
      await createNewCopy(String(item.id));
    }
    return { kind: "rest", refresh: true };
  }

  if (name === "edit_promotableversion") {
    if (!item || isFolder(item) || !item.id) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_PROMOTABLE,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.createPromotable) {
      await ctx.createPromotable(String(item.id));
    } else {
      await createPromotableVersion(String(item.id));
    }
    return { kind: "rest", refresh: true };
  }

  if (name === "copy_url_to_clipboard") {
    if (!item || isFolder(item)) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const url = resolveCopyableItemUrl(item);
    if (!url) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_COPY_URL_EMPTY };
    }
    const write =
      ctx.writeClipboard ??
      (async (text: string) => {
        if (typeof navigator === "undefined" || !navigator.clipboard) {
          throw new Error("clipboard");
        }
        await navigator.clipboard.writeText(text);
      });
    try {
      await write(url);
    } catch {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_COPY_URL_FAILED };
    }
    return { kind: "client" };
  }

  if (name === "purge") {
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      // TMX key string is the confirm body (message() applied by caller if needed)
      EXPLORER_MSG.CONFIRM_PURGE_BODY,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.onPurge) {
      await ctx.onPurge(item);
      return { kind: "rest", refresh: true };
    }
    const purged = await purgeSelectedItem(item);
    if (!purged) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    return { kind: "rest", refresh: true };
  }

  if (name === "publish_now") {
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_PUBLISH_NOW,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.onPublish) {
      await ctx.onPublish(item);
      return { kind: "rest", refresh: true };
    }
    const published = await publishSelectedItem(item);
    if (!published) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    return { kind: "rest", refresh: true };
  }

  if (isAaPreviewAction(action.name, ctx.parentName)) {
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const contentId = parseExplorerContentId(item.id);
    if (contentId == null) {
      return { kind: "rest", messageKey: EXPLORER_MSG.PREVIEW_UNAVAILABLE };
    }
    const templateId = parseTemplateIdFromAction(action);
    const href = buildAssemblyHostUrl(contentId, templateId);
    const open = ctx.openWindow ?? defaultOpenWindow;
    open(href, assemblyWindowName(contentId), ASSEMBLY_WINDOW_FEATURES);
    return { kind: "rest" };
  }

  const templateId = parseTemplateIdFromAction(action);
  if (templateId != null && item && !isFolder(item)) {
    const contentId = parseExplorerContentId(item.id);
    if (contentId == null) {
      return { kind: "rest", messageKey: EXPLORER_MSG.PREVIEW_UNAVAILABLE };
    }
    const fetchLoc = ctx.fetchPreview ?? fetchPreviewLocation;
    const loc = await fetchLoc(contentId, templateId);
    const href = resolvePreviewHref(loc.previewUrl);
    if (!href.toLowerCase().includes("/assembler/render")) {
      return { kind: "rest", messageKey: EXPLORER_MSG.PREVIEW_UNAVAILABLE };
    }
    const open = ctx.openWindow ?? defaultOpenWindow;
    open(href, `percTemplatePreview_${contentId}`);
    return { kind: "rest" };
  }

  if (PREVIEW_PARENT_NAMES.has(name) || name === "preview") {
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.PREVIEW_UNAVAILABLE };
    }
    const preview = ctx.onPreview ?? openPreviewItem;
    await Promise.resolve(preview(item));
    return { kind: "rest" };
  }

  if (isDataFlowActionUrl(action.url) || isAssemblerPreviewUrl(action.url)) {
    return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
  }

  return { kind: "client" };
}

async function dispatchSlotAction(
  action: MenuAction,
  ctx: ActionDispatchContext,
): Promise<ActionDispatchResult> {
  const name = normalizeActionName(action.name);
  const slot = ctx.slot;
  if (name === "arrange") {
    return { kind: "rest" };
  }
  if (!slotContextHasSlot(slot)) {
    return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_SLOT };
  }

  const add = ctx.addToSlot ?? addSlotRelationship;
  const remove = ctx.removeSlotRel ?? removeSlotRelationship;
  const move = ctx.moveSlotRel ?? moveSlotRelationship;
  const change = ctx.changeSlotTemplate ?? changeSlotTemplateSlot;

  if (name === "slot_add" || name === "paste_as_link_to_slot") {
    let dependentId: number | null = null;
    let snippetTemplateId: number | null = null;
    let folderId: number | undefined;
    if (name === "paste_as_link_to_slot" && ctx.item && !isFolder(ctx.item)) {
      dependentId = parseExplorerContentId(ctx.item.id);
      const templates = await fetchSlotAllowedTemplates(slot.slotId);
      snippetTemplateId = templates[0]?.id ?? parseTemplateIdFromAction(action);
    } else if (ctx.pickSlotDependent) {
      const picked = await ctx.pickSlotDependent(slot);
      if (!picked) {
        return { kind: "rest" };
      }
      dependentId = picked.contentId;
      snippetTemplateId = picked.templateId;
      folderId = picked.folderId;
    }
    if (dependentId == null || dependentId <= 0 || snippetTemplateId == null || snippetTemplateId <= 0) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TEMPLATE };
    }
    await add({
      ownerId: slot.ownerId,
      dependentId,
      slotId: slot.slotId,
      templateId: snippetTemplateId,
      folderId,
    });
    return { kind: "rest", refresh: true };
  }

  if (name === "slot_create") {
    if (!ctx.pickSlotCreate) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_SLOT };
    }
    const picked = await ctx.pickSlotCreate(slot);
    if (!picked) {
      return { kind: "rest" };
    }
    const create = ctx.createItem ?? createEditorItem;
    const created = await create({
      contentType: picked.contentType,
      folderPath: picked.folderPath,
      templateId: picked.templateId,
    });
    const contentId = parseExplorerContentId(created.itemId);
    if (contentId == null) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
    }
    await add({
      ownerId: slot.ownerId,
      dependentId: contentId,
      slotId: slot.slotId,
      templateId: picked.snippetTemplateId,
    });
    const open = ctx.openWindow ?? defaultOpenWindow;
    open(
      buildEditorHostUrl(contentId, "edit"),
      editorWindowName(contentId),
      EDITOR_WINDOW_FEATURES,
    );
    return { kind: "rest", refresh: true };
  }

  if (
    name === "arrange_moveupleft" ||
    name === "arrange_movedownright" ||
    name === "arrange_remove" ||
    name === "arrange_changetemplateslot" ||
    name === "change_template" ||
    name === "move_to_slot"
  ) {
    if (!slotContextHasRelationship(slot)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_RELATIONSHIP };
    }
    const relationshipId = slot.relationshipId as number;
    if (name === "arrange_remove") {
      await remove(relationshipId);
      return { kind: "rest", refresh: true };
    }
    if (name === "arrange_moveupleft") {
      await move(relationshipId, "UP");
      return { kind: "rest", refresh: true };
    }
    if (name === "arrange_movedownright") {
      await move(relationshipId, "DOWN");
      return { kind: "rest", refresh: true };
    }
    let nextSlot = slot.slotId;
    let nextTemplate =
      parseTemplateIdFromAction(action) ?? slot.snippetTemplateId ?? 0;
    if (ctx.pickSlotTemplateSlot) {
      const picked = await ctx.pickSlotTemplateSlot(slot);
      if (!picked) {
        return { kind: "rest" };
      }
      nextSlot = picked.slotId;
      nextTemplate = picked.templateId;
    }
    if (nextSlot <= 0 || nextTemplate <= 0) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TEMPLATE };
    }
    await change(relationshipId, nextSlot, nextTemplate);
    return { kind: "rest", refresh: true };
  }

  return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
}
