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
// REFACTORED: CP-JAVA11
package com.percussion.services.assembly;

import com.percussion.services.assembly.IPSAssemblyResult.Status;
import com.percussion.services.assembly.impl.nav.PSNavHelper;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.PSFilterException;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.guid.IPSGuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.jcr.Node;

/**
 * Represents a unit of work to be assembled with enhanced Java 11 support.
 *
 * <p>Each assembly item encapsulates all information necessary for content assembly,
 * including the content node, template, parameters, variables, and assembly context.
 * The assembly process transforms this item into an {@link IPSAssemblyResult}.
 *
 * <p>Assembly Item Lifecycle:
 * <ol>
 *   <li>Create item using {@link IPSAssemblyService#createAssemblyItem()}</li>
 *   <li>Configure item properties using setters</li>
 *   <li>Call {@link #normalize()} to validate and prepare the item</li>
 *   <li>Process item through assembly pipeline</li>
 * </ol>
 *
 * <p>Key features:
 * <ul>
 *   <li>Content node management with lazy loading</li>
 *   <li>Template and variable binding</li>
 *   <li>Parameter processing and validation</li>
 *   <li>Debug mode support</li>
 *   <li>Clone support for slot processing</li>
 *   <li>Optional-based safe navigation</li>
 * </ul>
 *
 * <p><strong>Important:</strong> Items created using {@link IPSAssemblyService#createAssemblyItem()}
 * must explicitly call {@link #normalize()} before assembly. After normalization,
 * setters should not be called as the item becomes immutable for assembly purposes.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSAssemblyItem extends Cloneable, Serializable {

    /**
     * Get the path of the content item being assembled.
     *
     * <p>Path formats supported:
     * <ul>
     *   <li>Folder path: {@code //folder1/folder2/.../foldern/itemname}</li>
     *   <li>With revision: {@code //folder1/.../itemname#revnumber}</li>
     *   <li>Direct content ID: {@code /cid#revision} (for items not in folders)</li>
     *   <li>Child items: {@code .../itemname/childname#nnn}</li>
     * </ul>
     *
     * <p>If revision is not specified, the current revision is used. The path may
     * be passed in directly or calculated from parameter values using {@code sys_folderid}.
     *
     * @return the path, never {@code null} or empty
     */
    String getPath();

    /**
     * Get the content node being assembled with Optional wrapper for safer access.
     *
     * @return Optional containing the node if loaded, empty otherwise
     */
    default Optional<Node> getNodeOptional() {
        return Optional.ofNullable(getNode());
    }

    /**
     * Get the content node being assembled.
     *
     * <p>The node handling follows these rules:
     * <ul>
     *   <li>If node is null (normal case), assembly engine loads from content manager</li>
     *   <li>If node is pre-loaded (e.g., slot finders), the existing node is used</li>
     *   <li>Calling this method forces loading if not already loaded</li>
     * </ul>
     *
     * @return the node, may be {@code null} if not yet loaded
     */
    Node getNode();

    /**
     * Check if the content node is loaded without forcing a load operation.
     *
     * @return {@code true} if the node is loaded for the assembly item
     */
    boolean hasNode();

    /**
     * Set or change the content node being referenced by the assembly item.
     *
     * <p>This method is primarily used internally by the assembly service and in
     * slot processing scenarios where assembly items are cloned with different nodes.
     * Setting the node also modifies the stored ID accordingly.
     *
     * @param node the node, may be {@code null} to reset (also sets ID to null)
     */
    void setNode(Node node);

    /**
     * Get the item filter for limiting results from slot finders and other operations.
     *
     * <p>The filter is derived from authtype or filter parameters as available.
     * If no filter is specified, no filtering will occur during assembly.
     *
     * @return the filter, may be {@code null} for no filtering
     * @throws PSFilterException if a filter was specified but not found
     */
    IPSItemFilter getFilter() throws PSFilterException;

    /**
     * Get the item filter with Optional wrapper for safer access.
     *
     * @return Optional containing the filter if available, empty otherwise
     */
    default Optional<IPSItemFilter> getFilterOptional() {
        try {
            return Optional.ofNullable(getFilter());
        } catch (PSFilterException e) {
            return Optional.empty();
        }
    }

    /**
     * Get assembly parameters that apply to the template and item.
     *
     * <p>Parameters include:
     * <ul>
     *   <li>Template-specific configuration values</li>
     *   <li>Assembly plugin parameters</li>
     *   <li>HTTP parameters from assembly servlet</li>
     *   <li>System parameters (sys_* values)</li>
     * </ul>
     *
     * <p>Repeated parameters will have multiple values in the string array.
     *
     * @return parameter map, may be empty but never {@code null}
     */
    Map<String, String[]> getParameters();

    /**
     * Get a single parameter value with default fallback.
     *
     * @param name the parameter name, not {@code null} or empty
     * @param defaultvalue the default value if parameter is not defined
     * @return the parameter value or default value
     * @throws IllegalArgumentException if name is null or empty
     */
    String getParameterValue(String name, String defaultvalue);

    /**
     * Get a single parameter value with Optional wrapper for safer access.
     *
     * @param name the parameter name, not {@code null} or empty
     * @return Optional containing the parameter value if present, empty otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<String> getParameterValueOptional(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        return Optional.ofNullable(getParameterValue(name, null));
    }

    /**
     * Get parameter values array with default fallback.
     *
     * @param name the parameter name, not {@code null} or empty
     * @param defaultvalues the default values if parameter is not defined
     * @return the parameter values or default values
     * @throws IllegalArgumentException if name is null or empty
     */
    String[] getParameterValues(String name, String[] defaultvalues);

    /**
     * Check if a parameter is present regardless of its value.
     *
     * <p>Use this method to distinguish between a parameter that is present but empty
     * versus a parameter that is not present at all.
     *
     * @param name the parameter name, not {@code null} or empty
     * @return {@code true} if the parameter is present, regardless of value
     * @throws IllegalArgumentException if name is null or empty
     */
    boolean hasParameter(String name);

    /**
     * Get all parameter names as a Stream for functional processing.
     *
     * @return Stream of parameter names, never {@code null}
     */
    default Stream<String> getParameterNames() {
        return getParameters().keySet().stream();
    }

    /**
     * Get additional variables to bind during template processing.
     *
     * <p>These variables represent context variables defined for assembly.
     * Additional variables bound during the assembly process are available
     * through {@link #getBindings()} instead.
     *
     * @return variable map, may be {@code null}
     */
    Map<String, String> getVariables();

    /**
     * Get variables with Optional wrapper for safer access.
     *
     * @return Optional containing variables if present, empty otherwise
     */
    default Optional<Map<String, String>> getVariablesOptional() {
        return Optional.ofNullable(getVariables());
    }

    /**
     * Get bindings calculated by the assembly service.
     *
     * <p>Bindings contain named values calculated during assembly, where each value
     * may be an object, sub-map, or list of values. This provides the final
     * variable context for template evaluation.
     *
     * @return bindings map, empty if no bindings calculated, otherwise named values
     */
    Map<String, Object> getBindings();

    /**
     * Get bindings with Optional wrapper for safer access.
     *
     * @return Optional containing bindings if present, empty otherwise
     */
    default Optional<Map<String, Object>> getBindingsOptional() {
        return Optional.ofNullable(getBindings());
    }

    /**
     * Get the template used for assembly.
     *
     * <p>The template is set by the assembly engine before bindings processing begins.
     * It may change during assembly (e.g., switching to global template after inner
     * content assembly, or dispatch template behavior).
     *
     * @return the current template, may be {@code null}
     */
    IPSAssemblyTemplate getTemplate();

    /**
     * Get the template with Optional wrapper for safer access.
     *
     * @return Optional containing the template if present, empty otherwise
     */
    default Optional<IPSAssemblyTemplate> getTemplateOptional() {
        return Optional.ofNullable(getTemplate());
    }

    /**
     * Get the original template GUID that was first set on this item.
     *
     * <p>Since templates may change during assembly (e.g., global templates, dispatch),
     * this method preserves the original template reference. The value is cleared
     * when the item is cloned.
     *
     * @return the original template ID, or current template ID if original is null,
     *         may be {@code null} if no template is set
     */
    IPSGuid getOriginalTemplateGuid();

    /**
     * Get the original template GUID with Optional wrapper.
     *
     * @return Optional containing the original template GUID if present, empty otherwise
     */
    default Optional<IPSGuid> getOriginalTemplateGuidOptional() {
        return Optional.ofNullable(getOriginalTemplateGuid());
    }

    /**
     * Get the reference ID that identifies this assembly request within a job.
     *
     * <p>Reference IDs allow callers to associate assembly results with requests.
     * While reference IDs may be reused across preview requests, they are never
     * repeated within publishing jobs.
     *
     * @return the reference ID, unique within a given job ID
     */
    long getReferenceId();

    /**
     * Get the reference ID that originated the unpublishing operation.
     *
     * @return the unpublish reference ID, may be {@code null} if {@link #isPublish()} is true
     */
    Long getUnpublishRefId();

    /**
     * Get the unpublish reference ID with Optional wrapper.
     *
     * @return Optional containing the unpublish reference ID if present, empty otherwise
     */
    default Optional<Long> getUnpublishRefIdOptional() {
        return Optional.ofNullable(getUnpublishRefId());
    }

    /**
     * Get the job ID that is unique per publishing run.
     *
     * <p>Job IDs help callers associate results and requests for a given run and
     * are used internally to determine if cached values can be reused. This value
     * should change for each new assembly job, including new previews.
     *
     * @return the job ID, unique per assembly run
     */
    long getJobId();

    /**
     * Get the GUID for the item being assembled.
     *
     * <p>The GUID is derived from either sys_contentid/sys_revision parameters
     * or extracted from the path. This method will never return {@code null}
     * for a valid assembly item.
     *
     * @return the item GUID, never {@code null} for valid items
     */
    IPSGuid getId();

    /**
     * Get the site ID if defined for the assembly item.
     *
     * <p>The site ID is extracted from the sys_siteid HTTP parameter. For slot items,
     * this may be the referenced site ID rather than the original site ID.
     *
     * @return the site ID, or {@code null} if not defined
     */
    IPSGuid getSiteId();

    /**
     * Get the site ID with Optional wrapper for safer access.
     *
     * @return Optional containing the site ID if present, empty otherwise
     */
    default Optional<IPSGuid> getSiteIdOptional() {
        return Optional.ofNullable(getSiteId());
    }

    /**
     * Get the folder content ID if defined for the assembly item.
     *
     * <p>The folder ID may be extracted from the sys_folderid HTTP parameter
     * or derived from the path information.
     *
     * @return the folder content ID, or 0 if not defined
     */
    int getFolderId();

    /**
     * Check if the item should be assembled in debug mode.
     *
     * <p>Debug mode outputs assembly information but doesn't run the plugin.
     * This setting is inherited by cloned items.
     *
     * @return {@code true} for debug mode assembly
     */
    boolean isDebug();

    /**
     * Check if this assembly is for publishing or unpublishing.
     *
     * <p>This returns {@code true} for publishing or preview operations.
     * Assembly plugins can use {@code false} to short-circuit processing
     * during unpublishing operations.
     *
     * @return {@code false} for unpublishing operations
     */
    boolean isPublish();

    /**
     * Get the user name for preview and active assembly operations.
     *
     * <p>The user name informs preview and active assembly which user is requesting
     * assembly, allowing display of appropriate item versions in preview mode.
     *
     * @return the user name, may be {@code null} but never empty
     */
    String getUserName();

    /**
     * Get the user name with Optional wrapper for safer access.
     *
     * @return Optional containing the user name if present, empty otherwise
     */
    default Optional<String> getUserNameOptional() {
        return Optional.ofNullable(getUserName());
    }

    /**
     * Get the parent assembly item if this item was cloned.
     *
     * <p>When assembly implementations clone items for processing contained items
     * (such as slot content), this method provides access to the parent item.
     * This is set automatically as part of the {@code clone()} method.
     *
     * @return the parent assembly item for cloned items, or {@code null} for constructed items
     */
    IPSAssemblyItem getCloneParentItem();

    /**
     * Get the clone parent item with Optional wrapper for safer access.
     *
     * @return Optional containing the parent item if this is a clone, empty otherwise
     */
    default Optional<IPSAssemblyItem> getCloneParentItemOptional() {
        return Optional.ofNullable(getCloneParentItem());
    }

    /**
     * Get the page number for paged item pages.
     *
     * <p>Page numbers are 1-based. Items containing a page number will also
     * contain a parent reference ID for tracking pagination relationships.
     *
     * @return the page number, or {@code null} if not a paged item
     */
    Integer getPage();

    /**
     * Get the page number with Optional wrapper for safer access.
     *
     * @return Optional containing the page number if present, empty otherwise
     */
    default Optional<Integer> getPageOptional() {
        return Optional.ofNullable(getPage());
    }

    /**
     * Set the page number for paged items.
     *
     * @param page the page number, may be {@code null}
     */
    void setPage(Integer page);

    /**
     * Get the parent reference ID for paged item pages.
     *
     * <p>For paged items, this returns the original paginated page's reference ID,
     * allowing status updates to mark the parent as failed if child pages fail.
     * This is also used to determine if pages should be evaluated for pagination.
     *
     * @return the parent reference ID, or {@code null} if not a page child item
     */
    Long getParentPageReferenceId();

    /**
     * Get the parent page reference ID with Optional wrapper.
     *
     * @return Optional containing the parent reference ID if present, empty otherwise
     */
    default Optional<Long> getParentPageReferenceIdOptional() {
        return Optional.ofNullable(getParentPageReferenceId());
    }

    /**
     * Set the parent reference ID for paged items.
     *
     * @param refid the parent reference ID, may be {@code null}
     */
    void setParentPageReferenceId(long refid);

    /**
     * Set the folder ID for the assembly item.
     *
     * @param folderId the new folder ID
     */
    void setFolderId(int folderId);

    /**
     * Gets the owner ID of the assembled item.
     * @return the ID. It may be <code>null</code> if unknown.
     */
    IPSGuid getOwnerId();

    /**
     * Sets the owner ID of the assembled item.
     * @param ownerId the owner ID. It may be <code>null</code> if unknown.
     */
    void setOwnerId(IPSGuid ownerId);

    /**
     * Set a new site id, see {@link #getSiteId()}
     *
     * @param siteid the new site id
     */
    void setSiteId(IPSGuid siteid);


    /**
     * Set the publishing server id to use with the delivery item.
     * @param pubserverid the ID of the publishing server.
     * It may be <code>null</code> if the publish-server is unknown.
     */
    void setPubServerId(Long pubserverid);

    /**
     * Get the publishing server id that is used for this item.
     * @return publishing server id. It is <code>null</code> if the publish-server is unknown.
     */
    Long getPubServerId();

    /**
     * Set new bindings, see {@link #getBindings()}
     *
     * @param bindings The bindings to set, may be <code>null</code>
     */
    void setBindings(Map<String, Object> bindings);

    /**
     * Set a new id for the referenced content item, see {@link #getId()}
     *
     * @param id The id to set, may be <code>null</code>
     */
    void setId(IPSGuid id);

    /**
     * Set the new parameters, see {@link #getParameters()}
     *
     * @param parameters The parameters to set, if <code>null</code> then the

