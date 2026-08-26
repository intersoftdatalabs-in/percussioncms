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

class ContentErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleContAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ContentErrorCodes code : ContentErrorCodes.values()) {
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
    for (ContentErrorCodes code : ContentErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void lifecycleEventsAreAuditable() {
    assertTrue(ContentErrorCodes.CREATE.isAuditable());
    assertEquals(AuditEventType.CONTENT_CREATE, ContentErrorCodes.CREATE.eventType());
    assertTrue(ContentErrorCodes.UPDATE.isAuditable());
    assertTrue(ContentErrorCodes.DELETE.isAuditable());
    assertTrue(ContentErrorCodes.RECYCLE.isAuditable());
    assertTrue(ContentErrorCodes.PAGE_PUBLISH_SCHEDULE.isAuditable());
    assertTrue(ContentErrorCodes.PAGE_REMOVAL_SCHEDULE.isAuditable());
  }

  @Test
  void conversionOperationalNoiseIsNotAuditable() {
    assertFalse(ContentErrorCodes.UNSUPPORTED_FILE_TYPE.isAuditable());
    assertFalse(ContentErrorCodes.CONTENT_CONVERSION_FAILED_NO_MESSAGE.isAuditable());
    assertFalse(ContentErrorCodes.UNSUPPORTED_MIMETYPE.isAuditable());
    assertFalse(ContentErrorCodes.UNSUPPORTED_CONVERT_METHOD.isAuditable());
  }

  @Test
  void preservesPhase2aAndLegacyNumericRanges() {
    assertEquals(2001, ContentErrorCodes.CREATE.numericCode());
    assertEquals(2003, ContentErrorCodes.DELETE.numericCode());
    assertEquals(2006, ContentErrorCodes.PAGE_REMOVAL_SCHEDULE.numericCode());
    assertEquals(17001, ContentErrorCodes.UNSUPPORTED_FILE_TYPE.numericCode());
    assertEquals(17010, ContentErrorCodes.UNSUPPORTED_CONVERT_CONSTRUCTOR.numericCode());
    assertEquals(1, ContentErrorCodes.MISSING_KEYWORD.numericCode());
    assertFalse(ContentErrorCodes.MISSING_KEYWORD.isAuditable());
  }
}
