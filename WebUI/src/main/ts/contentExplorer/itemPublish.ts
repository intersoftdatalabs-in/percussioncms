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
 * PercItemPublisherService).
 */

import { get, put } from "../api/client";
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

const LINKED_PATH_CONFIRM_LIMIT = 10;

/** Confirm copy for Take Down, including linked page paths when present. */
export function formatTakedownConfirmBody(
  linked: LinkedPageForTakedown[],
): string {
  const intro = message(EXPLORER_MSG.CONFIRM_TAKEDOWN);
  const paths = linked
    .map((row) => (row.pagePath ?? "").trim())
    .filter((path) => path.length > 0)
    .slice(0, LINKED_PATH_CONFIRM_LIMIT);
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
