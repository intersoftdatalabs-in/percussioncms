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
package com.intsof.percussioncms.auditlog.sink;

import com.intsof.percussioncms.auditlog.AuditRecord;

/** Destination for a prepared audit record (Log4j, repository, test capture, …). */
public interface AuditLogSink {

  /** Sink name for diagnostics (e.g. {@code log4j}, {@code repository}). */
  String name();

  /**
   * Write the record. Implementations should avoid throwing; if they throw, the service logs an
   * {@code AUDIT_SINK_FAILURE} marker and continues.
   */
  void write(AuditRecord record);
}
