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

import { asJsonRecord } from "../api/jsonList";
import { SERVICES_ROOT } from "../api/paths";
import { mapIdParam } from "./deepLinkMap";

/** One item-level publish/takedown history row (PSItemPublishingHistory). */
export interface ItemPublishingHistory {
  contentId?: number | string;
  revisionId?: number | string;
  server?: string;
  location?: string;
  publishedDate?: string | number | Date | null;
  operation?: string;
  status?: string;
  errorMessage?: string;
}

const WRAP_KEYS = [
  "ItemPublishingHistory",
  "itemPublishingHistory",
  "ItemPublishingHistoryList",
];

function asText(value: unknown): string {
  if (value == null) {
    return "";
  }
  return String(value).trim();
}

function asEntry(row: unknown): ItemPublishingHistory | null {
  const rec = asJsonRecord(row);
  if (!rec) {
    return null;
  }
  const nested = rec.ItemPublishingHistory;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return asEntry(nested);
  }
  const server = asText(rec.server);
  const location = asText(rec.location);
  const operation = asText(rec.operation);
  const status = asText(rec.status);
  const errorMessage = asText(rec.errorMessage);
  const contentId = rec.contentId as number | string | undefined;
  const revisionId = rec.revisionId as number | string | undefined;
  const publishedDate = rec.publishedDate as ItemPublishingHistory["publishedDate"];
  if (
    !server &&
    !location &&
    !operation &&
    !status &&
    contentId == null &&
    revisionId == null &&
    publishedDate == null
  ) {
    return null;
  }
  return {
    contentId,
    revisionId,
    server: server || undefined,
    location: location || undefined,
    publishedDate,
    operation: operation || undefined,
    status: status || undefined,
    errorMessage: errorMessage || undefined,
  };
}

/**
 * Unwrap JAXB/Jackson envelopes for {@code GET …/item/pubhistory/{id}}.
 * Empty list, single object, and {@code ItemPublishingHistory} wrap all become arrays.
 */
export function normalizeItemPublishingHistory(
  payload: unknown,
): ItemPublishingHistory[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload
      .map((row) => asEntry(row))
      .filter((row): row is ItemPublishingHistory => row != null);
  }
  const rec = asJsonRecord(payload);
  if (!rec) {
    return [];
  }
  for (const key of WRAP_KEYS) {
    if (key in rec) {
      const wrapped = rec[key];
      if (wrapped == null) {
        return [];
      }
      if (Array.isArray(wrapped) || asJsonRecord(wrapped)) {
        return normalizeItemPublishingHistory(wrapped);
      }
    }
  }
  const single = asEntry(rec);
  return single ? [single] : [];
}

export function publishedDateMillis(
  value: ItemPublishingHistory["publishedDate"],
): number {
  if (value == null || value === "") {
    return 0;
  }
  if (value instanceof Date) {
    const t = value.getTime();
    return Number.isFinite(t) ? t : 0;
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  const parsed = Date.parse(String(value));
  return Number.isFinite(parsed) ? parsed : 0;
}

export function formatPublishedDate(
  value: ItemPublishingHistory["publishedDate"],
): string {
  const ms = publishedDateMillis(value);
  if (ms <= 0) {
    return "—";
  }
  return new Date(ms).toLocaleString();
}

export function sortHistoryNewestFirst(
  rows: ItemPublishingHistory[],
): ItemPublishingHistory[] {
  return [...rows].sort(
    (a, b) => publishedDateMillis(b.publishedDate) - publishedDateMillis(a.publishedDate),
  );
}

/** Classic perc_paths.ITEM_PUB_HISTORY + id. */
export function itemPubHistoryUrl(itemId: string, root = SERVICES_ROOT): string {
  const id = itemId.trim();
  return `${root}/itemmanagement/item/pubhistory/${encodeURIComponent(id)}`;
}

/**
 * Path-style Publishing deep link for item history (status/logs).
 * Prefer this over {@code view=} query for BrowserRouter.
 */
export function itemHistoryShellHref(opts: {
  section: "status" | "logs";
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  const id = mapIdParam(opts.itemId);
  if (id) {
    params.set("itemId", id);
  }
  const q = params.toString();
  return q ? `/cm/app/publish/${opts.section}?${q}` : `/cm/app/publish/${opts.section}`;
}

export function spaItemHistoryHref(opts: {
  section: "status" | "logs";
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  params.set("entry", "publish");
  params.set("section", opts.section);
  const id = mapIdParam(opts.itemId);
  if (id) {
    params.set("itemId", id);
  }
  return `/cm/app/spa.jsp?${params.toString()}`;
}
