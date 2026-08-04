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

import com.percussion.share.validation.PSErrors.PSObjectError;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.validation.PSValidationErrors.PSFieldError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * An adapter to Spring Validation Framework.
 *
 * @author adamgent
 */
public abstract class PSSpringValidationException extends PSValidationException
    implements Errors, IPSValidationException {

  private static final long serialVersionUID = 1L;

  /** The wrapped Spring {@link Errors} container; may be {@code null} when not configured. */
  private Errors springValidationErrors;

  /**
   * Constructs a Spring validation exception that wraps the given cause.
   *
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSSpringValidationException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a Spring validation exception with the given message.
   *
   * @param message the detail message, may be {@code null}.
   */
  public PSSpringValidationException(String message) {
    super(message);
  }

  /**
   * Constructs a Spring validation exception with the given message and cause.
   *
   * @param message the detail message, may be {@code null}.
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSSpringValidationException(String message, Throwable cause) {
    super(message, cause);
    reject(cause, message);
  }

  /**
   * {@inheritDoc}
   *
   * @return this exception for fluent chaining, never {@code null}.
   * @throws PSSpringValidationException always thrown when this exception is currently in an
   *     invalid state.
   */
  @Override
  public PSSpringValidationException throwIfInvalid() throws PSSpringValidationException {
    if (hasErrors()) throw this;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @return a fresh {@link PSValidationErrors} view of the wrapped Spring errors, never {@code
   *     null}.
   */
  @Override
  public PSValidationErrors getValidationErrors() {
    PSValidationErrors ve = new PSValidationErrors();
    convert(ve, getSpringValidationErrors());
    return ve;
  }

  /**
   * Returns the underlying Spring {@link Errors} container that this exception adapts.
   *
   * @return the Spring errors container, may be {@code null} when none has been configured.
   */
  protected Errors getSpringValidationErrors() {
    return springValidationErrors;
  }

  /**
   * Replaces the underlying Spring {@link Errors} container.
   *
   * @param validationErrors the new Spring errors container, may be {@code null}.
   */
  protected void setSpringValidationErrors(Errors validationErrors) {
    this.springValidationErrors = validationErrors;
  }

  /**
   * Rejects the given exception by recording a global error whose code is the exception's canonical
   * class name.
   *
   * @param exception the throwable to reject, never {@code null}.
   * @param defaultMessage the default message to associate with the rejection, may be {@code null}.
   */
  public void reject(Throwable exception, String defaultMessage) {
    reject(exception.getClass().getCanonicalName(), defaultMessage);
  }

  /**
   * Copies Spring {@link Errors} content into the supplied {@link PSValidationErrors} container.
   *
   * @param v the destination container, never {@code null}.
   * @param errors the source Spring errors, never {@code null}.
   */
  protected void convert(PSValidationErrors v, org.springframework.validation.Errors errors) {
    v.setMethodName(errors.getObjectName());
    List<org.springframework.validation.FieldError> fes = errors.getFieldErrors();
    List<org.springframework.validation.ObjectError> oes = errors.getGlobalErrors();
    for (org.springframework.validation.FieldError fe : fes) {
      v.getFieldErrors().add(convert(fe));
    }
    for (org.springframework.validation.ObjectError oe : oes) {
      v.getGlobalErrors().add(convert(oe));
    }
  }

  /**
   * Converts a Spring {@link ObjectError} into a {@link PSObjectError}.
   *
   * @param oe the Spring object error, never {@code null}.
   * @return the converted {@link PSObjectError}, never {@code null}.
   */
  protected PSObjectError convert(org.springframework.validation.ObjectError oe) {
    PSObjectError oew = new PSObjectError();
    convert(oe, oew);
    return oew;
  }

  /**
   * Copies the relevant fields of a Spring {@link ObjectError} into the supplied {@link
   * PSObjectError}.
   *
   * @param oe the source Spring object error, never {@code null}.
   * @param oew the destination, never {@code null}.
   */
  protected void convert(org.springframework.validation.ObjectError oe, PSObjectError oew) {
    oew.setCode(oe.getCode());
    oew.setDefaultMessage(oe.getDefaultMessage());
    oew.setArguments(args(oe.getArguments()));
  }

  private List<String> args(Object[] args) {
    List<String> rvalue = new ArrayList<>();
    if (args == null) return rvalue;
    for (Object o : args) {
      rvalue.add("" + o);
    }
    return rvalue;
  }

  /**
   * Converts a Spring {@link FieldError} into a {@link PSFieldError}.
   *
   * @param oe the Spring field error, never {@code null}.
   * @return the converted {@link PSFieldError}, never {@code null}.
   */
  protected PSFieldError convert(org.springframework.validation.FieldError oe) {
    PSFieldError oew = new PSFieldError();
    convert(oe, oew);
    oew.setField(oe.getField());
    return oew;
  }

  // Delegate methods.

  @Override
  public String toString() {
    return super.toString() + springValidationErrors.toString();
  }

  /**
   * Adds all errors from the supplied instance into the wrapped Spring container.
   *
   * @param arg0 the source errors, never {@code null}.
   */
  public void addAllErrors(Errors arg0) {
    springValidationErrors.addAllErrors(arg0);
  }

  /**
   * Returns all errors (field and global) from the wrapped Spring container.
   *
   * @return the list of all errors, never {@code null}.
   */
  public List<ObjectError> getAllErrors() {
    return springValidationErrors.getAllErrors();
  }

  /**
   * Returns the total number of errors (field and global) recorded by the wrapped Spring container.
   *
   * @return the total error count.
   */
  public int getErrorCount() {
    return springValidationErrors.getErrorCount();
  }

  /**
   * Returns the first field error recorded by the wrapped Spring container, or {@code null} when
   * there are none.
   *
   * @return the first field error, may be {@code null}.
   */
  public FieldError getFieldError() {
    return springValidationErrors.getFieldError();
  }

  /**
   * Returns the first field error recorded for the supplied field, or {@code null} when there are
   * none.
   *
   * @param arg0 the field name, may be {@code null}.
   * @return the first field error for the field, may be {@code null}.
   */
  public FieldError getFieldError(String arg0) {
    return springValidationErrors.getFieldError(arg0);
  }

  /**
   * Returns the number of field errors recorded by the wrapped Spring container.
   *
   * @return the field error count.
   */
  public int getFieldErrorCount() {
    return springValidationErrors.getFieldErrorCount();
  }

  /**
   * Returns the number of field errors recorded for the supplied field.
   *
   * @param arg0 the field name, may be {@code null}.
   * @return the field error count for the field.
   */
  public int getFieldErrorCount(String arg0) {
    return springValidationErrors.getFieldErrorCount(arg0);
  }

  /**
   * Returns all field errors recorded by the wrapped Spring container.
   *
   * @return the list of field errors, never {@code null}.
   */
  public List<FieldError> getFieldErrors() {
    return springValidationErrors.getFieldErrors();
  }

  /**
   * Returns all field errors recorded for the supplied field.
   *
   * @param arg0 the field name, may be {@code null}.
   * @return the list of field errors for the field, never {@code null}.
   */
  public List<FieldError> getFieldErrors(String arg0) {
    return springValidationErrors.getFieldErrors(arg0);
  }

  /**
   * Returns the type of the supplied field as known to the wrapped Spring container.
   *
   * @param arg0 the field name, never {@code null}.
   * @return the field type, may be {@code null} when the field is unknown.
   */
  public Class<?> getFieldType(String arg0) {
    return springValidationErrors.getFieldType(arg0);
  }

  /**
   * Returns the value of the supplied field on the target object as known to the wrapped Spring
   * container.
   *
   * @param arg0 the field name, never {@code null}.
   * @return the field value, may be {@code null}.
   */
  public Object getFieldValue(String arg0) {
    return springValidationErrors.getFieldValue(arg0);
  }

  /**
   * Returns the first global error recorded by the wrapped Spring container, or {@code null} when
   * there are none.
   *
   * @return the first global error, may be {@code null}.
   */
  public ObjectError getGlobalError() {
    return springValidationErrors.getGlobalError();
  }

  /**
   * Returns the number of global errors recorded by the wrapped Spring container.
   *
   * @return the global error count.
   */
  public int getGlobalErrorCount() {
    return springValidationErrors.getGlobalErrorCount();
  }

  /**
   * Returns all global errors recorded by the wrapped Spring container.
   *
   * @return the list of global errors, never {@code null}.
   */
  public List<ObjectError> getGlobalErrors() {
    return springValidationErrors.getGlobalErrors();
  }

  /**
   * Returns the current nested path used by the wrapped Spring container.
   *
   * @return the nested path, never {@code null}.
   */
  public String getNestedPath() {
    return springValidationErrors.getNestedPath();
  }

  /**
   * Returns the object name associated with the wrapped Spring container.
   *
   * @return the object name, never {@code null}.
   */
  public String getObjectName() {
    return springValidationErrors.getObjectName();
  }

  /**
   * Indicates whether the wrapped Spring container has any field or global errors.
   *
   * @return {@code true} when at least one error is recorded.
   */
  public boolean hasErrors() {
    return springValidationErrors.hasErrors();
  }

  /**
   * Indicates whether the wrapped Spring container has any field errors.
   *
   * @return {@code true} when at least one field error is recorded.
   */
  public boolean hasFieldErrors() {
    return springValidationErrors.hasFieldErrors();
  }

  /**
   * Indicates whether the wrapped Spring container has any field errors for the supplied field.
   *
   * @param arg0 the field name, may be {@code null}.
   * @return {@code true} when at least one field error is recorded for the field.
   */
  public boolean hasFieldErrors(String arg0) {
    return springValidationErrors.hasFieldErrors(arg0);
  }

  /**
   * Indicates whether the wrapped Spring container has any global errors.
   *
   * @return {@code true} when at least one global error is recorded.
   */
  public boolean hasGlobalErrors() {
    return springValidationErrors.hasGlobalErrors();
  }

  /**
   * Pops the most recently pushed nested path from the wrapped Spring container.
   *
   * @throws IllegalStateException if there is no nested path to pop.
   */
  public void popNestedPath() throws IllegalStateException {
    springValidationErrors.popNestedPath();
  }

  /**
   * Pushes the supplied sub path onto the nested path of the wrapped Spring container.
   *
   * @param arg0 the sub path to push, never {@code null}.
   */
  public void pushNestedPath(String arg0) {
    springValidationErrors.pushNestedPath(arg0);
  }

  /**
   * Rejects the supplied error code with the given message-format arguments and default message on
   * the wrapped Spring container.
   *
   * @param errorCode the error code, may be {@code null}.
   * @param errorArgs the message-format arguments, may be {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   */
  public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
    springValidationErrors.reject(errorCode, errorArgs, defaultMessage);
  }

  /**
   * Rejects the supplied error code with the given default message on the wrapped Spring container.
   *
   * @param errorCode the error code, may be {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   */
  public void reject(String errorCode, String defaultMessage) {
    springValidationErrors.reject(errorCode, defaultMessage);
  }

  /**
   * Rejects the supplied error code on the wrapped Spring container.
   *
   * @param errorCode the error code, may be {@code null}.
   */
  public void reject(String errorCode) {
    springValidationErrors.reject(errorCode);
  }

  /**
   * Rejects the supplied value for the given field with the supplied error code, arguments, and
   * default message.
   *
   * @param field the field name, may be {@code null}.
   * @param errorCode the error code, may be {@code null}.
   * @param errorArgs the message-format arguments, may be {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   */
  public void rejectValue(
      String field, String errorCode, Object[] errorArgs, String defaultMessage) {
    springValidationErrors.rejectValue(field, errorCode, errorArgs, defaultMessage);
  }

  /**
   * Rejects the supplied value for the given field with the supplied error code and default
   * message.
   *
   * @param field the field name, may be {@code null}.
   * @param errorCode the error code, may be {@code null}.
   * @param defaultMessage the default message, may be {@code null}.
   */
  public void rejectValue(String field, String errorCode, String defaultMessage) {
    springValidationErrors.rejectValue(field, errorCode, defaultMessage);
  }

  /**
   * Rejects the supplied value for the given field with the supplied error code.
   *
   * @param field the field name, may be {@code null}.
   * @param errorCode the error code, may be {@code null}.
   */
  public void rejectValue(String field, String errorCode) {
    springValidationErrors.rejectValue(field, errorCode);
  }

  /**
   * Sets the nested path of the wrapped Spring container.
   *
   * @param arg0 the nested path, may be {@code null}.
   */
  public void setNestedPath(String arg0) {
    springValidationErrors.setNestedPath(arg0);
  }
}
