// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Validation utilities for Percussion Delivery Tier.
 *
 * @author Sunny Sal
 */
public final class PSDeliveryValidator {

  private PSDeliveryValidator() {
    // Utility class, do not instantiate.
  }

  /**
   * Validates that the provided strings are not null or empty.
   *
   * @param values Strings to validate.
   * @return Optional containing the first invalid string, or empty if all are valid.
   */
  public static Optional<String> firstInvalid(String... values) {
    return Stream.of(values).filter(s -> s == null || s.isEmpty()).findFirst();
  }
}
