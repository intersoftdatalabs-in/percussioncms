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
package com.percussion.content;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import java.util.Objects;

/** This exception is used to propagate errors that occur during content conversion operations. */
// REFACTORED: CP-JAVA11
public final class PSContentConversionException extends PSException {
  /**
   * Construct an exception with message code and single argument.
   *
   * @param msgCode the error message code
   * @param singleArg the single argument for the error message, may be {@code null}
   */
  public PSContentConversionException(int msgCode, Object singleArg) {
    super(msgCode, singleArg);
  }

  /**
   * Construct an exception with language, message code and single argument.
   *
   * @param language the language for the error message, may be {@code null}
   * @param msgCode the error message code
   * @param singleArg the single argument for the error message, may be {@code null}
   */
  public PSContentConversionException(String language, int msgCode, Object singleArg) {
    super(language, msgCode, singleArg);
  }

  /**
   * Construct an exception with message code and array of arguments.
   *
   * @param msgCode the error message code
   * @param arrayArgs the array of arguments for the error message, may be {@code null}
   */
  public PSContentConversionException(int msgCode, Object[] arrayArgs) {
    super(msgCode, arrayArgs);
  }

  /**
   * Construct an exception with language, message code and array of arguments.
   *
   * @param language the language for the error message, may be {@code null}
   * @param msgCode the error message code
   * @param arrayArgs the array of arguments for the error message, may be {@code null}
   */
  public PSContentConversionException(String language, int msgCode, Object[] arrayArgs) {
    super(language, msgCode, arrayArgs);
  }

  /**
   * Construct an exception with message code only.
   *
   * @param msgCode the error message code
   */
  public PSContentConversionException(int msgCode) {
    super(msgCode);
  }

  /**
   * Construct an exception with language and message code.
   *
   * @param language the language for the error message, may be {@code null}
   * @param msgCode the error message code
   */
  public PSContentConversionException(String language, int msgCode) {
    super(language, msgCode);
  }

  /**
   * Construct an exception with message code and cause.
   *
   * @param msgCode the error message code
   * @param cause the underlying cause, may be {@code null}
   */
  public PSContentConversionException(int msgCode, Throwable cause) {
    super(msgCode, cause);
  }

  /**
   * Construct an exception with cause only.
   *
   * @param cause the underlying cause, may not be {@code null}
   * @throws IllegalArgumentException if cause is {@code null}
   */
  public PSContentConversionException(Throwable cause) {
    super(Objects.requireNonNull(cause, "cause cannot be null"));
  }

  /**
   * Construct an exception with message code, arguments and cause.
   *
   * @param msgCode the error message code
   * @param arrayArgs the array of arguments for the error message, may be {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public PSContentConversionException(int msgCode, Object[] arrayArgs, Throwable cause) {
    super(msgCode, arrayArgs, cause);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg the single argument for the error message, may be {@code null}
   */
  public PSContentConversionException(IPSErrorCode code, Object singleArg) {
    super(code, singleArg);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments for the error message, may be {@code null}
   */
  public PSContentConversionException(IPSErrorCode code, Object[] arrayArgs) {
    super(code, arrayArgs);
  }

  /**
   * Typed construction with no message arguments.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSContentConversionException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Typed construction with message arguments and cause.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments for the error message, may be {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public PSContentConversionException(IPSErrorCode code, Object[] arrayArgs, Throwable cause) {
    super(code, arrayArgs, cause);
  }

}
