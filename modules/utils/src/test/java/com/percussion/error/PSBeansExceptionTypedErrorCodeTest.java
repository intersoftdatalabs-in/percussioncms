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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Issue #4264 companion: {@link PSBeansException} typed {@link IPSErrorCode} constructors retain
 * numeric + auditable metadata without coupling utils to audit-log enums.
 */
@Tag("UnitTest")
class PSBeansExceptionTypedErrorCodeTest {

  @Test
  void typedCtorRetainsCodeAndAuditableFlag() {
    IPSErrorCode code =
        new IPSErrorCode() {
          @Override
          public int numericCode() {
            return IPSBeansErrors.XML_PROCESSING_ERROR;
          }

          @Override
          public boolean isAuditable() {
            return false;
          }
        };
    PSBeansException ex = new PSBeansException(code, "detail");
    assertSame(code, ex.getTypedErrorCode());
    assertEquals(IPSBeansErrors.XML_PROCESSING_ERROR, ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void intCtorLeavesTypedCodeNull() {
    PSBeansException ex = new PSBeansException(IPSBeansErrors.XML_PROCESSING_ERROR, "detail");
    assertNull(ex.getTypedErrorCode());
    assertEquals(IPSBeansErrors.XML_PROCESSING_ERROR, ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void nullTypedCodeRejected() {
    assertThrows(IllegalArgumentException.class, () -> new PSBeansException((IPSErrorCode) null));
  }
}
