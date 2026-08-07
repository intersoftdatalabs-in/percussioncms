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

import com.percussion.share.validation.PSValidationErrors;

/**
 * The base validation exception for <strong>expected</strong> failures.
 *
 * <p>The REST conversion of these exceptions will be HTTP Code <code>400</code>. All other {@link
 * RuntimeException RuntimeExceptions} will be HTTP Code <code>500</code>.
 *
 * <p>The validation exceptions are loosly based on the Spring Frameworks Validation.
 *
 * <p>{@link #validationErrors} is {@code transient}: validation payloads are transported via JAXB /
 * REST mappers (see {@link IPSValidationException}), not via Java serialization of the exception
 * graph. {@link PSValidationErrors} is intentionally not {@link java.io.Serializable}.
 *
 * @see PSSpringValidationException
 * @see PSValidationErrors
 * @author adamgent
 */
public abstract class PSValidationException extends PSDataServiceException
    implements IPSValidationException {

  /**
   * The validation errors carried by this exception; may be {@code null}. Not part of Java
   * serialization (JAXB / REST transport only).
   */
  private transient PSValidationErrors validationErrors;

  /**
   * Constructs a validation exception that wraps the given validation errors.
   *
   * <p>Assigns the field directly (not via an overridable setter) so construction is {@code
   * this-escape} free under {@code -Xlint:all}.
   *
   * @param validationErrors the validation errors to wrap, never {@code null}.
   */
  public PSValidationException(PSValidationErrors validationErrors) {
    super(validationErrors.toString());
    this.validationErrors = validationErrors;
  }

  /**
   * Constructs a validation exception with the given message.
   *
   * @param message the detail message, may be {@code null}.
   */
  public PSValidationException(String message) {
    super(message);
  }

  /**
   * Constructs a validation exception with the given message and cause.
   *
   * @param message the detail message, may be {@code null}.
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a validation exception that wraps the given cause.
   *
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSValidationException(Throwable cause) {
    super(cause);
  }

  /**
   * {@inheritDoc}
   *
   * @return the wrapped validation errors, may be {@code null} when the exception was constructed
   *     without a {@link PSValidationErrors} instance.
   */
  @Override
  public PSValidationErrors getValidationErrors() {
    return validationErrors;
  }

  /**
   * Replaces the validation errors carried by this exception.
   *
   * @param validationErrors the new validation errors, may be {@code null}.
   */
  public final void setValidationErrors(PSValidationErrors validationErrors) {
    this.validationErrors = validationErrors;
  }

  /**
   * Throws this exception if any validation errors are present.
   *
   * @return this exception for fluent chaining, never {@code null}.
   * @throws PSValidationException always thrown when this exception is currently in an invalid
   *     state (i.e. it carries validation errors).
   */
  public PSValidationException throwIfInvalid() throws PSValidationException {
    if (validationErrors != null && validationErrors.hasErrors()) {
      throw this;
    }
    return this;
  }

  private static final long serialVersionUID = 1L;
}
