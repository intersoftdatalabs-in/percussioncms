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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { AuditLogQueryParams } from "./types";

/**
 * Convert an HTML {@code datetime-local} value to an ISO-8601 instant string.
 * Returns {@code undefined} when blank or unparsable.
 */
export function datetimeLocalToIso(
  value: string | null | undefined,
): string | undefined {
  if (value == null) {
    return undefined;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const d = new Date(trimmed);
  if (Number.isNaN(d.getTime())) {
    return undefined;
  }
  return d.toISOString();
}

/**
 * Build the query string (including leading {@code ?} when non-empty) for
 * {@code GET …/auditlog/entries}. Omits blank filters; clamps offset/limit.
 */
export function buildAuditLogQueryString(params: AuditLogQueryParams): string {
  const q = new URLSearchParams();

  const put = (key: string, raw: string | undefined): void => {
    if (raw == null) {
      return;
    }
    const v = raw.trim();
    if (v) {
      q.set(key, v);
    }
  };

  put("from", params.from);
  put("to", params.to);
  put("module", params.module);
  put("eventType", params.eventType);
  put("outcome", params.outcome);
  put("actor", params.actor);

  const offset =
    params.offset == null || !Number.isFinite(params.offset)
      ? 0
      : Math.max(0, Math.floor(params.offset));
  if (offset > 0) {
    q.set("offset", String(offset));
  }

  if (params.limit != null && Number.isFinite(params.limit)) {
    const limit = Math.max(1, Math.floor(params.limit));
    q.set("limit", String(limit));
  }

  const s = q.toString();
  return s ? `?${s}` : "";
}

/** Default page size aligned with server default (50). */
export const DEFAULT_AUDIT_LOG_PAGE_SIZE = 50;

/** Page size options for the Admin viewer. */
export const AUDIT_LOG_PAGE_SIZE_OPTIONS = [25, 50, 100] as const;
