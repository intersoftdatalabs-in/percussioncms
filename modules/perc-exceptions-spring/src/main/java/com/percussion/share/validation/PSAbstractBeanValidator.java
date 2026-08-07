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

import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSValidationException;
import net.sf.oval.exception.ValidationFailedException;
import net.sf.oval.integration.spring.SpringValidator;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Adapter that wraps the OVal-based Spring {@link SpringValidator} and exposes a {@link
 * PSBeanValidationException}-aware contract for validating plain Java bean objects. See Spring's
 * {@link Validator} for the general contract.
 *
 * @author adamgent
 * @param <FULL> the class to be validated.
 */
public abstract class PSAbstractBeanValidator<FULL> implements Validator {
  /**
   * Default constructor required for proxy-based instantiation frameworks; subclasses provide the
   * bean-specific behavior via {@link #doValidation(Object, PSBeanValidationException)}.
   */
  protected PSAbstractBeanValidator() {
    super();
  }

  /** The wrapped OVal Spring validator. */
  private SpringValidator ovalValidator = new SpringValidator();

  {
    ovalValidator.setValidator(new net.sf.oval.Validator());
  }

  /**
   * Validates the given object and returns the resulting exception container.
   *
   * @param obj the object to validate, never {@code null}.
   * @return a {@link PSBeanValidationException} that aggregates any validation errors; the
   *     exception is never {@code null} but is only meant to be thrown via {@link
   *     PSBeanValidationException#throwIfInvalid()}.
   * @throws PSValidationException if {@code obj} is {@code null}.
   */
  public PSBeanValidationException validate(FULL obj) throws PSValidationException {

    PSParameterValidationUtils.rejectIfNull("validate", "object", obj);
    PSBeanValidationException e =
        new PSBeanValidationException(obj, obj.getClass().getCanonicalName());
    validate(obj, e);
    return e;
  }

  /**
   * Template method that subclasses override to perform bean-specific validation in addition to
   * OVal constraint checks.
   *
   * @param obj the object being validated, never {@code null}.
   * @param e the exception container into which additional errors should be recorded, never {@code
   *     null}.
   * @throws PSValidationException if validation cannot proceed; the exception is added as a
   *     suppressed error on {@code e}.
   */
  protected abstract void doValidation(FULL obj, PSBeanValidationException e)
      throws PSValidationException;

  /**
   * Delegates the Spring {@link Validator#supports(Class)} contract to the underlying OVal
   * validator.
   *
   * @param clazz the candidate target class, never {@code null}.
   * @return {@code true} if the underlying validator supports {@code clazz}.
   */
  public boolean supports(Class<?> clazz) {
    return ovalValidator.supports(clazz);
  }

  /**
   * Validates the given object using the wrapped OVal validator and then forwards to {@link
   * #doValidation(Object, PSBeanValidationException)} when the {@link Errors} container is a {@link
   * PSBeanValidationException}.
   *
   * @param object the object to validate, may be {@code null} only when the wrapped validator
   *     accepts it.
   * @param errors the Spring {@link Errors} container to populate; must be a {@link
   *     PSBeanValidationException} to receive the additional {@link #doValidation} pass.
   */
  public void validate(Object object, Errors errors) {
    try {
      ovalValidator.validate(object, errors);
      if (errors instanceof PSBeanValidationException beanErrors) {
        try {
          // Spring Validator is typed to Object; FULL is compile-time only. castToFull uses
          // Class.cast when a type token is available, otherwise an unchecked bridge required by
          // the Spring contract.
          doValidation(castToFull(object), beanErrors);
        } catch (PSValidationException e) {
          beanErrors.addSuppressed(e);
        }
      }
    } catch (ValidationFailedException ex) {
      if (errors instanceof PSBeanValidationException beanErrors) {
        beanErrors.addSuppressed(ex);
      }
    }
  }

  /**
   * Converts the Spring {@link Validator} target to {@code FULL}.
   *
   * <p>When {@link #getFullType()} is non-null, uses a checked {@link Class#cast(Object)}.
   * Otherwise falls back to an unchecked cast required by type erasure and Spring's raw {@code
   * Object} contract (no Class token is available without breaking existing subclasses).
   *
   * @param object the target object, may be {@code null}.
   * @return the object as {@code FULL}.
   */
  protected FULL castToFull(Object object) {
    Class<FULL> type = getFullType();
    if (type != null) {
      return type.cast(object);
    }
    return uncheckedCast(object);
  }

  /**
   * Optional type token for {@code FULL}. Subclasses may override to enable a checked {@link
   * Class#cast(Object)} in {@link #castToFull(Object)}.
   *
   * @return the class of {@code FULL}, or {@code null} when not available.
   */
  protected Class<FULL> getFullType() {
    return null;
  }

  /**
   * Unchecked bridge for Spring's raw {@link Validator#validate(Object, Errors)} when no {@link
   * Class} token is available.
   *
   * @param object the target object, may be {@code null}.
   * @return the object as {@code FULL}.
   */
  @SuppressWarnings("unchecked")
  private static <T> T uncheckedCast(Object object) {
    return (T) object;
  }
}
