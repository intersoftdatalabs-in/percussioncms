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
package com.percussion.share.validation;

import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSErrors.PSObjectError;
import com.percussion.share.validation.PSValidationErrors.PSFieldError;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * A fluent patterned validation errors builder. http://en.wikipedia.org/wiki/Fluent_interface
 *
 * @author adamgent
 */
public class PSValidationErrorsBuilder {

  /** The {@link PSValidationErrors} being built up by this builder. */
  private PSValidationErrors validationErrors;

  /**
   * Constructs a new builder that records errors against the given object or method name.
   *
   * @param objectName the name of the method or object being validated, never {@code null}.
   */
  public PSValidationErrorsBuilder(String objectName) {
    super();
    this.validationErrors = new PSValidationErrors();
    this.validationErrors.setMethodName(objectName);
  }

  /**
   * Records a global error with the supplied code and default message.
   *
   * @param code the error code, may be {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   * @return this builder for fluent chaining, never {@code null}.
   */
  public PSValidationErrorsBuilder reject(String code, String defaultMessage) {
    PSObjectError objectError = new PSObjectError();
    objectError.setCode(code);
    objectError.setDefaultMessage(defaultMessage);
    validationErrors.getGlobalErrors().add(objectError);
    return this;
  }

  /**
   * Records a field error with the supplied code, default message, and rejected value.
   *
   * @param field the name of the field that failed validation, never {@code null}.
   * @param code the error code, never {@code null}.
   * @param defaultMessage the default message, never {@code null}.
   * @param value the value that was rejected, may be {@code null}.
   * @return this builder for fluent chaining, never {@code null}.
   */
  public PSValidationErrorsBuilder rejectField(
      String field, String code, String defaultMessage, Object value) {
    Validate.notNull(field, "field cannot be null");
    Validate.notNull(code, "code cannot be null");
    Validate.notNull(defaultMessage, "defaultMessage cannot be null");
    PSFieldError e = new PSFieldError();
    e.setCode(code);
    e.setDefaultMessage(defaultMessage);
    e.setField(field);
    validationErrors.getFieldErrors().add(e);
    return this;
  }

  /**
   * Convenience rule that records a field error when the supplied value is {@code null}.
   *
   * @param field the name of the field being validated, never {@code null}.
   * @param value the value to check, may be {@code null}.
   * @return this builder for fluent chaining, never {@code null}.
   */
  public PSValidationErrorsBuilder rejectIfNull(String field, Object value) {
    if (value == null) return rejectField(field, field + " cannot be null", value);
    return this;
  }

  /**
   * Convenience rule that records a field error when the supplied value is {@code null}, empty, or
   * contains only whitespace.
   *
   * @param field the name of the field being validated, never {@code null}.
   * @param value the value to check, may be {@code null}.
   * @return this builder for fluent chaining, never {@code null}.
   */
  public PSValidationErrorsBuilder rejectIfBlank(String field, String value) {
    if (StringUtils.isBlank(value)) return rejectField(field, field + " cannot be blank", value);
    return this;
  }

  /**
   * Records a field error using the current method name (configured on the underlying {@link
   * PSValidationErrors}) as the error code prefix.
   *
   * @param field the name of the field that failed validation, never {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   * @param value the value that was rejected, may be {@code null}.
   * @return this builder for fluent chaining, never {@code null}.
   */
  public PSValidationErrorsBuilder rejectField(String field, String defaultMessage, Object value) {
    rejectField(field, validationErrors.getMethodName() + "#" + field, defaultMessage, value);
    return this;
  }

  /**
   * Returns the current state of the validation errors and resets the builder's view of them.
   *
   * @return the validation errors accumulated so far, never {@code null}.
   */
  public PSValidationErrors build() {
    return validationErrors;
  }

  /**
   * Convenience method that throws a {@link PSParametersValidationException} if any errors have
   * been recorded.
   *
   * @return this builder for fluent chaining, never {@code null}.
   * @throws PSValidationException always thrown when this builder is currently in an invalid state.
   */
  public PSValidationErrorsBuilder throwIfInvalid() throws PSValidationException {
    new PSParametersValidationException(validationErrors).throwIfInvalid();
    return this;
  }
}
