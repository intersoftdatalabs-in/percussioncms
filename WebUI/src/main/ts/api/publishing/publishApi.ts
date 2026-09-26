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

import { del, get, post } from "../client";
import { PATHS } from "../paths";
import type { IncrementalQueuePage } from "./types";

function encodeSeg(value: string): string {
  return encodeURIComponent(value);
}

/** Full site publish to a named server (GET). */
export async function publishSite(
  siteName: string,
  serverName: string,
): Promise<unknown> {
  const url = `${PATHS.SITE_PUBLISH}/${encodeSeg(siteName)}/${encodeSeg(serverName)}`;
  return get<unknown>(url);
}

/** Incremental publish for site/server (GET). */
export async function incrementalPublishSite(
  siteName: string,
  serverName: string,
): Promise<unknown> {
  const url = `${PATHS.INCREMENTAL_PUBLISH}${encodeSeg(siteName)}/${encodeSeg(serverName)}`;
  return get<unknown>(url);
}

/**
 * Incremental publish with related-item approval payload (path segment as product today).
 */
export async function publishIncrementalWithApproval(
  siteName: string,
  serverName: string,
  relatedItems: string,
): Promise<unknown> {
  const url =
    `${PATHS.INCREMENTAL_PUBLISH}${encodeSeg(siteName)}/${encodeSeg(serverName)}/` +
    encodeSeg(relatedItems);
  return get<unknown>(url);
}

/** Paged incremental content queue. */
export async function getIncrementalItems(
  siteName: string,
  serverName: string,
  startIndex = 1,
  pageSize = 10,
): Promise<IncrementalQueuePage> {
  const url =
    `${PATHS.INCREMENTAL_LIST}${encodeSeg(siteName)}/${encodeSeg(serverName)}` +
    `?startIndex=${startIndex}&pageSize=${pageSize}`;
  const data = await get<unknown>(url);
  return (data ?? {}) as IncrementalQueuePage;
}

/**
 * Remove one content id from the incremental queue (HTTP 204).
 * 403 and 404 are thrown as {@link ApiError}.
 */
export async function removeIncrementalQueueItem(
  siteName: string,
  serverName: string,
  contentId: string,
): Promise<void> {
  const url =
    `${PATHS.INCREMENTAL_LIST}${encodeSeg(siteName)}/${encodeSeg(serverName)}/` +
    encodeSeg(contentId);
  await del<void>(url);
}

/**
 * Clear every item on the incremental queue for the site and server (HTTP 204).
 * 403 and 404 are thrown as {@link ApiError}.
 */
export async function clearIncrementalQueue(
  siteName: string,
  serverName: string,
): Promise<void> {
  const url =
    `${PATHS.INCREMENTAL_LIST}${encodeSeg(siteName)}/${encodeSeg(serverName)}`;
  await del<void>(url);
}

/**
 * Approve one content id already on the incremental queue (HTTP 204).
 * The item stays queued. 400, 403, and 404 are thrown as {@link ApiError}.
 */
export async function approveIncrementalQueueItem(
  siteName: string,
  serverName: string,
  contentId: string,
): Promise<void> {
  const url =
    `${PATHS.INCREMENTAL_LIST}${encodeSeg(siteName)}/${encodeSeg(serverName)}/` +
    `${encodeSeg(contentId)}/approve`;
  await post<void>(url);
}

/** Paged incremental related-items queue. */
export async function getIncrementalRelatedItems(
  siteName: string,
  serverName: string,
  startIndex = 1,
  pageSize = 10,
): Promise<IncrementalQueuePage> {
  const url =
    `${PATHS.INCREMENTAL_RELATED_LIST}${encodeSeg(siteName)}/${encodeSeg(serverName)}` +
    `?startIndex=${startIndex}&pageSize=${pageSize}`;
  const data = await get<unknown>(url);
  return (data ?? {}) as IncrementalQueuePage;
}
