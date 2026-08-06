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

import type { SitePublishLogRequest } from "./types";

/** Minuet date-range options (publishLogTemplates.jsp). */
export const LOG_DAYS_OPTIONS = [3, 5, 10] as const;

/** Minuet max-count options. */
export const LOG_MAXCOUNT_OPTIONS = [20, 30, 50] as const;

export const DEFAULT_LOG_DAYS = 5;
export const DEFAULT_LOG_MAXCOUNT = 20;

export interface LogsFilterInput {
  siteId?: string | number;
  /** Publishing server id; empty/whitespace = all servers (Minuet). */
  pubServerId?: string | number | null;
  days?: number;
  maxcount?: number;
  showOnlyFailures?: boolean;
  skipCount?: number;
}

/**
 * Build SitePublishLogRequest body for POST /pubstatus/logs.
 * Omits blank pubServerId so the service returns all servers for the site.
 */
export function buildLogRequest(input: LogsFilterInput): SitePublishLogRequest {
  const days =
    typeof input.days === "number" && input.days > 0
      ? input.days
      : DEFAULT_LOG_DAYS;
  const maxcount =
    typeof input.maxcount === "number" && input.maxcount > 0
      ? input.maxcount
      : DEFAULT_LOG_MAXCOUNT;

  const req: SitePublishLogRequest = {
    days,
    maxcount,
  };

  if (input.siteId != null && String(input.siteId).trim() !== "") {
    req.siteId = String(input.siteId);
  }

  const server = input.pubServerId;
  if (server != null && String(server).trim() !== "") {
    req.pubServerId = String(server);
  }

  if (input.showOnlyFailures === true) {
    req.showOnlyFailures = true;
  }

  if (typeof input.skipCount === "number" && input.skipCount > 0) {
    req.skipCount = input.skipCount;
  }

  return req;
}
