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
package com.intsof.percussioncms.auditlog.spi;

import com.intsof.percussioncms.auditlog.AuditRecord;

/**
 * SPI for durable audit storage (DB implementation lives in the {@code system} module).
 *
 * <p>Implementations must never throw in a way that fails the business request; the service catches
 * failures and emits {@code AUDIT_SINK_FAILURE} markers.
 */
public interface AuditLogRepository {

  /**
   * Persist an audit record. May block briefly; async wrappers are the caller's / service's
   * concern.
   *
   * @param record redacted, fully prepared record
   */
  void save(AuditRecord record);
}
