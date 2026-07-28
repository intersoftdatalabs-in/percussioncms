// REFACTORED: CP-JAVA11
package com.percussion.delivery.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logger utility for Percussion Delivery Tier. Uses Log4j 2.x API.
 *
 * @author Sunny Sal
 */
public final class PSDeliveryLogger {

  private static final Logger log = LogManager.getLogger(PSDeliveryLogger.class);

  private PSDeliveryLogger() {
    // Utility class, do not instantiate.
  }

  /**
   * Logs an informational message.
   *
   * @param message the message to log, may be <code>null</code>.
   */
  public static void info(String message) {
    log.info(message);
  }

  /**
   * Logs a warning message.
   *
   * @param message the message to log, may be <code>null</code>.
   */
  public static void warn(String message) {
    log.warn(message);
  }

  /**
   * Logs an error message with the supplied throwable.
   *
   * @param message the message to log, may be <code>null</code>.
   * @param throwable the throwable to log, may be <code>null</code>.
   */
  public static void error(String message, Throwable throwable) {
    log.error(message, throwable);
  }
}
