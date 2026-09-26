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
 * Publish now from the React Content Editor host (#4540 / parent #4532).
 *
 * <p>Reuses sitemanage demand-publish GETs ({@link itemPublishPaths}) and
 * {@link mapPublishResponse} so HTTP 200 {@code FORBIDDEN}/{@code BADCONFIG}
 * is not treated as success — same contract as Explorer
 * {@code publishSelectedItem}.</p>
 */

import { get, put } from "../api/client";
import type { LinkedPageForTakedown } from "../contentExplorer/itemPublish";
import { linkedPagePathsForConfirm } from "../contentExplorer/itemPublish";
import { message } from "../i18n/message";
import { itemPublishPaths } from "../publishing/itemPublishPaths";
import { mapPublishResponse } from "../publishing/publishActions";
import type { EditorHostMode } from "./editorHostUrl";
import { EDITOR_MSG } from "./messages";
import { isExplorerPageType } from "./pageTemplates";

export type EditorPublishKind = "page" | "asset" | "none";

function foldType(name: string | null | undefined): string {
  return String(name ?? "")
    .replace(/[\s_-]/g, "")
    .toLowerCase();
}

/** FastForward / CM1 page types that do not include the word "page". */
const PAGE_TYPE_NAMES: ReadonlySet<string> = new Set([
  "percpage",
  "page",
  "percblogpost",
  "percevent",
  "percpressrelease",
  "percgenericpage",
  "percnavon",
]);

/** Shared asset widgets whose type names omit "asset". */
const ASSET_TYPE_NAMES: ReadonlySet<string> = new Set([
  "percrichtext",
  "percfile",
  "percimage",
  "percflash",
  "percform",
  "perccalendar",
  "percperson",
  "percnavimage",
  "percsimpletext",
  "percfileasset",
  "percimageasset",
]);

/**
 * Whether the open editor item can demand-publish as a page or asset.
 *
 * <p>Templates, folders, and missing ids stay {@code none}. Content types
 * with allowed page templates (or percPage / *page*) are pages. Remaining
 * named types on this host are assets (Explorer uses path; the editor only
 * opens pages and assets).</p>
 */
export function resolveEditorPublishKind(
  contentType: string | null | undefined,
  extras?: {
    id?: string | null;
    allowedTemplateCount?: number;
  },
): EditorPublishKind {
  const id = String(extras?.id ?? "").trim();
  const token = foldType(contentType);
  if (!id || !token) {
    return "none";
  }
  if (token === "folder" || token === "site" || token.includes("template")) {
    return "none";
  }
  if (token.includes("asset") || ASSET_TYPE_NAMES.has(token)) {
    return "asset";
  }
  if (
    token.includes("page") ||
    isExplorerPageType(contentType) ||
    PAGE_TYPE_NAMES.has(token) ||
    (extras?.allowedTemplateCount ?? 0) > 0
  ) {
    return "page";
  }
  return "asset";
}

/** Edit mode + page/asset only. View / promote stay read-only. */
export function canPublishFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return mode === "edit" && (kind === "page" || kind === "asset");
}

/**
 * Read-only publish history for an already-open page or asset.
 * Edit and view only. Does not publish or take down. Promote, folders,
 * templates, and unsaved items (no id) stay hidden.
 */
export function canViewPublishHistoryFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return (
    (mode === "edit" || mode === "view") &&
    (kind === "page" || kind === "asset")
  );
}

/**
 * Demand-publish the open page or asset. Returns false when kind is none
 * or the id is blank (caller shows unavailable). Throws on HTTP errors and
 * on HTTP 200 preflight failures ({@code FORBIDDEN}, {@code BADCONFIG}, …).
 */
export async function publishEditorItem(
  itemId: string,
  kind: EditorPublishKind,
): Promise<boolean> {
  const id = itemId.trim();
  if (!id || kind === "none") {
    return false;
  }
  const paths = itemPublishPaths();
  const base = kind === "page" ? paths.pagePublish : paths.resourcePublish;
  const body = await get<unknown>(`${base}/${encodeURIComponent(id)}`);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Publish failed");
  }
  return true;
}

