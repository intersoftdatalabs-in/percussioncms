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
package com.percussion.error;

import java.util.Locale;

/**
 * This class lets us throw unchecked exceptions with a PSRuntimeException-like interface.
 *
 * <p>Please use this conservatively.
 */
public class PSRuntimeException extends RuntimeException implements IPSException {
  private static final long serialVersionUID = 1L;

  private PSRuntimeException() {
    super();
  }

  /**
   * Construct an exception for messages taking only a single argument.
   *
   * @param msgCode the error string to load
   * @param singleArg the argument to use as the sole argument in the error message
   */
  public PSRuntimeException(int msgCode, Object singleArg) {
    this(msgCode, new Object[] {singleArg});
  }

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param msgCode the error string to load
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   */
  public PSRuntimeException(int msgCode, Object[] arrayArgs) {
    super();
    m_exception = new PSException(msgCode, arrayArgs);
  }

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param msgCode the error string to load
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   * @param cause Root cause of the exception is getting passed in
   */
  public PSRuntimeException(int msgCode, Object[] arrayArgs, Throwable cause) {
    super(cause);
    m_exception = new PSException(msgCode, cause, arrayArgs);
  }

  /**
   * Construct an exception for messages taking no arguments.
   *
   * @param msgCode the error string to load
   */
  public PSRuntimeException(int msgCode) {
    this(msgCode, null);
  }

  /**
   * Typed construction from a catalogued {@link IPSErrorCode}.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSRuntimeException(IPSErrorCode code) {
    this(code, (Object[]) null);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg sole message argument; may be {@code null}
   */
  public PSRuntimeException(IPSErrorCode code, Object singleArg) {
    this(code, singleArg == null ? null : new Object[] {singleArg});
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs message arguments; may be {@code null}
   */
  public PSRuntimeException(IPSErrorCode code, Object[] arrayArgs) {
    super();
    m_exception = new PSException(code, arrayArgs);
  }

  /**
   * Typed construction with message arguments and a cause.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs message arguments; may be {@code null}
   * @param cause causal throwable; may be {@code null}
   */
  public PSRuntimeException(IPSErrorCode code, Object[] arrayArgs, Throwable cause) {
    super(cause);
    m_exception = new PSException(code, arrayArgs, cause);
  }

  /**
   * Returns the localized detail message of this exception.
   *
   * @param locale the locale to generate the message in
   * @return the localized detail message
   */
  public java.lang.String getLocalizedMessage(java.util.Locale locale) {
    return m_exception.getLocalizedMessage(locale);
  }

  /**
   * Returns the localized detail message of this exception in the default locale for this system.
   *
   * @return the localized detail message
   */
  public java.lang.String getLocalizedMessage() {
    return m_exception.getLocalizedMessage(Locale.getDefault());
  }

  /**
   * Returns the detail message of this exception.
   *
   * @return the detail message
   */
  public java.lang.String getMessage() {
    return m_exception.getLocalizedMessage(Locale.getDefault());
  }

  /**
   * Returns a description of this exception. The format used is "ExceptionClass: ExceptionMessage"
   *
   * @return the description
   */
  public java.lang.String toString() {
    String message = "";
    if (m_exception != null) {
      message = m_exception.getLocalizedMessage();
    }
    return this.getClass().getName() + ": " + message;
  }

  /**
   * Get the parsing error code associated with this exception.
   *
   * @return the error code
   */
  public int getErrorCode() {
    return m_exception.getErrorCode();
  }

  /**
   * Typed error code when constructed via {@link #PSRuntimeException(IPSErrorCode)} (or overloads);
   * otherwise {@code null} for legacy int construction.
   */
  public IPSErrorCode getTypedErrorCode() {
    return m_exception.getTypedErrorCode();
  }

  /**
   * Whether dual-write should consider this exception auditable. Prefer the typed code when
   * present; legacy int construction returns {@code false}.
   */
  public boolean isAuditable() {
    return m_exception.isAuditable();
  }

  /**
   * Get the parsing error arguments associated with this exception.
   *
   * @return the error arguments
   */
  public Object[] getErrorArguments() {
    return m_exception.getErrorArguments();
  }

  /**
   * Set the arguments for this exception.
   *
   * @param msgCode the error string to load
   * @param errorArg the argument to use as the sole argument in the error message
   */
  public void setArgs(int msgCode, Object errorArg) {
    m_exception.setArgs(msgCode, new Object[] {errorArg});
  }

  /**
   * Set the arguments for this exception.
   *
   * @param msgCode the error string to load
   * @param errorArgs the array of arguments to use as the arguments in the error message
   */
  public void setArgs(int msgCode, Object[] errorArgs) {
    m_exception.setArgs(msgCode, errorArgs);
  }

  protected PSException m_exception;
}
