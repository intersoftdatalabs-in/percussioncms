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
import { del, isApiError } from "../api/client";
import { PATHS } from "../api/paths";
import {
  checkInItem,
  checkOutItem,
  forceCheckInItem,
} from "../api/contentExplorer/itemWorkflowApi";
import {
  formatTakedownConfirmBody,
  isCheckinActionName,
  isCheckoutActionName,
  isForceCheckinActionName,
  isPublishingHistoryActionName,
  isRemoveFromStagingActionName,
  isStageActionName,
  isTakedownActionName,
  loadLinkedPagesForTakedown,
  publishSelectedItem,
  publishSelectedItems,
  describePublishBatch,
  removeFromStagingSelectedItem,
  removeFromStagingSelectedItems,
  resolvePublishKind,
  describeRemoveFromStagingBatch,
  describeStageBatch,
  describeTakedownBatch,
  partitionStageSelection,
  stageSelectedItem,
  stageSelectedItems,
  type PublishBatchResult,
  type RemoveFromStagingBatchResult,
  takedownSelectedItem,
  takedownSelectedItems,
  type StageBatchResult,
  type TakedownBatchResult,
} from "./itemPublish";
import {
  formatScheduleBatchFailure,
  getItemScheduleDates,
  isScheduleActionName,
  publishableScheduleTargets,
  scheduleSelectedItems,
  type ItemScheduleDates,
  type ScheduleBatchResult,
  type ScheduleItemFailure,
} from "./itemScheduleDates";
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
  contentTypeChoicesFromActions,
  isNewItemHostActionName,
  loadAllowedContentTypes,
  type ContentTypeChoice,
} from "./contentTypeChoices";
import {
  isNewItemHostName,
  parseExplorerContentId,
} from "./menuCatalogLoad";
import { resolveFolderPathFromSelection } from "./folderPath";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import {
  buildSitePathPreviewUrl,
  normalizeCmsPath,
  openPreviewItem,
  resolvePreviewKind,
} from "./previewItem";
import { isFolder } from "./selection";
import { parseWorkflowTransitionTrigger } from "./workflowMenuActions";
import { isWorkflowEligibleItem } from "./workflowEligibility";
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
  "quick_edit",
  "view_content",
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
  "publishing_history",
  "item_publishing_history",
  "pubhistory",
  "publish_history",
  "edit_properties",
  "view_properties",
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
  onShowItemProperties?: (readOnly: boolean) => void;
  onShowDependencies?: () => void;
  onShowRevisions?: (tab: "revisions" | "audit") => void;
  onShowPublishingHistory?: (item: PSPathItem) => void;
  flushCache?: () => Promise<void>;
  resetNav?: () => Promise<void>;
  createCopy?: (itemId: string) => Promise<void>;
  createPromotable?: (itemId: string) => Promise<void>;
  createItem?: typeof createEditorItem;
  loadPageTemplates?: typeof loadPageTemplates;
  pickPageTemplate?: (
    templates: PageTemplateChoice[],
  ) => Promise<string | null>;
  loadContentTypes?: () => Promise<ContentTypeChoice[]>;
  pickContentType?: (
    types: ContentTypeChoice[],
  ) => Promise<string | null>;
  onPublish?: (item: PSPathItem) => Promise<void>;
  onTakedown?: (item: PSPathItem) => Promise<void>;
  onStage?: (item: PSPathItem) => Promise<void>;
  /**
   * Checkbox multi-selection. When length is 2 or more, Publish now,
   * Stage, Take Down, Remove from Staging, Check Out, and workflow
   * transitions use one confirm for every eligible page/asset and skip
   * folders.
   */
  selectedItems?: readonly PSPathItem[];
  onRemoveFromStaging?: (item: PSPathItem) => Promise<void>;
  onSchedule?: (item: PSPathItem, dates: ItemScheduleDates) => Promise<void>;
  onForceCheckin?: (item: PSPathItem) => Promise<void>;
  pickScheduleDates?: (
    item: PSPathItem,
    current: ItemScheduleDates,
    options?: { applyCount: number },
  ) => Promise<ItemScheduleDates | null>;
  /** Parent menu name when the user activated a child (AA vs Preview). */
  parentName?: string;
  writeClipboard?: (text: string) => Promise<void>;
  confirm?: (body: string) => boolean;
  openWindow?: (url: string, target?: string, features?: string) => Window | null;
  fetchPreview?: typeof fetchPreviewLocation;
  runWorkflow?: (
    itemId: string,
    trigger: string,
    comment?: string,
  ) => Promise<void>;
  /**
   * Comment collector for comment-required workflow triggers. Return null or
   * blank to cancel. Defaults to {@code window.prompt}.
   */
  promptWorkflowComment?: (trigger: string) => string | null;
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
  /** Already-resolved operator text (batch stage details). Prefer over messageKey. */
  messageText?: string;
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
  if (
    name === "purge" ||
    name === "publish_now" ||
    isTakedownActionName(name) ||
    isStageActionName(name) ||
    isRemoveFromStagingActionName(name) ||
    isScheduleActionName(name) ||
    isForceCheckinActionName(name) ||
    isCheckoutActionName(name) ||
    isCheckinActionName(name)
  ) {
    return "rest";
  }
  if (
    name === "create_new_item" ||
    isNewItemHostActionName(action.name) ||
    isNewItemHostName(action.parentName)
  ) {
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
  if (isNewItemHostActionName(action.name)) {
    return true;
  }
  return isNewItemHostName(parentName) || isNewItemHostName(action.parentName);
}

