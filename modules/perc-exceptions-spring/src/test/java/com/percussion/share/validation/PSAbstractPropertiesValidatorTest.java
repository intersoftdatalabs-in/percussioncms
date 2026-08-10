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
package com.percussion.share.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.share.service.exception.PSPropertiesValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for {@link PSAbstractPropertiesValidator} Class.cast path (issue #2017
 * unchecked-cast remediation).
 */
public class PSAbstractPropertiesValidatorTest {

  private static final class MapValidator
      extends PSAbstractPropertiesValidator<Map<String, Object>> {
    @Override
    @SuppressWarnings("unchecked")
    protected Class<Map<String, Object>> getType() {
      return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    @Override
    protected void doValidation(Map<String, Object> properties, PSPropertiesValidationException e) {
      if (!properties.containsKey("required")) {
        e.rejectValue("required", "missing", "required key missing");
      }
    }
  }

  @Test
  @DisplayName("validate rejects map missing required key")
  void validateRejectsMissingKey() {
    MapValidator validator = new MapValidator();
    Map<String, Object> props = new HashMap<>();
    PSPropertiesValidationException result = validator.validate(props);

    assertTrue(result.hasFieldErrors());
    assertEquals("required", result.getFieldError().getField());
  }

  @Test
  @DisplayName("validate accepts map with required key")
  void validateAcceptsValidMap() {
    MapValidator validator = new MapValidator();
    Map<String, Object> props = new HashMap<>();
    props.put("required", "yes");
    PSPropertiesValidationException result = validator.validate(props);

    assertFalse(result.hasErrors());
  }

  @Test
  @DisplayName("supports only declared type")
  void supportsOnlyDeclaredType() {
    MapValidator validator = new MapValidator();
    assertTrue(validator.supports(Map.class));
    assertFalse(validator.supports(String.class));
  }
}
