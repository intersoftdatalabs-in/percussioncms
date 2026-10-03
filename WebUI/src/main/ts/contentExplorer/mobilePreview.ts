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
 * Content → Mobile preview for one selected page (#5078 / parent #4530).
 *
 * <p>Opens the same page surface as desktop Preview, with
 * {@code percmobilepreview=true}. Folders, assets, and an empty selection
 * do not open a window and are not success. Desktop
 * {@link openPreviewItem} is unchanged (render without the flag, or a
 * site path with {@code percmobilepreview=false}, including assets).</p>
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import { SERVICES_ROOT, withCmsContextPrefix } from "../api/paths";
import { resolvePublishKind } from "./itemPublish";
import {
  buildPageRenderPreviewUrl,
  buildSitePathPreviewUrl,
  resolvePagePreviewPath,
  resolvePreviewKind,
} from "./previewItem";
import { isAssetContentType, isFolder } from "./selection";

/** Outcome of Content → Mobile preview. Only {@code opened} is success. */
export type MobilePreviewResult =
  | { status: "opened"; url: string }
  | { status: "none" }
  | { status: "folder"; name: string }
  | { status: "not-page"; name: string }
  | { status: "no-target"; name: string }
  | { status: "blocked"; url: string };

function displayName(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name.length > 0) {
    return name;
  }
  const path = (item.path ?? "").trim();
  return path.length > 0 ? path : "item";
}

function typeAndCategory(item: PSPathItem): string {
  return `${item.type ?? ""} ${item.category ?? ""}`.toLowerCase();
}

/**
 * True only for a page. Assets, folders, and other non-page rows are false.
 * A Sites path or a page type counts even when the row has no content id
 * (site-path preview). An id alone does not make an unknown item a page.
 */
export function isMobilePreviewPage(item: PSPathItem | null | undefined): boolean {
  if (!item || isFolder(item) || isAssetContentType(item)) {
    return false;
  }
  const publish = resolvePublishKind(item);
  if (publish === "asset") {
    return false;
  }
  if (publish === "page") {
    return true;
  }
  if (resolvePreviewKind(item) !== "page") {
    return false;
  }
  const token = typeAndCategory(item);
  const path = (item.path ?? "").trim().toLowerCase().replace(/\\/g, "/");
  return token.includes("page") || path.startsWith("/sites/") || path === "/sites";
}

/**
 * Logical mobile-preview URL (no CMS context prefix). Empty when the item
 * is not a page or has no preview target.
 *
 * <p>A content id uses Page Management render plus
 * {@code percmobilepreview=true}. A path-only Sites page uses the Finder
 * site path with the same flag set to true. Desktop preview does not use
 * this builder.</p>
 */
export function resolveMobilePreviewUrl(
  item: PSPathItem,
  servicesRoot: string = SERVICES_ROOT,
): string {
  if (!isMobilePreviewPage(item)) {
    return "";
  }
  const id = (item.id ?? "").trim();
  if (id) {
    const render = buildPageRenderPreviewUrl(id, servicesRoot);
    if (!render) {
      return "";
    }
    return `${render}?percmobilepreview=true`;
  }
  return buildSitePathPreviewUrl(resolvePagePreviewPath(item), {
    mobilePreview: true,
  });
}

function windowNameForId(id: string): string {
  const safe = id.replace(/-/g, "_").replace(/[^A-Za-z0-9_]/g, "");
  return `percMobilePagePreview_${safe || "page"}`;
}

function defaultOpenWindow(url: string, target?: string): Window | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.open(url, target ?? "_blank");
}

/**
 * Open mobile preview for the selection. Does not change the item.
 * A null window (popup blocked) or a thrown opener is {@code blocked},
 * not {@code opened}.
 */
export function openMobilePreview(input: {
  item: PSPathItem | null | undefined;
  servicesRoot?: string;
  openWindow?: (url: string, target?: string) => Window | null;
}): MobilePreviewResult {
  const item = input.item ?? null;
  if (item == null) {
    return { status: "none" };
  }
  if (isFolder(item)) {
    return { status: "folder", name: displayName(item) };
  }
  if (!isMobilePreviewPage(item)) {
    return { status: "not-page", name: displayName(item) };
  }
  const logical = resolveMobilePreviewUrl(
    item,
    input.servicesRoot ?? SERVICES_ROOT,
  );
  if (!logical) {
    return { status: "no-target", name: displayName(item) };
  }
  const url = withCmsContextPrefix(logical);
  const open = input.openWindow ?? defaultOpenWindow;
  const target = windowNameForId(String(item.id ?? item.path ?? "page"));
  let opened: Window | null;
  try {
    opened = open(url, target);
  } catch {
    return { status: "blocked", url };
  }
  if (opened == null) {
    return { status: "blocked", url };
  }
  return { status: "opened", url };
}
