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
 * Explorer Publish Now / Take Down / Stage — existing sitemanage GETs
 * (and PUT takedown when linked pages exist), not the demandpublishing
 * servlet. Staging uses {@code …/page|resource/staging/{id}} and
 * {@code …/takedown/page|resource/staging/{id}} (classic
 * PercItemPublisherService). Schedule dates live in
 * {@code itemScheduleDates} (getitemdates / setitemdates). Item
 * publishing history is {@code GET …/item/pubhistory/{id}} (classic
 * PercPublishingHistoryDialog / PublishingShell #4536). PublishingShell
 * site-workspace takedown (#4538) reuses {@link takedownSelectedItem} and
 * {@link loadLinkedPagesForTakedown} — do not invent a second contract.
 */

import { get, isApiError, put } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { asObjectArray } from "../api/jsonList";
import { itemPublishPaths } from "../publishing/itemPublishPaths";
import { mapPublishResponse } from "../publishing/publishActions";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { normalizeCmsPath } from "./previewItem";
import { isFolder } from "./selection";

/** Demand-publish target. Stricter than preview — no unknown-id fallback. */
export type PublishKind = "page" | "asset" | "none";

function typeToken(item: PSPathItem): string {
  return `${item.type ?? ""} ${item.category ?? ""}`.toLowerCase();
}

/**
 * Classify whether Explorer can demand-publish this selection.
 *
 * <p>Unlike {@code resolvePreviewKind}, templates, links, and other non-page /
 * non-asset items with an id stay {@code "none"}. Preview's permissive
 * fallback would send those to {@code /publish/page/{id}}.</p>
 */
export function resolvePublishKind(
  item: PSPathItem | null | undefined,
): PublishKind {
  if (!item || isFolder(item)) {
    return "none";
  }
  const token = typeToken(item);
  const pathLower = normalizeCmsPath(item.path).toLowerCase();

  if (
    token.includes("asset") ||
    pathLower.startsWith("/assets/") ||
    pathLower === "/assets"
  ) {
    return (item.id ?? "").trim() ? "asset" : "none";
  }

  if (
    token.includes("page") ||
    pathLower.startsWith("/sites/") ||
    pathLower === "/sites"
  ) {
    return (item.id ?? "").trim() ? "page" : "none";
  }

  return "none";
}

const TAKEDOWN_ACTION_KEYS: ReadonlySet<string> = new Set([
  "take_down",
  "takedown",
  "unpublish",
]);

const STAGE_ACTION_KEYS: ReadonlySet<string> = new Set(["stage"]);

const REMOVE_FROM_STAGING_ACTION_KEYS: ReadonlySet<string> = new Set([
  "remove_from_staging",
  "unstage",
]);

const PUBLISHING_HISTORY_ACTION_KEYS: ReadonlySet<string> = new Set([
  "publishing_history",
  "item_publishing_history",
  "pubhistory",
  "publish_history",
]);

const FORCE_CHECKIN_ACTION_KEYS: ReadonlySet<string> = new Set([
  "force_checkin",
  "forcecheckin",
]);

const CHECKOUT_ACTION_KEYS: ReadonlySet<string> = new Set([
  "check_out",
  "checkout",
]);

const CHECKIN_ACTION_KEYS: ReadonlySet<string> = new Set([
  "check_in",
  "checkin",
]);

function actionNameKey(name: string | undefined | null): string {
  return (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
}

/** Catalog / toolbar names for Explorer Take Down (Finder “Take Down”). */
export function isTakedownActionName(
  name: string | undefined | null,
): boolean {
  return TAKEDOWN_ACTION_KEYS.has(actionNameKey(name));
}

/** Catalog / toolbar names for Explorer Stage (Finder “Stage”). */
export function isStageActionName(name: string | undefined | null): boolean {
  return STAGE_ACTION_KEYS.has(actionNameKey(name));
}

/**
 * Catalog / toolbar names for Explorer Remove from Staging (Finder
 * “Remove from Staging”).
 */
export function isRemoveFromStagingActionName(
  name: string | undefined | null,
): boolean {
  return REMOVE_FROM_STAGING_ACTION_KEYS.has(actionNameKey(name));
}

/** Catalog / toolbar names for Explorer Admin force check-in (#4561). */
export function isForceCheckinActionName(
  name: string | undefined | null,
): boolean {
  return FORCE_CHECKIN_ACTION_KEYS.has(actionNameKey(name));
}

/** Catalog / toolbar names for Explorer check-out of the selected item (#4699). */
export function isCheckoutActionName(
  name: string | undefined | null,
): boolean {
  return CHECKOUT_ACTION_KEYS.has(actionNameKey(name));
}

/** Catalog / toolbar names for Explorer check-in of the selected item (#4699). */
export function isCheckinActionName(
  name: string | undefined | null,
): boolean {
  return CHECKIN_ACTION_KEYS.has(actionNameKey(name));
}

/**
 * Catalog / toolbar names for Explorer Publishing History (Finder
 * {@code perc-pubhistory-button}).
 */
export function isPublishingHistoryActionName(
  name: string | undefined | null,
): boolean {
  return PUBLISHING_HISTORY_ACTION_KEYS.has(actionNameKey(name));
}

/** Pages that link to the item — listed on the takedown confirm. */
export interface LinkedPageForTakedown {
  id?: string;
  pagePath?: string;
  relationshipId?: string;
}

function asLinkedPage(row: unknown): LinkedPageForTakedown | null {
  if (row == null || typeof row !== "object") {
    return null;
  }
  const rec = row as Record<string, unknown>;
  const inner =
    rec.PageLinkedToItem != null && typeof rec.PageLinkedToItem === "object"
      ? (rec.PageLinkedToItem as Record<string, unknown>)
      : rec;
  const pagePath =
    typeof inner.pagePath === "string" ? inner.pagePath.trim() : "";
  const id = typeof inner.id === "string" ? inner.id.trim() : "";
  const relationshipId =
    typeof inner.relationshipId === "string"
      ? inner.relationshipId.trim()
      : "";
  if (!pagePath && !id && !relationshipId) {
    return null;
  }
  return {
    id: id || undefined,
    pagePath: pagePath || undefined,
    relationshipId: relationshipId || undefined,
  };
}

export function parseLinkedPagesForTakedown(
  body: unknown,
): LinkedPageForTakedown[] {
  return asObjectArray(body)
    .map(asLinkedPage)
    .filter((row): row is LinkedPageForTakedown => row != null);
}

/**
 * Load pages that link to this item. Linked-list failures must not block
 * takedown (classic Finder proceeds without the extra confirm).
 */
export async function loadLinkedPagesForTakedown(
  itemId: string,
): Promise<LinkedPageForTakedown[]> {
  const id = itemId.trim();
  if (!id) {
    return [];
  }
  try {
    const body = await get<unknown>(
      `${itemPublishPaths().linkedItems}/${encodeURIComponent(id)}`,
    );
    return parseLinkedPagesForTakedown(body);
  } catch {
    return [];
  }
}

/** Classic Finder confirm lists at most ten linked page paths. */
export const TAKEDOWN_LINKED_PATH_CONFIRM_LIMIT = 10;

/** Linked page paths shown on the takedown confirm (max ten). */
export function linkedPagePathsForConfirm(
  linked: LinkedPageForTakedown[],
): string[] {
  return linked
    .map((row) => (row.pagePath ?? "").trim())
    .filter((path) => path.length > 0)
    .slice(0, TAKEDOWN_LINKED_PATH_CONFIRM_LIMIT);
}

/** Confirm copy for Take Down, including linked page paths when present. */
export function formatTakedownConfirmBody(
  linked: LinkedPageForTakedown[],
): string {
  const intro = message(EXPLORER_MSG.CONFIRM_TAKEDOWN);
  const paths = linkedPagePathsForConfirm(linked);
  if (paths.length === 0) {
    return intro;
  }
  return `${intro}\n\n${message(EXPLORER_MSG.CONFIRM_TAKEDOWN_LINKED)}\n${paths.join("\n")}`;
}

/**
 * Demand-publish a page or asset. Other types return false so the
 * dispatcher can show that the action is not available.
 *
 * <p>HTTP 200 with an application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder and
 * {@code mapPublishResponse}. Those responses throw so Explorer does
 * not refresh as if the job started.</p>
 */
export async function publishSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await demandPublish(`${paths.pagePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  if (kind === "asset") {
    await demandPublish(`${paths.resourcePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  return false;
}

export interface PublishBatchResult {
  publishedIds: string[];
  skippedFolders: string[];
  skippedOther: string[];
  failures: StageItemFailure[];
}

function failureFromPublish(item: PSPathItem, err: unknown): StageItemFailure {
  if (isApiError(err)) {
    return {
      id: (item.id ?? "").trim(),
      name: stageItemLabel(item),
      status: err.status,
      message: `HTTP ${err.status}`,
    };
  }
  const text = err instanceof Error ? err.message : "publish failed";
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || "publish failed",
  };
}

/**
 * Demand-publish every eligible page/asset. Folders and other types are
 * skipped. A 403/404/409 (or application-level preflight failure) on one
 * item is recorded and the rest of the selection still runs.
 */
export async function publishSelectedItems(
  items: readonly PSPathItem[],
): Promise<PublishBatchResult> {
  const plan = partitionStageSelection(items);
  const publishedIds: string[] = [];
  const failures: StageItemFailure[] = [];
  for (const item of plan.eligible) {
    try {
      const published = await publishSelectedItem(item);
      if (!published) {
        failures.push({
          id: (item.id ?? "").trim(),
          name: stageItemLabel(item),
          message: "not published",
        });
      } else {
        publishedIds.push((item.id ?? "").trim());
      }
    } catch (err: unknown) {
      failures.push(failureFromPublish(item, err));
    }
  }
  return {
    publishedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

/** Operator-visible reason when folders or individual items were not fully published. */
export function describePublishBatch(
  result: PublishBatchResult,
): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.PUBLISH_SKIPPED_FOLDERS,
        "{names}",
        result.skippedFolders.join(", "),
      ),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.PUBLISH_SKIPPED_OTHER,
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
      fillTemplate(EXPLORER_MSG.PUBLISH_BATCH_INCOMPLETE, "{detail}", detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

async function demandPublish(url: string): Promise<void> {
  const body = await get<unknown>(url);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Publish failed");
  }
}

/**
 * Take down (unpublish) a page or asset. Other types return false so the
 * dispatcher can show that the action is not available.
 *
 * <p>HTTP 200 with application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder and
 * {@code mapPublishResponse}. Linked pages trigger PUT of the list (same
 * as {@code PercItemPublisherService.takeDownItem}); otherwise GET.</p>
 */
export async function takedownSelectedItem(
  item: PSPathItem,
  linked: LinkedPageForTakedown[] = [],
): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await demandTakedown(
      `${paths.pageTakedown}/${encodeURIComponent(id)}`,
      linked,
    );
    return true;
  }
  if (kind === "asset") {
    await demandTakedown(
      `${paths.resourceTakedown}/${encodeURIComponent(id)}`,
      linked,
    );
    return true;
  }
  return false;
}

export interface TakedownBatchResult {
  takenDownIds: string[];
  skippedFolders: string[];
  skippedOther: string[];
  failures: StageItemFailure[];
}

function failureFromTakedown(item: PSPathItem, err: unknown): StageItemFailure {
  if (isApiError(err)) {
    return {
      id: (item.id ?? "").trim(),
      name: stageItemLabel(item),
      status: err.status,
      message: `HTTP ${err.status}`,
    };
  }
  const text = err instanceof Error ? err.message : "takedown failed";
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || "takedown failed",
  };
}

/**
 * Take down every eligible page/asset. Folders and other types are skipped.
 * Each item uses the same linked-page lookup and GET/PUT as
 * {@link takedownSelectedItem}. A failure on one item is recorded and the
 * rest of the selection still runs.
 */
export async function takedownSelectedItems(
  items: readonly PSPathItem[],
): Promise<TakedownBatchResult> {
  const plan = partitionStageSelection(items);
  const takenDownIds: string[] = [];
  const failures: StageItemFailure[] = [];
  for (const item of plan.eligible) {
    try {
      const linked = await loadLinkedPagesForTakedown(item.id ?? "");
      const takenDown = await takedownSelectedItem(item, linked);
      if (!takenDown) {
        failures.push({
          id: (item.id ?? "").trim(),
          name: stageItemLabel(item),
          message: "not taken down",
        });
      } else {
        takenDownIds.push((item.id ?? "").trim());
      }
    } catch (err: unknown) {
      failures.push(failureFromTakedown(item, err));
    }
  }
  return {
    takenDownIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

/** Operator-visible reason when folders or individual items were not fully taken down. */
export function describeTakedownBatch(
  result: TakedownBatchResult,
): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.TAKEDOWN_SKIPPED_FOLDERS,
        "{names}",
        result.skippedFolders.join(", "),
      ),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.TAKEDOWN_SKIPPED_OTHER,
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
      fillTemplate(EXPLORER_MSG.TAKEDOWN_BATCH_INCOMPLETE, "{detail}", detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

async function demandTakedown(
  url: string,
  linked: LinkedPageForTakedown[],
): Promise<void> {
  const body =
    linked.length > 0
      ? await put<unknown>(url, linked)
      : await get<unknown>(url);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Takedown failed");
  }
}

/**
 * Stage a page or asset. Other types return false so the dispatcher can
 * show that the action is not available.
 *
 * <p>HTTP 200 with application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder.</p>
 */
export async function stageSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await demandPublish(`${paths.pageStaging}/${encodeURIComponent(id)}`);
    return true;
  }
  if (kind === "asset") {
    await demandPublish(`${paths.resourceStaging}/${encodeURIComponent(id)}`);
    return true;
  }
  return false;
}

