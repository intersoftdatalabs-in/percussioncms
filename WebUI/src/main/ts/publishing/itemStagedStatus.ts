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

import { formatApiError, isApiError } from "../api/client";
import { message, MSG } from "../i18n/message";
import { mapIdParam } from "./deepLinkMap";
import {
  normalizeItemPublishingHistory,
  publishedDateMillis,
  type ItemPublishingHistory,
} from "./itemHistory";

/**
 * Read-only status/log readback for a staged item (#5838, parent #4531).
 *
 * <p>Reuses the existing read-only {@code GET …/item/pubhistory/{id}}
 * adaptor — no REST/sitemanage companion is needed because the surface
 * already exists. This module performs no writes on any path: it only
 * normalizes the GET payload into a status summary plus log lines and
 * maps fetch failures to operator text.</p>
 */

/** One log line in the staged-item readback (a pubhistory row). */
export type StagedItemLogLine = ItemPublishingHistory;

/** Client-side severity filter for staged-item log lines (#5887). */
export type StagedStatusSeverityFilter = "all" | "failure" | "success";

/**
 * Whether a log line matches a severity bucket. Severity is derived from
 * the row status only (case-insensitive): FAILURE lines are failures,
 * SUCCESS lines are successes; any other status matches "all" alone.
 */
export function matchesStagedStatusSeverity(
  line: StagedItemLogLine,
  filter: StagedStatusSeverityFilter,
): boolean {
  if (filter === "all") {
    return true;
  }
  const status = String(line.status ?? "").toUpperCase();
  if (filter === "failure") {
    return status === "FAILURE";
  }
  return status === "SUCCESS";
}

/**
 * Client-side severity filter for already-loaded staged-item log lines.
 * Pure and read-only: never fetches, never mutates the input array.
 */
export function filterStagedStatusLines(
  lines: StagedItemLogLine[],
  filter: StagedStatusSeverityFilter,
): StagedItemLogLine[] {
  if (filter === "all") {
    return lines;
  }
  return lines.filter((line) => matchesStagedStatusSeverity(line, filter));
}

/** Read-only status summary derived from the log lines. */
export interface StagedItemStatusSummary {
  totalCount: number;
  failureCount: number;
  latestStatus?: string;
  latestOperation?: string;
  latestServer?: string;
  latestLocation?: string;
  latestDateMillis: number;
}

/** Normalize a pubhistory GET payload into staged-item log lines. */
export function normalizeStagedStatusLog(
  payload: unknown,
): StagedItemLogLine[] {
  return normalizeItemPublishingHistory(payload);
}

/** Derive the read-only status summary from already-normalized log lines. */
export function summarizeStagedStatus(
  lines: StagedItemLogLine[],
): StagedItemStatusSummary {
  let latest: StagedItemLogLine | null = null;
  let latestMs = 0;
  let failures = 0;
  for (const line of lines) {
    if (String(line.status ?? "").toUpperCase() === "FAILURE") {
      failures += 1;
    }
    const ms = publishedDateMillis(line.publishedDate);
    if (latest == null || ms >= latestMs) {
      latest = line;
      latestMs = ms;
    }
  }
  return {
    totalCount: lines.length,
    failureCount: failures,
    latestStatus: latest?.status,
    latestOperation: latest?.operation,
    latestServer: latest?.server,
    latestLocation: latest?.location,
    latestDateMillis: latestMs,
  };
}

/**
 * Map staged-status fetch failures: HTTP 400 invalid id, 403 forbidden,
 * 404 unknown item, 409 conflict. Other failures keep the server message.
 * Never treat these as empty status.
 */
export function stagedStatusErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_BADCONFIG));
    }
    if (err.status === 404) {
      return formatApiError(err, message(MSG.PUBLISH_NOW_NOT_FOUND));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_STAGED_STATUS_CONFLICT));
    }
  }
  const text = formatApiError(err, message(MSG.PUBLISH_STAGED_STATUS_ERROR));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (/\bBADCONFIG\b|\bBAD REQUEST\b/i.test(text)) {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  if (/\b404\b|\bNOT FOUND\b/i.test(text)) {
    return message(MSG.PUBLISH_NOW_NOT_FOUND);
  }
  if (/\b409\b|\bCONFLICT\b/i.test(text)) {
    return message(MSG.PUBLISH_STAGED_STATUS_CONFLICT);
  }
  return text;
}

/** Path-style Publishing deep link for staged-item status on the site workspace. */
export function itemStagedStatusShellHref(opts: {
  siteId?: string;
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  const siteId = mapIdParam(opts.siteId);
  const itemId = mapIdParam(opts.itemId);
  if (siteId) {
    params.set("siteId", siteId);
  }
  if (itemId) {
    params.set("itemId", itemId);
  }
  const q = params.toString();
  return q ? `/cm/app/publish/sites?${q}` : "/cm/app/publish/sites";
}

export function spaItemStagedStatusHref(opts: {
  siteId?: string;
  itemId?: string;
}): string {
  const params = new URLSearchParams();
  params.set("entry", "publish");
  params.set("section", "sites");
  const siteId = mapIdParam(opts.siteId);
  const itemId = mapIdParam(opts.itemId);
  if (siteId) {
    params.set("siteId", siteId);
  }
  if (itemId) {
    params.set("itemId", itemId);
  }
  return `/cm/app/spa.jsp?${params.toString()}`;
}
