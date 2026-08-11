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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import org.junit.jupiter.api.Test;

/**
 * Typed {@link IPSErrorCode} construction on {@link PSException} and objectstore subclasses that
 * live in utils (no audit-log dependency).
 */
class PSExceptionTypedErrorCodeTest {

  private static final IPSErrorCode SAMPLE =
      new IPSErrorCode() {
        @Override
        public int numericCode() {
          return 2011;
        }

        @Override
        public boolean isAuditable() {
          return false;
        }
      };

  private static final IPSErrorCode AUDITABLE =
      new IPSErrorCode() {
        @Override
        public int numericCode() {
          return 9002;
        }

        @Override
        public boolean isAuditable() {
          return true;
        }
      };

  @Test
  void typedCtorSetsNumericAndRetainsCode() {
    PSException ex = new PSException(SAMPLE, "PSXContentEditor");
    assertEquals(2011, ex.getErrorCode());
    assertSame(SAMPLE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    assertEquals(1, ex.getErrorArguments().length);
    assertEquals("PSXContentEditor", ex.getErrorArguments()[0]);
  }

  @Test
  void typedCtorNoArgs() {
    PSException ex = new PSException(SAMPLE);
    assertEquals(2011, ex.getErrorCode());
    assertSame(SAMPLE, ex.getTypedErrorCode());
  }

  @Test
  void typedCtorArrayArgs() {
    Object[] args = {"a", "b"};
    PSException ex = new PSException(SAMPLE, args);
    assertEquals(2011, ex.getErrorCode());
    assertSame(SAMPLE, ex.getTypedErrorCode());
    assertEquals(2, ex.getErrorArguments().length);
  }

  @Test
  void typedCtorRejectsNullCode() {
    assertThrows(IllegalArgumentException.class, () -> new PSException((IPSErrorCode) null));
  }

  @Test
  void legacyIntCtorHasNoTypedCode() {
    PSException ex = new PSException(2011, "x");
    assertEquals(2011, ex.getErrorCode());
    assertNull(ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void setErrorCodeClearsTypedCode() {
    PSException ex = new PSException(SAMPLE);
    assertSame(SAMPLE, ex.getTypedErrorCode());
    ex.setErrorCode(2012);
    assertEquals(2012, ex.getErrorCode());
    assertNull(ex.getTypedErrorCode());
  }

  @Test
  void setArgsTypedRetainsCode() {
    PSException ex = new PSException(2011);
    ex.setArgs(AUDITABLE, new Object[] {"Directory", "ldap1", "jdoe"});
    assertEquals(9002, ex.getErrorCode());
    assertSame(AUDITABLE, ex.getTypedErrorCode());
    assertTrue(ex.isAuditable());
  }

  @Test
  void copyCtorPreservesTypedCode() {
    PSException src = new PSException(AUDITABLE, "x");
    PSException copy = new PSException(src);
    assertEquals(9002, copy.getErrorCode());
    assertSame(AUDITABLE, copy.getTypedErrorCode());
    assertTrue(copy.isAuditable());
  }

  @Test
  void unknownNodeTypeTypedCtor() {
    PSUnknownNodeTypeException ex =
        new PSUnknownNodeTypeException(SAMPLE, "PSXContentEditor");
    assertEquals(2011, ex.getErrorCode());
    assertSame(SAMPLE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
