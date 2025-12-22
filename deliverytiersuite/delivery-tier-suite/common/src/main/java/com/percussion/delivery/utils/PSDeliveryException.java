// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

/**
 * Exception for errors in the Percussion Delivery Tier.
 *
 * @author Sunny Sal
 */
public class PSDeliveryException extends RuntimeException {

  public PSDeliveryException(String message) {
    super(message);
  }

  public PSDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
