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
package com.percussion.services.publisher;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.extension.IPSExtension;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.system.utils.IPSHtmlParameters;

import javax.jcr.query.QueryResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A template expander takes a content GUID and returns zero or more template
 * GUIDs to use for publishing (or any other purpose). This interface provides
 * modern Java 11 patterns for template expansion with enhanced parameter
 * validation and safe content list handling.
 *
 * @author dougrand
 */
public interface IPSTemplateExpander extends IPSExtension {

   /**
    * Find the templates that are appropriate for the given content GUID.
    *
    * @param results The filtered results from the generator, having had items
    *        removed by the filter. The expander can consider any data from the
    *        rows when deciding what templates to expand to. Generally
    *        "rx:sys_contenttypeid" is important in this process, but the
    *        expander may consider other properties. Never {@code null}.
    *
    * @param parameters parameters from the request and the database that
    *        control the behavior of the template expander. May be {@code null}
    *        or empty if the specific expander allows this.
    *        The engine will put the following parameters:
    *        <ul>
    *        <li>{@link IPSHtmlParameters#SYS_SITEID} - The ID of the publishing site</li>
    *        <li>{@link IPSHtmlParameters#SYS_CONTEXT} - The ID of the delivery context</li>
    *        </ul>
    *
    * @param summaryMap a map of content id to component summary passed from the
    *        engine to the expander as an efficiency measure. All content IDs in
    *        {@code results} will be in this map.
    *
    * @return some number of items to create the content list from. The number
    *         of items in the list is dependent on the expansion.
    * @throws PSPublisherException if expansion fails
    * @throws IllegalArgumentException if results or summaryMap is null
    */
   List<PSContentListItem> expand(QueryResult results,
         Map<String, String> parameters,
         Map<Integer, PSComponentSummary> summaryMap)
         throws PSPublisherException;

   /**
    * Expand templates safely with Optional wrapper for error handling.
    *
    * @param results the query results to expand, never {@code null}
    * @param parameters expansion parameters, may be {@code null}
    * @param summaryMap component summary map, never {@code null}
    * @return Optional containing the expanded content list, or empty if expansion fails
    */
   default Optional<List<PSContentListItem>> expandSafely(QueryResult results,
         Map<String, String> parameters,
         Map<Integer, PSComponentSummary> summaryMap) {
      try {
         Objects.requireNonNull(results, "Query results cannot be null");
         Objects.requireNonNull(summaryMap, "Summary map cannot be null");
         return Optional.ofNullable(expand(results, parameters, summaryMap));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Stream the expanded content list items for functional processing.
    *
    * @param results the query results to expand, never {@code null}
    * @param parameters expansion parameters, may be {@code null}
    * @param summaryMap component summary map, never {@code null}
    * @return Stream of expanded content list items, never {@code null}
    * @throws PSPublisherException if expansion fails
    */
   default Stream<PSContentListItem> streamExpanded(QueryResult results,
         Map<String, String> parameters,
         Map<Integer, PSComponentSummary> summaryMap)
         throws PSPublisherException {
      return expand(results, parameters, summaryMap).stream();
   }

   /**
    * Validates the required parameters for template expansion.
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
    * Gets a component summary for the specified content ID safely.
    *
    * @param summaryMap the summary map, may be {@code null}
    * @param contentId the content ID to look up
    * @return Optional containing the component summary, or empty if not found
    */
   default Optional<PSComponentSummary> getSummary(Map<Integer, PSComponentSummary> summaryMap,
         Integer contentId) {
      return Optional.ofNullable(summaryMap)
                     .map(map -> map.get(contentId));
   }

   /**
    * Checks if the summary map contains all required content IDs.
    *
    * @param summaryMap the summary map to check, may be {@code null}
    * @param contentIds the content IDs to verify, never {@code null}
    * @return true if all content IDs are present in the summary map
    */
   default boolean hasSummariesForAll(Map<Integer, PSComponentSummary> summaryMap,
         List<Integer> contentIds) {
      Objects.requireNonNull(contentIds, "Content IDs list cannot be null");

      if (summaryMap == null || summaryMap.isEmpty()) {
         return contentIds.isEmpty();
      }

      return contentIds.stream().allMatch(summaryMap::containsKey);
   }
}
