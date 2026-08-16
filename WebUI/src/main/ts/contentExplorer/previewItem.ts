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
 * Product preview URL builders and open handler for Explorer selection (#2733).
 *
 * <p>Replaces the US1 default no-op preview with the same product surfaces the
 * classic Finder uses:</p>
 * <ul>
 *   <li><strong>Pages</strong> — {@code GET …/pagemanagement/render/page/{id}}
 *       (legacy {@code PAGE_PREVIEW}) when a content id is present; otherwise a
 *       site-path friendly URL with {@code percmobilepreview} (Finder selection
 *       path).</li>
 *   <li><strong>Assets</strong> — {@code GET …/assetmanagement/asset/assetViewUrl/{id}}
 *       (plain text URL) then {@code window.open}.</li>
 * </ul>
 *
 * <p>Pure builders are unit-tested without DOM; open path accepts injectable
 * deps for Vitest.</p>
 */

import { get } from "../api/client";
import { PATHS, SERVICES_ROOT } from "../api/paths";
import type { PSPathItem } from "../api/contentExplorer/types";
import { isFolder, isPageOrAssetContentType } from "./selection";

/** Discriminator used by UI chrome and skip logic in Playwright. */
export type PreviewKind = "page" | "asset" | "none";

export interface PreviewOpenDeps {
  /** Override services root (tests / non-default context). */
  servicesRoot?: string;
  /** Fetch helper for asset view URL (default {@link get}). */
  fetchText?: (url: string) => Promise<string>;
  /** Window open (default {@code window.open}). */
  openWindow?: (
    url: string,
    target?: string,
    features?: string,
  ) => Window | null;
}

/**
 * Normalize a CMS path to a single leading slash form (no trailing slash
 * except bare root). Paths are logical CMS paths (always {@code /}), not OS
 * file paths.
 */
export function normalizeCmsPath(path: string | undefined | null): string {
  if (path == null) return "";
  let p = String(path).trim().replace(/\\/g, "/");
  // Collapse repeated slashes (logical CMS path, not UNC / OS).
  p = p.replace(/\/{2,}/g, "/");
  if (!p.startsWith("/") && p.length > 0) {
    p = `/${p}`;
  }
  if (p.length > 1 && p.endsWith("/")) {
    p = p.replace(/\/+$/, "");
  }
  return p;
}

/**
 * Build the site-path friendly preview URL used by classic Finder when a page
 * path is known (folderPaths + name, strip double-slash).
 *
 * @returns Absolute-path URL starting with {@code /} plus query, or empty when
 *   the path is not under Sites.
 */
export function buildSitePathPreviewUrl(
  itemPath: string,
  options?: { revisionId?: string | number; mobilePreview?: boolean },
): string {
  const path = normalizeCmsPath(itemPath);
  if (!path) return "";
  // Site pages live under /Sites/…; Assets use the assetViewUrl service.
  const lower = path.toLowerCase();
  if (!lower.startsWith("/sites/")) {
    return "";
  }
  const mobile =
    options?.mobilePreview === undefined ? false : Boolean(options.mobilePreview);
  const q = new URLSearchParams();
  if (options?.revisionId != null && String(options.revisionId).length > 0) {
    q.set("sys_revision", String(options.revisionId));
  }
  q.set("percmobilepreview", String(mobile));
  return `${path}?${q.toString()}`;
}

/**
 * Render-service page preview URL (legacy {@code PAGE_PREVIEW}).
 * Requires a non-blank content id.
 */
export function buildPageRenderPreviewUrl(
  contentId: string,
  servicesRoot: string = SERVICES_ROOT,
): string {
  const id = (contentId ?? "").trim();
  if (!id) return "";
  const root = (servicesRoot || SERVICES_ROOT).replace(/\/+$/, "");
  return `${root}/pagemanagement/render/page/${encodeURIComponent(id)}`;
}

/**
 * Asset view-url service path (legacy {@code ASSET_VIEW_URL_FOR_ASSET_ID}).
 * The service returns a plain-text URL body that must be opened separately.
 */
export function buildAssetViewUrlRequestPath(
  assetId: string,
  servicesRoot: string = SERVICES_ROOT,
): string {
  const id = (assetId ?? "").trim();
  if (!id) return "";
  const root = (servicesRoot || SERVICES_ROOT).replace(/\/+$/, "");
  return `${root}/assetmanagement/asset/assetViewUrl/${encodeURIComponent(id)}`;
}

function typeToken(item: PSPathItem): string {
  return `${item.type ?? ""} ${item.category ?? ""}`.toLowerCase();
}

/** Stock asset type names — not {@code category: ASSET} (see {@link resolvePreviewKind}). */
const ASSET_PREVIEW_TYPE_KEYS = new Set([
  "asset",
  "percasset",
  "rffimage",
  "rfffile",
]);

function isAssetPreviewType(pathLower: string, typeOnly: string): boolean {
  if (ASSET_PREVIEW_TYPE_KEYS.has(typeOnly)) {
    return true;
  }
  return pathLower.startsWith("/assets/") || pathLower === "/assets";
}

/**
 * Classify whether Explorer can open a product preview for this selection.
 */
export function resolvePreviewKind(item: PSPathItem | null | undefined): PreviewKind {
  if (!item) {
    return "none";
  }
  const token = typeToken(item);
  const typeOnly = (item.type ?? "").trim().toLowerCase();
  const path = normalizeCmsPath(item.path);
  const pathLower = path.toLowerCase();
  const hasId = Boolean((item.id ?? "").trim());

  // Listed percPage / Page / customer items win over folder heuristics (#3456).
  // Do not use category ASSET for kind: the server defaults every non-percPage
  // item (including FastForward pages and customer types) to ASSET.
  if (isPageOrAssetContentType(item) && !token.includes("folder")) {
    if (isAssetPreviewType(pathLower, typeOnly)) {
      return hasId ? "asset" : "none";
    }
    if (hasId || pathLower.startsWith("/sites/")) {
      return "page";
    }
  }

  if (isFolder(item)) {
    return "none";
  }

  if (isAssetPreviewType(pathLower, typeOnly)) {
    // Need an id for the assetViewUrl service.
    return hasId ? "asset" : "none";
  }

  if (
    token.includes("page") ||
    pathLower.startsWith("/sites/") ||
    pathLower === "/sites"
  ) {
    // Page: id (render) or Sites path (friendly URL).
    if (hasId || pathLower.startsWith("/sites/")) {
      return "page";
    }
    return "none";
  }

  // Unknown non-folder: try page render when id present (content items).
  if (hasId) {
    return "page";
  }
  return "none";
}

export function isPreviewableItem(item: PSPathItem | null | undefined): boolean {
  return resolvePreviewKind(item) !== "none";
}

/**
 * Resolve the URL (or service path to fetch) for opening preview.
 *
 * <p>For pages returns a ready-to-open URL. For assets returns the assetViewUrl
 * service path (caller must GET then open the body). Returns empty when not
 * previewable.</p>
 */
export function resolvePreviewTarget(
  item: PSPathItem,
  servicesRoot: string = SERVICES_ROOT,
): { kind: PreviewKind; url: string; needsFetch: boolean } {
  const kind = resolvePreviewKind(item);
  if (kind === "none") {
    return { kind, url: "", needsFetch: false };
  }
  if (kind === "asset") {
    return {
      kind,
      url: buildAssetViewUrlRequestPath(String(item.id), servicesRoot),
      needsFetch: true,
    };
  }
  // page
  const id = (item.id ?? "").trim();
  if (id) {
    return {
      kind: "page",
      url: buildPageRenderPreviewUrl(id, servicesRoot),
      needsFetch: false,
    };
  }
  const siteUrl = buildSitePathPreviewUrl(item.path);
  return {
    kind: siteUrl ? "page" : "none",
    url: siteUrl,
    needsFetch: false,
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
  return window.open(url, target ?? "_blank", features ?? "");
}

function windowNameForId(id: string, prefix: string): string {
  // IE rejected dashes in window names; keep portable names.
  const safe = id.replace(/-/g, "_").replace(/[^A-Za-z0-9_]/g, "");
  return `${prefix}${safe || "item"}`;
}

/**
 * Open product preview for a selection. Throws when the item is not
 * previewable or when the asset view URL cannot be resolved.
 */
export async function openPreviewItem(
  item: PSPathItem,
  deps: PreviewOpenDeps = {},
): Promise<void> {
  const servicesRoot = deps.servicesRoot ?? SERVICES_ROOT;
  const openWindow = deps.openWindow ?? defaultOpenWindow;
  const fetchText =
    deps.fetchText ??
    (async (url: string) => {
      const body = await get<string>(url);
      return typeof body === "string" ? body : String(body ?? "");
    });

  const target = resolvePreviewTarget(item, servicesRoot);
  if (target.kind === "none" || !target.url) {
    throw new Error("Preview is not available for this item");
  }

  if (target.kind === "asset" && target.needsFetch) {
    // Prefer PATHS when available so tests that override PATHS stay consistent.
    const requestUrl =
      target.url ||
      `${PATHS.ASSET_VIEW_URL}/${encodeURIComponent(String(item.id).trim())}`;
    const viewUrl = (await fetchText(requestUrl)).trim();
    if (!viewUrl) {
      throw new Error("Asset preview URL was empty");
    }
    openWindow(viewUrl, windowNameForId(String(item.id ?? "asset"), "percAssetPreview_"));
    return;
  }

  openWindow(
    target.url,
    windowNameForId(String(item.id ?? item.path ?? "page"), "percPagePreview_"),
  );
}
