/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import type { IncrementalQueuePage } from "./types";

/** Normalize queue payload into a stable list of items. */
export function extractQueueItems(page: IncrementalQueuePage | null | undefined): unknown[] {
  if (page == null) {
    return [];
  }
  if (Array.isArray(page.items)) {
    return page.items;
  }
  for (const key of ["SitePublishItem", "contentItems", "results", "items"]) {
    const v = (page as Record<string, unknown>)[key];
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
