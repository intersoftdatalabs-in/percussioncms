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

import org.springframework.validation.BeanPropertyBindingResult;

/**
 * Used to validate Java Bean data objects (aka POJOs with out behavior).
 *
 * @author adamgent
 */
@SuppressWarnings("this-escape")
public class PSBeanValidationException extends PSSpringValidationException {

  private static final long serialVersionUID = 8097878230304938879L;

  /**
   * Constructs a bean validation exception that wraps the given cause.
   *
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSBeanValidationException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a bean validation exception for the given target and method.
   *
   * @param target the bean being validated, may be {@code null}.
   * @param methodName the canonical name used to identify the bean in the resulting binding result,
   *     never {@code null}.
   */
  public PSBeanValidationException(Object target, String methodName) {
    super(methodName);
    init(target, methodName);
  }

  /**
   * Constructs a bean validation exception with a custom message and cause.
   *
   * @param target the bean being validated, may be {@code null}.
   * @param methodName the canonical name used to identify the bean in the resulting binding result,
   *     never {@code null}.
   * @param message the detail message, may be {@code null}.
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSBeanValidationException(
      Object target, String methodName, String message, Throwable cause) {
    super(message, cause);
    init(target, methodName);
  }

  /**
   * Initializes this exception with a Spring {@link BeanPropertyBindingResult} for the supplied
   * target.
   *
   * @param target the bean being validated, may be {@code null}.
   * @param objectName the name to associate with the binding result, never {@code null}.
   */
  protected void init(Object target, String objectName) {
    setSpringValidationErrors(new BeanPropertyBindingResult(target, objectName));
  }
}
