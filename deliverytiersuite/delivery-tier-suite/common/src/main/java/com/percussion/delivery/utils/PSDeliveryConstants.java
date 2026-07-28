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

  /** HTTP header name used to carry the delivery tier site identifier. */
  public static final String DELIVERY_SITE_HEADER = "Perc-Delivery-Site";

  /** HTTP header name used to carry the delivery tier user identifier. */
  public static final String DELIVERY_USER_HEADER = "Perc-Delivery-User";

  /** HTTP header name used to carry the delivery tier page identifier. */
  public static final String DELIVERY_PAGE_HEADER = "Perc-Delivery-Page";

  /** HTTP header name used to carry a unique delivery tier request id. */
  public static final String DELIVERY_REQUEST_ID = "Perc-Delivery-Request-Id";
}
