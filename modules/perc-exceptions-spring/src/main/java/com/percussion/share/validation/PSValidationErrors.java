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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * A data object that represents validation errors.
 *
 * <p>The object some what mirrors the Spring validation framework.
 *
 * <p>The object is safe serialize with JAXB.
 *
 * @author adamgent
 */
@XmlRootElement(name = "ValidationErrors")
public class PSValidationErrors extends PSErrors {

  private List<PSFieldError> fieldErrors = new ArrayList<>();
  private List<PSObjectError> globalErrors = new ArrayList<>();
  private String methodName;

  /** Default no-arg constructor required for JAXB serialization. */
  public PSValidationErrors() {
    super();
  }

  /**
   * Indicates whether this object carries any field or global errors.
   *
   * @return {@code true} when at least one field or global error is recorded.
   */
  public boolean hasErrors() {
    return (!(globalErrors.isEmpty() && fieldErrors.isEmpty()));
  }

  /**
   * Returns the name of the method that produced the validation errors.
   *
   * @return the method name, may be {@code null}.
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Sets the name of the method that produced the validation errors.
   *
   * @param methodName the method name, may be {@code null}.
   */
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  /**
   * Returns the list of field errors recorded for the validated object.
   *
   * @return the field errors list, never {@code null}.
   */
  public List<PSFieldError> getFieldErrors() {
    return fieldErrors;
  }

  /**
   * Replaces the list of field errors.
   *
   * @param fieldErrors the new field errors list, may be {@code null}.
   */
  public void setFieldErrors(List<PSFieldError> fieldErrors) {
    this.fieldErrors = fieldErrors;
  }

  /**
   * {@inheritDoc}
   *
   * @return the first global error, or {@code null} when no global errors are recorded.
   */
  @Override
  public PSObjectError getGlobalError() {
    if (getGlobalErrors() == null || getGlobalErrors().isEmpty()) return null;
    return getGlobalErrors().get(0);
  }

  /**
   * {@inheritDoc}
   *
   * @param globalError the global error to insert at the head of the global errors list, may be
   *     {@code null}.
   */
  @Override
  public void setGlobalError(PSObjectError globalError) {
    getGlobalErrors().add(0, globalError);
  }

  /**
   * Returns the list of global errors recorded for the validated object.
   *
   * @return the global errors list, never {@code null}.
   */
  public List<PSObjectError> getGlobalErrors() {
    return globalErrors;
  }

  /**
   * Replaces the list of global errors.
   *
   * @param objectErrors the new global errors list, may be {@code null}.
   */
  public void setGlobalErrors(List<PSObjectError> objectErrors) {
    this.globalErrors = objectErrors;
  }

  /**
   * JAXB-serializable representation of a single field error.
   *
   * @author adamgent
   */
  public static class PSFieldError extends PSObjectError {
    /** Default constructor required for JAXB serialization. */
    public PSFieldError() {
      super();
    }

    private String field;
    private Object rejectedValue;
    private boolean bindingFailure;

    /**
     * Returns the name of the field that failed validation.
     *
     * @return the field name, may be {@code null}.
     */
    public String getField() {
      return field;
    }

    /**
     * Sets the name of the field that failed validation.
     *
     * @param field the field name, may be {@code null}.
     */
    public void setField(String field) {
      this.field = field;
    }

    /**
     * Returns the value that was rejected during validation.
     *
     * @return the rejected value, may be {@code null}.
     */
    public Object getRejectedValue() {
      return rejectedValue;
    }

    /**
     * Sets the value that was rejected during validation.
     *
     * @param rejectedValue the rejected value, may be {@code null}.
     */
    public void setRejectedValue(Object rejectedValue) {
      this.rejectedValue = rejectedValue;
    }

    /**
     * Indicates whether this error is the result of a binding failure (such as a type conversion
     * error) rather than a constraint violation.
     *
     * @return {@code true} when this is a binding failure.
     */
    public boolean isBindingFailure() {
      return bindingFailure;
    }

    /**
     * Marks this error as a binding failure or a regular validation failure.
     *
     * @param bindingFailure {@code true} to mark this as a binding failure.
     */
    public void setBindingFailure(boolean bindingFailure) {
      this.bindingFailure = bindingFailure;
    }

    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer("PSFieldError{");
      sb.append("field='").append(field).append('\'');
      sb.append(", rejectedValue=").append(rejectedValue);
      sb.append(", bindingFailure=").append(bindingFailure);
      sb.append('}');
      return sb.toString();
    }
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("PSValidationErrors{");
    sb.append("fieldErrors=").append(fieldErrors);
    sb.append(", globalErrors=").append(globalErrors);
    sb.append(", methodName='").append(methodName).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
