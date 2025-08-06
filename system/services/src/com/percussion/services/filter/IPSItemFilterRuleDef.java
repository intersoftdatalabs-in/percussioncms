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
package com.percussion.services.filter;

import com.percussion.utils.guid.IPSGuid;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A rule definition associates a specific filter rule (stored by the extensions
 * manager) with a set of parameters to be used with it. Rules are used
 * to remove items that do not meet one or more criteria, and to adjust
 * what revisions of items are used.
 * 
 * @see IPSItemFilterRule
 * @author dougrand
 */
public interface IPSItemFilterRuleDef extends Comparable<IPSItemFilterRuleDef> {

   /**
    * Get the GUID representation of this item filter rule definition.
    * @return the GUID, never {@code null}
    */
   IPSGuid getGUID();

   /**
    * Set the GUID representation of the rule definition.
    * @param newguid the new GUID, never {@code null}
    * @throws IllegalArgumentException if newguid is null
    */
   void setGUID(IPSGuid newguid);

   /**
    * Get the filter rule associated with this definition.
    *
    * @return the rule, never {@code null}
    * @throws PSFilterException if the rule can't be found
    */
   IPSItemFilterRule getRule() throws PSFilterException;

   /**
    * Get the rule name associated with this definition.
    *
    * @return the name, never {@code null}
    * @throws PSFilterException if the rule can't be found
    */
   String getRuleName() throws PSFilterException;

   /**
    * Get the rule name safely without throwing exceptions.
    *
    * @return Optional containing the rule name, or empty if not available
    */
   default Optional<String> getRuleNameSafely() {
      try {
         return Optional.ofNullable(getRuleName());
      } catch (PSFilterException e) {
         return Optional.empty();
      }
   }

   /**
    * Set the rule on this definition.
    *
    * @param rulename rule name, never {@code null} or empty
    * @throws IllegalArgumentException if rulename is null or empty
    */
   void setRule(String rulename);
   
   /**
    * Get the named parameter to use with the rule.
    *
    * @param name the name of the parameter to get, never {@code null}
    * @return the parameter value, may be empty or {@code null}
    * @throws IllegalArgumentException if name is null
    */
   String getParam(String name);

   /**
    * Get the named parameter safely with Optional wrapper.
    *
    * @param name the name of the parameter to get, never {@code null}
    * @return Optional containing the parameter value, or empty if not set
    * @throws IllegalArgumentException if name is null
    */
   default Optional<String> getParamSafely(String name) {
      Objects.requireNonNull(name, "Parameter name cannot be null");
      return Optional.ofNullable(getParam(name))
             .filter(value -> !value.trim().isEmpty());
   }

   /**
    * Get the parameters for this rule. The parameters are returned
    * as an immutable map.
    *
    * @return an immutable map of the parameters, never {@code null}
    */
   Map<String, String> getParams();

   /**
    * Add or change a parameter for the rule.
    *
    * @param name name of the parameter, never {@code null} or empty
    * @param value value, never {@code null} or empty
    * @throws IllegalArgumentException if name or value is null or empty
    */
   void setParam(String name, String value);

   /**
    * Remove the parameter for the rule.
    *
    * @param name name of the parameter to remove, never {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   void removeParam(String name);

   /**
    * Check if a parameter exists for this rule.
    *
    * @param name the parameter name to check, never {@code null}
    * @return true if the parameter exists and has a non-empty value
    * @throws IllegalArgumentException if name is null
    */
   default boolean hasParam(String name) {
      return getParamSafely(name).isPresent();
   }

   /**
    * Get the number of parameters configured for this rule.
    *
    * @return the parameter count, always non-negative
    */
   default int getParamCount() {
      return getParams().size();
   }

   /**
    * Check if this rule definition has any parameters configured.
    *
    * @return true if parameters exist, false otherwise
    */
   default boolean hasParams() {
      return !getParams().isEmpty();
   }

   /**
    * Default comparison implementation based on GUID.
    *
    * @param other the other rule definition to compare to
    * @return comparison result based on GUID
    */
   @Override
   default int compareTo(IPSItemFilterRuleDef other) {
      if (other == null) return 1;
      return getGUID().compareTo(other.getGUID());
   }
}
