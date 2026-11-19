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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class PSPasswordPolicyValidatorTest {

  @Test
  void resolveMinLengthDefaultsAndClampsBounds() {
    assertEquals(12, PSPasswordPolicyValidator.resolveMinLength(null));
    Properties empty = new Properties();
    assertEquals(12, PSPasswordPolicyValidator.resolveMinLength(empty));

    Properties invalid = new Properties();
    invalid.setProperty(PSPasswordPolicyValidator.PROP_PASSWORD_MIN_LENGTH, "abc");
    assertEquals(12, PSPasswordPolicyValidator.resolveMinLength(invalid));

    Properties tooLow = new Properties();
    tooLow.setProperty(PSPasswordPolicyValidator.PROP_PASSWORD_MIN_LENGTH, "4");
    assertEquals(8, PSPasswordPolicyValidator.resolveMinLength(tooLow));

    Properties tooHigh = new Properties();
    tooHigh.setProperty(PSPasswordPolicyValidator.PROP_PASSWORD_MIN_LENGTH, "250");
    assertEquals(150, PSPasswordPolicyValidator.resolveMinLength(tooHigh));

    Properties custom = new Properties();
    custom.setProperty(PSPasswordPolicyValidator.PROP_PASSWORD_MIN_LENGTH, "16");
    assertEquals(16, PSPasswordPolicyValidator.resolveMinLength(custom));
  }

  @Test
  void validateEnforcesMinAndMaxBounds() {
    assertTrue(PSPasswordPolicyValidator.validate(null, 12).isPresent());
    assertTrue(PSPasswordPolicyValidator.validate("           ", 12).isPresent());
    assertTrue(PSPasswordPolicyValidator.validate("a".repeat(11), 12).isPresent());
    assertTrue(PSPasswordPolicyValidator.isValid("a".repeat(12), 12));
    assertTrue(PSPasswordPolicyValidator.isValid("a".repeat(150), 12));
    assertFalse(PSPasswordPolicyValidator.isValid("a".repeat(151), 12));

    assertThrows(
        IllegalArgumentException.class,
        () -> PSPasswordPolicyValidator.validateOrThrow("shortpw", 12));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPasswordPolicyValidator.validateOrThrow("a".repeat(151), new Properties()));
    PSPasswordPolicyValidator.validateOrThrow("ValidPassword12!", new Properties());
  }
}
