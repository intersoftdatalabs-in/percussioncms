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
package com.percussion.error;

/**
 * Minimal error-code surface for typed exception construction without coupling {@code utils} to
 * {@code audit-log}.
 *
 * <p>Package {@code *ErrorCodes} enums in {@code com.intsof.percussioncms.auditlog} implement the
 * richer {@code SystemErrorCode} interface, which extends this type. Call sites in modules that
 * depend on {@code audit-log} should prefer those enums (for example {@code
 * ObjectStoreErrorCodes}) when throwing {@link PSException} subclasses.
 *
 * <p>Legacy {@code IPS*Errors} int constants remain valid via the existing {@code int}-based
 * constructors; this interface is the migration target for dual-write / {@code isAuditable}
 * awareness.
 */
public interface IPSErrorCode {

  /**
   * Numeric code matching the historical {@code IPS*Errors} constant used for message lookup and
   * legacy dual-write registry resolution.
   */
  int numericCode();

  /**
   * When {@code true}, reporting this code should dual-write to the durable audit store. Default
   * {@code false} keeps operational / structural codes off the audit path.
   */
  default boolean isAuditable() {
    return false;
  }
}
