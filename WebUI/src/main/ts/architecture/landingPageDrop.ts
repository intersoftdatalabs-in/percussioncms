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
 * Map an Explorer / Finder listing HTML5 drop onto a Navigation section
 * to {@code POST /section/replaceLandingPage} (#3660 / parent #3092).
 *
 * <p>Folder, asset, empty, and unknown MIME drops must not POST. CM1 used
 * Finder {@code perc-listing-category-PAGE} on the site-map box; the SPA
 * uses a typed MIME so dragover can accept pages without {@code getData}
 * (empty until drop).</p>
 */

import { canReplaceLandingPage } from "../api/architecture/sectionMutations";
import type { NavTreeNode } from "../api/architecture/types";
import {
  canPostReplaceLandingPage,
  resolveLandingPagePick,
} from "./landingPagePicker";

/** Explorer page listing (CM1 Finder PAGE category). */
export const FINDER_PAGE_MIME = "application/x-percussion-finder-page";

/** Explorer folder listing — drop must not replace a landing page. */
export const FINDER_FOLDER_MIME = "application/x-percussion-finder-folder";

/** Other Finder items (assets, unknown). */
export const FINDER_ITEM_MIME = "application/x-percussion-finder-item";

export interface FinderItemDragPayload {
  id?: string | null;
  name?: string | null;
  path?: string | null;
  type?: string | null;
  category?: string | null;
}

/** Minimal DataTransfer surface used by mapping (jsdom + live). */
export interface DropDataLike {
  types?: Iterable<string> | ArrayLike<string> | null;
  getData: (format: string) => string;
}

export type LandingPageDropReason =
  | "empty"
  | "notPage"
  | "invalidTarget"
  | "invalidMime"
  | "wrongSite"
  | "busy";

export type LandingPageDropResult =
  | {
      ok: true;
      sectionId: string;
      pageId: string;
      pageLabel: string;
    }
  | { ok: false; reason: LandingPageDropReason };

export interface LandingPageDropRequest {
  sectionId: string;
  pageId: string;
  pageLabel: string;
}

export interface LandingPageDropOptions {
  selectedSite?: string | null;
  busy?: boolean;
}

const FASTFORWARD_PAGE_TYPES = new Set([
  "page",
  "percpage",
  "rffhome",
  "rffevent",
  "rffgeneric",
  "rffgenericword",
  "rffbrief",
  "rffcalendar",
  "rffcontacts",
  "rffpressrelease",
  "rffexternallink",
  "rffautoindex",
]);

function listDropTypes(data: DropDataLike | null | undefined): string[] {
  if (!data?.types) {
    return [];
  }
  try {
    return Array.from(data.types as Iterable<string>).map((t) => String(t));
  } catch {
    return [];
  }
}

function typesLower(data: DropDataLike | null | undefined): string[] {
  return listDropTypes(data).map((t) => t.toLowerCase());
}

function readData(data: DropDataLike, format: string): string {
  try {
    return String(data.getData(format) ?? "");
  } catch {
    return "";
  }
}

function parsePayloadText(raw: string): FinderItemDragPayload | null {
  const t = String(raw ?? "").trim();
  if (!t) {
    return null;
  }
  try {
    const obj = JSON.parse(t) as unknown;
    if (obj == null || typeof obj !== "object" || Array.isArray(obj)) {
      return null;
    }
    const rec = obj as Record<string, unknown>;
    return {
      id: rec.id != null ? String(rec.id) : null,
      name: rec.name != null ? String(rec.name) : null,
      path: rec.path != null ? String(rec.path) : null,
      type: rec.type != null ? String(rec.type) : null,
      category: rec.category != null ? String(rec.category) : null,
    };
  } catch {
    return null;
  }
}

/**
 * JSON payload written on Explorer listing dragstart.
 */
export function serializeFinderItemDrag(item: FinderItemDragPayload): string {
  return JSON.stringify({
    id: item.id ?? "",
    name: item.name ?? "",
    path: item.path ?? "",
    type: item.type ?? "",
    category: item.category ?? "",
  });
}

/**
 * Prefer the page MIME, then generic item, then JSON / text fallbacks.
 */
export function parseFinderItemDrag(
  data: DropDataLike | null | undefined,
): FinderItemDragPayload | null {
  if (!data) {
    return null;
  }
  const order = [
    FINDER_PAGE_MIME,
    FINDER_FOLDER_MIME,
    FINDER_ITEM_MIME,
    "application/json",
    "text/plain",
  ];
  for (const mime of order) {
    const parsed = parsePayloadText(readData(data, mime));
    if (parsed) {
      return parsed;
    }
  }
  return null;
}

export function siteNameFromItemPath(
  path: string | null | undefined,
): string | null {
  const raw = String(path ?? "")
    .trim()
    .replace(/\\/g, "/");
  if (!raw) {
    return null;
  }
  const parts = raw.split("/").filter(Boolean);
  const idx = parts.findIndex((p) => p.toLowerCase() === "sites");
  if (idx >= 0 && parts[idx + 1]) {
    return parts[idx + 1];
  }
  return null;
}

export function pageBelongsToSelectedSite(
  path: string | null | undefined,
  selectedSite: string | null | undefined,
): boolean {
  const siteName = String(selectedSite ?? "").trim();
  if (!siteName) {
    return true;
  }
  const fromPath = siteNameFromItemPath(path);
  if (!fromPath) {
    return true;
  }
  return fromPath.toLowerCase() === siteName.toLowerCase();
}

/**
 * True when the payload is a Finder PAGE (or landing page), not a folder
 * or asset. MIME {@link FINDER_PAGE_MIME} is enough even if type is empty.
 */
export function isFinderPagePayload(
  item: FinderItemDragPayload | null | undefined,
  declaredPageMime = false,
): boolean {
  if (!item) {
    return declaredPageMime;
  }
  const type = String(item.type ?? "")
    .trim()
    .toLowerCase();
  const category = String(item.category ?? "")
    .trim()
    .toLowerCase();
  if (category === "folder" || type === "folder") {
    return false;
  }
  if (
    category === "asset" ||
    type === "asset" ||
    type === "percasset" ||
    type === "rffimage" ||
    type === "rfffile" ||
    type === "rffnavimage"
  ) {
    return false;
  }
  if (category === "page" || category === "landing_page") {
    return true;
  }
  if (FASTFORWARD_PAGE_TYPES.has(type)) {
    return true;
  }
  if (declaredPageMime) {
    const pick = resolveLandingPagePick({ items: [item] });
    return pick.ok || Boolean(String(item.id ?? "").trim());
  }
  const pick = resolveLandingPagePick({ items: [item] });
  return pick.ok;
}

/**
 * dragover: accept when types declare a Finder page and the target is a
 * regular section. {@code getData} is often empty until drop.
 */
export function canAcceptLandingPageDragOver(
  data: DropDataLike | null | undefined,
  target: NavTreeNode | null,
  options?: LandingPageDropOptions,
): boolean {
  if (options?.busy) {
    return false;
  }
  if (!canReplaceLandingPage(target)) {
    return false;
  }
  const types = typesLower(data);
  if (types.includes(FINDER_FOLDER_MIME) && !types.includes(FINDER_PAGE_MIME)) {
    return false;
  }
  if (types.includes(FINDER_PAGE_MIME)) {
    return true;
  }
  return mapLandingPageDrop(data, target, options).ok;
}

/**
 * drop: map DataTransfer + target section to a replaceLandingPage request,
 * or a reason to skip the POST.
 */
export function mapLandingPageDrop(
  data: DropDataLike | null | undefined,
  target: NavTreeNode | null,
  options?: LandingPageDropOptions,
): LandingPageDropResult {
  if (options?.busy) {
    return { ok: false, reason: "busy" };
  }
  if (!target || !canReplaceLandingPage(target)) {
    return { ok: false, reason: "invalidTarget" };
  }
  const types = typesLower(data);
  const declaredPage = types.includes(FINDER_PAGE_MIME);
  const declaredFolder =
    types.includes(FINDER_FOLDER_MIME) && !declaredPage;
  if (declaredFolder) {
    return { ok: false, reason: "notPage" };
  }
  const payload = parseFinderItemDrag(data);
  if (!payload) {
    if (declaredPage) {
      return { ok: false, reason: "empty" };
    }
    if (types.length === 0) {
      return { ok: false, reason: "empty" };
    }
    return { ok: false, reason: "invalidMime" };
  }
  const id = String(payload.id ?? "").trim();
  if (!id) {
    return { ok: false, reason: "empty" };
  }
  if (!isFinderPagePayload(payload, declaredPage)) {
    return { ok: false, reason: "notPage" };
  }
  if (!pageBelongsToSelectedSite(payload.path, options?.selectedSite)) {
    return { ok: false, reason: "wrongSite" };
  }
  if (!canPostReplaceLandingPage(target.id, id)) {
    return { ok: false, reason: "empty" };
  }
  const label =
    String(payload.name || payload.path || id).trim() || id;
  return {
    ok: true,
    sectionId: target.id,
    pageId: id,
    pageLabel: label,
  };
}

/** MIME to set on Explorer listing dragstart. */
export function finderDragMimeForItem(item: {
  type?: string | null;
  category?: string | null;
}): string {
  const type = String(item.type ?? "")
    .trim()
    .toLowerCase();
  const category = String(item.category ?? "")
    .trim()
    .toLowerCase();
  if (category === "folder" || type === "folder") {
    return FINDER_FOLDER_MIME;
  }
  if (isFinderPagePayload(item, false)) {
    return FINDER_PAGE_MIME;
  }
  return FINDER_ITEM_MIME;
}
