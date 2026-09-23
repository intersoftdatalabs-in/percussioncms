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

import { isApiError } from "../api/client";
import type { IncrementalQueuePage } from "./types";

export type QueueRemoveFailure = "forbidden" | "not_found" | "failed";

/** Map a remove-one-item failure so 403 and 404 stay visible (not success). */
export function queueRemoveFailure(err: unknown): QueueRemoveFailure {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}

/** Normalize queue payload into a stable list of items. */
export function extractQueueItems(page: IncrementalQueuePage | null | undefined): unknown[] {
  if (page == null) {
    return [];
  }
  if (Array.isArray(page.items)) {
    return page.items;
  }
  const root = page as Record<string, unknown>;
  // Product path list shape: { PagedItemList: { childrenInPage: [...] } }
  const paged = root.PagedItemList;
  if (paged && typeof paged === "object") {
    const children = (paged as Record<string, unknown>).childrenInPage;
    if (Array.isArray(children)) {
      return children;
    }
    if (children && typeof children === "object") {
      return [children];
    }
  }
  if (Array.isArray(root.childrenInPage)) {
    return root.childrenInPage;
  }
  for (const key of ["SitePublishItem", "contentItems", "results", "items"]) {
    const v = root[key];
    if (Array.isArray(v)) {
      return v;
    }
    if (v && typeof v === "object") {
      return [v];
    }
  }
  return [];
}

export function isQueueEmpty(page: IncrementalQueuePage | null | undefined): boolean {
  return extractQueueItems(page).length === 0;
}

/** Content id shown on an incremental queue row (empty when the item has none). */
export function queueItemId(item: unknown): string {
  if (item == null || typeof item !== "object") {
    return "";
  }
  const obj = item as Record<string, unknown>;
  const raw = obj.id ?? obj.contentId ?? obj.contentid ?? obj.itemId;
  if (raw == null || raw === "") {
    return "";
  }
  return String(raw);
}

/**
 * Title or name for an incremental queue row.
 * Falls back to the content id, then an em dash when both are missing.
 */
export function queueItemLabel(item: unknown): string {
  if (item == null || typeof item !== "object") {
    return "—";
  }
  const obj = item as Record<string, unknown>;
  const name =
    obj.name ?? obj.title ?? obj.label ?? obj.sys_title ?? obj.fileName;
  if (name != null && String(name).trim() !== "") {
    return String(name);
  }
  const id = queueItemId(item);
  return id !== "" ? id : "—";
}

/** Whether another page may exist given startIndex/pageSize/totalCount. */
export function hasMorePages(
  page: IncrementalQueuePage | null | undefined,
  startIndex: number,
  pageSize: number,
): boolean {
  if (page == null) {
    return false;
  }
  const items = extractQueueItems(page);
  if (typeof page.totalCount === "number") {
    return startIndex + items.length - 1 < page.totalCount;
  }
  // If server returns a full page, assume more may exist.
  return items.length >= pageSize;
}
