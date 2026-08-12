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
 * Wire types for {@code GET /services/auditlog/entries} (rest AuditLogResource).
 * Mirrors {@code com.percussion.rest.auditlog.SystemAuditLogEntry / SystemAuditLogPage}.
 *
 * <p>On the wire under {@code JacksonContextResolver} WRAP_ROOT_VALUE the page is nested as
 * {@code {"SystemAuditLogPage":{ entries, total, … }}} and a detail row as
 * {@code {"SystemAuditLogEntry":{…}}}. Prefer {@link unwrapSystemAuditLogPage} /
 * {@link unwrapSystemAuditLogEntry} before reading fields (#3089).
 */

export interface SystemAuditLogEntry {
  auditId?: string;
  /** ISO-8601 instant string from Jackson Instant serialization. */
  eventTime?: string;
  moduleCode?: string;
  messageCode?: number;
  eventType?: string;
  outcome?: string;
  actor?: string;
  target?: string;
  sourceIp?: string;
  sourceHost?: string;
  userMessage?: string;
  logMessage?: string;
  correlationId?: string;
  attributesJson?: string;
  serverNode?: string;
}

export interface SystemAuditLogPage {
  entries?: SystemAuditLogEntry[];
  total?: number;
  offset?: number;
  limit?: number;
}

/** Client-side query filters for the list endpoint. */
export interface AuditLogQueryParams {
  from?: string;
  to?: string;
  module?: string;
  eventType?: string;
  outcome?: string;
  actor?: string;
  offset?: number;
  limit?: number;
}
