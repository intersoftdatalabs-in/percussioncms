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
 *
 * <p>REST {@code JacksonContextResolver} enables {@code WRAP_ROOT_VALUE}, so list/detail
 * responses arrive as {@code {"SystemAuditLogPage":{…}}} / {@code {"SystemAuditLogEntry":{…}}}.
 * Without client unwrap the Admin Security Audit Log viewer always shows 0 rows even when the
 * durable store and Log4j dual-write succeeded (#3089, same class as #2708 / #3039).
 */

import { get, isApiError } from "../client";
import { PATHS } from "../paths";
import { buildAuditLogQueryString } from "./buildQuery";
import type {
  AuditLogQueryParams,
  SystemAuditLogEntry,
  SystemAuditLogPage,
} from "./types";

/** Jackson WRAP_ROOT_VALUE root for {@code SystemAuditLogPage}. */
export const SYSTEM_AUDIT_LOG_PAGE_ROOT = "SystemAuditLogPage";

/** Jackson WRAP_ROOT_VALUE root for {@code SystemAuditLogEntry}. */
export const SYSTEM_AUDIT_LOG_ENTRY_ROOT = "SystemAuditLogEntry";

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

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function isAuditPageShape(o: Record<string, unknown>): boolean {
  return (
    Array.isArray(o.entries) ||
    typeof o.total === "number" ||
    typeof o.offset === "number" ||
    typeof o.limit === "number"
  );
}

function isAuditEntryShape(o: Record<string, unknown>): boolean {
  return (
    typeof o.auditId === "string" ||
    typeof o.moduleCode === "string" ||
    typeof o.eventType === "string" ||
    typeof o.userMessage === "string" ||
    typeof o.logMessage === "string"
  );
}

/**
 * Normalize a list response to a flat {@link SystemAuditLogPage}.
 *
 * <p>Prefers {@code {"SystemAuditLogPage":{…}}} (production WRAP_ROOT_VALUE); also accepts a
 * flat body (unit tests / proxies that already unwrapped).
 */
export function unwrapSystemAuditLogPage(payload: unknown): SystemAuditLogPage {
  if (payload == null || typeof payload !== "object") {
    return { entries: [], total: 0, offset: 0, limit: 50 };
  }
  const root = asRecord(payload);
  if (!root) {
    return { entries: [], total: 0, offset: 0, limit: 50 };
  }
  const nested = asRecord(
    root[SYSTEM_AUDIT_LOG_PAGE_ROOT] ?? root.systemAuditLogPage,
  );
  const body = nested && isAuditPageShape(nested) ? nested : isAuditPageShape(root) ? root : null;
  if (!body) {
    return { entries: [], total: 0, offset: 0, limit: 50 };
  }
  const rawEntries = body.entries;
  const entries: SystemAuditLogEntry[] = Array.isArray(rawEntries)
    ? rawEntries.map((row) => unwrapSystemAuditLogEntry(row) ?? (row as SystemAuditLogEntry))
    : [];
  return {
    entries,
    total: typeof body.total === "number" ? body.total : entries.length,
    offset: typeof body.offset === "number" ? body.offset : 0,
    limit: typeof body.limit === "number" ? body.limit : 50,
  };
}

/**
 * Normalize a detail response to a flat {@link SystemAuditLogEntry}.
 *
 * <p>Prefers {@code {"SystemAuditLogEntry":{…}}}; accepts flat bodies for tests.
 */
export function unwrapSystemAuditLogEntry(
  payload: unknown,
): SystemAuditLogEntry | null {
  const root = asRecord(payload);
  if (!root) {
    return null;
  }
  const nested = asRecord(
    root[SYSTEM_AUDIT_LOG_ENTRY_ROOT] ?? root.systemAuditLogEntry,
  );
  if (nested && isAuditEntryShape(nested)) {
    return nested as SystemAuditLogEntry;
  }
  if (isAuditEntryShape(root)) {
    return root as SystemAuditLogEntry;
  }
  return null;
}

/** Query a page of durable system audit log entries. */
export async function queryAuditLogEntries(
  params: AuditLogQueryParams = {},
): Promise<SystemAuditLogPage> {
  const qs = buildAuditLogQueryString(params);
  try {
    const payload = await get<unknown>(`${PATHS.AUDIT_LOG_ENTRIES}${qs}`);
    return unwrapSystemAuditLogPage(payload);
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
    const payload = await get<unknown>(
      `${PATHS.AUDIT_LOG_ENTRIES}/${encodeURIComponent(id)}`,
    );
    const entry = unwrapSystemAuditLogEntry(payload);
    if (!entry) {
      throw new Error("Audit log entry response was empty or mis-shaped");
    }
    return entry;
  } catch (err) {
    rethrowIfForbidden(err);
  }
}
