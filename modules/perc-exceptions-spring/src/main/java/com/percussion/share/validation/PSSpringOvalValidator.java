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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.FieldContext;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.ValidationFailedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Implements spring's validator. Supports validating nested complex properties, which should be
 * marked with {@link PSValidateNestedProperty}.
 *
 * @author SergeyZ
 */
public class PSSpringOvalValidator implements Validator, InitializingBean {

  private net.sf.oval.Validator validator;

  /**
   * Constructs a validator that delegates to the supplied OVal validator.
   *
   * @param validator the OVal validator to delegate to, never {@code null}.
   */
  public PSSpringOvalValidator(net.sf.oval.Validator validator) {
    this.validator = validator;
  }

  /**
   * Indicates whether this validator supports the given class. This implementation returns {@code
   * true} for all classes because OVal itself is consulted at validation time.
   *
   * @param clazz the candidate class, never {@code null}.
   * @return always {@code true}.
   */
  public boolean supports(Class<?> clazz) {
    return true;
  }

  /**
   * Validates the given target, writing any constraint violations and nested-property errors into
   * the supplied {@link Errors} container.
   *
   * @param target the object to validate, may be {@code null} only when OVal accepts it.
   * @param errors the container that receives any validation errors, never {@code null}.
   */
  public void validate(Object target, Errors errors) {
    doValidate(target, errors, "");
  }

  private void doValidate(Object target, Errors errors, String fieldPrefix) {
    try {
      for (ConstraintViolation violation : validator.validate(target)) {

        // Note: getContext() is deprecated, but we need it for backward compatibility
        @SuppressWarnings("deprecation")
        OValContext ctx = violation.getContext();
        String errorCode = violation.getErrorCode();
        String errorMessage = violation.getMessage();

        if (ctx instanceof FieldContext) {
          String fieldName = fieldPrefix + ((FieldContext) ctx).getField().getName();
          errors.rejectValue(fieldName, errorCode, errorMessage);
        } else {
          errors.reject(errorCode, errorMessage);
        }
      }

      try {
        Field[] fields = getFields(target);
        for (Field field : fields) {
          String name = field.getName();
          PSValidateNestedProperty validate = field.getAnnotation(PSValidateNestedProperty.class);
          if (validate != null) {
            // Use trySetAccessible() instead of deprecated isAccessible()
            if (!field.canAccess(target)) {
              field.setAccessible(true);
            }
            Object nestedProperty = field.get(target);
            if (nestedProperty != null) {
              doValidate(nestedProperty, errors, name + ".");
            }
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    } catch (final ValidationFailedException ex) {
      errors.reject(ex.getMessage());
    }
  }

  private Field[] getFields(Object target) {
    Class<?> clazz = target.getClass();
    List<Field> fields = doGetFields(clazz);
    return fields.toArray(new Field[fields.size()]);
  }

  private List<Field> doGetFields(Class<?> clazz) {
    ArrayList<Field> list = new ArrayList<>();
    Field[] fields = clazz.getDeclaredFields();
    list.addAll(Arrays.asList(fields));
    if (clazz.getSuperclass() != null) {
      list.addAll(doGetFields(clazz.getSuperclass()));
    }
    return list;
  }

  /**
   * {@inheritDoc}
   *
   * @throws Exception if the underlying OVal validator has not been configured.
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(validator, "Property [validator] is not set");
  }

  /**
   * Returns the OVal validator that backs this Spring adapter.
   *
   * @return the OVal validator, may be {@code null} until {@link #setValidator} is called.
   */
  public net.sf.oval.Validator getValidator() {
    return validator;
  }

  /**
   * Sets the OVal validator that backs this Spring adapter.
   *
   * @param validator the OVal validator to delegate to, never {@code null}.
   */
  public void setValidator(net.sf.oval.Validator validator) {
    this.validator = validator;
  }
}
