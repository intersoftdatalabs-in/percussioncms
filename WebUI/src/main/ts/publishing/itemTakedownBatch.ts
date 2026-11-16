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

import { formatApiError, get, isApiError } from "../api/client";
import {
  parseLinkedPagesForTakedown,
  takedownSelectedItem,
  type LinkedPageForTakedown,
} from "../contentExplorer/itemPublish";
import { message, MSG } from "../i18n/message";
import { mapIdParam } from "./deepLinkMap";
import { itemPublishPaths } from "./itemPublishPaths";
import { pathItemForTakedown, type TakedownKind } from "./itemTakedown";

/**
 * Item ids for one batch takedown run. Split first, then sanitize each
 * token ({@link mapIdParam} rejects comma/space-joined input as a whole).
 * Duplicates are taken down once, order is preserved.
 */
export function parseTakedownItemIds(
  raw: string | null | undefined,
): string[] {
  if (raw == null) {
    return [];
  }
  const seen = new Set<string>();
  const ids: string[] = [];
  for (const token of raw.split(/[\s,;]+/)) {
    const safe = mapIdParam(token);
    if (!safe || seen.has(safe)) {
      continue;
    }
    seen.add(safe);
    ids.push(safe);
  }
  return ids;
}

/** One item that did not review/take down, with its named reason. */
export interface TakedownBatchFailure {
  id: string;
  message: string;
}

/** One reviewed item with the linked pages listed on its confirm. */
export interface TakedownBatchReviewedItem {
  id: string;
  linked: LinkedPageForTakedown[];
}

/** Outcome of one multi-item review run. A failed linked-page lookup names
 * the item but still reviews it with no linked pages (classic Finder still
 * confirms), so a later item can be reviewed. */
export interface TakedownBatchReviewReport {
  total: number;
  reviewed: TakedownBatchReviewedItem[];
  failures: TakedownBatchFailure[];
}

/** Outcome of one multi-item takedown run. A per-item failure never stops
 * the rest. */
export interface TakedownBatchReport {
  total: number;
  takenDown: string[];
  failures: TakedownBatchFailure[];
}

/** Visible per-item detail: `id (reason); …`. Item ids are data, not chrome. */
export function formatTakedownBatchDetail(
  failures: readonly TakedownBatchFailure[],
): string {
  return failures.map((failure) => `${failure.id} (${failure.message})`).join("; ");
}

/** Named failure text for one takedown row (shared by single + batch). */
export function takedownBatchFailureMessage(err: unknown): string {
  return failureMessage(err, message(MSG.PUBLISH_ERROR));
}

function failureMessage(err: unknown, fallback: string): string {
  if (isApiError(err)) {
    return apiFailureMessage(err, fallback);
  }
  if (err instanceof Error && err.message.trim()) {
    return textFailureMessage(err.message.trim(), fallback);
  }
  return fallback;
}

/** Named message for free-form failure text (application-level bodies). */
function textFailureMessage(text: string, fallback: string): string {
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(MSG.PUBLISH_FORBIDDEN);
  }
  if (/\bBADCONFIG\b/i.test(text)) {
    return message(MSG.PUBLISH_BADCONFIG);
  }
  if (/\b404\b|\bNOT FOUND\b/i.test(text)) {
    return message(MSG.PUBLISH_NOW_NOT_FOUND);
  }
  if (/\b409\b|\bCONFLICT\b|checked out|editing this/i.test(text)) {
    return message(MSG.PUBLISH_SCHEDULE_CONFLICT);
  }
  return text || fallback;
}

/** Named message for HTTP failures (server detail wins, else fallback). */
function apiFailureMessage(
  err: { status: number; statusText: string; body: unknown },
  fallback: string,
): string {
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
    return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_CONFLICT));
  }
  return formatApiError(err, fallback);
}

/**
 * Raw linked-page lookup that throws on failure (unlike
 * {@code loadLinkedPagesForTakedown}, which returns [] so classic Finder
 * still confirms). The batch review catches per id and names the failure.
 */
async function fetchLinkedPages(
  itemId: string,
): Promise<LinkedPageForTakedown[]> {
  const body = await get<unknown>(
    `${itemPublishPaths().linkedItems}/${encodeURIComponent(itemId)}`,
  );
  return parseLinkedPagesForTakedown(body);
}

/**
 * Review every id: load linked pages per item for the single batch confirm.
 * A failed lookup names the item in {@code failures} but still reviews it
 * with no linked pages, mirroring the single-item fallback.
 */
export async function reviewTakedownBatch(
  ids: readonly string[],
  loadLinked: (itemId: string) => Promise<LinkedPageForTakedown[]> = fetchLinkedPages,
): Promise<TakedownBatchReviewReport> {
  const reviewed: TakedownBatchReviewedItem[] = [];
  const failures: TakedownBatchFailure[] = [];
  for (const id of ids) {
    try {
      reviewed.push({ id, linked: await loadLinked(id) });
    } catch (err: unknown) {
      reviewed.push({ id, linked: [] });
      failures.push({ id, message: failureMessage(err, "Review failed") });
    }
  }
  return { total: ids.length, reviewed, failures };
}

/**
 * Take down every id through the existing GET/PUT takedown routes.
 * Continues after a per-item failure so a later row can still be taken
 * down. {@code linkedById} carries the reviewed linked pages per id;
 * ids without an entry take down with no linked pages.
 */
export async function runTakedownBatch(
  ids: readonly string[],
  kind: TakedownKind,
  linkedById: Readonly<Record<string, LinkedPageForTakedown[]>> = {},
  takeDown: typeof takedownSelectedItem = takedownSelectedItem,
): Promise<TakedownBatchReport> {
  const takenDown: string[] = [];
  const failures: TakedownBatchFailure[] = [];
  for (const id of ids) {
    try {
      const ok = await takeDown(
        pathItemForTakedown(id, kind),
        linkedById[id] ?? [],
      );
      if (!ok) {
        failures.push({ id, message: "not taken down" });
      } else {
        takenDown.push(id);
      }
    } catch (err: unknown) {
      failures.push({ id, message: failureMessage(err, "Take down failed") });
    }
  }
  return { total: ids.length, takenDown, failures };
}
