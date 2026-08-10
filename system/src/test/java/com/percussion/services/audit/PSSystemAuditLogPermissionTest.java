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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSSystemAuditLogPermissionTest {

  @Test
  void adminAlwaysAllowedEvenWithoutProperty() {
    assertTrue(PSSystemAuditLogPermission.allows(List.of("Admin"), role -> false));
    assertTrue(PSSystemAuditLogPermission.allows(List.of("admin"), role -> false));
  }

  @Test
  void grantedRoleWithTruthyPropertyAllowed() {
    assertTrue(PSSystemAuditLogPermission.allows(List.of("Editor"), role -> "Editor".equals(role)));
  }

  @Test
  void nonAdminWithoutPropertyDenied() {
    assertFalse(PSSystemAuditLogPermission.allows(List.of("Editor", "Author"), role -> false));
  }

  @Test
  void emptyOrNullRolesDenied() {
    assertFalse(PSSystemAuditLogPermission.allows(null, role -> true));
    assertFalse(PSSystemAuditLogPermission.allows(List.of(), role -> true));
  }

  @Test
  void truthyPropertyValues() {
    assertTrue(PSSystemAuditLogPermission.isTruthyPropertyValue("true"));
    assertTrue(PSSystemAuditLogPermission.isTruthyPropertyValue("YES"));
    assertTrue(PSSystemAuditLogPermission.isTruthyPropertyValue("1"));
    assertTrue(PSSystemAuditLogPermission.isTruthyPropertyValue(List.of("y")));
    assertFalse(PSSystemAuditLogPermission.isTruthyPropertyValue("false"));
    assertFalse(PSSystemAuditLogPermission.isTruthyPropertyValue("0"));
    assertFalse(PSSystemAuditLogPermission.isTruthyPropertyValue(""));
    assertFalse(PSSystemAuditLogPermission.isTruthyPropertyValue((String) null));
    assertFalse(PSSystemAuditLogPermission.isTruthyPropertyValue(Set.of("maybe")));
  }
}
