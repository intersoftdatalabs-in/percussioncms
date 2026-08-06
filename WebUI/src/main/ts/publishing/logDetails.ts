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

/** One published/attempted item from job details (PSSitePublishItem). */
export interface PublishLogItem {
  status?: string;
  operation?: string;
  fileName?: string;
  fileLocation?: string;
  elapsedTime?: number | string;
  contentid?: string | number;
  revisionid?: string | number;
  itemStatusId?: string | number;
  templateid?: string | number;
  folderid?: string | number;
  [key: string]: unknown;
}

/** Normalize details POST response into a list of SitePublishItem rows. */
export function extractLogItems(details: unknown): PublishLogItem[] {
  if (details == null) {
    return [];
  }
  if (Array.isArray(details)) {
    return details as PublishLogItem[];
  }
  if (typeof details !== "object") {
    return [];
  }
  const obj = details as Record<string, unknown>;
  for (const key of ["SitePublishItem", "items", "results"]) {
    const v = obj[key];
    if (Array.isArray(v)) {
      return v as PublishLogItem[];
    }
    if (v && typeof v === "object") {
      return [v as PublishLogItem];
    }
  }
  return [];
}

/** Client-side filter for log item table (status/operation/location/filename). */
export function filterLogItems(
  items: PublishLogItem[],
  query: string,
): PublishLogItem[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return items;
  }
  return items.filter((item) => {
    const hay = [
      item.status,
      item.operation,
      item.fileName,
      item.fileLocation,
      item.contentid,
    ]
      .map((x) => String(x ?? "").toLowerCase())
      .join(" ");
    return hay.includes(q);
  });
}
