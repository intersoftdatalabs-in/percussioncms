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
import { findChildren } from "../api/contentExplorer/pathApi";
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
 * Classify which demand endpoint serves this selection.
 *
 * <p>This is an <strong>endpoint-affinity</strong> classifier, not an
 * availability gate (#6580 reversal, parent #4530). {@code "page"} and
 * {@code "asset"} pick the page- vs resource-addressed demand endpoint
 * ({@code …/publish/page|resource/{id}}, staging, takedown, schedule dates,
 * {@code …/item/pubhistory}). Generic custom-type Items (Rhythmyx Items that
 * live outside {@code /Sites} and {@code /Assets} with no page/asset token)
 * return {@code "none"} here, but the publish family must NOT treat that as
 * "cannot publish": Pages, Assets, and Items are all 1st-class entities, so
 * submitters default a {@code "none"}-with-id row to the page endpoint and
 * let the engine accept or reject with a named error — the same all-children
 * submit the ondemand/publish-now generator performs server-side
 * ({@code PSPublisherService.executeDemandPublish} takes any numeric content
 * ids; only nested folders publish separately and empty ids cannot be
 * submitted).</p>
 *
 * <p>{@code "none"} therefore means only "no page/asset endpoint affinity":
 * a missing / null row, a folder, a row without an id, or a generic Item
 * with no page/asset token. The publish family defaults a
 * {@code "none"}-with-id row to the page endpoint instead of gating on it.
 * Non-publish families with genuinely page- or asset-scoped contracts
 * (page templates, mobile preview, item/folder community-workflow
 * assignment) may still read this signal; availability for Publish Now,
 * stage / remove-from-staging, Take Down, schedule / clear-schedule,
 * Publishing History, and checkout / check-in / force check-in is decided
 * with {@code isFolder} + id presence instead.</p>
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
 * Demand-publish any item with an id. Generic Items (no page/asset token)
 * ride the page endpoint and the engine accepts or rejects with a named
 * error — Pages, Assets, and Items are all 1st-class entities (#6580
 * reversal: all-children submit, mirroring the ondemand generator which
 * takes any numeric content ids). Returns false for folders and rows
 * with no id.
 *
 * <p>HTTP 200 with an application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder and
 * {@code mapPublishResponse}. Those responses throw so Explorer does
 * not refresh as if the job started.</p>
 */
export async function publishSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id || isFolder(item)) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "asset") {
    await demandPublish(`${paths.resourcePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  await demandPublish(`${paths.pagePublish}/${encodeURIComponent(id)}`);
  return true;
}

/**
 * Demand-publish every child with an id. Nested folders are skipped
 * (publish those folders separately). Rows without an id cannot be
 * submitted and are recorded as named failures so no child goes
 * unmentioned. Application-level preflight failures are recorded and
 * do not count as a started job.
 */
export async function publishSelectedFolder(
  folder: PSPathItem,
): Promise<PublishBatchResult> {
  const path = normalizeCmsPath(folder.path ?? "");
  if (!isFolder(folder) || !path) {
    return {
      publishedIds: [],
      skippedFolders: [],
      failures: [],
    };
  }
  const children = await findChildren(path);
  const publishedIds: string[] = [];
  const skippedFolders: string[] = [];
  const failures: StageItemFailure[] = [];
  const seen = new Set<string>();
  for (const child of children) {
    const id = (child.id ?? "").trim();
    const key = id || `name:${stageItemLabel(child)}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    if (isFolder(child)) {
      skippedFolders.push(stageItemLabel(child));
      continue;
    }
    if (!id) {
      failures.push({
        id,
        name: stageItemLabel(child),
        message: "missing content id",
      });
      continue;
    }
    try {
      const published = await publishSelectedItem(child);
      if (!published) {
        failures.push({
          id,
          name: stageItemLabel(child),
          message: "not published",
        });
      } else {
        publishedIds.push(id);
      }
    } catch (err: unknown) {
      failures.push(failureFromPublish(child, err));
    }
  }
  return { publishedIds, skippedFolders, failures };
}

