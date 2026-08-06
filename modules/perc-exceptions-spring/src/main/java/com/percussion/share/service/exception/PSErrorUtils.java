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
package com.percussion.share.service.exception;

import static org.apache.commons.lang3.Validate.*;

import com.percussion.error.IPSException;
import com.percussion.share.validation.PSErrorCause;
import com.percussion.share.validation.PSErrors;
import com.percussion.share.validation.PSErrors.PSObjectError;

/**
 * Utilities for create {@link PSErrors} objects.
 *
 * @author adamgent
 */
@SuppressWarnings("serial")
public class PSErrorUtils {

  /**
   * Default constructor required for proxy-based instantiation frameworks; this is a static utility
   * class and should not be instantiated.
   */
  public PSErrorUtils() {
    super();
  }

  /**
   * Creates a {@link PSErrors} object that represents the given exception. The resulting errors
   * object contains a single global error whose code is taken from {@link
   * IPSException#getErrorCode()} when applicable or the exception class's canonical name otherwise.
   *
   * @param exception the exception to convert, never {@code null}.
   * @return the populated errors object, never {@code null}.
   * @throws NullPointerException if {@code exception} is {@code null}.
   */
  public static PSErrors createErrorsFromException(Throwable exception) {
    notNull(exception, "exception cannot be null");
    PSErrors errors = new PSErrors();
    PSObjectError oe = new PSObjectError();

    if (exception instanceof IPSException) {
      oe.setCode(Integer.toString(((IPSException) exception).getErrorCode()));
    } else {
      oe.setCode(exception.getClass().getCanonicalName());
    }
    String cause = exception.getMessage();
    if (exception.getCause() != null) {
      if (exception.getCause().getLocalizedMessage() != null) {
        cause = exception.getCause().getLocalizedMessage();
      } else if (exception.getCause().getMessage() != null) {
        cause = exception.getCause().getMessage();
      }
    }
    if (cause == null || cause.isEmpty()) {
      cause = "Server error processing request, see log for details.";
    }
    oe.setDefaultMessage(cause);
    oe.setCause(new PSErrorCause(exception));
    errors.setGlobalError(oe);
    return errors;
  }

  /**
   * Wraps the given errors in a runtime exception that carries them forward.
   *
   * @param errors the errors to wrap, never {@code null}.
   * @return a {@link PSProxyException} carrying the supplied errors, never {@code null}.
   * @throws NullPointerException if {@code errors} is {@code null}.
   */
  public static RuntimeException createExceptionFromErrors(PSErrors errors) {
    notNull(errors, "errors cannot be null");
    return new PSProxyException(errors);
  }

  /**
   * A runtime exception that carries a {@link PSErrors} payload across boundaries where checked
   * exceptions cannot be thrown.
   *
   * @author adamgent
   */
  public static class PSProxyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The message returned by {@link #getMessage()}; may be {@code null}. */
    private String message;

    /** The wrapped errors; never {@code null} after {@link #convert(PSErrors)} is invoked. */
    protected PSErrors errors;

    /**
     * Constructs a proxy exception that wraps the supplied errors.
     *
     * @param errors the errors to carry, may be {@code null} until {@link #convert(PSErrors)} is
     *     called.
     */
    public PSProxyException(PSErrors errors) {
      super();
      this.errors = errors;
    }

    /**
     * Replaces the carried errors and synchronizes this exception's message with the global error's
     * default message.
     *
     * @param errors the new errors to carry, never {@code null}.
     */
    protected void convert(PSErrors errors) {
      this.errors = errors;
      notNull(errors, "errors cannot be null");
      PSObjectError oe = errors.getGlobalError();
      setMessage(oe.getDefaultMessage());
    }

    @Override
    public String getMessage() {
      return message;
    }

    /**
     * Sets the message returned by {@link #getMessage()}.
     *
     * @param message the message to expose, may be {@code null}.
     */
    protected void setMessage(String message) {
      this.message = message;
    }
  }
}
