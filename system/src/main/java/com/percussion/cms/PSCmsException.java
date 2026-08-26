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
package com.percussion.cms;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;
import java.util.Objects;

/** This class is used when an error occurs during CMS operations. */
public class PSCmsException extends PSException {

  /** Serialization id for {@link java.io.Serializable} ({@code -Xlint:serial}). */
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception for messages taking no arguments.
   *
   * @param msgCode the error string to load
   */
  public PSCmsException(int msgCode) {
    super(msgCode);
  }

  /**
   * Typed construction with no message arguments.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSCmsException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Construct an exception with message code and cause.
   *
   * @param code the error code
   * @param cause the underlying cause, may be {@code null}
   */
  public PSCmsException(int code, Throwable cause) {
    super(code, cause);
  }

  /**
   * Typed construction with a cause and no extra message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public PSCmsException(IPSErrorCode code, Throwable cause) {
    super(code, (Object[]) null, cause);
  }

  /**
   * Construct an exception with cause only.
   *
   * @param cause the underlying cause, may not be {@code null}
   * @throws IllegalArgumentException if cause is {@code null}
   */
  public PSCmsException(Throwable cause) {
    super(Objects.requireNonNull(cause, "cause cannot be null"));
  }

  /**
   * Construct an exception from a class derived from PSException.
   *
   * @param ex The exception to use. Its message code and arguments are stored. May not be {@code
   *     null}.
   * @throws IllegalArgumentException if ex is {@code null}
   */
  public PSCmsException(PSException ex) {
    this(
        Objects.requireNonNull(ex, "PSException cannot be null").getErrorCode(),
        ex.getErrorArguments(),
        ex);
  }

  /**
   * Construct an exception for messages taking 2 specific arguments.
   *
   * @param msgCode the error string to load
   * @param contentId the contentid on which the error occurred. May be 0.
   * @param revisionId the revisionid on which the error occurred. May be 0.
   */
  public PSCmsException(int msgCode, int contentId, int revisionId) {
    this(msgCode, new Integer[] {contentId, revisionId});
  }

  /**
   * Construct an exception for messages taking a single string argument.
   *
   * @param msgCode the error string to load
   * @param singleMessage the sole argument to use as the arguments in the error message, may be
   *     {@code null}
   */
  public PSCmsException(int msgCode, String singleMessage) {
    super(msgCode, singleMessage);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleMessage the sole argument to use as the arguments in the error message, may be
   *     {@code null}
   */
  public PSCmsException(IPSErrorCode code, String singleMessage) {
    super(code, singleMessage);
  }

  /**
   * Construct an exception for messages taking multiple arguments.
   *
   * @param msgCode the error string to load
   * @param arrayArgs the array of arguments to use as the arguments in the error message, may be
   *     {@code null}
   */
  public PSCmsException(int msgCode, Object[] arrayArgs) {
    super(msgCode, arrayArgs);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments to use as the arguments in the error message, may be
   *     {@code null}
   */
  public PSCmsException(IPSErrorCode code, Object[] arrayArgs) {
    super(code, arrayArgs);
  }

  /**
   * Construct an exception for messages taking multiple arguments with cause.
   *
   * @param msgCode the error string to load
   * @param arrayArgs the array of arguments to use as the arguments in the error message, may be
   *     {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public PSCmsException(int msgCode, Object[] arrayArgs, Throwable cause) {
    super(msgCode, arrayArgs, cause);
  }

  /**
   * Typed construction with message arguments and a cause.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments to use as the arguments in the error message, may be
   *     {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public PSCmsException(IPSErrorCode code, Object[] arrayArgs, Throwable cause) {
    super(code, arrayArgs, cause);
  }
}
