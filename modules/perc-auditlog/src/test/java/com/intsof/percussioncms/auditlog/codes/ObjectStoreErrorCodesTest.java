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

import com.intsof.percussioncms.auditlog.AuditModule;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ObjectStoreErrorCodesTest {

  @Test
  void everyConstantHasDesnModuleUniqueNumericAndTemplates() {
    Set<Integer> seen = new HashSet<>();
    for (ObjectStoreErrorCodes code : ObjectStoreErrorCodes.values()) {
      assertEquals(AuditModule.DESN, code.module());
      assertTrue(code.numericCode() >= 2011, code.name());
      assertTrue(code.numericCode() <= 2320, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("DESN-"));
    }
    // Batch A (63) + batch B (60) = 123 non-colliding IPSObjectStoreErrors ints.
    assertEquals(123, ObjectStoreErrorCodes.values().length);
  }

  @Test
  void allCatalogCodesAreNonAuditable() {
    // Auditable ACL dual-write remains on DesignErrorCodes; ObjectStore is structure noise.
    for (ObjectStoreErrorCodes code : ObjectStoreErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsObjectStoreErrorsNumericValues() {
    assertEquals(2011, ObjectStoreErrorCodes.XML_ELEMENT_NULL.numericCode());
    assertEquals(2012, ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode());
    assertEquals(2020, ObjectStoreErrorCodes.APP_VERSION_DOES_NOT_MATCH.numericCode());
    assertEquals(2101, ObjectStoreErrorCodes.GET_APP_LOG_NO_DATA.numericCode());
    assertEquals(2102, ObjectStoreErrorCodes.CONN_OBJ_NULL.numericCode());
    assertEquals(2200, ObjectStoreErrorCodes.PIPE_NAME_EMPTY.numericCode());
    assertEquals(2209, ObjectStoreErrorCodes.APP_NAME_EMPTY.numericCode());
    assertEquals(2238, ObjectStoreErrorCodes.DATAENC_KEY_STRENGTH_REQD.numericCode());
    assertEquals(2260, ObjectStoreErrorCodes.NOTIFIER_FROM_TOO_BIG.numericCode());
    // Batch B anchors
    assertEquals(2261, ObjectStoreErrorCodes.ROLE_NAME_EMPTY.numericCode());
    assertEquals(2274, ObjectStoreErrorCodes.ROLESET_PROVIDER_TYPE_INVALID.numericCode());
    assertEquals(2294, ObjectStoreErrorCodes.LITERAL_DATE_INVALID.numericCode());
    assertEquals(2309, ObjectStoreErrorCodes.JDBC_DRIVER_CLASS_LOAD_ERROR.numericCode());
    assertEquals(2320, ObjectStoreErrorCodes.UPDATEPIPE_NO_SYNC_TYPES.numericCode());
  }

  @Test
  void batchBRangeIsContiguousFrom2261Through2320() {
    Set<Integer> batchB = new HashSet<>();
    for (ObjectStoreErrorCodes code : ObjectStoreErrorCodes.values()) {
      int n = code.numericCode();
      if (n >= 2261 && n <= 2320) {
        batchB.add(n);
      }
    }
    assertEquals(60, batchB.size());
    for (int expected = 2261; expected <= 2320; expected++) {
      assertTrue(batchB.contains(expected), "missing batch B int " + expected);
    }
  }

  @Test
  void doesNotIncludeDesignOwnedAclInts() {
    Set<Integer> designOwned =
        Set.of(
            2201, 2202, 2203, 2204, 2205, 2206, 2207, 2208, 2213, 2214, 2218, 2327, 2351, 2352,
            2353, 2354, 2355, 2356);
    for (ObjectStoreErrorCodes code : ObjectStoreErrorCodes.values()) {
      assertFalse(
          designOwned.contains(code.numericCode()),
          "must not re-register Design ACL int: " + code.numericCode());
    }
  }
}
