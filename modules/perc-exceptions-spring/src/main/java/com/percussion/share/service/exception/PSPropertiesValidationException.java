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
 * <p>The property map field is a concrete {@link HashMap} so the serializable exception hierarchy
 * satisfies {@code -Xlint:serial} (interface {@link Map} is not {@link java.io.Serializable}).
 * Constructors install a {@link MapBindingResult} via the parent constructor (direct field write;
 * no {@code this-escape}). This class is {@code final} so constructors cannot be observed by a
 * partially initialized subclass.
 *
 * @author adamgent
 */
public final class PSPropertiesValidationException extends PSSpringValidationException {

  private static final long serialVersionUID = 1L;

  /** The property map that backs the binding result; never {@code null}. */
  private final HashMap<String, Object> properties;

  /**
   * Constructs a properties validation exception for the given target and method.
   *
   * @param target the property-like object being validated, may be {@code null}.
   * @param methodName the name to associate with the resulting binding result, never {@code null}.
   */
  public PSPropertiesValidationException(Object target, String methodName) {
    this(new HashMap<>(), methodName, methodName, null, false);
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
    this(new HashMap<>(), methodName, message, cause, true);
  }

  /**
   * Primary constructor: owns the property map and installs a {@link MapBindingResult} through the
   * parent so no instance methods run on a partially initialized subclass.
   */
  private PSPropertiesValidationException(
      HashMap<String, Object> properties,
      String methodName,
      String message,
      Throwable cause,
      boolean rejectCause) {
    super(message, cause, new MapBindingResult(properties, methodName), rejectCause);
    this.properties = properties;
  }

  /**
   * Initializes the binding result for the given target using its runtime type.
   *
   * @param target the property-like object being validated, may be {@code null}.
   * @param objectName the name to associate with the resulting binding result, never {@code null}.
   */
  public void init(Object target, String objectName) {
    init(getProperties(), objectName);
  }

  /**
   * Initializes the binding result with the supplied property map.
   *
   * @param properties the property map to validate, never {@code null}.
   * @param objectName the name to associate with the resulting binding result, never {@code null}.
   */
  public void init(Map<String, Object> properties, String objectName) {
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
   * Replaces the contents of the underlying property map and rebuilds the binding result.
   *
   * <p>Clears and repopulates the existing {@link HashMap} so the field remains the same concrete
   * serializable instance under {@code -Xlint:serial}.
   *
   * @param parameters the new property map, never {@code null}.
   */
  public void setProperties(Map<String, Object> parameters) {
    this.properties.clear();
    this.properties.putAll(parameters);
    init(this.properties, getObjectName());
  }
}
