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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSBeanValidationException} constructors that install the Spring
 * binding result without overridable method calls (issue #2017 this-escape remediation).
 */
public class PSBeanValidationExceptionTest {

  @Test
  @DisplayName("target+method constructor seeds object name and empty errors")
  void targetMethodConstructorSeedsBindingResult() {
    Object target = new Object();
    PSBeanValidationException ex = new PSBeanValidationException(target, "com.example.Bean");

    assertEquals("com.example.Bean", ex.getObjectName());
    assertFalse(ex.hasErrors());
    assertEquals(0, ex.getErrorCount());
  }

  @Test
  @DisplayName("message+cause constructor records cause as global rejection")
  void messageCauseConstructorRejectsCause() {
    RuntimeException cause = new RuntimeException("boom");
    PSBeanValidationException ex =
        new PSBeanValidationException(new Object(), "bean", "detail", cause);

    assertTrue(ex.hasErrors());
    assertTrue(ex.hasGlobalErrors());
    assertEquals(RuntimeException.class.getCanonicalName(), ex.getGlobalError().getCode());
    assertSame(cause, ex.getCause());
  }

  @Test
  @DisplayName("throwIfInvalid returns this when no errors")
  void throwIfInvalidReturnsWhenClean() throws Exception {
    PSBeanValidationException ex = new PSBeanValidationException(new Object(), "bean");
    assertSame(ex, ex.throwIfInvalid());
  }
}
