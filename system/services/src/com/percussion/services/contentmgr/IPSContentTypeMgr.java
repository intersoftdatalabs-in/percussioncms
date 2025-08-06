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
package com.percussion.services.contentmgr;

import com.percussion.services.contentmgr.data.PSContentTemplateDesc;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Modern Java 11 interface for content type management operations with JCR integration.
 *
 * <p>This interface provides comprehensive content type management capabilities including:
 * <ul>
 *   <li>Node definition creation, loading, and persistence with Optional-based safe access</li>
 *   <li>Stream-based content type filtering and searching for efficiency</li>
 *   <li>Template and workflow association management</li>
 *   <li>Asynchronous operations with CompletableFuture support</li>
 *   <li>Enhanced validation and error handling</li>
 * </ul>
 *
 * <p>Content types are represented using JSR-170 {@link javax.jcr.nodetype.NodeDefinition}
 * interface, providing standard JCR compatibility while offering modern Java 11 enhancements.
 *
 * <p>All operations are thread-safe and provide both synchronous and asynchronous variants
 * for optimal performance in different scenarios.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSContentTypeMgr extends NodeTypeManager {

    /**
     * Creates a new concrete node definition object with assigned GUID.
     *
     * <p>The returned node definition is ready for configuration and has been
     * assigned a unique GUID for identification within the system.
     *
     * @return a new node definition, never null
     */
    IPSNodeDefinition createNodeDefinition();

    /**
     * Safely creates a new node definition, returning an Optional result.
     *
     * @return an Optional containing the new node definition, or empty if creation fails
     */
    default Optional<IPSNodeDefinition> createNodeDefinitionSafely() {
        try {
            return Optional.of(createNodeDefinition());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Loads node definitions from the database by their GUIDs.
     *
     * <p>For Rhythmyx systems, these are typically legacy GUIDs. The returned list
     * may contain fewer elements than requested if some definitions are not found.
     *
     * @param typeIds GUIDs referencing existing node definitions, must not be null or empty
     * @return immutable list of found definitions, never null but may be smaller than input
     * @throws RepositoryException specifically NoSuchNodeTypeException if none found
     * @throws IllegalArgumentException if typeIds is null or empty
     */
    List<IPSNodeDefinition> loadNodeDefinitions(List<IPSGuid> typeIds) throws RepositoryException;

    /**
     * Asynchronously loads node definitions by their GUIDs.
     *
     * @param typeIds GUIDs referencing existing node definitions, must not be null
     * @return a CompletableFuture containing the list of definitions
     * @throws IllegalArgumentException if typeIds is null or empty
     */
    default CompletableFuture<List<IPSNodeDefinition>> loadNodeDefinitionsAsync(List<IPSGuid> typeIds) {
        Objects.requireNonNull(typeIds, "Type IDs cannot be null");
        if (typeIds.isEmpty()) {
            throw new IllegalArgumentException("Type IDs cannot be empty");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadNodeDefinitions(typeIds);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to load node definitions asynchronously", e);
            }
        });
    }

    /**
     * Safely loads node definitions, returning an Optional result.
     *
     * @param typeIds GUIDs referencing existing node definitions, must not be null
     * @return an Optional containing the list of definitions, or empty if loading fails
     */
    default Optional<List<IPSNodeDefinition>> loadNodeDefinitionsSafely(List<IPSGuid> typeIds) {
        try {
            return Optional.of(loadNodeDefinitions(typeIds));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Finds node definitions that reference the specified template GUID.
     *
     * @param templateId the template GUID to search for, must not be null
     * @return immutable list of matching node definitions, never null but may be empty
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if templateId is null
     */
    List<IPSNodeDefinition> findNodeDefinitionsByTemplate(IPSGuid templateId) throws RepositoryException;

    /**
     * Streams node definitions by template for efficient processing.
     *
     * @param templateId the template GUID to search for, must not be null
     * @return a stream of matching node definitions, never null
     * @throws IllegalArgumentException if templateId is null
     */
    default Stream<IPSNodeDefinition> streamNodeDefinitionsByTemplate(IPSGuid templateId) {
        Objects.requireNonNull(templateId, "Template ID cannot be null");
        try {
            return findNodeDefinitionsByTemplate(templateId).stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Finds a single node definition by exact name match.
     *
     * @param name the name to match exactly, must not be null or empty
     * @return the matching node definition
     * @throws NoSuchNodeTypeException if the definition doesn't exist
     * @throws RepositoryException if the name is not unique or other repository problems
     * @throws IllegalArgumentException if name is null or empty
     */
    IPSNodeDefinition findNodeDefinitionByName(String name) throws RepositoryException;

    /**
     * Safely finds a node definition by name, returning an Optional result.
     *
     * @param name the name to search for, must not be null or empty
     * @return an Optional containing the node definition, or empty if not found
     */
    default Optional<IPSNodeDefinition> findNodeDefinitionByNameSafely(String name) {
        try {
            Objects.requireNonNull(name, "Name cannot be null");
            if (name.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(findNodeDefinitionByName(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Saves a collection of node definitions to the database.
     *
     * @param definitions the node definitions to save, must not be null or empty
     * @throws RepositoryException if saving fails
     * @throws IllegalArgumentException if definitions is null or empty
     */
    void saveNodeDefinitions(List<IPSNodeDefinition> definitions) throws RepositoryException;

    /**
     * Asynchronously saves node definitions to the database.
     *
     * @param definitions the node definitions to save, must not be null or empty
     * @return a CompletableFuture that completes when saving is done
     * @throws IllegalArgumentException if definitions is null or empty
     */
    default CompletableFuture<Void> saveNodeDefinitionsAsync(List<IPSNodeDefinition> definitions) {
        Objects.requireNonNull(definitions, "Definitions cannot be null");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Definitions cannot be empty");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                saveNodeDefinitions(definitions);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to save node definitions asynchronously", e);
            }
        });
    }

    /**
     * Deletes a collection of node definitions from the database.
     *
     * @param definitions the node definitions to delete, must not be null or empty
     * @throws RepositoryException if deletion fails
     * @throws IllegalArgumentException if definitions is null or empty
     */
    void deleteNodeDefinitions(List<IPSNodeDefinition> definitions) throws RepositoryException;

    /**
     * Asynchronously deletes node definitions from the database.
     *
     * @param definitions the node definitions to delete, must not be null or empty
     * @return a CompletableFuture that completes when deletion is done
     * @throws IllegalArgumentException if definitions is null or empty
     */
    default CompletableFuture<Void> deleteNodeDefinitionsAsync(List<IPSNodeDefinition> definitions) {
        Objects.requireNonNull(definitions, "Definitions cannot be null");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("Definitions cannot be empty");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                deleteNodeDefinitions(definitions);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to delete node definitions asynchronously", e);
            }
        });
    }

    /**
     * Finds node definitions by name pattern or exact match.
     *
     * @param name the name or pattern to search for, must not be null or empty
     * @return immutable list of matching definitions, never null but may be empty
     * @throws RepositoryException if there is a loading problem
     * @throws IllegalArgumentException if name is null or empty
     */
    List<IPSNodeDefinition> findNodeDefinitionsByName(String name) throws RepositoryException;

    /**
     * Streams node definitions by name for efficient processing.
     *
     * @param name the name or pattern to search for, must not be null or empty
     * @return a stream of matching definitions, never null
     * @throws IllegalArgumentException if name is null or empty
     */
    default Stream<IPSNodeDefinition> streamNodeDefinitionsByName(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.trim().isEmpty()) {
            return Stream.empty();
        }
        try {
            return findNodeDefinitionsByName(name).stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Finds all node definitions for object type 1 (content items).
     *
     * @return immutable list of all item node definitions, never null but may be empty
     * @throws RepositoryException if there is a loading problem
     */
    List<IPSNodeDefinition> findAllItemNodeDefinitions() throws RepositoryException;

    /**
     * Streams all item node definitions for efficient processing.
     *
     * @return a stream of all item node definitions, never null
     */
    default Stream<IPSNodeDefinition> streamAllItemNodeDefinitions() {
        try {
            return findAllItemNodeDefinitions().stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Finds content type template association for specific template and content type.
     *
     * @param templateId the template GUID, must not be null
     * @param contentTypeId the content type GUID, must not be null
     * @return the template association, or null if not found
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if any parameter is null
     */
    PSContentTemplateDesc findContentTypeTemplateAssociation(IPSGuid templateId, IPSGuid contentTypeId)
            throws RepositoryException;

    /**
     * Safely finds content type template association, returning an Optional result.
     *
     * @param templateId the template GUID, must not be null
     * @param contentTypeId the content type GUID, must not be null
     * @return an Optional containing the association, or empty if not found
     */
    default Optional<PSContentTemplateDesc> findContentTypeTemplateAssociationSafely(
            IPSGuid templateId, IPSGuid contentTypeId) {
        try {
            return Optional.ofNullable(findContentTypeTemplateAssociation(templateId, contentTypeId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Finds all workflow associations for the specified content type.
     *
     * @param contentTypeId the content type GUID, must not be null
     * @return immutable list of workflow associations, may be empty but never null
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if contentTypeId is null
     */
    List<PSContentTypeWorkflow> findContentTypeWorkflowAssociations(IPSGuid contentTypeId)
            throws RepositoryException;

    /**
     * Streams workflow associations for efficient processing.
     *
     * @param contentTypeId the content type GUID, must not be null
     * @return a stream of workflow associations, never null
     * @throws IllegalArgumentException if contentTypeId is null
     */
    default Stream<PSContentTypeWorkflow> streamContentTypeWorkflowAssociations(IPSGuid contentTypeId) {
        Objects.requireNonNull(contentTypeId, "Content type ID cannot be null");
        try {
            return findContentTypeWorkflowAssociations(contentTypeId).stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Finds node definitions that reference the specified workflow GUID.
     *
     * @param workflowId the workflow GUID, must not be null
     * @return immutable list of matching node definitions, never null but may be empty
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if workflowId is null
     */
    List<IPSNodeDefinition> findNodeDefinitionsByWorkflow(IPSGuid workflowId) throws RepositoryException;

    /**
     * Streams node definitions by workflow for efficient processing.
     *
     * @param workflowId the workflow GUID, must not be null
     * @return a stream of matching node definitions, never null
     * @throws IllegalArgumentException if workflowId is null
     */
    default Stream<IPSNodeDefinition> streamNodeDefinitionsByWorkflow(IPSGuid workflowId) {
        Objects.requireNonNull(workflowId, "Workflow ID cannot be null");
        try {
            return findNodeDefinitionsByWorkflow(workflowId).stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Checks if a node definition exists with the specified name.
     *
     * @param name the name to check, must not be null or empty
     * @return true if a definition exists with the name, false otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default boolean nodeDefinitionExists(String name) {
        return findNodeDefinitionByNameSafely(name).isPresent();
    }

    /**
     * Gets the count of node definitions matching the specified name pattern.
     *
     * @param name the name or pattern to count, must not be null or empty
     * @return the count of matching definitions
     * @throws IllegalArgumentException if name is null or empty
     */
    default long countNodeDefinitionsByName(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        if (name.trim().isEmpty()) {
            return 0;
        }
        try {
            return findNodeDefinitionsByName(name).size();
        } catch (RepositoryException e) {
            return 0;
        }
    }

    /**
     * Gets all unique template IDs associated with any content type.
     *
     * @return a set of template GUIDs, never null but may be empty
     */
    default Set<IPSGuid> getAllAssociatedTemplateIds() {
        return streamAllItemNodeDefinitions()
            .filter(Objects::nonNull)
            .map(def -> {
                try {
                    return streamNodeDefinitionsByTemplate(def.getGUID());
                } catch (Exception e) {
                    return Stream.<IPSNodeDefinition>empty();
                }
            })
            .flatMap(Stream::sequential)
            .map(IPSNodeDefinition::getGUID)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
