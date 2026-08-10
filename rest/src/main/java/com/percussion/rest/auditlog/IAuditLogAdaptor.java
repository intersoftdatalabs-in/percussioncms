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
package com.percussion.rest.auditlog;

import java.util.List;

/**
 * Adaptor contract for the system security audit log query and export API (Phase 3 / #2618, Phase 5
 * export / #2715).
 *
 * <p>Implementations enforce AuthZ (Admin role or role property {@code
 * sys_securityAuditLogViewer}) and map domain rows to wire DTOs. Unauthorized callers must surface
 * as {@link SecurityException} (resource maps to HTTP 403).
 */
public interface IAuditLogAdaptor {

  /**
   * Query durable audit rows.
   *
   * @param fromIso optional inclusive lower bound (ISO-8601 instant); null/blank ignored
   * @param toIso optional exclusive upper bound (ISO-8601 instant); null/blank ignored
   * @param moduleCode optional module filter (e.g. AUTH)
   * @param eventType optional event type filter
   * @param outcome optional outcome filter (SUCCESS / FAILURE / …)
   * @param actor optional actor (user name) filter; case-insensitive
   * @param offset zero-based row offset (negative treated as 0)
   * @param limit page size (clamped server-side)
   */
  SystemAuditLogPage query(
      String fromIso,
      String toIso,
      String moduleCode,
      String eventType,
      String outcome,
      String actor,
      int offset,
      int limit);

  /**
   * Load one entry by audit id.
   *
   * @return entry when present
   * @throws SecurityException when the caller is not allowed
   * @throws java.util.NoSuchElementException or return null when missing — implementations should
   *     return null so the resource can map 404
   */
  SystemAuditLogEntry findById(String auditId);

  /**
   * Export durable audit rows matching the same filters as {@link #query}, up to a server-clamped
   * max row count (newest first). Same AuthZ as query.
   *
   * @param fromIso optional inclusive lower bound (ISO-8601 instant); null/blank ignored
   * @param toIso optional exclusive upper bound (ISO-8601 instant); null/blank ignored
   * @param moduleCode optional module filter (e.g. AUTH)
   * @param eventType optional event type filter
   * @param outcome optional outcome filter (SUCCESS / FAILURE / …)
   * @param actor optional actor (user name) filter; case-insensitive
   * @param maxRows requested max rows (clamped server-side; non-positive → default)
   * @return entries newest first (never null; may be empty)
   * @throws SecurityException when the caller is not allowed
   * @throws IllegalArgumentException when date filters are invalid
   */
  List<SystemAuditLogEntry> export(
      String fromIso,
      String toIso,
      String moduleCode,
      String eventType,
      String outcome,
      String actor,
      int maxRows);
}
