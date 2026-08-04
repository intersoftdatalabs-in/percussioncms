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

import static org.apache.commons.lang3.Validate.*;

import com.percussion.share.service.exception.PSPropertiesValidationException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Adapter that validates property-like objects (such as {@link java.util.Map}s or {@link
 * java.util.Properties}) by exposing a Spring {@link Validator} facade and producing {@link
 * PSPropertiesValidationException} instances.
 *
 * @author adamgent
 * @param <PROPERTIES> property like object.
 */
public abstract class PSAbstractPropertiesValidator<PROPERTIES> implements Validator {

  /**
   * Default constructor required for proxy-based instantiation frameworks; subclasses provide the
   * type-specific behavior via {@link #getType()}.
   */
  protected PSAbstractPropertiesValidator() {
    super();
  }

  /**
   * Returns the concrete class that this validator supports. Subclasses must supply a non-null
   * value.
   *
   * @return the property class supported by this validator, never {@code null}.
   */
  protected abstract Class<PROPERTIES> getType();

  /**
   * Validates the given properties object and returns the resulting exception container.
   *
   * @param obj the properties object to validate, never {@code null}.
   * @return a {@link PSPropertiesValidationException} that aggregates any validation errors, never
   *     {@code null}.
   */
  public PSPropertiesValidationException validate(PROPERTIES obj) {
    PSPropertiesValidationException e =
        new PSPropertiesValidationException(obj, obj.getClass().getCanonicalName());
    validate(obj, e);
    return e;
  }

  /**
   * Indicates whether this validator supports the given class.
   *
   * @param klass the candidate class, may be {@code null}.
   * @return {@code true} only when {@code klass} equals the type returned by {@link #getType()}.
   */
  public boolean supports(Class<?> klass) {
    notNull(getType(), "getType() cannot return null");
    if (klass == getType()) return true;
    return false;
  }

  /**
   * Template method that subclasses override to perform property-specific validation.
   *
   * @param properties the property-like object being validated, never {@code null}.
   * @param e the exception container into which validation errors should be recorded, never {@code
   *     null}.
   */
  protected abstract void doValidation(PROPERTIES properties, PSPropertiesValidationException e);

  /**
   * Spring {@link Validator} entry point that forwards to {@link #doValidation} after the supplied
   * {@code errors} container has been cast to {@link PSPropertiesValidationException}.
   *
   * @param properties the property-like object to validate, expected to be of type {@code
   *     PROPERTIES}.
   * @param errors the {@link PSPropertiesValidationException} container to populate, never {@code
   *     null}.
   */
  public void validate(Object properties, Errors errors) {
    doValidation((PROPERTIES) properties, (PSPropertiesValidationException) errors);
  }
}
