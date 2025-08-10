// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

/**
 * Common constants for Percussion Delivery Tier.
 *
 * @author Sunny Sal
 */
public final class PSDeliveryConstants {

  private PSDeliveryConstants() {
    // Utility class, do not instantiate.
  }

  public static final String DELIVERY_SITE_HEADER = "Perc-Delivery-Site";
  public static final String DELIVERY_USER_HEADER = "Perc-Delivery-User";
  public static final String DELIVERY_PAGE_HEADER = "Perc-Delivery-Page";
  public static final String DELIVERY_REQUEST_ID = "Perc-Delivery-Request-Id";
}
