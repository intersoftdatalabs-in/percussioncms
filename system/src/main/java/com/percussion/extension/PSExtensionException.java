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
package com.percussion.extension;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import java.util.Objects;

/**
 * Exception class for exceptional error conditions relating to extensions, extension handlers, and
 * extension managers.
 */
public final class PSExtensionException extends PSException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructor that takes the underlying cause.
   *
   * @param cause the underlying cause, may not be {@code null}
   * @throws IllegalArgumentException if cause is {@code null}
   */
  public PSExtensionException(Throwable cause) {
    super(Objects.requireNonNull(cause, "cause cannot be null"));
  }

  /**
   * Constructor that takes the error message.
   *
   * @param msg the error message, may not be {@code null}
   * @throws IllegalArgumentException if msg is {@code null}
   */
  public PSExtensionException(String msg) {
    super(Objects.requireNonNull(msg, "message cannot be null"));
  }

  /**
   * Create a chained exception with a specific message.
   *
   * @param message message to use in exception. If {@code null} then use the localized message from
   *     the original exception
   * @param cause the original exception, may not be {@code null}
   * @throws IllegalArgumentException if cause is {@code null}
   */
  public PSExtensionException(String message, Throwable cause) {
    super(message, Objects.requireNonNull(cause, "cause cannot be null"));
  }

  /**
   * Construct an exception for messages taking only a single argument.
   *
   * @param code the error string to load
   * @param arg the argument to use as the sole argument in the error message
   */
  public PSExtensionException(int code, Object arg) {
    this(code, new Object[] {arg});
  }

  /**
   * Construct an exception for messages taking only a single argument.
   *
   * @param language language string to use while looking up for the message text in the resource
   *     bundle
   * @param code the error string to load
   * @param arg the argument to use as the sole argument in the error message
   */
  public PSExtensionException(String language, int code, Object arg) {
    super(language, code, arg);
  }

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param code the error string to load
   * @param args the array of arguments to use as the arguments in the error message
   */
  public PSExtensionException(int code, Object[] args) {
    super(code, args);
  }

  /**
   * Typed construction with no message arguments.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSExtensionException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param arg the argument to use as the sole argument in the error message
   */
  public PSExtensionException(IPSErrorCode code, Object arg) {
    super(code, arg);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param args the array of arguments to use as the arguments in the error message
   */
  public PSExtensionException(IPSErrorCode code, Object[] args) {
    super(code, args);
  }

  /**
   * Typed construction with a cause and message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param cause the underlying cause, may be {@code null}
   * @param args the arguments for the error message
   */
  public PSExtensionException(IPSErrorCode code, Throwable cause, Object... args) {
    super(code, args, cause);
  }

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param language language string to use while looking up for the message text in the resource
   *     bundle
   * @param code the error string to load
   * @param args the array of arguments to use as the arguments in the error message
   */
  public PSExtensionException(String language, int code, Object[] args) {
    super(language, code, args);
  }

  /**
   * Construct an exception for extension-specific errors.
   *
   * @param ext the extension reference, may not be {@code null}
   * @param errorText the error text, may not be {@code null}
   * @throws IllegalArgumentException if ext or errorText is {@code null}
   */
  public PSExtensionException(PSExtensionRef ext, String errorText) {
    super(Objects.requireNonNull(errorText, "errorText cannot be null"));
    Objects.requireNonNull(ext, "extension reference cannot be null");
  }

  /**
   * Construct an exception for extension-specific errors with language.
   *
   * @param language language string to use while looking up for the message text in the resource
   *     bundle
   * @param ext the extension reference, may not be {@code null}
   * @param errorText the error text, may not be {@code null}
   * @throws IllegalArgumentException if ext or errorText is {@code null}
   */
  public PSExtensionException(String language, PSExtensionRef ext, String errorText) {
    super(Objects.requireNonNull(errorText, "errorText cannot be null"));
    Objects.requireNonNull(ext, "extension reference cannot be null");
  }

  /**
   * Construct an exception for handler-specific errors.
   *
   * @param handlerName the handler name, may not be {@code null}
   * @param errorText the error text, may not be {@code null}
   * @throws IllegalArgumentException if handlerName or errorText is {@code null}
   */
  public PSExtensionException(String handlerName, String errorText) {
    super(Objects.requireNonNull(errorText, "errorText cannot be null"));
    Objects.requireNonNull(handlerName, "handlerName cannot be null");
  }

  /**
   * Construct an exception for handler-specific errors with language.
   *
   * @param language language string to use while looking up for the message text in the resource
   *     bundle
   * @param handlerName the handler name, may not be {@code null}
   * @param errorText the error text, may not be {@code null}
   * @throws IllegalArgumentException if handlerName or errorText is {@code null}
   */
  public PSExtensionException(String language, String handlerName, String errorText) {
    super(Objects.requireNonNull(errorText, "errorText cannot be null"));
    Objects.requireNonNull(handlerName, "handlerName cannot be null");
  }

  /**
   * Construct an exception with message code, cause, and arguments.
   *
   * @param code the error message code
   * @param cause the underlying cause, may be {@code null}
   * @param args the arguments for the error message
   */
  public PSExtensionException(int code, Throwable cause, Object... args) {
    super(code, args, cause);
  }
}
