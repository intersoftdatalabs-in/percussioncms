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
package com.percussion.taxonomy.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link GenericValidator}. */
public class GenericValidatorTest {

  @Test
  public void isBlankOrNull_nullAndBlank() {
    assertTrue(GenericValidator.isBlankOrNull(null));
    assertTrue(GenericValidator.isBlankOrNull(""));
    assertTrue(GenericValidator.isBlankOrNull("   "));
  }

  @Test
  public void isBlankOrNull_nonBlank() {
    assertFalse(GenericValidator.isBlankOrNull("x"));
    assertFalse(GenericValidator.isBlankOrNull(" value "));
  }
}
