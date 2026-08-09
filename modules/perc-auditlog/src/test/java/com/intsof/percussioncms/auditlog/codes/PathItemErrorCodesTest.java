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
package com.intsof.percussioncms.auditlog.codes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PathItemErrorCodesTest {

  @Test
  void everyConstantHasContModuleUniqueNumericAndTemplates() {
    Set<Integer> seen = new HashSet<>();
    for (PathItemErrorCodes code : PathItemErrorCodes.values()) {
      assertEquals(AuditModule.CONT, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("CONT-"));
    }
  }

  @Test
  void auditableCodesRequireEventType() {
    for (PathItemErrorCodes code : PathItemErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name());
      } else {
        assertNull(code.eventType(), code.name());
      }
    }
  }

  @Test
  void permissionDenialsAreAuditable() {
    assertTrue(PathItemErrorCodes.FOLDER_PERMISSION_DENIED.isAuditable());
    assertEquals(
        AuditEventType.ACCESS_DENIED, PathItemErrorCodes.FOLDER_PERMISSION_DENIED.eventType());
    assertTrue(PathItemErrorCodes.FOLDER_CREATE_ERROR.isAuditable());
    assertTrue(PathItemErrorCodes.CONTENT_TYPE_NOT_VISIBLE_BY_COMMUNITY.isAuditable());
    assertTrue(PathItemErrorCodes.FAIL_OPEN_FOLDER.isAuditable());
  }

  @Test
  void pathLookupNoiseIsNotAuditable() {
    assertFalse(PathItemErrorCodes.CONTENT_ITEM_CANNOT_BE_LOCATED.isAuditable());
    assertFalse(PathItemErrorCodes.INVALID_FOLDER_ID.isAuditable());
    assertFalse(PathItemErrorCodes.DUPLICATE_ITEM_NAME.isAuditable());
    assertFalse(PathItemErrorCodes.FOLDER_OPERATION_FAILED.isAuditable());
  }

  @Test
  void preservesLegacyIpsCmsErrorsNumericValues() {
    assertEquals(13007, PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode());
    assertEquals(13008, PathItemErrorCodes.FOLDER_CREATE_ERROR.numericCode());
    assertEquals(13104, PathItemErrorCodes.CONTENT_ITEM_CANNOT_BE_LOCATED.numericCode());
    assertEquals(13212, PathItemErrorCodes.DUPLICATE_ITEM_NAME.numericCode());
  }
}
