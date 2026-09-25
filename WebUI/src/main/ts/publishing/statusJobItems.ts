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
 * Content rows for a Status job detail (#4886).
 *
 * <p>Reuses {@code POST …/pubstatus/details} ({@link extractLogItems}), the same
 * payload as Logs. A missing content id cannot be opened. HTTP 403/404 on that
 * list stay in the detail panel.</p>
 */

import { isApiError } from "../api/client";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import { extractLogItems, type PublishLogItem } from "./logDetails";

export interface StatusJobItemRow {
  /** Display id from the payload. Empty when the row has none. */
  contentId: string;
  /** Name or title when the payload has one; otherwise empty. */
  title: string;
  /** Parsed CMS content id, or null when Open must not be offered. */
  openId: number | null;
}

export type StatusJobItemsFailure = "forbidden" | "not_found" | "failed";

const TITLE_KEYS = ["title", "name", "contentTitle", "pageTitle", "fileName"];

function asText(value: unknown): string {
  if (typeof value === "string") {
    return value.trim();
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return String(value);
  }
  return "";
}

function itemTitle(item: PublishLogItem): string {
  const record = item as Record<string, unknown>;
  for (const key of TITLE_KEYS) {
    const text = asText(record[key]);
    if (text !== "") {
      return text;
    }
  }
  return "";
}

/** Rows for the status job item table. An empty payload is an empty list. */
export function statusJobItemRows(details: unknown): StatusJobItemRow[] {
  return extractLogItems(details).map((item) => {
    const rawId = item.contentid;
    const openId = parseExplorerContentId(rawId ?? undefined);
    const contentId =
      openId != null ? String(openId) : asText(rawId) === "0" ? "" : asText(rawId);
    return {
      contentId: openId != null ? String(openId) : contentId,
      title: itemTitle(item),
      openId,
    };
  });
}

/** Map a details-list failure. 403 and 404 stay in the detail panel. */
export function statusJobItemsFailure(err: unknown): StatusJobItemsFailure {
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
