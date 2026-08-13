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

function asFiniteNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const n = Number(value);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return undefined;
}

function isAuditPageShape(o: Record<string, unknown>): boolean {
  return (
    o.entries != null ||
    typeof o.total === "number" ||
    typeof o.total === "string" ||
    typeof o.offset === "number" ||
    typeof o.limit === "number"
  );
}

/**
 * Coerce Jackson list envelopes to a flat {@link SystemAuditLogEntry} array.
 *
 * <p>WRAP_ROOT_VALUE / JAXB one-item lists often arrive as
 * {@code { SystemAuditLogEntry: [ {...}, ... ] }}, a single wrapped object,
 * or a non-array. Mapping those as arrays throws {@code TypeError: map is not
 * a function} and blanks Admin → System Tools (#3195, same class as #3202).
 */
export function asAuditLogEntries(raw: unknown): SystemAuditLogEntry[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    const out: SystemAuditLogEntry[] = [];
    for (const row of raw) {
      const entry = unwrapSystemAuditLogEntry(row);
      if (entry) {
        out.push(entry);
      } else if (row != null && typeof row === "object") {
        out.push(row as SystemAuditLogEntry);
      }
    }
    return out;
  }
  const rec = asRecord(raw);
  if (!rec) {
    return [];
  }
  const wrapped =
    rec[SYSTEM_AUDIT_LOG_ENTRY_ROOT] ??
    rec.systemAuditLogEntry ??
    rec.entry;
  if (wrapped != null && wrapped !== raw) {
    return asAuditLogEntries(wrapped);
  }
  const single = unwrapSystemAuditLogEntry(raw);
  return single ? [single] : [];
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

function pageFromBody(body: Record<string, unknown>): SystemAuditLogPage {
  const entries = asAuditLogEntries(body.entries);
  return {
    entries,
    total: asFiniteNumber(body.total) ?? entries.length,
    offset: asFiniteNumber(body.offset) ?? 0,
    limit: asFiniteNumber(body.limit) ?? 50,
  };
}

/**
 * Normalize a list response to a flat {@link SystemAuditLogPage}.
 *
 * <p>Prefers {@code {"SystemAuditLogPage":{…}}} (production WRAP_ROOT_VALUE),
 * including nested WRAP_ROOT envelopes; also accepts a flat body. {@code
 * entries} is always an array after unwrap (#3195).
 */
export function unwrapSystemAuditLogPage(payload: unknown): SystemAuditLogPage {
  return unwrapSystemAuditLogPageInner(payload, 0);
}

function unwrapSystemAuditLogPageInner(
  payload: unknown,
  depth: number,
): SystemAuditLogPage {
  const empty: SystemAuditLogPage = {
    entries: [],
    total: 0,
    offset: 0,
    limit: 50,
  };
  if (payload == null || typeof payload !== "object" || depth > 6) {
    return empty;
  }
  const root = asRecord(payload);
  if (!root) {
    return empty;
  }
  const nestedRaw = root[SYSTEM_AUDIT_LOG_PAGE_ROOT] ?? root.systemAuditLogPage;
  const nested = asRecord(nestedRaw);
  if (nested) {
    const fromNested = unwrapSystemAuditLogPageInner(nested, depth + 1);
    if ((fromNested.entries?.length ?? 0) > 0 || isAuditPageShape(nested)) {
      return isAuditPageShape(nested) ? pageFromBody(nested) : fromNested;
    }
  }
  if (isAuditPageShape(root)) {
    return pageFromBody(root);
  }
  return empty;
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
