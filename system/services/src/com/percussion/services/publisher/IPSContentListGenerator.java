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
// REFACTORED: CP-JAVA11
package com.percussion.services.publisher;

import com.percussion.extension.IPSExtension;
import com.percussion.system.utils.IPSHtmlParameters;

import javax.jcr.query.QueryResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A generator creates a candidate list of content GUIDs to be published.
 * This interface provides modern Java 11 patterns for content list generation
 * with enhanced parameter validation and safe query result handling.
 *
 * @author dougrand
 */
public interface IPSContentListGenerator extends IPSExtension {

   /**
    * Generate a list of candidate GUIDs for publishing.
    * 
    * @param parameters parameters that control the behavior of the generator,
    *        derived from the database and the request, may be {@code null} or empty.
    *        The engine will put the following parameters:
    *        <ul>
    *        <li>{@link IPSHtmlParameters#SYS_SITEID} - The ID of the publishing site</li>
    *        <li>{@link IPSHtmlParameters#SYS_CONTEXT} - The ID of the delivery context</li>
    *        </ul>
    *
    * @return a list of query results to publish, may be empty but never {@code null}.
    *         Each row in the result will contain at least the values "rx:sys_contentid"
    *         and "rx:sys_contenttypeid". Data in the rows may be used in the expander.
    *         Only "rx:sys_contentid" will be used by the filter.
    *
    * @throws PSPublisherException if any error occurs during generation
    * @throws IllegalArgumentException if parameters contain invalid values
    */
   QueryResult generate(Map<String, String> parameters) throws PSPublisherException;

   /**
    * Generate content list safely with Optional wrapper for error handling.
    *
    * @param parameters parameters that control the behavior of the generator
    * @return Optional containing the query result, or empty if generation fails
    */
   default Optional<QueryResult> generateSafely(Map<String, String> parameters) {
      try {
         return Optional.ofNullable(generate(parameters));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Validates the required parameters for content list generation.
    *
    * @param parameters the parameters to validate, may be {@code null}
    * @return true if required parameters are present and valid
    */
   default boolean validateParameters(Map<String, String> parameters) {
      if (parameters == null || parameters.isEmpty()) {
         return false;
      }

      var siteId = parameters.get(IPSHtmlParameters.SYS_SITEID);
      var context = parameters.get(IPSHtmlParameters.SYS_CONTEXT);

      return siteId != null && !siteId.trim().isEmpty() &&
             context != null && !context.trim().isEmpty();
   }

   /**
    * Gets the site ID from the parameters safely.
    *
    * @param parameters the parameter map, may be {@code null}
    * @return Optional containing the site ID, or empty if not present
    */
   default Optional<String> getSiteId(Map<String, String> parameters) {
      return Optional.ofNullable(parameters)
                     .map(p -> p.get(IPSHtmlParameters.SYS_SITEID))
                     .filter(id -> !id.trim().isEmpty());
   }

   /**
    * Gets the delivery context from the parameters safely.
    *
    * @param parameters the parameter map, may be {@code null}
    * @return Optional containing the context ID, or empty if not present
    */
   default Optional<String> getDeliveryContext(Map<String, String> parameters) {
      return Optional.ofNullable(parameters)
                     .map(p -> p.get(IPSHtmlParameters.SYS_CONTEXT))
                     .filter(context -> !context.trim().isEmpty());
   }

   /**
    * Gets a parameter value safely with Optional wrapper.
    *
    * @param parameters the parameter map, may be {@code null}
    * @param parameterName the parameter name to retrieve, never {@code null}
    * @return Optional containing the parameter value, or empty if not present
    * @throws IllegalArgumentException if parameterName is null
    */
   default Optional<String> getParameterSafely(Map<String, String> parameters, String parameterName) {
      Objects.requireNonNull(parameterName, "Parameter name cannot be null");
      return Optional.ofNullable(parameters)
                     .map(p -> p.get(parameterName))
                     .filter(value -> value != null && !value.trim().isEmpty());
   }
}
