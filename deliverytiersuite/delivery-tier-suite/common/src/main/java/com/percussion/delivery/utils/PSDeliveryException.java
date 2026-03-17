// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

/**
 * Exception for errors in the Percussion Delivery Tier.
 *
 * @author Sunny Sal
 */
public class PSDeliveryException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PSDeliveryException(String message) {
    super(message);
  }

  public PSDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
