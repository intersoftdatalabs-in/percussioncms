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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.validation.MapBindingResult;

/**
 * Used to validate property objects like {@link Map} and {@link Properties}.
 *
 * @author adamgent
 */
@SuppressWarnings({"serial", "this-escape"})
public class PSPropertiesValidationException extends PSSpringValidationException {

  private static final long serialVersionUID = 1L;

  /** The property map that backs the binding result; never {@code null}. */
  private Map<String, Object> properties = new HashMap<>();

  /**
   * Constructs a properties validation exception for the given target and method.
   *
   * @param target the property-like object being validated, may be {@code null}.
   * @param methodName the name to associate with the resulting binding result, never {@code null}.
   */
  public PSPropertiesValidationException(Object target, String methodName) {
    super(methodName);
    init(target, methodName);
    setProperties(getProperties());
  }

  /**
   * Constructs a properties validation exception with a custom message and cause.
   *
   * @param target the property-like object being validated, may be {@code null}.
   * @param methodName the name to associate with the resulting binding result, never {@code null}.
   * @param message the detail message, may be {@code null}.
   * @param cause the underlying cause, may be {@code null}.
   */
  public PSPropertiesValidationException(
      Object target, String methodName, String message, Throwable cause) {
    super(message, cause);
    init(target, methodName);
    setProperties(getProperties());
  }

  /**
   * Initializes the binding result for the given target using its runtime type.
   *
   * @param target the property-like object being validated, may be {@code null}.
   * @param objectName the name to associate with the resulting binding result, never {@code null}.
   */
  protected void init(Object target, String objectName) {
    init(getProperties(), objectName);
  }

  /**
   * Initializes the binding result with the supplied property map.
   *
   * @param properties the property map to validate, never {@code null}.
   * @param objectName the name to associate with the resulting binding result, never {@code null}.
   */
  protected void init(Map<String, Object> properties, String objectName) {
    MapBindingResult mbr = new MapBindingResult(properties, objectName);
    setSpringValidationErrors(mbr);
  }

  /**
   * Returns the underlying property map that backs the binding result.
   *
   * @return the property map, never {@code null}.
   */
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
   * Replaces the underlying property map and rebuilds the binding result.
   *
   * @param parameters the new property map, never {@code null}.
   */
  public void setProperties(Map<String, Object> parameters) {
    this.properties = parameters;
    init(parameters, getObjectName());
  }
}
