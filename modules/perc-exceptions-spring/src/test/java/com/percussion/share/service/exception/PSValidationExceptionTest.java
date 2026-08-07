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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.validation.PSValidationErrors.PSFieldError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSValidationException} field assignment and throwIfInvalid (issue
 * #2017).
 */
public class PSValidationExceptionTest {

  private static final class DummyValidationException extends PSValidationException {
    private static final long serialVersionUID = 1L;

    DummyValidationException(PSValidationErrors errors) {
      super(errors);
    }

    DummyValidationException(String message) {
      super(message);
    }
  }

  @Test
  @DisplayName("errors constructor assigns validationErrors without setter")
  void errorsConstructorAssignsField() {
    PSValidationErrors errors = new PSValidationErrors();
    errors.setMethodName("m");
    DummyValidationException ex = new DummyValidationException(errors);

    assertSame(errors, ex.getValidationErrors());
    assertEquals("m", ex.getValidationErrors().getMethodName());
  }

  @Test
  @DisplayName("throwIfInvalid throws when field errors present")
  void throwIfInvalidThrowsWhenInvalid() {
    PSValidationErrors errors = new PSValidationErrors();
    PSFieldError field = new PSFieldError();
    field.setField("name");
    field.setCode("required");
    field.setDefaultMessage("required");
    errors.getFieldErrors().add(field);

    DummyValidationException ex = new DummyValidationException(errors);
    assertThrows(PSValidationException.class, ex::throwIfInvalid);
  }

  @Test
  @DisplayName("throwIfInvalid returns this when empty")
  void throwIfInvalidReturnsWhenEmpty() throws Exception {
    DummyValidationException ex = new DummyValidationException("ok");
    assertSame(ex, ex.throwIfInvalid());
  }
}