export interface StageItemFailure {
  id: string;
  name: string;
  status?: number;
  message: string;
}

/** Outcome of staging a checkbox multi-selection. One item failure does not erase the rest. */
export interface StageBatchResult {
  stagedIds: string[];
  skippedFolders: string[];
  skippedOther: string[];
  failures: StageItemFailure[];
}

export interface StageSelectionPlan {
  eligible: PSPathItem[];
  skippedFolders: string[];
  skippedOther: string[];
}

function stageItemLabel(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name) {
    return name;
  }
  const path = (item.path ?? "").trim();
  if (path) {
    return path;
  }
  return (item.id ?? "").trim() || "item";
}

function fillTemplate(key: string, token: string, value: string): string {
  return message(key).split(token).join(value);
}

/**
 * Split a multi-selection into pages/assets that can be staged and rows
 * that must be skipped (folders, templates, other types). Duplicate ids
 * are staged once.
 */
export function partitionStageSelection(
  items: readonly PSPathItem[],
): StageSelectionPlan {
  const eligible: PSPathItem[] = [];
  const skippedFolders: string[] = [];
  const skippedOther: string[] = [];
  const seen = new Set<string>();
  for (const item of items) {
    const id = (item.id ?? "").trim();
    const key = id || `name:${stageItemLabel(item)}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    if (isFolder(item)) {
      skippedFolders.push(stageItemLabel(item));
      continue;
    }
    if (!id || resolvePublishKind(item) === "none") {
      skippedOther.push(stageItemLabel(item));
      continue;
    }
    eligible.push(item);
  }
  return { eligible, skippedFolders, skippedOther };
}

function failureFromStage(item: PSPathItem, err: unknown): StageItemFailure {
  if (isApiError(err)) {
    return {
      id: (item.id ?? "").trim(),
      name: stageItemLabel(item),
      status: err.status,
      message: `HTTP ${err.status}`,
    };
  }
  const text = err instanceof Error ? err.message : "stage failed";
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || "stage failed",
  };
}

/**
 * Stage every eligible page/asset. Folders and other types are skipped.
 * A 403/404/409 (or application-level preflight failure) on one item is
 * recorded and the rest of the selection still runs.
 */
export async function stageSelectedItems(
  items: readonly PSPathItem[],
): Promise<StageBatchResult> {
  const plan = partitionStageSelection(items);
  const stagedIds: string[] = [];
  const failures: StageItemFailure[] = [];
  for (const item of plan.eligible) {
    try {
      const staged = await stageSelectedItem(item);
      if (!staged) {
        failures.push({
          id: (item.id ?? "").trim(),
          name: stageItemLabel(item),
          message: "not staged",
        });
      } else {
        stagedIds.push((item.id ?? "").trim());
      }
    } catch (err: unknown) {
      failures.push(failureFromStage(item, err));
    }
  }
  return {
    stagedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

/** Operator-visible reason when folders or individual items were not fully staged. */
export function describeStageBatch(result: StageBatchResult): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.STAGE_SKIPPED_FOLDERS,
        "{names}",
        result.skippedFolders.join(", "),
      ),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.STAGE_SKIPPED_OTHER,
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
      fillTemplate(EXPLORER_MSG.STAGE_BATCH_INCOMPLETE, "{detail}", detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

/**
 * Remove a page or asset from staging. Other types return false so the
 * dispatcher can show that the action is not available.
 *
 * <p>HTTP 200 with application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder.</p>
 */
export async function removeFromStagingSelectedItem(
  item: PSPathItem,
): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await demandPublish(
      `${paths.pageStagingTakedown}/${encodeURIComponent(id)}`,
    );
    return true;
  }
  if (kind === "asset") {
    await demandPublish(
      `${paths.resourceStagingTakedown}/${encodeURIComponent(id)}`,
    );
    return true;
  }
  return false;
}

/** Outcome of removing a checkbox multi-selection from staging. */
export interface RemoveFromStagingBatchResult {
  removedIds: string[];
  skippedFolders: string[];
  skippedOther: string[];
  failures: StageItemFailure[];
}

function failureFromRemove(item: PSPathItem, err: unknown): StageItemFailure {
  if (isApiError(err)) {
    return {
      id: (item.id ?? "").trim(),
      name: stageItemLabel(item),
      status: err.status,
      message: `HTTP ${err.status}`,
    };
  }
  const text =
    err instanceof Error ? err.message : "remove from staging failed";
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || "remove from staging failed",
  };
}

/**
 * Remove every eligible page/asset from staging. Folders and other types
 * are skipped. A 403/404/409 (or application-level preflight failure) on
 * one item is recorded and the rest of the selection still runs.
 */
export async function removeFromStagingSelectedItems(
  items: readonly PSPathItem[],
): Promise<RemoveFromStagingBatchResult> {
  const plan = partitionStageSelection(items);
  const removedIds: string[] = [];
  const failures: StageItemFailure[] = [];
  for (const item of plan.eligible) {
    try {
      const removed = await removeFromStagingSelectedItem(item);
      if (!removed) {
        failures.push({
          id: (item.id ?? "").trim(),
          name: stageItemLabel(item),
          message: "not removed",
        });
      } else {
        removedIds.push((item.id ?? "").trim());
      }
    } catch (err: unknown) {
      failures.push(failureFromRemove(item, err));
    }
  }
  return {
    removedIds,
    skippedFolders: plan.skippedFolders,
    skippedOther: plan.skippedOther,
    failures,
  };
}

/** Operator-visible reason when folders or individual items were not fully unstaged. */
export function describeRemoveFromStagingBatch(
  result: RemoveFromStagingBatchResult,
): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolders.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.UNSTAGE_SKIPPED_FOLDERS,
        "{names}",
        result.skippedFolders.join(", "),
      ),
    );
  }
  if (result.skippedOther.length > 0) {
    parts.push(
      fillTemplate(
        EXPLORER_MSG.UNSTAGE_SKIPPED_OTHER,
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
      fillTemplate(
        EXPLORER_MSG.UNSTAGE_BATCH_INCOMPLETE,
        "{detail}",
        detail,
      ),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}
