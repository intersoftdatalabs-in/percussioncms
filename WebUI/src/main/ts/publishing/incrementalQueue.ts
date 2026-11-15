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

export type QueueApproveFailure = "bad_request" | "forbidden" | "not_found" | "failed";

export type QueueUnapproveFailure = QueueApproveFailure;

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

/** Map an approve-one-item failure so 400, 403, and 404 stay visible (not success). */
export function queueApproveFailure(err: unknown): QueueApproveFailure {
  if (isApiError(err)) {
    if (err.status === 400) {
      return "bad_request";
    }
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}

/** Map an unapprove-one-item failure so 400, 403, and 404 stay visible (not success). */
export function queueUnapproveFailure(err: unknown): QueueUnapproveFailure {
  return queueApproveFailure(err);
}

/** Whether a reloaded queue row already carries an approved workflow marker. */
export function queueItemApproved(item: unknown): boolean {
  if (item == null || typeof item !== "object") {
    return false;
  }
  const obj = item as Record<string, unknown>;
  const raw = obj.approved ?? obj.workflowState ?? obj.stateName ?? obj.status;
  if (typeof raw === "boolean") {
    return raw;
  }
  if (raw == null) {
    return false;
  }
  return /approv/i.test(String(raw));
}

/** Approval facet for the Site workspace incremental-queue list (#6259). */
export type QueueApprovalFilter = "all" | "approved" | "pending";

/**
 * Narrow already-loaded queue rows by approval facet. {@code all} returns
 * the original list. Approval reuses {@link queueItemApproved} so the
 * facet matches the Approved badge shown on each row.
 */
export function filterQueueByApproval(
  items: unknown[],
  mode: QueueApprovalFilter,
): unknown[] {
  if (mode === "approved") {
    return items.filter((item) => queueItemApproved(item));
  }
  if (mode === "pending") {
    return items.filter((item) => !queueItemApproved(item));
  }
  return items;
}

/** Normalize a payload slot to a list: arrays pass through, single objects wrap. */
function asItemList(value: unknown): unknown[] | null {
  if (Array.isArray(value)) {
    return value;
  }
  if (value && typeof value === "object") {
    return [value];
  }
  return null;
}

/** Product path list shape: { PagedItemList: { childrenInPage: [...] } }. */
function extractPagedItemList(
  root: Record<string, unknown>,
): unknown[] | null {
  const paged = root.PagedItemList;
  if (!paged || typeof paged !== "object") {
    return null;
  }
  return asItemList((paged as Record<string, unknown>).childrenInPage);
}

const LEGACY_QUEUE_KEYS = [
  "SitePublishItem",
  "contentItems",
  "results",
  "items",
] as const;

function extractLegacyKey(root: Record<string, unknown>): unknown[] | null {
  for (const key of LEGACY_QUEUE_KEYS) {
    const list = asItemList(root[key]);
    if (list) {
      return list;
    }
  }
  return null;
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
  const paged = extractPagedItemList(root);
  if (paged) {
    return paged;
  }
  if (Array.isArray(root.childrenInPage)) {
    return root.childrenInPage;
  }
  return extractLegacyKey(root) ?? [];
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
