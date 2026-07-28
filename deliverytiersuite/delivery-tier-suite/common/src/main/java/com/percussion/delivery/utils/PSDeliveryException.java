// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

/**
 * Exception for errors in the Percussion Delivery Tier.
 *
 * @author Sunny Sal
 */
public class PSDeliveryException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new delivery tier runtime exception with the supplied message.
   *
   * @param message the detail message, may be <code>null</code>.
   */
  public PSDeliveryException(String message) {
    super(message);
  }

  /**
   * Constructs a new delivery tier runtime exception with the supplied message and cause.
   *
   * @param message the detail message, may be <code>null</code>.
   * @param cause the underlying cause, may be <code>null</code>.
   */
  public PSDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