/**
 * Same eligibility as {@link canPublishFromEditor}. Templates, folders, and
 * unsaved items (no id) stay hidden. View and promote stay read-only.
 */
export function canStageFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return canPublishFromEditor(mode, kind);
}

/**
 * Stage the open page or asset via sitemanage
 * {@code publish/page|resource/staging/{id}}. Returns false when kind is
 * none or the id is blank. HTTP errors and HTTP 200 preflight failures
 * ({@code FORBIDDEN}, {@code BADCONFIG}, …) throw and are not success.
 */
export async function stageEditorItem(
  itemId: string,
  kind: EditorPublishKind,
): Promise<boolean> {
  const id = itemId.trim();
  if (!id || kind === "none") {
    return false;
  }
  const paths = itemPublishPaths();
  const base = kind === "page" ? paths.pageStaging : paths.resourceStaging;
  const body = await get<unknown>(`${base}/${encodeURIComponent(id)}`);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Stage failed");
  }
  return true;
}

/**
 * Same eligibility as {@link canStageFromEditor}. Removing from staging is
 * not Take down and is not available in view or promote mode.
 */
export function canRemoveFromStagingFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return canStageFromEditor(mode, kind);
}

/**
 * Remove the open page or asset from staging via sitemanage
 * {@code takedown/page|resource/staging/{id}}. Returns false when kind is
 * none or the id is blank. HTTP errors and HTTP 200 preflight failures
 * ({@code FORBIDDEN}, {@code BADCONFIG}, …) throw and are not success.
 */
export async function removeEditorItemFromStaging(
  itemId: string,
  kind: EditorPublishKind,
): Promise<boolean> {
  const id = itemId.trim();
  if (!id || kind === "none") {
    return false;
  }
  const paths = itemPublishPaths();
  const base =
    kind === "page" ? paths.pageStagingTakedown : paths.resourceStagingTakedown;
  const body = await get<unknown>(`${base}/${encodeURIComponent(id)}`);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(
      preflight.message || preflight.token || "Remove from staging failed",
    );
  }
  return true;
}

/**
 * Same eligibility as {@link canPublishFromEditor}. Folders, templates, and
 * items with no id (new unsaved create) stay ineligible.
 */
export function canTakedownFromEditor(
  mode: EditorHostMode,
  kind: EditorPublishKind,
): boolean {
  return canPublishFromEditor(mode, kind);
}

/** Confirm copy for editor Take down, including linked page paths when present. */
export function formatEditorTakedownConfirm(
  linked: LinkedPageForTakedown[],
): string {
  const intro = message(EDITOR_MSG.CONFIRM_TAKE_DOWN);
  const paths = linkedPagePathsForConfirm(linked);
  if (paths.length === 0) {
    return intro;
  }
  return `${intro}\n\n${message(EDITOR_MSG.CONFIRM_TAKE_DOWN_LINKED)}\n${paths.join("\n")}`;
}

/**
 * Take down (unpublish) the open page or asset. Returns false when kind is
 * none or the id is blank. Linked pages PUT the list (same as Explorer
 * {@code takedownSelectedItem}); otherwise GET. HTTP 200 preflight failures
 * throw and are not success.
 */
export async function takedownEditorItem(
  itemId: string,
  kind: EditorPublishKind,
  linked: LinkedPageForTakedown[] = [],
): Promise<boolean> {
  const id = itemId.trim();
  if (!id || kind === "none") {
    return false;
  }
  const paths = itemPublishPaths();
  const base = kind === "page" ? paths.pageTakedown : paths.resourceTakedown;
  const url = `${base}/${encodeURIComponent(id)}`;
  const body =
    linked.length > 0 ? await put<unknown>(url, linked) : await get<unknown>(url);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Takedown failed");
  }
  return true;
}
