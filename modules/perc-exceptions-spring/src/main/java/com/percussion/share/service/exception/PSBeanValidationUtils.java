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

import com.percussion.share.validation.PSAbstractBeanValidator;
import com.percussion.share.validation.PSValidationErrors;

/**
 * Utility class providing convenience methods for validating beans using the default {@link
 * PSAbstractBeanValidator} instance.
 *
 * @author adamgent
 */
public class PSBeanValidationUtils {

  /**
   * Default constructor required for proxy-based instantiation frameworks; this is a static utility
   * class and should not be instantiated.
   */
  public PSBeanValidationUtils() {
    super();
  }

  /**
   * The default no-op bean validator. Subclasses or callers may supply a more specific validator
   * via the {@code validate(...)} overloads; the default validator simply records the call.
   */
  public static PSAbstractBeanValidator<Object> defaultValidator = new DefaultValidator();

  /**
   * Default {@link PSAbstractBeanValidator} that performs no additional validation beyond what is
   * contributed by the underlying Spring/OVal mechanism.
   *
   * @author adamgent
   */
  public static class DefaultValidator extends PSAbstractBeanValidator<Object> {

    /** Default constructor required for proxy-based instantiation frameworks. */
    public DefaultValidator() {
      super();
    }

    @Override
    protected void doValidation(Object obj, PSBeanValidationException e) {
      // Do nothing.
    }
  }

  /**
   * Validates the given object and returns its validation errors, throwing a {@link
   * PSBeanValidationException} if any errors are present.
   *
   * @param <FULL> the type of the object to validate.
   * @param obj the object to validate, never {@code null}.
   * @return the validation errors, never {@code null}.
   * @throws PSBeanValidationException if validation produces any errors.
   */
  public static <FULL> PSValidationErrors getValidationErrorsOrFailIfInvalid(FULL obj)
      throws PSBeanValidationException {
    try {
      PSBeanValidationException e = defaultValidator.validate(obj);
      e.throwIfInvalid();
      return e.getValidationErrors();
    } catch (PSValidationException e) {
      throw new PSBeanValidationException(e);
    }
  }

  /**
   * Validates the given object and returns the resulting exception container so callers can decide
   * how to surface the errors.
   *
   * @param <FULL> the type of the object to validate.
   * @param obj the object to validate, never {@code null}.
   * @return a {@link PSBeanValidationException} carrying any validation errors, never {@code null}.
   */
  public static <FULL> PSBeanValidationException validate(FULL obj) {
    try {
      PSBeanValidationException e = defaultValidator.validate(obj);
      return e;
    } catch (PSValidationException e) {
      return new PSBeanValidationException(e);
    }
  }

  /**
   * Returns the validation errors recorded for the given object, or an empty result when there are
   * none.
   *
   * @param <FULL> the type of the object to validate.
   * @param obj the object to validate, never {@code null}.
   * @return the validation errors, never {@code null}.
   */
  public static <FULL> PSValidationErrors getValidationErrors(FULL obj) {
    try {
      PSBeanValidationException e = defaultValidator.validate(obj);
      return e.getValidationErrors();
    } catch (PSValidationException e) {
      return new PSBeanValidationException(e).getValidationErrors();
    }
  }

  /**
   * Validates the given object, writing any errors into the supplied exception container.
   *
   * @param <FULL> the type of the object to validate.
   * @param obj the object to validate, never {@code null}.
   * @param errors the container that receives any validation errors, never {@code null}.
   */
  public static <FULL> void validate(FULL obj, PSBeanValidationException errors) {
    defaultValidator.validate(obj, errors);
  }
}
