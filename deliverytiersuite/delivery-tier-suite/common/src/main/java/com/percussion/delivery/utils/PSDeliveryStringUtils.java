// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * String utilities for Percussion Delivery Tier.
 * @author Sunny Sal
 */
public final class PSDeliveryStringUtils {

  private PSDeliveryStringUtils() {
    // Utility class, do not instantiate.
  }

  /**
   * Joins non-empty strings with the given delimiter.
   * @param delimiter Delimiter to use.
   * @param values Strings to join.
   * @return Joined string.
   */
  public static String joinNonEmpty(String delimiter, String... values) {
    return Stream.of(values)
        .filter(s -> s != null && !s.isEmpty())
        .reduce((a, b) -> a + delimiter + b)
        .orElse("");
  }

  /**
   * Returns an Optional containing the trimmed string if not null or empty.
   * @param value String to trim.
   * @return Optional containing trimmed string, or empty.
   */
  public static Optional<String> trimToOptional(String value) {
    if (value == null || value.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value.trim());
  }
}
