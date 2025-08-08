// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility methods for Percussion Delivery Tier.
 * @author Sunny Sal
 */
public final class PSDeliveryUtils {

    private PSDeliveryUtils() {
        // Utility class, do not instantiate.
    }

    /**
     * Returns the first non-empty string from the provided arguments.
     * @param values Strings to check.
     * @return Optional containing the first non-empty string, or empty if none found.
     */
    public static Optional<String> firstNonEmpty(String... values) {
        return Stream.of(values)
                .filter(s -> s != null && !s.isEmpty())
                .findFirst();
    }

    /**
     * Checks if the given string is null or empty.
     * @param value String to check.
     * @return true if null or empty, false otherwise.
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}