export interface PublishBatchResult {
  publishedIds: string[];
  skippedFolders: string[];
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
 * Demand-publish every selected item. Folders are skipped (their children
 * publish via the folder flow). Every non-folder row is submitted —
 * including generic Items — and a 403/404/409 (or application-level
 * preflight failure) on one item is recorded while the rest of the
 * selection still runs.
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
    throw new Error(
      preflight.message ||
        preflight.token ||
        message(EXPLORER_MSG.DEMAND_PUBLISH_FAILED),
    );
  }
}

/**
 * Take down (unpublish) any item with an id. Generic Items ride the page
 * endpoint and the engine accepts or rejects with a named error
 * (#6580 reversal). Returns false for folders and rows with no id.
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
  if (!id || isFolder(item)) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "asset") {
    await demandTakedown(
      `${paths.resourceTakedown}/${encodeURIComponent(id)}`,
      linked,
    );
    return true;
  }
  await demandTakedown(
    `${paths.pageTakedown}/${encodeURIComponent(id)}`,
    linked,
  );
  return true;
}

export interface TakedownBatchResult {
  takenDownIds: string[];
  skippedFolders: string[];
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
 * Take down every selected item. Folders are skipped. Each item uses the
 * same linked-page lookup and GET/PUT as {@link takedownSelectedItem}.
 * A failure on one item is recorded and the rest of the selection still
 * runs.
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
    throw new Error(
      preflight.message ||
        preflight.token ||
        message(EXPLORER_MSG.DEMAND_TAKEDOWN_FAILED),
    );
  }
}

/**
 * Stage any item with an id. Generic Items ride the page staging endpoint
 * and the engine accepts or rejects with a named error (#6580 reversal).
 * Returns false for folders and rows with no id.
 *
 * <p>HTTP 200 with application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder.</p>
 */
export async function stageSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id || isFolder(item)) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "asset") {
    await demandPublish(`${paths.resourceStaging}/${encodeURIComponent(id)}`);
    return true;
  }
  await demandPublish(`${paths.pageStaging}/${encodeURIComponent(id)}`);
  return true;
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
  failures: StageItemFailure[];
}

export interface StageSelectionPlan {
  eligible: PSPathItem[];
  skippedFolders: string[];
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
  return (item.id ?? "").trim() || message(EXPLORER_MSG.BULK_STAGE_UNNAMED_ITEM);
}

function fillTemplate(key: string, token: string, value: string): string {
  return message(key).split(token).join(value);
}

/**
 * Split a multi-selection into submittable rows and folders. Every
 * non-folder row is eligible — Pages, Assets, and generic Items alike
 * (#6580 reversal: submit all, let the engine accept or reject). Only
 * folders are skipped (their children run through the folder flow).
 * Duplicate ids are staged once.
 */
export function partitionStageSelection(
  items: readonly PSPathItem[],
): StageSelectionPlan {
  const eligible: PSPathItem[] = [];
  const skippedFolders: string[] = [];
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
    eligible.push(item);
  }
  return { eligible, skippedFolders };
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
  const text = err instanceof Error ? err.message : message(EXPLORER_MSG.BULK_STAGE_FAILED);
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || message(EXPLORER_MSG.BULK_STAGE_FAILED),
  };
}

/**
 * Stage every selected item. Folders are skipped. A 403/404/409 (or
 * application-level preflight failure) on one item is recorded and the
 * rest of the selection still runs.
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
          message: message(EXPLORER_MSG.BULK_STAGE_NOT_STAGED),
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
 * Direct children of a folder for the Stage / Remove-from-Staging confirm
 * dialog (#6530, parent #4530). Guards mirror {@code stageSelectedFolder}:
 * not a folder (or no path) resolves to no children so dispatch keeps the
 * legacy "select a folder" / empty-folder messaging. The dialog partitions
 * the children itself (nested folders skipped, per-item report).
 */
export async function listStageFolderChildren(
  folder: PSPathItem,
): Promise<PSPathItem[]> {
  const path = normalizeCmsPath(folder.path ?? "");
  if (!isFolder(folder) || !path) {
    return [];
  }
  return findChildren(path);
}

/**
 * Stage items that are direct children of a folder. Nested folders are
 * skipped (stage those folders separately). Rows without an id are
 * recorded as named failures. Application-level preflight failures are
 * recorded and do not count as staged (#6419, parent #4530).
 */