async function resolveNewItemContentType(
  action: MenuAction,
  ctx: ActionDispatchContext,
): Promise<{ contentType?: string; messageKey?: string; cancelled?: boolean }> {
  if (!isNewItemHostActionName(action.name)) {
    return { contentType: action.name };
  }
  const fromChildren = contentTypeChoicesFromActions(action.children);
  const load = ctx.loadContentTypes ?? loadAllowedContentTypes;
  const types =
    fromChildren.length > 0 ? fromChildren : await load();
  if (types.length === 0) {
    return { messageKey: EXPLORER_MSG.ACTION_NEEDS_TYPE };
  }
  if (types.length === 1) {
    return { contentType: types[0].name };
  }
  if (!ctx.pickContentType) {
    return { messageKey: EXPLORER_MSG.ACTION_NEEDS_TYPE };
  }
  const picked = await ctx.pickContentType(types);
  if (!picked) {
    return { cancelled: true };
  }
  return { contentType: picked };
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

interface CheckoutBatchFailure {
  name: string;
  status?: number;
  message: string;
}

function checkoutItemLabel(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name) {
    return name;
  }
  const id = (item.id ?? "").trim();
  return id || "item";
}

function describeCheckoutBatch(result: {
  skippedFolders: string[];
  skippedOther: string[];
  failures: CheckoutBatchFailure[];
}): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      message(EXPLORER_MSG.CHECKOUT_SKIPPED_FOLDERS)
        .split("{names}")
        .join(result.skippedFolders.join(", ")),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      message(EXPLORER_MSG.CHECKOUT_SKIPPED_OTHER)
        .split("{names}")
        .join(result.skippedOther.join(", ")),
    );
  }
  if (result.failures.length > 0) {
    const detail = result.failures
      .map((failure) =>
        failure.status != null
          ? `${failure.name} (HTTP ${failure.status})`
          : `${failure.name} (${failure.message})`,
      )
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.CHECKOUT_BATCH_INCOMPLETE)
        .split("{detail}")
        .join(detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

/**
 * Check out every eligible page/asset. Folders are skipped. A 403 or 409
 * on one item is named and the rest of the selection still runs.
 */
async function checkoutMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeCheckoutBatch({
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.CHECKOUT_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.CHECKOUT_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_CHECKOUT_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const checkedOutIds: string[] = [];
  const failures: CheckoutBatchFailure[] = [];
  for (const row of plan.eligible) {
    const id = (row.id ?? "").trim();
    try {
      await checkOutItem(id);
      checkedOutIds.push(id);
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "checkout failed";
      failures.push({
        name: checkoutItemLabel(row),
        status,
        message: text || "checkout failed",
      });
    }
  }
  const messageText = describeCheckoutBatch({
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  });
  const incomplete = failures.length > 0;
  return {
    kind: "rest",
    refresh: checkedOutIds.length > 0,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.CHECKOUT_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.CHECKOUT_SKIPPED_FOLDERS
        : undefined,
  };
}

/**
 * Check in every eligible page/asset. Folders are skipped. An HTTP failure
 * on one item is named and the rest of the selection still runs. Cancel
 * checks in nothing (#4872).
 */
async function checkinMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeCheckinBatch({
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.CHECKIN_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.CHECKIN_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_CHECKIN_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const checkedInIds: string[] = [];
  const failures: CheckoutBatchFailure[] = [];
  for (const row of plan.eligible) {
    const id = (row.id ?? "").trim();
    try {
      await checkInItem(id);
      checkedInIds.push(id);
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "check-in failed";
      failures.push({
        name: checkoutItemLabel(row),
        status,
        message: text || "check-in failed",
      });
    }
  }
  const messageText = describeCheckinBatch({
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  });
  const incomplete = failures.length > 0;
  return {
    kind: "rest",
    refresh: checkedInIds.length > 0,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.CHECKIN_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.CHECKIN_SKIPPED_FOLDERS
        : undefined,
  };
}

function describeCheckinBatch(result: {
  skippedFolders: string[];
  skippedOther: string[];
  failures: CheckoutBatchFailure[];
}): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      message(EXPLORER_MSG.CHECKIN_SKIPPED_FOLDERS)
        .split("{names}")
        .join(result.skippedFolders.join(", ")),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      message(EXPLORER_MSG.CHECKIN_SKIPPED_OTHER)
        .split("{names}")
        .join(result.skippedOther.join(", ")),
    );
  }
  if (result.failures.length > 0) {
    const detail = result.failures
      .map((failure) =>
        failure.status != null
          ? `${failure.name} (HTTP ${failure.status})`
          : `${failure.name} (${failure.message})`,
      )
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.CHECKIN_BATCH_INCOMPLETE)
        .split("{detail}")
        .join(detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

async function publishMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describePublishBatch({
      publishedIds: [],
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.PUBLISH_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.PUBLISH_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_PUBLISH_NOW_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const result = await runPublishBatch(items, ctx.onPublish);
  const messageText = describePublishBatch(result);
  const anyPublished = result.publishedIds.length > 0;
  const incomplete = result.failures.length > 0;
  return {
    kind: "rest",
    refresh: anyPublished,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.PUBLISH_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.PUBLISH_SKIPPED_FOLDERS
        : undefined,
  };
}

async function runPublishBatch(
  items: readonly PSPathItem[],
  onPublish: ActionDispatchContext["onPublish"],
): Promise<PublishBatchResult> {
  if (!onPublish) {
    return publishSelectedItems(items);
  }
  const plan = partitionStageSelection(items);
  const publishedIds: string[] = [];
  const failures: PublishBatchResult["failures"] = [];
  for (const item of plan.eligible) {
    try {
      await onPublish(item);
      publishedIds.push((item.id ?? "").trim());
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "publish failed";
      failures.push({
        id: (item.id ?? "").trim(),
        name: (item.name ?? item.id ?? "item").trim() || "item",
        status,
        message: text || "publish failed",
      });
    }
  }
  return {
    publishedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

async function takedownMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeTakedownBatch({
      takenDownIds: [],
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.TAKEDOWN_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.TAKEDOWN_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_TAKEDOWN_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const result = await runTakedownBatch(items, ctx.onTakedown);
  const messageText = describeTakedownBatch(result);
  const anyTakenDown = result.takenDownIds.length > 0;
  const incomplete = result.failures.length > 0;
  return {
    kind: "rest",
    refresh: anyTakenDown,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.TAKEDOWN_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.TAKEDOWN_SKIPPED_FOLDERS
        : undefined,
  };
}

async function runTakedownBatch(
  items: readonly PSPathItem[],
  onTakedown: ActionDispatchContext["onTakedown"],
): Promise<TakedownBatchResult> {
  if (!onTakedown) {
    return takedownSelectedItems(items);
  }
  const plan = partitionStageSelection(items);
  const takenDownIds: string[] = [];
  const failures: TakedownBatchResult["failures"] = [];
  for (const item of plan.eligible) {
    try {
      await onTakedown(item);
      takenDownIds.push((item.id ?? "").trim());
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "takedown failed";
      failures.push({
        id: (item.id ?? "").trim(),
        name: (item.name ?? item.id ?? "item").trim() || "item",
        status,
        message: text || "takedown failed",
      });
    }
  }
  return {
    takenDownIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

async function stageMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeStageBatch({
      stagedIds: [],
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.STAGE_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.STAGE_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_STAGE_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const result = await runStageBatch(items, ctx.onStage);
  const messageText = describeStageBatch(result);
  const anyStaged = result.stagedIds.length > 0;
  const incomplete = result.failures.length > 0;
  return {
    kind: "rest",
    refresh: anyStaged,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.STAGE_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.STAGE_SKIPPED_FOLDERS
        : undefined,
  };
}

async function runStageBatch(
  items: readonly PSPathItem[],
  onStage: ActionDispatchContext["onStage"],
): Promise<StageBatchResult> {
  if (!onStage) {
    return stageSelectedItems(items);
  }
  const plan = partitionStageSelection(items);
  const stagedIds: string[] = [];
  const failures: StageBatchResult["failures"] = [];
  for (const item of plan.eligible) {
    try {
      await onStage(item);
      stagedIds.push((item.id ?? "").trim());
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "stage failed";
      failures.push({
        id: (item.id ?? "").trim(),
        name: (item.name ?? item.id ?? "item").trim() || "item",
        status,
        message: text || "stage failed",
      });
    }
  }
  return {
    stagedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

async function removeFromStagingMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
): Promise<ActionDispatchResult> {
  const plan = partitionStageSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeRemoveFromStagingBatch({
      removedIds: [],
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "rest",
      messageKey: EXPLORER_MSG.UNSTAGE_NOTHING_ELIGIBLE,
      messageText:
        noted ?? message(EXPLORER_MSG.UNSTAGE_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_REMOVE_FROM_STAGING_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "rest" };
  }
  const result = await runRemoveFromStagingBatch(items, ctx.onRemoveFromStaging);
  const messageText = describeRemoveFromStagingBatch(result);
  const anyRemoved = result.removedIds.length > 0;
  const incomplete = result.failures.length > 0;
  return {
    kind: "rest",
    refresh: anyRemoved,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.UNSTAGE_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.UNSTAGE_SKIPPED_FOLDERS
        : undefined,
  };
}

async function runRemoveFromStagingBatch(
  items: readonly PSPathItem[],
  onRemove: ActionDispatchContext["onRemoveFromStaging"],
): Promise<RemoveFromStagingBatchResult> {
  if (!onRemove) {
    return removeFromStagingSelectedItems(items);
  }
  const plan = partitionStageSelection(items);
  const removedIds: string[] = [];
  const failures: RemoveFromStagingBatchResult["failures"] = [];
  for (const item of plan.eligible) {
    try {
      await onRemove(item);
      removedIds.push((item.id ?? "").trim());
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "remove from staging failed";
      failures.push({
        id: (item.id ?? "").trim(),
        name: (item.name ?? item.id ?? "item").trim() || "item",
        status,
        message: text || "remove from staging failed",
      });
    }
  }
  return {
    removedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

async function applyScheduleDates(
  targets: readonly PSPathItem[],
  dates: ItemScheduleDates,
  onSchedule?: (item: PSPathItem, dates: ItemScheduleDates) => Promise<void>,
): Promise<ScheduleBatchResult> {
  if (!onSchedule) {
    return scheduleSelectedItems(targets, dates);
  }
  const failures: ScheduleItemFailure[] = [];
  let saved = 0;
  for (const item of targets) {
    const id = (item.id ?? "").trim();
    try {
      await onSchedule(item, { ...dates, itemId: id });
      saved += 1;
    } catch (err: unknown) {
      const text = err instanceof Error ? err.message.trim() : "";
      failures.push({
        id,
        name: (item.name ?? "").trim() || id,
        message: text || "Schedule failed",
      });
    }
  }
  return { saved, skipped: 0, failures };
}

function workflowItemLabel(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name) return name;
  const path = (item.path ?? "").trim();
  if (path) return path;
  return (item.id ?? "").trim() || "item";
}

function fillWorkflow(key: string, token: string, value: string): string {
  return message(key).split(token).join(value);
}

interface WorkflowBatchFailure {
  name: string;
  status?: number;
  message: string;
}

function partitionWorkflowSelection(items: readonly PSPathItem[]): {
  eligible: PSPathItem[];
  skippedFolders: string[];
  skippedOther: string[];
} {
  const eligible: PSPathItem[] = [];
  const skippedFolders: string[] = [];
  const skippedOther: string[] = [];
  const seen = new Set<string>();
  for (const item of items) {
    const id = (item.id ?? "").trim();
    const key = id || `name:${workflowItemLabel(item)}`;
    if (seen.has(key)) continue;
    seen.add(key);
    if (isFolder(item)) {
      skippedFolders.push(workflowItemLabel(item));
      continue;
    }
    if (!isWorkflowEligibleItem(item)) {
      skippedOther.push(workflowItemLabel(item));
      continue;
    }
    eligible.push(item);
  }
  return { eligible, skippedFolders, skippedOther };
}

function describeWorkflowBatch(result: {
  skippedFolders: string[];
  skippedOther: string[];
  failures: WorkflowBatchFailure[];
}): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      fillWorkflow(
        EXPLORER_MSG.WORKFLOW_SKIPPED_FOLDERS,
        "{names}",
        result.skippedFolders.join(", "),
      ),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      fillWorkflow(
        EXPLORER_MSG.WORKFLOW_SKIPPED_OTHER,
        "{names}",
        result.skippedOther.join(", "),
      ),
    );
  }
  if (result.failures.length > 0) {
    const detail = result.failures
      .map((failure) =>
        failure.status != null
          ? `${failure.name} (HTTP ${failure.status})`
          : `${failure.name} (${failure.message})`,
      )
      .join("; ");
    parts.push(
      fillWorkflow(EXPLORER_MSG.WORKFLOW_BATCH_INCOMPLETE, "{detail}", detail),
    );
  }
  if (parts.length === 0) return undefined;
  return parts.join(" ");
}

function promptWorkflowComment(
  ctx: ActionDispatchContext,
  trigger: string,
): string | null {
  const prompt =
    ctx.promptWorkflowComment ??
    ((name: string) => {
      if (typeof window === "undefined" || typeof window.prompt !== "function") {
        return null;
      }
      return window.prompt(
        `${message(EXPLORER_MSG.WORKFLOW_COMMENT_PROMPT)} (${name})`,
      );
    });
  const entered = prompt(trigger);
  if (entered == null || String(entered).trim().length === 0) {
    return null;
  }
  return String(entered).trim();
}

async function transitionMultiSelection(
  ctx: ActionDispatchContext,
  items: readonly PSPathItem[],
  trigger: string,
  commentRequired: boolean,
): Promise<ActionDispatchResult> {
  const plan = partitionWorkflowSelection(items);
  if (plan.eligible.length === 0) {
    const noted = describeWorkflowBatch({
      skippedFolders: plan.skippedFolders,
      skippedOther: plan.skippedOther,
      failures: [],
    });
    return {
      kind: "workflow",
      messageKey: EXPLORER_MSG.WORKFLOW_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.WORKFLOW_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = fillWorkflow(
    fillWorkflow(EXPLORER_MSG.CONFIRM_WORKFLOW_MULTI, "{count}", String(plan.eligible.length)),
    "{trigger}",
    trigger,
  );
  const ok = (ctx.confirm ?? ((body) => window.confirm(body)))(confirmBody);
  if (!ok) {
    return { kind: "workflow" };
  }
  let comment: string | undefined;
  if (commentRequired) {
    const entered = promptWorkflowComment(ctx, trigger);
    if (entered == null) {
      return { kind: "workflow", messageKey: EXPLORER_MSG.WORKFLOW_COMMENT_REQUIRED };
    }
    comment = entered;
  }
  const transitionedIds: string[] = [];
  const failures: WorkflowBatchFailure[] = [];
  for (const row of plan.eligible) {
    const id = (row.id ?? "").trim();
    try {
      if (ctx.runWorkflow) {
        await ctx.runWorkflow(id, trigger, comment);
      }
      transitionedIds.push(id);
    } catch (err: unknown) {
      const status = isApiError(err) ? err.status : undefined;
      const text =
        err instanceof Error
          ? err.message
          : status != null
            ? `HTTP ${status}`
            : "transition failed";
      failures.push({
        name: workflowItemLabel(row),
        status,
        message: text || "transition failed",
      });
    }
  }
  const messageText = describeWorkflowBatch({
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  });
  const incomplete = failures.length > 0;
  return {
    kind: "workflow",
    refresh: transitionedIds.length > 0,
    messageText,
    messageKey: incomplete
      ? EXPLORER_MSG.WORKFLOW_BATCH_INCOMPLETE
      : messageText
        ? EXPLORER_MSG.WORKFLOW_SKIPPED_FOLDERS
        : undefined,
  };
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
    if (trigger == null) {
      return { kind, messageKey: EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED };
    }
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return transitionMultiSelection(ctx, multi, trigger, action.commentRequired === true);
    }
    if (!item?.id) {
      return { kind, messageKey: EXPLORER_MSG.WORKFLOW_TRANSITION_FAILED };
    }
    let comment: string | undefined;
    if (action.commentRequired === true) {
      const entered = promptWorkflowComment(ctx, trigger);
      if (entered == null) {
        return { kind, messageKey: EXPLORER_MSG.WORKFLOW_COMMENT_REQUIRED };
      }
      comment = entered;
    }
    try {
      if (ctx.runWorkflow) {
        await ctx.runWorkflow(String(item.id), trigger, comment);
      }
    } catch (err: unknown) {
      if (isApiError(err)) {
        if (err.status === 403) {
          return { kind, messageKey: EXPLORER_MSG.WORKFLOW_TRANSITION_FORBIDDEN };
        }
        if (err.status === 409) {
          return { kind, messageKey: EXPLORER_MSG.WORKFLOW_TRANSITION_CONFLICT };
        }
      }
      throw err;
    }
    return { kind, refresh: true };
  }

  if (isNewItemAction(action, ctx.parentName)) {
    const folder = resolveFolderPathFromSelection(
      ctx.folderPath,
      item?.path,
      item?.type,
    );
    if (!folder) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_FOLDER };
    }
    const resolved = await resolveNewItemContentType(action, ctx);
    if (resolved.cancelled) {
      return { kind: "rest" };
    }
    if (!resolved.contentType) {
      return {
        kind: "rest",
        messageKey: resolved.messageKey ?? EXPLORER_MSG.ACTION_NEEDS_TYPE,
      };
    }
    const contentType = resolved.contentType;
    let templateId: string | undefined;
    if (isExplorerPageType(contentType)) {
      const load = ctx.loadPageTemplates ?? loadPageTemplates;
      const templates = await load(folder, contentType);
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
      contentType,
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
    const promote = editorName === "revision_promote";
    const href = buildEditorHostUrl(
      contentId,
      promote ? "promote" : view ? "view" : "edit",
    );
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

  if (name === "edit_properties" || name === "view_properties") {
    if (!item || isFolder(item)) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    ctx.onShowItemProperties?.(name === "view_properties");
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

  if (isPublishingHistoryActionName(name)) {
    if (!item || isFolder(item)) {
      return { kind: "client", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    if (resolvePublishKind(item) === "none") {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    ctx.onShowPublishingHistory?.(item);
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
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return publishMultiSelection(ctx, multi);
    }
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

  if (isTakedownActionName(name)) {
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return takedownMultiSelection(ctx, multi);
    }
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const linked = await loadLinkedPagesForTakedown(item.id ?? "");
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      formatTakedownConfirmBody(linked),
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.onTakedown) {
      await ctx.onTakedown(item);
      return { kind: "rest", refresh: true };
    }
    const takenDown = await takedownSelectedItem(item, linked);
    if (!takenDown) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    return { kind: "rest", refresh: true };
  }

  if (isStageActionName(name)) {
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return stageMultiSelection(ctx, multi);
    }
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_STAGE,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.onStage) {
      await ctx.onStage(item);
      return { kind: "rest", refresh: true };
    }
    const staged = await stageSelectedItem(item);
    if (!staged) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    return { kind: "rest", refresh: true };
  }

  if (isRemoveFromStagingActionName(name)) {
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return removeFromStagingMultiSelection(ctx, multi);
    }
    if (!item || isFolder(item)) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_REMOVE_FROM_STAGING,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    if (ctx.onRemoveFromStaging) {
      await ctx.onRemoveFromStaging(item);
      return { kind: "rest", refresh: true };
    }
    const removed = await removeFromStagingSelectedItem(item);
    if (!removed) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    return { kind: "rest", refresh: true };
  }

  if (isScheduleActionName(name)) {
    const checked = ctx.selectedItems ?? [];
    const multi = checked.length >= 2;
    const targets = multi
      ? publishableScheduleTargets(checked)
      : item && resolvePublishKind(item) !== "none"
        ? [item]
        : [];
    if (targets.length === 0) {
      const onlyFolders =
        !multi && (!item || isFolder(item))
          ? true
          : multi &&
            checked.every((row) => isFolder(row) || !(row.id ?? "").trim());
      if (onlyFolders) {
        return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
      }
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    const seed = targets[0];
    const current = await getItemScheduleDates(seed.id ?? "");
    let picked: ItemScheduleDates | null = current;
    if (ctx.pickScheduleDates) {
      picked = await ctx.pickScheduleDates(seed, current, {
        applyCount: targets.length,
      });
      if (!picked) {
        return { kind: "rest" };
      }
    }
    const confirmKey =
      targets.length > 1
        ? EXPLORER_MSG.CONFIRM_SCHEDULE_MULTI
        : EXPLORER_MSG.CONFIRM_SCHEDULE;
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(confirmKey);
    if (!ok) {
      return { kind: "rest" };
    }
    const batch = await applyScheduleDates(targets, picked, ctx.onSchedule);
    if (batch.saved === 0 && batch.failures.length === 0) {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    if (batch.failures.length > 0) {
      return {
        kind: "rest",
        messageText: formatScheduleBatchFailure(batch),
        messageKey: EXPLORER_MSG.SCHEDULE_PARTIAL,
        refresh: batch.saved > 0,
      };
    }
    return { kind: "rest", refresh: true };
  }

  if (isCheckoutActionName(name)) {
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return checkoutMultiSelection(ctx, multi);
    }
    if (!item || isFolder(item) || !item.id) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    if (resolvePublishKind(item) === "none") {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    try {
      await checkOutItem(String(item.id));
    } catch (err: unknown) {
      if (isApiError(err)) {
        if (err.status === 403) {
          return { kind: "rest", messageKey: EXPLORER_MSG.CHECKOUT_FORBIDDEN };
        }
        if (err.status === 409 || err.status === 400) {
          return { kind: "rest", messageKey: EXPLORER_MSG.CHECKOUT_CONFLICT };
        }
      }
      throw err;
    }
    return { kind: "rest", refresh: true };
  }

  if (isCheckinActionName(name)) {
    const multi = ctx.selectedItems ?? [];
    if (multi.length >= 2) {
      return checkinMultiSelection(ctx, multi);
    }
    if (!item || isFolder(item) || !item.id) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    if (resolvePublishKind(item) === "none") {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    try {
      await checkInItem(String(item.id));
    } catch (err: unknown) {
      if (isApiError(err)) {
        if (err.status === 403) {
          return { kind: "rest", messageKey: EXPLORER_MSG.CHECKIN_FORBIDDEN };
        }
        if (err.status === 409 || err.status === 400) {
          return { kind: "rest", messageKey: EXPLORER_MSG.CHECKIN_CONFLICT };
        }
      }
      throw err;
    }
    return { kind: "rest", refresh: true };
  }

  if (isForceCheckinActionName(name)) {
    if (!item || isFolder(item) || !item.id) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_ITEM };
    }
    if (resolvePublishKind(item) === "none") {
      return { kind: "unavailable", messageKey: EXPLORER_MSG.ACTION_UNAVAILABLE };
    }
    const ok = (ctx.confirm ?? ((b) => window.confirm(b)))(
      EXPLORER_MSG.CONFIRM_FORCE_CHECKIN,
    );
    if (!ok) {
      return { kind: "rest" };
    }
    try {
      if (ctx.onForceCheckin) {
        await ctx.onForceCheckin(item);
      } else {
        await forceCheckInItem(String(item.id));
      }
    } catch (err: unknown) {
      if (isApiError(err)) {
        if (err.status === 403) {
          return { kind: "rest", messageKey: EXPLORER_MSG.FORCE_CHECKIN_FORBIDDEN };
        }
        if (err.status === 404) {
          return { kind: "rest", messageKey: EXPLORER_MSG.FORCE_CHECKIN_NOT_FOUND };
        }
        if (err.status === 409 || err.status === 400) {
          return {
            kind: "rest",
            messageKey: EXPLORER_MSG.FORCE_CHECKIN_NOT_CHECKED_OUT,
          };
        }
      }
      throw err;
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
    if (!picked.contentType.trim() || !picked.folderPath.trim()) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TYPE };
    }
    if (picked.snippetTemplateId <= 0) {
      return { kind: "rest", messageKey: EXPLORER_MSG.ACTION_NEEDS_TEMPLATE };
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
    const relationshipId = slot.relationshipId;
    if (name === "arrange_remove") {
      if (ctx.confirm && !ctx.confirm(EXPLORER_MSG.CONFIRM_SLOT_REMOVE)) {
        return { kind: "rest" };
      }
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
