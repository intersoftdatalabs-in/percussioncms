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

/**
 * Typed client for the system security audit log REST API (Phase 3 / #2618).
 *
 * <pre>
 *   GET /services/auditlog/entries
 *   GET /services/auditlog/entries/{auditId}
 * </pre>
 *
 * AuthZ (server): Admin role or role property {@code sys_securityAuditLogViewer}.
 * Unauthorized → HTTP 403.
 */

import { get, isApiError } from "../client";
import { PATHS } from "../paths";
import { buildAuditLogQueryString } from "./buildQuery";
import type {
  AuditLogQueryParams,
  SystemAuditLogEntry,
  SystemAuditLogPage,
} from "./types";

export class AuditLogForbiddenError extends Error {
  readonly status = 403;
  constructor(message = "Not allowed to view the security audit log") {
    super(message);
    this.name = "AuditLogForbiddenError";
  }
}

function rethrowIfForbidden(err: unknown): never {
  if (isApiError(err) && err.status === 403) {
    throw new AuditLogForbiddenError(
      typeof err.body === "string" && err.body.trim()
        ? err.body.trim()
        : "Not allowed to view the security audit log",
    );
  }
  throw err;
}

function asPage(payload: unknown): SystemAuditLogPage {
  if (payload == null || typeof payload !== "object") {
    return { entries: [], total: 0, offset: 0, limit: 50 };
  }
  const p = payload as SystemAuditLogPage;
  const entries = Array.isArray(p.entries) ? p.entries : [];
  return {
    entries,
    total: typeof p.total === "number" ? p.total : entries.length,
    offset: typeof p.offset === "number" ? p.offset : 0,
    limit: typeof p.limit === "number" ? p.limit : 50,
  };
}

/** Query a page of durable system audit log entries. */
export async function queryAuditLogEntries(
  params: AuditLogQueryParams = {},
): Promise<SystemAuditLogPage> {
  const qs = buildAuditLogQueryString(params);
  try {
    const payload = await get<unknown>(`${PATHS.AUDIT_LOG_ENTRIES}${qs}`);
    return asPage(payload);
  } catch (err) {
    rethrowIfForbidden(err);
  }
}

/** Load one audit entry by AUDIT_ID (UUID). */
export async function getAuditLogEntry(
  auditId: string,
): Promise<SystemAuditLogEntry> {
  const id = (auditId ?? "").trim();
  if (!id) {
    throw new Error("auditId is required");
  }
  try {
    return await get<SystemAuditLogEntry>(
      `${PATHS.AUDIT_LOG_ENTRIES}/${encodeURIComponent(id)}`,
    );
  } catch (err) {
    rethrowIfForbidden(err);
  }
}