export async function stageSelectedFolder(
  folder: PSPathItem,
): Promise<StageBatchResult> {
  const path = normalizeCmsPath(folder.path ?? "");
  if (!isFolder(folder) || !path) {
    return {
      stagedIds: [],
      skippedFolders: [],
      failures: [],
    };
  }
  const children = await findChildren(path);
  const stagedIds: string[] = [];
  const skippedFolders: string[] = [];
  const failures: StageItemFailure[] = [];
  const seen = new Set<string>();
  for (const child of children) {
    const id = (child.id ?? "").trim();
    const key = id || `name:${stageItemLabel(child)}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    if (isFolder(child)) {
      skippedFolders.push(stageItemLabel(child));
      continue;
    }
    if (!id) {
      failures.push({
        id,
        name: stageItemLabel(child),
        message: "missing content id",
      });
      continue;
    }
    try {
      const staged = await stageSelectedItem(child);
      if (!staged) {
        failures.push({
          id,
          name: stageItemLabel(child),
          message: "not staged",
        });
      } else {
        stagedIds.push(id);
      }
    } catch (err: unknown) {
      failures.push(failureFromStage(child, err));
    }
  }
  return { stagedIds, skippedFolders, failures };
}

/**
 * Remove any item with an id from staging. Generic Items ride the page
 * staging-takedown endpoint and the engine accepts or rejects with a
 * named error (#6580 reversal). Returns false for folders and rows
 * with no id.
 *
 * <p>HTTP 200 with application-level preflight status
 * ({@code FORBIDDEN}, {@code BADCONFIG}, {@code NOSTAGING_SERVERS},
 * {@code INVALID}, …) is a failure — same as classic Finder.</p>
 */
export async function removeFromStagingSelectedItem(
  item: PSPathItem,
): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id || isFolder(item)) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "asset") {
    await demandPublish(
      `${paths.resourceStagingTakedown}/${encodeURIComponent(id)}`,
    );
    return true;
  }
  await demandPublish(
    `${paths.pageStagingTakedown}/${encodeURIComponent(id)}`,
  );
  return true;
}

/** Outcome of removing a checkbox multi-selection from staging. */
export interface RemoveFromStagingBatchResult {
  removedIds: string[];
  skippedFolders: string[];
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
    err instanceof Error
      ? err.message
      : message(EXPLORER_MSG.BULK_UNSTAGE_FAILED);
  return {
    id: (item.id ?? "").trim(),
    name: stageItemLabel(item),
    message: text || message(EXPLORER_MSG.BULK_UNSTAGE_FAILED),
  };
}

/**
 * Remove every selected item from staging. Folders are skipped. A
 * 403/404/409 (or application-level preflight failure) on one item is
 * recorded and the rest of the selection still runs.
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
          message: message(EXPLORER_MSG.BULK_UNSTAGE_NOT_REMOVED),
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

/** One row of the bulk stage/unstage per-item report (#6469, parent #4530). */
export type BulkStageRowOutcome =
  | "ok"
  | "failed"
  | "skipped-folder";

export interface BulkStageRow {
  name: string;
  outcome: BulkStageRowOutcome;
  detail?: string;
}

/**
 * Batch shape the per-item report consumes. Both {@link StageBatchResult}
 * (via {@code stagedIds}) and {@link RemoveFromStagingBatchResult} (via
 * {@code removedIds}) map onto it at the call site.
 */
export interface BulkStageReportInput {
  okIds: string[];
  skippedFolders: string[];
  failures: StageItemFailure[];
}

/**
 * Build the per-item report rows for a bulk stage/unstage run (#6469).
 * Rows follow the selection order; failures that name no selected row are
 * appended so no outcome is dropped silently.
 */
export function buildBulkStageRows(
  items: readonly PSPathItem[],
  result: BulkStageReportInput,
): BulkStageRow[] {
  const ok = new Set(
    result.okIds.map((id) => (id ?? "").trim()).filter((id) => id !== ""),
  );
  const failures = indexBulkStageFailures(result.failures);
  const folders = new Set(result.skippedFolders);
  const rows: BulkStageRow[] = [];
  const seen = new Set<string>();
  for (const item of items) {
    const id = (item.id ?? "").trim();
    const key = id || `name:${stageItemLabel(item)}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    const row = bulkStageRowForSelection(item, ok, failures, folders);
    if (row) {
      rows.push(row);
    }
  }
  appendUnselectedFailures(items, result.failures, failures, rows);
  return rows;
}

