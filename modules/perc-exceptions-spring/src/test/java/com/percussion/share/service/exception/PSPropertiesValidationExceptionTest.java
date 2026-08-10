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
package com.percussion.share.service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSPropertiesValidationException} (issue #2017 serial / this-escape
 * remediation).
 */
public class PSPropertiesValidationExceptionTest {

  @Test
  @DisplayName("constructor seeds empty HashMap-backed binding result")
  void constructorSeedsEmptyProperties() {
    PSPropertiesValidationException ex = new PSPropertiesValidationException(new Object(), "props");

    assertEquals("props", ex.getObjectName());
    assertTrue(ex.getProperties().isEmpty());
    assertInstanceOf(HashMap.class, ex.getProperties());
    assertFalse(ex.hasErrors());
  }

  @Test
  @DisplayName("setProperties replaces map contents and keeps HashMap instance type")
  void setPropertiesReplacesContents() {
    PSPropertiesValidationException ex = new PSPropertiesValidationException(new Object(), "props");
    Map<String, Object> incoming = Map.of("a", 1, "b", "two");

    ex.setProperties(incoming);

    assertEquals(2, ex.getProperties().size());
    assertEquals(1, ex.getProperties().get("a"));
    assertEquals("two", ex.getProperties().get("b"));
    assertInstanceOf(HashMap.class, ex.getProperties());
  }

  @Test
  @DisplayName("message+cause constructor records cause as global rejection")
  void messageCauseConstructorRejectsCause() {
    IllegalStateException cause = new IllegalStateException("bad");
    PSPropertiesValidationException ex =
        new PSPropertiesValidationException(new Object(), "props", "msg", cause);

    assertTrue(ex.hasGlobalErrors());
    assertEquals(IllegalStateException.class.getCanonicalName(), ex.getGlobalError().getCode());
  }
}
