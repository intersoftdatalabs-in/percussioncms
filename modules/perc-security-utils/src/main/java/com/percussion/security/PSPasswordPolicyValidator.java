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

import java.util.Optional;
import java.util.Properties;

/**
 * Shared server-side password policy validator enforced across user creation, updates, password
 * change, and account recovery password reset flows.
 */
public final class PSPasswordPolicyValidator {

  public static final String PROP_PASSWORD_MIN_LENGTH = "auth.password.minLength";
  public static final int DEFAULT_MIN_LENGTH = 12;
  public static final int MIN_ALLOWED_LENGTH = 8;
  public static final int MAX_ALLOWED_LENGTH = 150;

  private PSPasswordPolicyValidator() {}

  /** Resolves the configured minimum password length, clamped to {@code [8, 150]}. */
  public static int resolveMinLength(Properties props) {
    if (props == null) {
      return DEFAULT_MIN_LENGTH;
    }
    String raw = props.getProperty(PROP_PASSWORD_MIN_LENGTH);
    if (raw == null || raw.isBlank()) {
      return DEFAULT_MIN_LENGTH;
    }
    try {
      int parsed = Integer.parseInt(raw.trim());
      return clampMinLength(parsed);
    } catch (NumberFormatException ex) {
      return DEFAULT_MIN_LENGTH;
    }
  }

  /**
   * Validates {@code password} against {@code minLength} (clamped to {@code [8, 150]}) and hard max
   * {@code 150}. Returns a non-empty error message if invalid.
   */
  public static Optional<String> validate(String password, int minLength) {
    int effectiveMin = clampMinLength(minLength);
    if (password == null || password.isBlank()) {
      return Optional.of("Password is required and must not be blank.");
    }
    int len = password.length();
    if (len < effectiveMin) {
      return Optional.of(
          "Password must be at least " + effectiveMin + " characters long.");
    }
    if (len > MAX_ALLOWED_LENGTH) {
      return Optional.of(
          "Password must be at most " + MAX_ALLOWED_LENGTH + " characters long.");
    }
    return Optional.empty();
  }

  /** Validates {@code password} against {@code minLength}, throwing {@link IllegalArgumentException}. */
  public static void validateOrThrow(String password, int minLength) {
    Optional<String> violation = validate(password, minLength);
    if (violation.isPresent()) {
      throw new IllegalArgumentException(violation.get());
    }
  }

  /**
   * Resolves minimum length from {@code props} and validates {@code password}, throwing {@link
   * IllegalArgumentException} on violation.
   */
  public static void validateOrThrow(String password, Properties props) {
    validateOrThrow(password, resolveMinLength(props));
  }

  /** Returns {@code true} when {@code password} satisfies the policy bounds. */
  public static boolean isValid(String password, int minLength) {
    return validate(password, minLength).isEmpty();
  }

  private static int clampMinLength(int minLength) {
    return Math.max(MIN_ALLOWED_LENGTH, Math.min(MAX_ALLOWED_LENGTH, minLength));
  }
}
