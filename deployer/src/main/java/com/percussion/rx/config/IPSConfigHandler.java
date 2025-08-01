// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.rx.design.IPSDesignModel;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;

import java.util.List;
import java.util.Map;

/**
 * Configure handler represents a bean from configure definition file instantiated by Spring.
 * Responsible for applying properties on design objects through property setters.
 *
 * @author bjoginipally
 */
public interface IPSConfigHandler {

  /**
   * The state of a design object.
   */
  enum ObjectState {
    /** The design object is defined in current configuration only. */
    CURRENT,
    /** The design object is defined in previous configuration only. */
    PREVIOUS,
    /** The design object is defined in both current and previous configurations. */
    BOTH
  }

  /**
   * Processes the properties for the design object.
   *
   * @param obj the (single) design object, may be {@code null} if "type" and "name"/"names" are not provided.
   * @param state the state of the specified design object, may be {@code null} if obj is {@code null}.
   * @param associationSets list of association sets, may be {@code null}.
   * @return {@code true} if the design object has been modified.
   */
  boolean process(Object obj, ObjectState state, List<IPSAssociationSet> associationSets);

  /**
   * De-configures the properties previously applied. Called during uninstall.
   *
   * @param obj the (single) design object, may be {@code null} if "type" and "name"/"names" are not provided.
   * @param associationSets list of association sets, may be {@code null}.
   * @return {@code true} if the design object has been modified.
   */
  boolean unprocess(Object obj, List<IPSAssociationSet> associationSets);

  /**
   * Returns the property setters of the handler, may be {@code null} or empty.
   *
   * @return list of property setters.
   */
  List<IPSPropertySetter> getPropertySetters();

  /**
   * Sets property setters for this handler.
   *
   * @param setters property setters.
   */
  void setPropertySetters(List<IPSPropertySetter> setters);

  /**
   * Gets the type enum of the design object, may be {@code null} if not provided.
   *
   * @return the type enum, may be {@code null}.
   */
  PSTypeEnum getType();

  /**
   * Gets the name of the design object.
   *
   * @return the name, may be {@code null} if not defined.
   */
  String getName();

  /**
   * Sets the name of the design object.
   *
   * @param name the name, may not be {@code null} or empty.
   */
  void setName(String name);

  /**
   * Gets the Design Objects loaded, created, or found from the cache.
   * Must maintain the cache for loaded/created objects.
   *
   * @param cachedObjs the cached Design Objects, maps name to object.
   * @return the Design Objects (with their state), never {@code null}, may be empty.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  List<PSPair<Object, ObjectState>> getDesignObjects(Map<String, Object> cachedObjs) throws PSNotFoundException;

  /**
   * Gets the Design Object names along with their related state.
   *
   * @return the list of name/state pairs, never {@code null}, may be empty.
   */
  List<PSPair<String, ObjectState>> getObjectNames();

  /**
   * Determines if the handler provides the configured Design Objects.
   *
   * @return {@code true} if the Design Objects will be provided by the handler.
   */
  boolean isGetDesignObjects();

  /**
   * Returns additional properties specific for this handler.
   *
   * @return the additional properties, never {@code null}, may be empty.
   */
  Map<String, Object> getExtraProperties();

  /**
   * Sets the handler-specific properties.
   *
   * @param props the handler-specific properties, never {@code null}, may be empty.
   */
  void setExtraProperties(Map<String, Object> props);

  /**
   * Gets the extra properties used in previous configuration.
   *
   * @return the previous properties, may be {@code null} or empty.
   */
  Map<String, Object> getPrevExtraProperties();

  /**
   * Sets the extra properties used in previous configuration.
   *
   * @param props the extra properties, may be {@code null} or empty.
   */
  void setPrevExtraProperties(Map<String, Object> props);

  /**
   * Saves the processed result.
   *
   * @param model the model of the design object, never {@code null}.
   * @param obj the design object processed, never {@code null}.
   * @param state the state of the specified design object, may be {@code null} if obj is {@code null}.
   * @param assocList the associations processed, may be {@code null} or empty.
   * @return the guid of the updated object.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  IPSGuid saveResult(IPSDesignModel model, Object obj, ObjectState state, List<IPSAssociationSet> assocList)
      throws PSNotFoundException;

  /**
   * Validates the design objects specified in current config against another handler.
   *
   * @param other the handler to validate against, not {@code null}.
   * @return a list of validation results, may be empty.
   */
  List<PSConfigValidation> validate(IPSConfigHandler other);

  /**
   * Returns the property defs of all the setters the handler consists of.
   *
   * @param obj the design object from which the values of properties are obtained, may be {@code null}.
   * @return a map of replacement name of the property and the value, never {@code null}, may be empty.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  Map<String, Object> getPropertyDefs(Object obj) throws PSNotFoundException;

  /**
   * Gets the first available design object loaded, created, or found from the cache.
   *
   * @param cachedObjs the cached Design Objects, maps name to object.
   * @return the Design Object, may be {@code null} if not found.
   * @throws PSNotFoundException if a referenced object is not found.
   */
  Object getDefaultDesignObject(Map<String, Object> cachedObjs) throws PSNotFoundException;
}
