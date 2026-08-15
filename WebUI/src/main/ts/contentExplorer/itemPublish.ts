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
 * Explorer Publish Now — existing sitemanage publish-now GET, not the
 * demandpublishing servlet page (that 404s from the SPA).
 */

import { get } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { itemPublishPaths } from "../publishing/itemPublishPaths";
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

/**
 * Demand-publish a page or asset. Other types return false so the
 * dispatcher can show that the action is not available.
 */
export async function publishSelectedItem(item: PSPathItem): Promise<boolean> {
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  const kind = resolvePublishKind(item);
  const paths = itemPublishPaths();
  if (kind === "page") {
    await get<unknown>(`${paths.pagePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  if (kind === "asset") {
    await get<unknown>(`${paths.resourcePublish}/${encodeURIComponent(id)}`);
    return true;
  }
  return false;
}
