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
<<<<<<< HEAD
=======
import org.apache.commons.lang3.Validate;
>>>>>>> development-8.1.x
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * An adapter to validate property like objects.
 *
 * @author adamgent
 * @param <PROPERTIES> property like object.
 */
public abstract class PSAbstractPropertiesValidator<PROPERTIES> implements Validator {

  protected abstract Class<PROPERTIES> getType();

  public PSPropertiesValidationException validate(PROPERTIES obj) {
    PSPropertiesValidationException e =
        new PSPropertiesValidationException(obj, obj.getClass().getCanonicalName());
    validate(obj, e);
    return e;
  }

  @SuppressWarnings("unchecked")
<<<<<<< HEAD
  public boolean supports(Class<?> klass) {
    notNull(getType(), "getType() cannot return null");
=======
  public boolean supports(Class klass) {
    Validate.notNull(getType());
>>>>>>> development-8.1.x
    if (klass == getType()) return true;
    return false;
  }

  protected abstract void doValidation(PROPERTIES properties, PSPropertiesValidationException e);

  @SuppressWarnings("unchecked")
  public void validate(Object properties, Errors errors) {
    doValidation((PROPERTIES) properties, (PSPropertiesValidationException) errors);
  }
}
