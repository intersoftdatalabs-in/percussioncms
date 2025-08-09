// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import java.io.InputStream;
import java.util.Optional;

/**
 * Resource utilities for Percussion Delivery Tier.
 * @author Sunny Sal
 */
public final class PSDeliveryResourceUtils {

  private PSDeliveryResourceUtils() {
    // Utility class, do not instantiate.
  }

  /**
   * Loads a resource as an InputStream.
   * @param resourcePath Path to the resource.
   * @return Optional containing InputStream if found, empty otherwise.
   */
  public static Optional<InputStream> loadResource(String resourcePath) {
    var stream = PSDeliveryResourceUtils.class.getClassLoader().getResourceAsStream(resourcePath);
    return Optional.ofNullable(stream);
  }
}
