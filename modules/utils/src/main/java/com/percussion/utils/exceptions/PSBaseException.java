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
package com.percussion.utils.exceptions;

import com.percussion.error.IPSErrorCode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Abstract base class to handle loading exception messages and formatting message strings, handling
 * arguments and using error codes as keys to the bundle. Derived exception classes need to
 * implement {@link #getResourceBundleBaseName()} to provide the bundle class name or fully
 * qualified properties file name.
 */
public abstract class PSBaseException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param msgCode The code of the error string to load.
   * @param arrayArgs The array of arguments to use as the arguments in the error message. May be
   *     <code>null</code>, and may contain <code>null</code> elements.
   */
  public PSBaseException(int msgCode, Object... arrayArgs) {
    for (int i = 0; arrayArgs != null && i < arrayArgs.length; i++) {
      if (arrayArgs[i] == null) arrayArgs[i] = "";
    }

    m_code = msgCode;
    m_args = arrayArgs;
  }

  /**
   * Construct an exception for messages taking no arguments.
   *
   * @param msgCode The error string to load.
   */
  public PSBaseException(int msgCode) {
    this(msgCode, (Object[]) null);
  }

  /**
   * Same as {@link #PSBaseException(int, Object...)} but takes one additional parameter to indicate
   * the exception that caused this exception.
   *
   * @param msgCode The code of the error string to load.
   * @param cause The original exception that caused this exception to be thrown, may be <code>null
   *     </code>.
   * @param arrayArgs The array of arguments to use as the arguments in the error message. May be
   *     <code>null</code>, and may contain <code>null</code> elements.
   */
  public PSBaseException(int msgCode, Throwable cause, Object... arrayArgs) {
    // Pass cause to Exception super first (no post-construction initCause / this-escape)
    super(cause);
    for (int i = 0; arrayArgs != null && i < arrayArgs.length; i++) {
      if (arrayArgs[i] == null) arrayArgs[i] = "";
    }
    m_code = msgCode;
    m_args = arrayArgs;
  }

  /**
   * Typed construction from a catalogued {@link IPSErrorCode}. Sets the legacy numeric code for
   * message lookup and retains the typed code for {@link #getTypedErrorCode()} / {@link
   * #isAuditable()}.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs arguments for message formatting, may be {@code null}
   */
  public PSBaseException(IPSErrorCode code, Object... arrayArgs) {
    this(requireCode(code).numericCode(), arrayArgs);
    m_typedErrorCode = code;
  }

  /**
   * Typed construction with a cause.
   *
   * @param code catalogued error code, never {@code null}
   * @param cause original exception, may be {@code null}
   * @param arrayArgs arguments for message formatting, may be {@code null}
   */
  public PSBaseException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
    this(requireCode(code).numericCode(), cause, arrayArgs);
    m_typedErrorCode = code;
  }

  /**
   * Convenience constructor that accepts a raw message string. Preserves legacy callers which
   * provide a message directly rather than a numeric message code. This constructor is intended for
   * small, behavior-preserving compatibility changes during migration to the newer message-code
   * based API.
   *
   * @param message the exception message
   */
  public PSBaseException(String message) {
    super(message);
    this.m_code = 0;
    this.m_args = new Object[] {message};
  }

  /**
   * Convenience constructor that accepts a raw message string and a cause. Preserves legacy callers
   * which expect this signature.
   *
   * @param message the exception message
   * @param cause the causing throwable
   */
  public PSBaseException(String message, Throwable cause) {
    // super already installs the cause — do not call initCause afterward (IllegalStateException +
    // this-escape)
    super(message, cause);
    this.m_code = 0;
    this.m_args = new Object[] {message};
  }

  /**
   * Returns the localized detail message of this exception.
   *
   * @param locale The locale to generate the message in. If <code>null
   *    </code>, the default locale is used.
   * @return The localized detail message, never <code>null</code>, may be empty.
   */
  public String getLocalizedMessage(Locale locale) {
    return createMessage(m_code, m_args, locale);
  }

  /**
   * Returns the localized detail message of this exception in the default locale for this system.
   *
   * @return The localized detail message, never <code>null</code>, may be empty.
   */
  @Override
  public String getLocalizedMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }

  /**
   * Returns the localized detail message of this exception in the default locale for this system.
   *
   * @return The localized detail message, never <code>null</code>, may be empty.
   */
  @Override
  public String getMessage() {
    return getLocalizedMessage();
  }

  /**
   * Returns a description of this exception. The format used is "ExceptionClass: ExceptionMessage"
   *
   * @return the description, never <code>null</code> or empty.
   */
  @Override
  public String toString() {
    return this.getClass().getName() + ": " + getLocalizedMessage();
  }

  /**
   * Get the parsing error code associated with this exception.
   *
   * @return The error code
   */
  public int getErrorCode() {
    return m_code;
  }

  /**
   * Typed error code when constructed via {@link #PSBaseException(IPSErrorCode, Object...)} (or the
   * cause overload); otherwise {@code null} for legacy int construction.
   */
  public IPSErrorCode getTypedErrorCode() {
    return m_typedErrorCode;
  }

  /**
   * Whether dual-write should consider this exception auditable. Prefer the typed code when present;
   * legacy int construction returns {@code false}.
   */
  public boolean isAuditable() {
    return m_typedErrorCode != null && m_typedErrorCode.isAuditable();
  }

  private static IPSErrorCode requireCode(IPSErrorCode code) {
    if (code == null) {
      throw new IllegalArgumentException("code may not be null");
    }
    return code;
  }

  /**
   * Get the parsing error arguments associated with this exception.
   *
   * @return The error arguments, may be <code>null</code>.
   */
  public Object[] getErrorArguments() {
    return m_args;
  }

  /**
   * Get the stack trace for the specified exception as a string.
   *
   * @param t The throwable (usually an exception), never <code>null</code>.
   */
  public static String getStackTraceAsString(Throwable t) {
    if (t == null) throw new IllegalArgumentException("t may not be null");

    // for unknown exceptions, it's useful to log the stack trace
    StringWriter stackTrace = new StringWriter();
    PrintWriter writer = new PrintWriter(stackTrace);
    t.printStackTrace(writer);
    writer.flush();
    writer.close();

    return stackTrace.toString();
  }

  /**
   * Create a formatted message for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param msgCode The code of the error string to load.
   * @param arrayArgs The array of arguments to use as the arguments in the error message, may be
   *     <code>null</code> or empty.
   * @param loc The locale to use, may be <code>null</code>, in which case the default locale is
   *     used.
   * @return The formatted message, never <code>null</code>. If the appropriate message cannot be
   *     created, a message is constructed from the msgCode and args and is returned.
   */
  private String createMessage(int msgCode, Object[] arrayArgs, Locale loc) {
    if (arrayArgs == null) arrayArgs = new Object[0];

    String msg = getErrorText(msgCode, true, loc);

    if (msg != null) {
      try {
        msg = MessageFormat.format(msg, arrayArgs);
      } catch (IllegalArgumentException e) {
        // some problem with formatting
        msg = null;
      }
    }

    if (msg == null) {
      String sArgs = "";
      String sep = "";

      for (int i = 0; i < arrayArgs.length; i++) {
        sArgs += sep + arrayArgs[i].toString();
        sep = "; ";
      }

      msg = "" + String.valueOf(msgCode) + ": " + sArgs;
    }

    return msg;
  }

  /**
   * Get the error text associated with the specified error code.
   *
   * @param code The error code.
   * @param nullNotFound If <code>true</code>, return <code>null</code> if the error string is not
   *     found, if <code>false</code>, return the code as a string if the error string is not found.
   * @param loc The locale to use, may be <code>null</code>, in which case the default locale is
   *     used.
   * @return the error text, may be <code>null</code> or empty.
   */
  public String getErrorText(int code, boolean nullNotFound, Locale loc) {
    if (loc == null) loc = Locale.getDefault();

    ResourceBundle errorList = null;
    String errorMsg = null;
    try {
      errorList = getErrorStringBundle(loc);
      if (errorList != null) {
        errorMsg = errorList.getString(String.valueOf(code));
        return errorMsg;
      }
    } catch (MissingResourceException e) {
      // let the nullNotFound deal with this at the end.
    }

    return (nullNotFound ? null : String.valueOf(code));
  }

  /**
   * Get the base name of the resource bundle, a fully qualified class name
   *
   * @return The base name of the resource bundle, never <code>null</code> or empty.
   */
  protected abstract String getResourceBundleBaseName();

  /**
   * Get the default resource bundle for the specified locale.
   *
   * @param loc The locale of the resource bundle, it may be <code>null</code>.
   * @return The default resource bundle, never <code>null</code>.
   * @throws MissingResourceException if fail to load the default resource bundle.
   */
  private ResourceBundle getErrorStringBundle(Locale loc) throws MissingResourceException {
    if (m_bundle == null) {
      m_bundle = ResourceBundle.getBundle(getResourceBundleBaseName(), loc);
    }

    return m_bundle;
  }

  /**
   * The resource bundle containing error message formats. <code>null</code> until the first call to
   * {@link #getErrorStringBundle(Locale) getErrorStringBundle}, never <code>null</code> or modified
   * after that unless an exception occurred loading the bundle.
   */
  private transient ResourceBundle m_bundle = null;

  /** The error code of this exception, set during ctor, never modified after that. */
  private int m_code;

  /**
   * Typed catalog code when constructed via {@link IPSErrorCode} overloads; otherwise {@code null}
   * for legacy int construction.
   */
  private transient IPSErrorCode m_typedErrorCode;

  /**
   * The array of arguments to use to format the message with. Set during ctor, may be <code>null
   * </code>, never modified after that.
   */
  private transient Object[] m_args = null;
}
