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
package com.percussion.services.utils.general;

import com.percussion.cms.PSCmsException;
import com.percussion.server.PSRequestParsingException;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.PSTemplateNotImplementedException;
import com.percussion.services.filter.PSFilterException;
import com.percussion.util.PSParseUrlQueryString;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import org.apache.commons.lang.StringUtils;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Various utility methods to allow easy direct calling of the assembly service.
 * This class provides convenient static methods for assembly operations using modern
 * Java 11 features for enhanced performance and type safety.
 * <p>
 * All methods in this class are thread-safe and use efficient stream processing
 * where applicable.
 */
public final class PSAssemblyServiceUtils {

    // Private constructor to prevent instantiation
    private PSAssemblyServiceUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Utility method to make a call to the assembly service to assemble a
     * document based on the URL passed in. This method expects there is
     * only one result for this URL (non multi-page).
     *
     * @param url the URL to retrieve the assembled document, not {@code null} or empty
     * @param extraParams extra parameters that will be appended to the passed in
     *                    URL. May be {@code null} or empty
     * @return the assembly result for the URL, may be {@code null} if an
     *         exception occurred when retrieving the result
     * @throws PSRequestParsingException if there was an error when trying to
     *                                   parse parameters from the supplied template URL
     * @throws PSCmsException if CMS error occurs
     * @throws PSAssemblyException if assembly error occurs
     * @throws PSTemplateNotImplementedException if template is not implemented
     * @throws RepositoryException if repository error occurs
     * @throws PSFilterException if filter error occurs
     * @throws ItemNotFoundException if item is not found
     * @throws IllegalArgumentException if url is null or empty
     */
    public static IPSAssemblyResult getAssembledDocumentResult(
            String url, Map<String, Object> extraParams)
            throws PSRequestParsingException, PSAssemblyException, PSCmsException,
                   ItemNotFoundException, PSFilterException, RepositoryException,
                   PSTemplateNotImplementedException {

        Objects.requireNonNull(url, "url cannot be null");
        if (url.trim().isEmpty()) {
            throw new IllegalArgumentException("url cannot be empty");
        }

        var service = PSAssemblyServiceLocator.getAssemblyService();
        var item = service.createAssemblyItem();

        /* The HashMap returned by the parseParameters method has an overridden
         * put method which creates ArrayLists for key values if the key already
         * exists in the map, so create a new HashMap with the mappings in order
         * to use putAll with expected behavior.
         */
        var params = new HashMap<>(PSParseUrlQueryString.parseParameters(url));
        if (extraParams != null && !extraParams.isEmpty()) {
            params.putAll(extraParams);
        }

        // Use modern parameter processing with streams
        params.forEach((key, value) -> {
            if (value instanceof String stringValue) {
                item.setParameter(key, stringValue);
            } else if (value instanceof String[] arrayValue && arrayValue.length > 0) {
                item.setParameter(key, arrayValue[0]);
            }
        });

        item.normalize();

        var results = service.assemble(List.of(item));
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    /**
     * Safely assemble a document, returning an Optional for null-safe access.
     *
     * @param url the URL to retrieve the assembled document, not {@code null} or empty
     * @param extraParams extra parameters, may be {@code null}
     * @return an Optional containing the assembly result if successful, empty otherwise
     * @throws IllegalArgumentException if url is null or empty
     */
    public static Optional<IPSAssemblyResult> getAssembledDocumentResultSafe(
            String url, Map<String, Object> extraParams) {
        try {
            return Optional.ofNullable(getAssembledDocumentResult(url, extraParams));
        } catch (Exception e) {
            // Log error but return empty Optional
            return Optional.empty();
        }
    }

    /**
     * Asynchronously assemble a document for high-performance scenarios.
     *
     * @param url the URL to retrieve the assembled document, not {@code null} or empty
     * @param extraParams extra parameters, may be {@code null}
     * @return a CompletableFuture containing the assembly result
     * @throws IllegalArgumentException if url is null or empty
     */
    public static CompletableFuture<Optional<IPSAssemblyResult>> getAssembledDocumentResultAsync(
            String url, Map<String, Object> extraParams) {
        return CompletableFuture.supplyAsync(() ->
            getAssembledDocumentResultSafe(url, extraParams));
    }

    /**
     * Assemble multiple documents efficiently using batch processing.
     *
     * @param urls the URLs to assemble, not {@code null}
     * @param extraParams common extra parameters for all URLs, may be {@code null}
     * @return a list of assembly results in the same order as the input URLs
     * @throws IllegalArgumentException if urls is null
     */
    public static List<Optional<IPSAssemblyResult>> assembleMultipleDocuments(
            List<String> urls, Map<String, Object> extraParams) {
        Objects.requireNonNull(urls, "urls cannot be null");

        return urls.stream()
            .map(url -> getAssembledDocumentResultSafe(url, extraParams))
            .toList();
    }

    /**
     * Extract unique template slots from a collection of assembly results.
     *
     * @param results the assembly results to process, not {@code null}
     * @return a set of unique template slots
     * @throws IllegalArgumentException if results is null
     */
    public static Set<IPSTemplateSlot> extractUniqueSlots(
            Collection<IPSAssemblyResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
            .filter(Objects::nonNull)
            .map(IPSAssemblyResult::getSlots)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Filter assembly results by status using Stream API.
     *
     * @param results the assembly results to filter, not {@code null}
     * @param status the status to filter by, not {@code null}
     * @return a list of results matching the specified status
     * @throws IllegalArgumentException if results or status is null
     */
    public static List<IPSAssemblyResult> filterByStatus(
            Collection<IPSAssemblyResult> results, Status status) {
        Objects.requireNonNull(results, "results cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        return results.stream()
            .filter(result -> status.equals(result.getStatus()))
            .toList();
    }

    /**
     * Check if all assembly results have the specified status.
     *
     * @param results the assembly results to check, not {@code null}
     * @param status the status to check for, not {@code null}
     * @return {@code true} if all results have the specified status, {@code false} otherwise
     * @throws IllegalArgumentException if results or status is null
     */
    public static boolean allHaveStatus(Collection<IPSAssemblyResult> results, Status status) {
        Objects.requireNonNull(results, "results cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        return results.stream()
            .allMatch(result -> status.equals(result.getStatus()));
    }

    /**
     * Count assembly results by status.
     *
     * @param results the assembly results to count, not {@code null}
     * @return a map of status to count
     * @throws IllegalArgumentException if results is null
     */
    public static Map<Status, Long> countByStatus(Collection<IPSAssemblyResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
            .collect(Collectors.groupingBy(
                IPSAssemblyResult::getStatus,
                Collectors.counting()
            ));
    }

    /**
     * Extract all content IDs from assembly results.
     *
     * @param results the assembly results to process, not {@code null}
     * @return a set of unique content IDs
     * @throws IllegalArgumentException if results is null
     */
    public static Set<IPSGuid> extractContentIds(Collection<IPSAssemblyResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
            .filter(Objects::nonNull)
            .map(IPSAssemblyResult::getContentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Create a summary map of assembly results with statistics.
     *
     * @param results the assembly results to summarize, not {@code null}
     * @return a map containing various statistics about the results
     * @throws IllegalArgumentException if results is null
     */
    public static Map<String, Object> createResultSummary(Collection<IPSAssemblyResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        var summary = new HashMap<String, Object>();
        summary.put("totalCount", results.size());
        summary.put("statusCounts", countByStatus(results));
        summary.put("uniqueContentIds", extractContentIds(results).size());
        summary.put("uniqueSlots", extractUniqueSlots(results).size());

        return summary;
    }

    /**
     * Validate assembly parameters for common issues.
     *
     * @param params the parameters to validate, may be {@code null}
     * @return {@code true} if parameters are valid, {@code false} otherwise
     */
    public static boolean validateAssemblyParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return true; // Empty params are valid
        }

        return params.entrySet().stream()
            .allMatch(entry ->
                StringUtils.isNotBlank(entry.getKey()) &&
                entry.getValue() != null
            );
    }

    /**
     * Clean assembly parameters by removing invalid entries.
     *
     * @param params the parameters to clean, may be {@code null}
     * @return a new map with only valid parameters
     */
    public static Map<String, Object> cleanAssemblyParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new HashMap<>();
        }

        return params.entrySet().stream()
            .filter(entry ->
                StringUtils.isNotBlank(entry.getKey()) &&
                entry.getValue() != null
            )
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (existing, replacement) -> existing, // Keep existing on collision
                HashMap::new
            ));
    }
}
