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
package com.intsof.percussioncms.auditlog;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for an audit record, shared across dual sinks ({@code server.log} and durable
 * store).
 */
public final class AuditLogId {

  private final String value;

  private AuditLogId(String value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  /** Generates a new random UUID-based log id. */
  public static AuditLogId generate() {
    return new AuditLogId(UUID.randomUUID().toString());
  }

  /**
   * Wraps an existing id string (e.g. when replaying or testing).
   *
   * @param value non-blank id
   */
  public static AuditLogId of(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Audit log id must not be blank");
    }
    return new AuditLogId(value.trim());
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuditLogId that)) {
      return false;
    }
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
