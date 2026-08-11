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

class DeliveryErrorCodesTest {

  @Test
  void everyConstantHasExplicitModulePubAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (DeliveryErrorCodes code : DeliveryErrorCodes.values()) {
      assertEquals(AuditModule.PUB, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("PUB-"));
    }
    assertEquals(12, DeliveryErrorCodes.values().length);
  }

  @Test
  void decryptCredentialsIsAuditableViaEnum() {
    assertTrue(DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.isAuditable());
    assertEquals(
        AuditEventType.OTHER, DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.eventType());
    assertEquals(7, DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.numericCode());
  }

  @Test
  void operationalDeliveryNoiseIsNotAuditable() {
    assertFalse(DeliveryErrorCodes.ABORT_FAILURE.isAuditable());
    assertNull(DeliveryErrorCodes.ABORT_FAILURE.eventType());
    assertFalse(DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION.isAuditable());
    assertFalse(DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE.isAuditable());
  }

  @Test
  void preservesLegacyIpsDeliveryErrorsNumericValues() {
    assertEquals(1, DeliveryErrorCodes.ABORT_FAILURE.numericCode());
    assertEquals(4, DeliveryErrorCodes.COPY_FILE_FAILED.numericCode());
    assertEquals(11, DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION.numericCode());
    assertEquals(12, DeliveryErrorCodes.CANNOT_DELIVER_NO_DELIVERYTYPE.numericCode());
  }
}
