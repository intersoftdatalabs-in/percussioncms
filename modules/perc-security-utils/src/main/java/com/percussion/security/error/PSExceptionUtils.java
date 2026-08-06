/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.security.error;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Provides common exception utilities for logging and message extraction.
 *
 * @author Percussion Security Team
 * @since 8.2.0
 */
public class PSExceptionUtils {

  /** Private constructor to prevent instantiation. */
  private PSExceptionUtils() {}

  // Logger removed as it was unused.
  /**
   * Returns a formatted message for logging purposes, including class and line number.
   *
   * <p>If a cause exists, its message and location are appended.
   *
   * @param exception the exception to extract details from
   * @return a formatted string for logging
   */
  /**
   * Returns a formatted message for logging purposes, including class and line number.
   *
   * <p>If a cause exists, its message and location are appended.
   *
   * @param exception the exception to extract details from
   * @return a formatted string for logging
   */
  public static String getMessageForLog(Throwable exception) {
    // Try localized message first and if there isn't one, use default.
    var message = exception.getLocalizedMessage();
    // Get line number and class
    var stackTrace = exception.getStackTrace();
    int line = stackTrace.length > 0 ? stackTrace[0].getLineNumber() : -1;
    String clazz = stackTrace.length > 0 ? stackTrace[0].getClassName() : "Unknown";
    if (message == null || message.isEmpty()) {
      message = exception.getMessage();
    }
    message = message + " C:" + clazz + ":L:" + line;
    // Add cause if there is one
    if (exception.getCause() != null) {
      var cause = exception.getCause();
      var causeMessage = cause.getMessage();
      var causeStack = cause.getStackTrace();
      int causeLine = causeStack.length > 0 ? causeStack[0].getLineNumber() : -1;
      String causeClass = causeStack.length > 0 ? causeStack[0].getClassName() : "Unknown";
      message += " Cause:" + causeMessage + " C:" + causeClass + ":L:" + causeLine;
    }
    return message;
  }

  /**
   * Use when outputting error messages or warnings to the log based on exceptions.
   *
   * <p>The message will be written out localized if the localized message is available and the
   * error message will include C:[ClassName] L:[Line Number] to aid in problem diagnosis without
   * flooding the log with stack traces, Stack traces should only ever be written to the debug log.
   *
   * @param exception A valid exception, never null;
   * @return A string with the message, never null;
   */
  public static String getMessageForLog(Exception exception) {
    return getMessageForLog((Throwable) exception);
  }

  /**
   * Use when outputting stack trace etc to debug log.
   *
   * @param e A valid exception
   * @return A safe debug string to wrote to the log.
   */
  public static String getDebugMessageForLog(Exception e) {
    try (StringWriter sw = new StringWriter()) {
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      return sw.toString().trim();
    } catch (IOException ioException) {
      return "Unable to extract stack trace for exception. Error: "
          + PSExceptionUtils.getMessageForLog(ioException);
    }
  }
}
