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
import { publishSelectedItem } from "../contentExplorer/itemPublish";
import { message, MSG } from "../i18n/message";
import { mapIdParam } from "./deepLinkMap";
import {
  pathItemForPublishNow,
  type PublishNowKind,
} from "./itemPublishNow";

/**
 * Item ids for one batch publish-now run. Split first, then sanitize each
 * token ({@link mapIdParam} rejects comma/space-joined input as a whole).
 * Duplicates are published once, order is preserved.
 */
export function parsePublishNowItemIds(
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

/** One item that did not publish, with its named reason. */
export interface PublishNowBatchFailure {
  id: string;
  message: string;
}

/** Outcome of one multi-item publish-now run. A per-item failure never stops
 * the rest, so a later id can still publish. */
export interface PublishNowBatchReport {
  total: number;
  published: string[];
  failures: PublishNowBatchFailure[];
}

/** Visible per-item detail: `id (reason); …`. Item ids are data, not chrome. */
export function formatPublishNowBatchDetail(
  failures: readonly PublishNowBatchFailure[],
): string {
  return failures.map((failure) => `${failure.id} (${failure.message})`).join("; ");
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

/** Named failure text for one publish-now row (shared by single + batch). */
export function publishNowBatchFailureMessage(err: unknown): string {
  if (isApiError(err)) {
    return apiFailureMessage(err, message(MSG.PUBLISH_ERROR));
  }
  if (err instanceof Error && err.message.trim()) {
    return textFailureMessage(err.message.trim(), message(MSG.PUBLISH_ERROR));
  }
  return message(MSG.PUBLISH_ERROR);
}

/**
 * Publish every id through the existing demand-publish page/asset routes.
 * Continues after a per-item failure so a later id can still publish.
 * A `false` return (no id, unknown kind) is a named failure, not success.
 */
export async function runPublishNowBatch(
  ids: readonly string[],
  kind: PublishNowKind,
  publish: typeof publishSelectedItem = publishSelectedItem,
): Promise<PublishNowBatchReport> {
  const published: string[] = [];
  const failures: PublishNowBatchFailure[] = [];
  for (const id of ids) {
    try {
      const ok = await publish(pathItemForPublishNow(id, kind));
      if (!ok) {
        failures.push({ id, message: "not published" });
      } else {
        published.push(id);
      }
    } catch (err: unknown) {
      failures.push({ id, message: publishNowBatchFailureMessage(err) });
    }
  }
  return { total: ids.length, published, failures };
}
