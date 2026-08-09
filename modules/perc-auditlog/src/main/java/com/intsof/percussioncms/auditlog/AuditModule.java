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

import java.util.Locale;
import java.util.Objects;

/**
 * Short-code registry for CMS modules that emit system error / audit codes.
 *
 * <p>Used in the canonical message form {@code [PUB-1001]-[<logId>] …}.
 */
public enum AuditModule {
  SYS("SYS", "System"),
  AUTH("AUTH", "Authentication"),
  USER("USER", "User management"),
  CONT("CONT", "Content"),
  WF("WF", "Workflow"),
  PUB("PUB", "Publishing"),
  DESN("DESN", "Design objects"),
  SEC("SEC", "Security"),
  CFG("CFG", "Configuration"),
  AUDIT("AUDIT", "Audit subsystem");

  private final String code;
  private final String displayName;

  AuditModule(String code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  /** Short module code used in {@code [PUB-1001]} prefixes. */
  public String code() {
    return code;
  }

  /** Human-readable English display name (i18n keys may override in UI). */
  public String displayName() {
    return displayName;
  }

  /**
   * Resolve a module from its short code (case-insensitive).
   *
   * @param code short code such as {@code PUB}
   * @return matching module
   * @throws IllegalArgumentException if unknown
   */
  public static AuditModule fromCode(String code) {
    Objects.requireNonNull(code, "code");
    String normalized = code.trim().toUpperCase(Locale.ROOT);
    for (AuditModule m : values()) {
      if (m.code.equals(normalized)) {
        return m;
      }
    }
    throw new IllegalArgumentException("Unknown audit module code: " + code);
  }
}
