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
package com.percussion.services.assembly;

import com.percussion.services.catalog.IPSCataloger;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.Node;
import com.percussion.services.contentmgr.IPSNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;

/**
 * The assembly service acts as a top-level assembler that dispatches to
 * component assemblers based on the passed template, and additionally
 * supplies methods to manipulate templates and slots with enhanced Java 11 support.
 *
 * <p>This service provides comprehensive content assembly capabilities including
 * template processing, slot management, and variable binding. The implementation
 * leverages modern Java 11 features including Optional return types, Stream API,
 * and enhanced validation patterns.
 *
 * <p>Assembly Process Flow:
 * <ol>
 *   <li>Load the content item from the repository</li>
 *   <li>Bind initial site and context variables passed with the assembly item</li>
 *   <li>Bind HTTP parameters passed with the assembly items</li>
 *   <li>Bind extension functions for template evaluation</li>
 *   <li>Evaluate template bindings to create final bound variables</li>
 *   <li>Bind extra objects for plugins (e.g., $sys.asm to assembler facade)</li>
 *   <li>Invoke the appropriate assembly plugin</li>
 *   <li>Construct and return the result object</li>
 * </ol>
 *
 * <p>Key features:
 * <ul>
 *   <li>Template-based content assembly</li>
 *   <li>Slot content management</li>
 *   <li>Variable binding and evaluation</li>
 *   <li>Plugin-based assembly architecture</li>
 *   <li>Optional-based safe navigation</li>
 *   <li>Stream-based data processing</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSAssemblyService extends IPSAssembler, IPSTemplateService, IPSCataloger {

    /**
     * Parameter name for assembly URL used in legacy migration and templating code.
     */
    String ASSEMBLY_URL = "assemblyurl";

    /**
     * Create an assembly item to be used with the assembly service using modern patterns.
     *
     * <p>This is the preferred method for creating assembly items. After calling this method,
     * use the setters on the assembly item to configure the assembly parameters, then call
     * {@link IPSAssemblyItem#normalize()} before assembling the item.
     *
     * @return an uninitialized assembly item, never {@code null}
     * // @implNote This method replaces the deprecated factory method with a cleaner API
     */
    IPSAssemblyItem createAssemblyItem();

    /**
     * Process an assembly item using data from the assembly servlet with enhanced parameter validation.
     *
     * <p>This method extracts assembly parameters from the servlet request, creates an assembly item,
     * and processes it through the assembly pipeline. The following parameters are supported:
     *
     * <table>
     * <caption>Assembly Servlet Parameters</caption>
     * <tr><th>Parameter</th><th>Description</th><th>Required</th></tr>
     * <tr><td>sys_path</td><td>Content item path (format: /cid#revision)</td><td>Optional*</td></tr>
     * <tr><td>sys_contentid</td><td>Content item ID</td><td>Optional*</td></tr>
     * <tr><td>sys_revision</td><td>Content revision (-1 for current)</td><td>Optional</td></tr>
     * <tr><td>sys_folderid</td><td>Containing folder ID</td><td>Optional*</td></tr>
     * <tr><td>sys_filter</td><td>Item filter name for slot evaluation</td><td>Optional</td></tr>
     * <tr><td>sys_authtype</td><td>Authentication type ID</td><td>Optional</td></tr>
     * <tr><td>sys_siteid</td><td>Site ID for variable binding</td><td>Optional</td></tr>
     * <tr><td>sys_context</td><td>Assembly context for link generation</td><td>Required</td></tr>
     * <tr><td>sys_mode</td><td>Assembly mode (AA for Active Assembly)</td><td>Optional</td></tr>
     * <tr><td>sys_debug</td><td>Debug mode flag (true/false)</td><td>Optional</td></tr>
     * </table>
     *
     * <p><em>*Either sys_path OR (sys_contentid + sys_folderid) must be specified</em>
     *
     * @param request the servlet request containing assembly parameters, not {@code null}
     * @param template the template name to use for assembly, may be {@code null} if templateid is specified
     * @param templateid the numeric template ID, may be {@code null} if template is specified
     * @return the assembly result, never {@code null}
     * @throws PSAssemblyException if assembly fails or required parameters are missing
     * @throws IllegalArgumentException if both template and templateid are null/empty
     */
    IPSAssemblyResult processServletRequest(HttpServletRequest request,
                                           String template, String templateid)
            throws PSAssemblyException;

    /**
     * Load a content finder by name using the extensions manager.
     *
     * @param finder the finder name to load, not {@code null} or empty
     * @return a finder instance, never {@code null}
     * @throws PSAssemblyException if the finder cannot be loaded or doesn't exist
     * @throws IllegalArgumentException if finder name is null or empty
     */
    IPSContentFinder loadContentFinder(String finder) throws PSAssemblyException;

    /**
     * Get a content finder by name with Optional wrapper for safer access.
     *
     * <p>This method provides a safer alternative to {@link #loadContentFinder(String)}
     * by returning an Optional instead of throwing an exception when the finder
     * is not found.
     *
     * @param finderName the name of the content finder, not {@code null} or empty
     * @return Optional containing the finder if found, empty otherwise
     * @throws IllegalArgumentException if finderName is null or empty
     */
    default Optional<IPSContentFinder> getContentFinder(String finderName) {
        if (finderName == null || finderName.trim().isEmpty()) {
            throw new IllegalArgumentException("finderName cannot be null or empty");
        }
        try {
            return Optional.of(loadContentFinder(finderName));
        } catch (PSAssemblyException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if a content finder with the specified name exists.
     *
     * @param finderName the name of the content finder to check
     * @return true if the finder exists, false otherwise
     */
    default boolean hasContentFinder(String finderName) {
        return getContentFinder(finderName).isPresent();
    }

    /**
     * Get all available content finder names as a Stream for functional processing.
     *
     * @return Stream of content finder names, never {@code null}
     * // @implNote Implementation should provide efficient streaming of finder names
     */
    default Stream<String> getContentFinderNames() {
        // Default implementation returns empty stream - implementations should override
        return Stream.empty();
    }

    /**
     * Load a slot content finder by name.
     *
     * @param finder the finder name to load, not {@code null} or empty
     * @return a slot content finder instance
     * @throws PSAssemblyException if the finder cannot be loaded
     */
    default IPSSlotContentFinder loadFinder(String finder) throws PSAssemblyException {
        throw new PSAssemblyException(AssemblyErrorCodes.MISSING_FINDER);
    }

    /**
     * Set the current assembly item in the implementation (compatibility hook).
     */
    default void setCurrentAssemblyItem(IPSAssemblyItem item) {
        // Default no-op
    }

    /**
     * Build a landing page link for the given assembly result and node. Defaults to throwing an exception if not implemented.
     */
    default String getLandingPageLink(IPSAssemblyResult result, IPSNode node, IPSGuid templateId) throws PSAssemblyException {
        throw new PSAssemblyException(AssemblyErrorCodes.LANDING_PAGE_URL_1);
    }

    /**
     * Get the current assembly item if set.
     */
    default IPSAssemblyItem getCurrentAssemblyItem() {
        return null;
    }

    /**
     * Legacy compatibility hook - process a list of assembly items using legacy implementations.
     * Default implementation is a no-op to preserve backward compatibility.
     *
     * @param items the list of items to process, never {@code null}
     */
    default void handleItemTemplates(List<IPSAssemblyItem> items) {
        // Default no-op implementation for compatibility
    }

    /**
     * Create an assembly item using the legacy factory method with enhanced validation.
     *
     * <p><strong>Deprecated:</strong> Use {@link #createAssemblyItem()} and call setters
     * followed by {@link IPSAssemblyItem#normalize()} instead for better maintainability.
     *
     * @param path the path of the content item, not {@code null} or empty
     * @param jobid the job identifier for caching purposes
     * @param refid the request identifier within a publishing run
     * @param template the template for rendering, not {@code null}
     * @param variables site or context variables for the assembler
     * @param params HTTP parameters for the assembly process
     * @param optionalNode optional JCR node, may be {@code null}
     * @param debug debug mode flag
     * @return an assembly item ready for processing
     * @throws PSAssemblyException if item creation fails
     * @throws IllegalArgumentException if required parameters are invalid
     * @deprecated use {@link #createAssemblyItem()} and setters instead
     */
    @Deprecated
    IPSAssemblyItem createAssemblyItem(String path, long jobid, int refid,
                                       IPSAssemblyTemplate template,
                                       Map<String, String> variables,
                                       Map<String, String[]> params,
                                       Node optionalNode, boolean debug)
            throws PSAssemblyException;
}
