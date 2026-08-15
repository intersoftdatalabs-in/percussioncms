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

import { fetchPreviewLocation } from "../api/contentExplorer/assemblyApi";
import { del } from "../api/client";
import { PATHS } from "../api/paths";
import type { MenuAction, PSPathItem } from "../api/contentExplorer/types";
import { classifyUrl, safeNavigate } from "../util/safeNavigate";
import { parseExplorerContentId } from "./menuCatalogLoad";
import { EXPLORER_MSG } from "./messages";
import {
  buildSitePathPreviewUrl,
  normalizeCmsPath,
  openPreviewItem,
  resolvePreviewKind,
} from "./previewItem";
import { isFolder } from "./selection";
import { parseWorkflowTransitionTrigger } from "./workflowMenuActions";

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

const AA_UNAVAILABLE_NAMES = new Set([
  "item_activeassembly",
  "enterpriseitem_activeassembly",
  "corporateitem_activeassembly",
  "item_assembly",
  "aa_table_editor",
  "slot_add",
  "slot_create",
  "arrange",
  "arrange_moveupleft",
  "arrange_movedownright",
  "arrange_changetemplateslot",
  "arrange_remove",
  "change_template",
  "paste_as_link_to_slot",
  "move_to_slot",
]);

const P1_PANEL_NAMES = new Set(["translate", "item_viewdependents"]);

const P1_UNAVAILABLE_NAMES = new Set([
  "flush_cache",
  "navreset",
  "workflow_newversion",
  "edit_promotableversion",
  "workflow_revisions",
  "workflow_audittrail",
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
  writeClipboard?: (text: string) => Promise<void>;
  confirm?: (body: string) => boolean;
  openWindow?: (url: string, target?: string, features?: string) => Window | null;
  fetchPreview?: typeof fetchPreviewLocation;
  runWorkflow?: (itemId: string, trigger: string) => Promise<void>;
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
  if (EDITOR_NAMES.has(name) || isContentEditorActionUrl(action.url)) {
    return "editor";
  }
  if (AA_UNAVAILABLE_NAMES.has(name) || P1_UNAVAILABLE_NAMES.has(name)) {
    return "unavailable";
  }
  if (P1_PANEL_NAMES.has(name) || name === "copy_url_to_clipboard") {
    return "client";
  }
  if (name === "lifecycle_analysis") {
    return "legacy-file";
  }
  if (isAssemblerPreviewUrl(action.url) || PREVIEW_PARENT_NAMES.has(name)) {
    return "rest";
  }
  if (name === "purge") {
    return "rest";
  }
  if (name === "publish_now" || name === "create_new_item") {
    return "unavailable";
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

  if (kind === "editor") {
    return { kind, messageKey: EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE };
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

  if (name === "publish_now" || name === "create_new_item") {
    return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
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
