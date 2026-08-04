/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.content.ui.aa.actions;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.StringUtils;

/**
 * An exception that is used when a error or exception occurs when executing an AA client action. If
 * constructed with a nested {@link InvocationTargetException} extracts the nested exception from
 * it.
 */
public class PSAAClientActionException extends Exception {

  /** Default no-argument constructor; see {@link Exception#Exception()}. */
  public PSAAClientActionException() {
    super();
  }

  /**
   * Constructs an exception with the supplied message and cause. See {@link
   * Exception#Exception(String, Throwable)}.
   *
   * @param message the detail message, may be {@code null}.
   * @param cause the underlying cause; if it is an {@link InvocationTargetException} the nested
   *     cause is unwrapped.
   */
  public PSAAClientActionException(String message, Throwable cause) {
    super(message, maybeGetNestedException(cause));
  }

  /**
   * Constructs an exception with the supplied message. See {@link Exception#Exception(String)}.
   *
   * @param message the detail message, may be {@code null}.
   */
  public PSAAClientActionException(String message) {
    super(message);
  }

  /**
   * Constructs an exception wrapping the supplied cause. See {@link
   * Exception#Exception(Throwable)}.
   *
   * @param cause the underlying cause; if it is an {@link InvocationTargetException} the nested
   *     cause is unwrapped.
   */
  public PSAAClientActionException(Throwable cause) {
    super(maybeGetNestedException(cause));
  }

  /**
   * If the passed exception is {@link InvocationTargetException}, returns the nested exception,
   * otherwise returns the specified exception. Is static, so it can be called from a constructor.
   *
   * @param t the exception to extract cause exception from. If <code>null</code>, the method
   *     returns null.
   */
  private static Throwable maybeGetNestedException(Throwable t) {
    return t instanceof InvocationTargetException ? t.getCause() : t;
  }

  /**
   * Creates a message from this exception message and the cause.
   *
   * @param message this exception message. Can be <code>null</code> or empty.
   * @param causeMessage the message of the cause exception. Can be <code>null</code> or empty.
   */
  private String composeMessageFromCause(final String message, final String causeMessage) {
    if (getCause() == null) {
      return message;
    } else {
      return StringUtils.isBlank(message) ? causeMessage : message;
    }
  }

  /**
   * {@inheritDoc} This implementation excludes cause exception class name if cause is specified.
   */
  @Override
  public String getMessage() {
    return composeMessageFromCause(
        super.getMessage(), getCause() == null ? null : getCause().getMessage());
  }

  /**
   * {@inheritDoc} This implementation excludes cause exception class name if cause is specified.
   */
  @Override
  public String getLocalizedMessage() {
    return composeMessageFromCause(
        super.getLocalizedMessage(), getCause() == null ? null : getCause().getLocalizedMessage());
  }
}
