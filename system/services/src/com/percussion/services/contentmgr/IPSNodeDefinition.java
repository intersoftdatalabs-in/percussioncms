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
package com.percussion.services.contentmgr;

import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.nodetype.NodeDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Modern Java 11 expanded JSR-170 node definition interface with enhanced Rhythmyx functionality.
 *
 * <p>This interface extends the standard JCR {@link NodeDefinition} with Percussion-specific
 * capabilities including:
 * <ul>
 *   <li>Enhanced metadata management (description, labels, object types)</li>
 *   <li>Template and workflow GUID associations with Stream support</li>
 *   <li>Internal name management with validation</li>
 *   <li>Query request URL generation</li>
 *   <li>Optional-based safe access methods</li>
 * </ul>
 *
 * <p>Node definition names follow JSR-170 conventions with "rx:" prefix and
 * space-to-underscore conversion. Use internal name methods for raw name access.
 *
 * <p>All operations include comprehensive validation and null safety following
 * Java 11 best practices.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSNodeDefinition extends NodeDefinition, IPSCatalogItem {

    /**
     * Gets the description of this node definition.
     *
     * @return the description, may be null if not set
     */
    String getDescription();

    /**
     * Safely gets the description with Optional wrapper.
     *
     * @return an Optional containing the description, or empty if not set
     */
    default Optional<String> getDescriptionSafely() {
        return Optional.ofNullable(getDescription());
    }

    /**
     * Sets the description for this node definition.
     *
     * @param description the description to set, may be null to clear
     */
    void setDescription(String description);

    /**
     * Sets the description with validation for non-empty strings.
     *
     * @param description the description to set, must not be empty if provided
     * @throws IllegalArgumentException if description is empty (but not null)
     */
    default void setDescriptionSafely(String description) {
        if (description != null && description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        setDescription(description);
    }

    /**
     * Gets whether this node definition should be hidden from menus.
     *
     * @return true if hidden from menu, false otherwise, may be null if not set
     */
    Boolean getHideFromMenu();

    /**
     * Safely gets the hide from menu flag with Optional wrapper.
     *
     * @return an Optional containing the hide flag, or empty if not set
     */
    default Optional<Boolean> getHideFromMenuSafely() {
        return Optional.ofNullable(getHideFromMenu());
    }

    /**
     * Checks if this node definition is hidden from menus.
     *
     * @return true if hidden from menu, false if visible or not set
     */
    default boolean isHiddenFromMenu() {
        return getHideFromMenuSafely().orElse(false);
    }

    /**
     * Sets whether this node definition should be hidden from menus.
     *
     * @param hideFromMenu the hide flag, must not be null
     * @throws IllegalArgumentException if hideFromMenu is null
     */
    void setHideFromMenu(Boolean hideFromMenu);

    /**
     * Safely sets the hide from menu flag with validation.
     *
     * @param hideFromMenu the hide flag, must not be null
     * @throws IllegalArgumentException if hideFromMenu is null
     */
    default void setHideFromMenuSafely(Boolean hideFromMenu) {
        Objects.requireNonNull(hideFromMenu, "Hide from menu flag cannot be null");
        setHideFromMenu(hideFromMenu);
    }

    /**
     * Gets the object type identifier for this node definition.
     *
     * @return the object type, may be null if not set
     */
    Integer getObjectType();

    /**
     * Safely gets the object type with Optional wrapper.
     *
     * @return an Optional containing the object type, or empty if not set
     */
    default Optional<Integer> getObjectTypeSafely() {
        return Optional.ofNullable(getObjectType());
    }

    /**
     * Sets the object type identifier for this node definition.
     *
     * @param objectType the object type to set, may be null to clear
     */
    void setObjectType(Integer objectType);

    /**
     * Sets the object type with validation for positive values.
     *
     * @param objectType the object type, must be positive if provided
     * @throws IllegalArgumentException if objectType is not positive
     */
    default void setObjectTypeSafely(Integer objectType) {
        if (objectType != null && objectType <= 0) {
            throw new IllegalArgumentException("Object type must be positive: " + objectType);
        }
        setObjectType(objectType);
    }

    /**
     * Sets the JSR-170 formatted name for this node definition.
     *
     * @param name the name to set, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    void setName(String name);

    /**
     * Safely sets the name with enhanced validation.
     *
     * @param name the name to set, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    default void setNameSafely(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        setName(name);
    }

    /**
     * Gets the internal (raw) name without JSR-170 transformations.
     *
     * @return the internal name, never null or empty in a valid definition
     */
    String getInternalName();

    /**
     * Safely gets the internal name with Optional wrapper.
     *
     * @return an Optional containing the internal name, or empty if invalid
     */
    default Optional<String> getInternalNameSafely() {
        try {
            var internalName = getInternalName();
            return (internalName != null && !internalName.trim().isEmpty())
                    ? Optional.of(internalName)
                    : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Sets the internal name without performing transformations.
     *
     * @param name the internal name, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    void setInternalName(String name);

    /**
     * Safely sets the internal name with validation.
     *
     * @param name the internal name, must not be null or empty
     * @throws IllegalArgumentException if name is null or empty
     */
    default void setInternalNameSafely(String name) {
        Objects.requireNonNull(name, "Internal name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Internal name cannot be empty");
        }
        setInternalName(name);
    }

    /**
     * Gets the template (variant) GUIDs as an immutable set.
     *
     * @return immutable set of template GUIDs, never null but may be empty
     */
    Set<IPSGuid> getVariantGuids();

    /**
     * Streams the template GUIDs for efficient processing.
     *
     * @return a stream of template GUIDs, never null but may be empty
     */
    default Stream<IPSGuid> streamVariantGuids() {
        return getVariantGuids().stream();
    }

    /**
     * Checks if this node definition has any template associations.
     *
     * @return true if there are template GUIDs, false otherwise
     */
    default boolean hasVariantGuids() {
        return !getVariantGuids().isEmpty();
    }

    /**
     * Gets the count of associated template GUIDs.
     *
     * @return the number of template GUIDs
     */
    default int getVariantGuidCount() {
        return getVariantGuids().size();
    }

    /**
     * Adds a template GUID to the association list.
     *
     * @param guid the template GUID, must not be null
     * @throws IllegalArgumentException if guid is null
     */
    void addVariantGuid(IPSGuid guid);

    /**
     * Safely adds a template GUID with validation.
     *
     * @param guid the template GUID, must not be null
     * @throws IllegalArgumentException if guid is null
     */
    default void addVariantGuidSafely(IPSGuid guid) {
        Objects.requireNonNull(guid, "Template GUID cannot be null");
        addVariantGuid(guid);
    }

    /**
     * Removes a template GUID from the association list.
     *
     * @param guid the template GUID to remove, must not be null
     * @throws IllegalArgumentException if guid is null
     */
    void removeVariantGuid(IPSGuid guid);

    /**
     * Safely removes a template GUID with validation.
     *
     * @param guid the template GUID to remove, must not be null
     * @throws IllegalArgumentException if guid is null
     */
    default void removeVariantGuidSafely(IPSGuid guid) {
        Objects.requireNonNull(guid, "Template GUID cannot be null");
        removeVariantGuid(guid);
    }

    /**
     * Checks if a specific template GUID is associated with this node definition.
     *
     * @param guid the template GUID to check, must not be null
     * @return true if the GUID is associated, false otherwise
     * @throws IllegalArgumentException if guid is null
     */
    default boolean hasVariantGuid(IPSGuid guid) {
        Objects.requireNonNull(guid, "Template GUID cannot be null");
        return getVariantGuids().contains(guid);
    }

    /**
     * Gets the workflow GUIDs as an immutable set.
     *
     * @return immutable set of workflow GUIDs, never null but may be empty
     */
    Set<IPSGuid> getWorkflowGuids();

    /**
     * Streams the workflow GUIDs for efficient processing.
     *
     * @return a stream of workflow GUIDs, never null but may be empty
     */
    default Stream<IPSGuid> streamWorkflowGuids() {
        return getWorkflowGuids().stream();
    }

    /**
     * Checks if this node definition has any workflow associations.
     *
     * @return true if there are workflow GUIDs, false otherwise
     */
    default boolean hasWorkflowGuids() {
        return !getWorkflowGuids().isEmpty();
    }

    /**
     * Gets the count of associated workflow GUIDs.
     *
     * @return the number of workflow GUIDs
     */
    default int getWorkflowGuidCount() {
        return getWorkflowGuids().size();
    }

    /**
     * Gets the display label for this node definition.
     *
     * @return the label, may be null if not set
     */
    String getLabel();

    /**
     * Safely gets the label with Optional wrapper.
     *
     * @return an Optional containing the label, or empty if not set
     */
    default Optional<String> getLabelSafely() {
        return Optional.ofNullable(getLabel());
    }

    /**
     * Sets the display label for this node definition.
     *
     * @param label the label to set, may be null to clear
     */
    void setLabel(String label);

    /**
     * Sets the label with validation for non-empty strings.
     *
     * @param label the label to set, must not be empty if provided
     * @throws IllegalArgumentException if label is empty (but not null)
     */
    default void setLabelSafely(String label) {
        if (label != null && label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }
        setLabel(label);
    }

    /**
     * Gets the query request URL for retrieving content items of this type.
     *
     * @return the query request URL, may be null if not configured
     */
    String getQueryRequest();

    /**
     * Safely gets the query request URL with Optional wrapper.
     *
     * @return an Optional containing the query URL, or empty if not configured
     */
    default Optional<String> getQueryRequestSafely() {
        return Optional.ofNullable(getQueryRequest());
    }

    /**
     * Checks if this node definition has a configured query request URL.
     *
     * @return true if query URL is configured, false otherwise
     */
    default boolean hasQueryRequest() {
        return getQueryRequestSafely()
                .map(url -> !url.trim().isEmpty())
                .orElse(false);
    }
}
