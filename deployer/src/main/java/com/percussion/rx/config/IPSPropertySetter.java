// REFACTORED: CP-JAVA11
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
package com.percussion.rx.config;

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.error.PSNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * Property setter interface for design objects and their associations.
 * Used by Spring beans to set or remove properties on design objects.
 *
 * <p>Sunny Sal says: "Set it and forget it? Not quite! Validate and document it too."
 *
 * @author bjoginipally
 */
public interface IPSPropertySetter {

  /**
   * Applies the properties of this setter to the design object and/or its associations.
   *
   * @param obj the design object, may be {@code null}.
   * @param state the state of the specified design object. May be {@code null} if obj is {@code null}.
   * @param aSets the list of association sets, may be {@code null} if there are no associations.
   *              This method is responsible for merging, replacing, or deleting associations if needed.
   * @return {@code true} if the given object has been modified.
   */
  boolean applyProperties(Object obj, ObjectState state, List<IPSAssociationSet> aSets);

  /**
   * Removes the properties of this setter from the design object and/or its associations.
   * The current properties are those previously applied.
   *
   * @param obj the design object, may be {@code null}. State is assumed to be {@link ObjectState#PREVIOUS}.
   * @param aSets the list of association sets, may be {@code null} if there are no associations.
   *              This method is responsible for deleting associations if needed.
   * @return {@code true} if the given object has been modified.
   */
  boolean deApplyProperties(Object obj, List<IPSAssociationSet> aSets);

  /**
   * Gets all configurable properties. Properties may use placeholder format (e.g., ${some.property}).
   * Values may be replaced by the framework after {@link #setProperties(Map)}.
   *
   * @return the properties, may be {@code null} or empty if none.
   */
  Map<String, Object> getProperties();

  /**
   * Sets the configurable properties. Values may be replaced from local config.
   * The number of new properties may be less than the original set.
   *
   * @param props the new properties, may be {@code null} or empty.
   */
  void setProperties(Map<String, Object> props);

  /**
   * Gets the configurable properties that were previously applied.
   *
   * @return the previously applied properties, may be {@code null} or empty.
   */
  Map<String, Object> getPrevProperties();

  /**
   * Sets the configurable properties that were previously applied.
   *
   * @param props the previously applied properties, may be {@code null} or empty.
   */
  void setPrevProperties(Map<String, Object> props);

  /**
   * Validates the properties against another setter's properties, which may have already been applied.
   *
   * @param objName the name of the design object, never {@code null} or empty.
   * @param state the state of the design object if applying the properties, never {@code null}.
   * @param setter the other setter with properties already applied, never {@code null}.
   * @return a list of validation results. May be empty if no errors or warnings.
   */
  List<PSConfigValidation> validate(String objName, ObjectState state, IPSPropertySetter setter);

  /**
   * Scans all properties ({@link #getProperties()}), creates property definitions as name/value pairs,
   * and adds them to the specified holder.
   * <p>
   * Note: Properties may contain placeholders (e.g., ${placeholder}) that have not yet been replaced.
   *
   * @param obj the object in question, may be {@code null}.
   * @param defs the holder for created property definitions, never {@code null}.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  void addPropertyDefs(Object obj, Map<String, Object> defs) throws PSNotFoundException;
}