/**
 * First failure wins per id so duplicate reports do not double-count.
 * Rows without an id are keyed by their label so an id-less failure
 * still finds its row instead of dropping silently.
 */
function indexBulkStageFailures(
  failures: readonly StageItemFailure[],
): Map<string, StageItemFailure> {
  const byId = new Map<string, StageItemFailure>();
  for (const failure of failures) {
    const id = (failure.id ?? "").trim();
    const key = id || `name:${(failure.name ?? "").trim() || "item"}`;
    if (!byId.has(key)) {
      byId.set(key, failure);
    }
  }
  return byId;
}

function failedBulkStageRow(
  failure: StageItemFailure,
  fallback: string,
): BulkStageRow {
  return {
    name: failure.name || fallback,
    outcome: "failed",
    detail:
      failure.status != null
        ? `HTTP ${failure.status}`
        : failure.message || undefined,
  };
}

/** Null when the row was neither staged nor named anywhere in the result. */
function bulkStageRowForSelection(
  item: PSPathItem,
  ok: ReadonlySet<string>,
  failures: ReadonlyMap<string, StageItemFailure>,
  folders: ReadonlySet<string>,
): BulkStageRow | null {
  const id = (item.id ?? "").trim();
  const label = stageItemLabel(item);
  const failure = failures.get(id || `name:${label}`);
  if (failure) {
    return failedBulkStageRow(failure, label);
  }
  if (id && ok.has(id)) {
    return { name: label, outcome: "ok" };
  }
  if (folders.has(label)) {
    return { name: label, outcome: "skipped-folder" };
  }
  return null;
}

function appendUnselectedFailures(
  items: readonly PSPathItem[],
  failures: readonly StageItemFailure[],
  indexed: ReadonlyMap<string, StageItemFailure>,
  rows: BulkStageRow[],
): void {
  for (const failure of failures) {
    const id = (failure.id ?? "").trim();
    const key = id || `name:${(failure.name ?? "").trim() || "item"}`;
    if (indexed.get(key) !== failure) {
      continue;
    }
    const known = items.some((item) => {
      const rowId = (item.id ?? "").trim();
      const rowKey = rowId || `name:${stageItemLabel(item)}`;
      return rowKey === key;
    });
    if (!known) {
      rows.push(failedBulkStageRow(failure, id || failure.name || "item"));
    }
  }
}

/**
 * Remove items that are direct children of a folder from staging.
 * Nested folders are skipped (unstage those folders separately). Rows
 * without an id are recorded as named failures. Application-level
 * preflight failures are recorded and do not count as removed (#6419,
 * parent #4530).
 */
export async function removeFromStagingSelectedFolder(
  folder: PSPathItem,
): Promise<RemoveFromStagingBatchResult> {
  const path = normalizeCmsPath(folder.path ?? "");
  if (!isFolder(folder) || !path) {
    return {
      removedIds: [],
      skippedFolders: [],
      failures: [],
    };
  }
  const children = await findChildren(path);
  const removedIds: string[] = [];
  const skippedFolders: string[] = [];
  const failures: StageItemFailure[] = [];
  const seen = new Set<string>();
  for (const child of children) {
    const id = (child.id ?? "").trim();
    const key = id || `name:${stageItemLabel(child)}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    if (isFolder(child)) {
      skippedFolders.push(stageItemLabel(child));
      continue;
    }
    if (!id) {
      failures.push({
        id,
        name: stageItemLabel(child),
        message: "missing content id",
      });
      continue;
    }
    try {
      const removed = await removeFromStagingSelectedItem(child);
      if (!removed) {
        failures.push({
          id,
          name: stageItemLabel(child),
          message: "not removed",
        });
      } else {
        removedIds.push(id);
      }
    } catch (err: unknown) {
      failures.push(failureFromRemove(child, err));
    }
  }
  return { removedIds, skippedFolders, failures };
}
