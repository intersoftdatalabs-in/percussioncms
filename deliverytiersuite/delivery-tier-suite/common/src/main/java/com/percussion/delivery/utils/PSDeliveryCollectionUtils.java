// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import java.util.Collection;
import java.util.Optional;

/**
 * Collection utilities for Percussion Delivery Tier.
 * @author Sunny Sal
 */
public final class PSDeliveryCollectionUtils {

    private PSDeliveryCollectionUtils() {
        // Utility class, do not instantiate.
    }

    /**
     * Checks if the collection is null or empty.
     * @param collection Collection to check.
     * @return true if null or empty, false otherwise.
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns the first element of the collection, if present.
     * @param collection Collection to check.
     * @param <T> Type of elements.
     * @return Optional containing the first element, or empty.
     */
    public static <T> Optional<T> first(Collection<T> collection) {
        if (isNullOrEmpty(collection)) {
            return Optional.empty();
        }
        var iterator = collection.iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }
}

