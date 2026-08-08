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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.share.validation.PSErrors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSErrorUtils} (issue #2017 serial remediation on proxy exception
 * payload).
 */
public class PSErrorUtilsTest {

  @Test
  @DisplayName("createErrorsFromException populates global error from throwable")
  void createErrorsFromException() {
    RuntimeException ex = new RuntimeException("server blew up");
    PSErrors errors = PSErrorUtils.createErrorsFromException(ex);

    assertNotNull(errors.getGlobalError());
    assertEquals(RuntimeException.class.getCanonicalName(), errors.getGlobalError().getCode());
    assertEquals("server blew up", errors.getGlobalError().getDefaultMessage());
    assertNotNull(errors.getGlobalError().getCause());
  }

  @Test
  @DisplayName("createExceptionFromErrors returns PSProxyException")
  void createExceptionFromErrors() {
    PSErrors errors = PSErrorUtils.createErrorsFromException(new RuntimeException("x"));
    RuntimeException proxy = PSErrorUtils.createExceptionFromErrors(errors);

    assertInstanceOf(PSErrorUtils.PSProxyException.class, proxy);
  }

  @Test
  @DisplayName("createErrorsFromException rejects null")
  void createErrorsFromExceptionRejectsNull() {
    assertThrows(NullPointerException.class, () -> PSErrorUtils.createErrorsFromException(null));
  }
}
