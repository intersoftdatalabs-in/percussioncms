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
package com.percussion.services.audit;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Authorization for the system security audit log viewer (Phase 3).
 *
 * <p>Access is granted when the caller is in the {@link #ADMIN_ROLE} <strong>or</strong> any of
 * their roles has the Rhythmyx role property {@link #ROLE_PROPERTY} set to a truthy value.
 *
 * <p>Operators assign {@code sys_securityAuditLogViewer=true} on non-Admin roles via Server Admin /
 * role properties. Admin is always allowed even without the property row.
 */
public final class PSSystemAuditLogPermission {

  /** Role property name (Rhythmyx role attribute). */
  public static final String ROLE_PROPERTY = "sys_securityAuditLogViewer";

  /** Built-in role that always may view the security audit log. */
  public static final String ADMIN_ROLE = "Admin";

  private PSSystemAuditLogPermission() {}

  /**
   * @param userRoles role names for the authenticated principal; null treated as empty
   * @param roleHasViewerProperty for each role name, whether that role has a truthy {@link
   *     #ROLE_PROPERTY} value
   * @return true when Admin or any role carries the property
   */
  public static boolean allows(
      Collection<String> userRoles, Predicate<String> roleHasViewerProperty) {
    Objects.requireNonNull(roleHasViewerProperty, "roleHasViewerProperty");
    if (userRoles == null || userRoles.isEmpty()) {
      return false;
    }
    for (String role : userRoles) {
      if (role == null || role.isBlank()) {
        continue;
      }
      if (ADMIN_ROLE.equalsIgnoreCase(role.trim())) {
        return true;
      }
      if (roleHasViewerProperty.test(role.trim())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether a role property value collection grants viewer access.
   *
   * <p>Truthy: {@code true}, {@code yes}, {@code y}, {@code 1} (case-insensitive). Empty/null is
   * false. Explicit negatives ({@code false}, {@code no}, {@code n}, {@code 0}) are false.
   */
  public static boolean isTruthyPropertyValue(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return false;
    }
    for (String raw : values) {
      if (raw == null) {
        continue;
      }
      String v = raw.trim().toLowerCase(Locale.ROOT);
      if (v.isEmpty()) {
        continue;
      }
      if ("false".equals(v) || "no".equals(v) || "n".equals(v) || "0".equals(v)) {
        continue;
      }
      if ("true".equals(v) || "yes".equals(v) || "y".equals(v) || "1".equals(v)) {
        return true;
      }
    }
    return false;
  }

  /** Single-value convenience for unit tests and simple maps. */
  public static boolean isTruthyPropertyValue(String value) {
    if (value == null) {
      return false;
    }
    return isTruthyPropertyValue(java.util.List.of(value));
  }
}
