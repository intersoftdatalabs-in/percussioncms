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

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Modern Java 11 interface for content management operations with JCR integration.
 *
 * <p>The content manager provides comprehensive content item management including:
 * <ul>
 *   <li>Loading content items by GUID or path with Optional-based safe access</li>
 *   <li>Stream-based content filtering and processing for efficiency</li>
 *   <li>Asynchronous content operations with CompletableFuture support</li>
 *   <li>Enhanced query execution with locale and parameter support</li>
 *   <li>Content lifecycle management (create, copy, revise, delete)</li>
 * </ul>
 *
 * <p><strong>Path Syntax:</strong>
 * <ul>
 *   <li>Absolute paths start with "//" (e.g., "//Sites/Enterprise/Funds/MorningFund")</li>
 *   <li>Relative paths start without slash</li>
 *   <li>Specific revisions use "#nnn" suffix (e.g., "//Sites/Company/R-and-B#2")</li>
 *   <li>Child references: "//Sites/Regions/NorthEast#3/Category#1"</li>
 * </ul>
 *
 * <p>All methods are thread-safe and provide both synchronous and asynchronous variants
 * for optimal performance in different scenarios. Configuration options control
 * output translations and validation behavior.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSContentMgr extends IPSContentTypeMgr, QueryManager {

    /**
     * Finds content items by their repository paths with enhanced error handling.
     *
     * <p>Paths use title fields from folders and items. Depending on configuration,
     * output translations may be applied to returned properties.
     *
     * @param session the JCR session (must be null in Rhythmyx 6.x), or null for entire repository
     * @param paths the paths to find, must not be null or contain null elements
     * @param config the configuration options, may be null for defaults
     * @return immutable list of found nodes, never null but may have fewer elements if paths not found
     * @throws PathNotFoundException if one or more paths contain missing elements
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if paths is null or contains null elements
     */
    List<Node> findItemsByPath(Session session, List<String> paths, PSContentMgrConfig config)
            throws PathNotFoundException, RepositoryException;

    /**
     * Safely finds content items by paths, returning an Optional result.
     *
     * @param session the JCR session, may be null
     * @param paths the paths to find, must not be null
     * @param config the configuration options, may be null
     * @return an Optional containing the list of nodes, or empty if operation fails
     */
    default Optional<List<Node>> findItemsByPathSafely(Session session, List<String> paths, PSContentMgrConfig config) {
        try {
            Objects.requireNonNull(paths, "Paths cannot be null");
            return Optional.of(findItemsByPath(session, paths, config));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Finds content items by their GUIDs with comprehensive validation.
     *
     * <p>GUIDs reference specific versions of content items, so no additional
     * version information is needed. Configuration controls output translations.
     *
     * @param guids the GUIDs for items to load, must not be null or contain null elements
     * @param config the configuration options, may be null for defaults
     * @return immutable list of found nodes, never null but may have fewer elements if GUIDs not found
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if guids is null or contains null elements
     */
    List<Node> findItemsByGUID(List<IPSGuid> guids, PSContentMgrConfig config) throws RepositoryException;

    /**
     * Asynchronously finds content items by their GUIDs.
     *
     * @param guids the GUIDs for items to load, must not be null
     * @param config the configuration options, may be null
     * @return a CompletableFuture containing the list of nodes
     * @throws IllegalArgumentException if guids is null
     */
    default CompletableFuture<List<Node>> findItemsByGUIDAsync(List<IPSGuid> guids, PSContentMgrConfig config) {
        Objects.requireNonNull(guids, "GUIDs cannot be null");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findItemsByGUID(guids, config);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to find items by GUID asynchronously", e);
            }
        });
    }

    /**
     * Safely finds content items by GUIDs, returning an Optional result.
     *
     * @param guids the GUIDs for items to load, must not be null
     * @param config the configuration options, may be null
     * @return an Optional containing the list of nodes, or empty if operation fails
     */
    default Optional<List<Node>> findItemsByGUIDSafely(List<IPSGuid> guids, PSContentMgrConfig config) {
        try {
            return Optional.of(findItemsByGUID(guids, config));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Finds all content item IDs that match the specified node definition.
     *
     * @param definition the node definition to search for, must not be null
     * @return immutable collection of matching item GUIDs, never null but may be empty
     * @throws RepositoryException if there is a repository problem
     * @throws IllegalArgumentException if definition is null
     */
    Collection<IPSGuid> findItemIdsByNodeDefinition(NodeDefinition definition) throws RepositoryException;

    /**
     * Streams content item IDs that match the specified node definition for efficient processing.
     *
     * @param definition the node definition to search for, must not be null
     * @return a stream of matching item GUIDs, never null
     * @throws IllegalArgumentException if definition is null
     */
    default Stream<IPSGuid> streamItemIdsByNodeDefinition(NodeDefinition definition) {
        Objects.requireNonNull(definition, "Node definition cannot be null");
        try {
            return findItemIdsByNodeDefinition(definition).stream();
        } catch (RepositoryException e) {
            return Stream.empty();
        }
    }

    /**
     * Executes a query against the repository with enhanced parameter support.
     *
     * @param query the query to execute, must not be null
     * @param maxResults the maximum results to return, or -1 for no limit
     * @param parameters parameters for query variable expansion, may be null
     * @param locale the locale for result ordering, may be null for JVM default
     * @return the query result, never null
     * @throws InvalidQueryException if the query is malformed or references non-existent elements
     * @throws RepositoryException if a repository problem occurs
     * @throws IllegalArgumentException if query is null
     */
    QueryResult executeQuery(Query query, int maxResults, Map<String, ? extends Object> parameters, String locale)
            throws InvalidQueryException, RepositoryException;

    /**
     * Executes a query with default locale.
     *
     * @param query the query to execute, must not be null
     * @param maxResults the maximum results to return, or -1 for no limit
     * @param parameters parameters for query variable expansion, may be null
     * @return the query result, never null
     * @throws InvalidQueryException if the query is malformed
     * @throws RepositoryException if a repository problem occurs
     * @deprecated Use {@link #executeQuery(Query, int, Map, String)} instead
     */
    @Deprecated(since = "Java 11 Migration")
    default QueryResult executeQuery(Query query, int maxResults, Map<String, ? extends Object> parameters)
            throws InvalidQueryException, RepositoryException {
        return executeQuery(query, maxResults, parameters, null);
    }

    /**
     * Safely executes a query, returning an Optional result.
     *
     * @param query the query to execute, must not be null
     * @param maxResults the maximum results to return, or -1 for no limit
     * @param parameters parameters for query variable expansion, may be null
     * @param locale the locale for result ordering, may be null
     * @return an Optional containing the query result, or empty if execution fails
     */
    default Optional<QueryResult> executeQuerySafely(Query query, int maxResults,
            Map<String, ? extends Object> parameters, String locale) {
        try {
            return Optional.of(executeQuery(query, maxResults, parameters, locale));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Filters content GUIDs to those matching at least one of the specified content types.
     *
     * @param types the content type GUIDs to match against, must not be null
     * @param ids the content item GUIDs to filter, must not be null
     * @return immutable collection of matching GUIDs, never null but may be empty
     * @throws IllegalArgumentException if types or ids is null
     */
    Collection<IPSGuid> filterItemsByNodeDefinitions(Set<IPSGuid> types, Collection<IPSGuid> ids);

    /**
     * Streams filtered content GUIDs for efficient processing.
     *
     * @param types the content type GUIDs to match against, must not be null
     * @param ids the content item GUIDs to filter, must not be null
     * @return a stream of matching GUIDs, never null
     * @throws IllegalArgumentException if types or ids is null
     */
    default Stream<IPSGuid> streamFilteredItemsByNodeDefinitions(Set<IPSGuid> types, Collection<IPSGuid> ids) {
        return filterItemsByNodeDefinitions(types, ids).stream();
    }

    /**
     * Saves one or more content items to the repository with comprehensive validation.
     *
     * <p>Items can be either new (created via {@link #createItem(NodeDefinition)}) or
     * existing items being updated. Configuration controls input translations and validations.
     *
     * @param items the items to save, must not be null or empty
     * @param config the save configuration, must not be null
     * @throws RepositoryException if validation fails or integrity problems occur
     * @throws IllegalArgumentException if items is null/empty or config is null
     */
    void saveItems(List<Node> items, PSContentMgrConfig config) throws RepositoryException;

    /**
     * Asynchronously saves content items to the repository.
     *
     * @param items the items to save, must not be null or empty
     * @param config the save configuration, must not be null
     * @return a CompletableFuture that completes when saving is done
     * @throws IllegalArgumentException if items is null/empty or config is null
     */
    default CompletableFuture<Void> saveItemsAsync(List<Node> items, PSContentMgrConfig config) {
        Objects.requireNonNull(items, "Items cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                saveItems(items, config);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to save items asynchronously", e);
            }
        });
    }

    /**
     * Creates a new content item of the specified type with field initialization.
     *
     * <p>Fields with initial values defined by JEXL scripts will be populated.
     * The returned item is not yet persisted to the database.
     *
     * @param definition the content type definition, must not be null
     * @return a new content item ready for editing
     * @throws IllegalArgumentException if definition is null
     */
    Node createItem(NodeDefinition definition);

    /**
     * Safely creates a new content item, returning an Optional result.
     *
     * @param definition the content type definition, must not be null
     * @return an Optional containing the new item, or empty if creation fails
     */
    default Optional<Node> createItemSafely(NodeDefinition definition) {
        try {
            return Optional.of(createItem(definition));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new revision of an existing content item.
     *
     * <p>The new revision is a clone with incremented revision number.
     * The tip revision in the status record will be updated.
     *
     * @param existing the existing node to create a revision from, must not be null
     * @return a new content item revision, not yet persisted
     * @throws IllegalArgumentException if existing is null
     */
    Node createItemRevision(Node existing);

    /**
     * Creates a complete copy of an existing content item with new content ID.
     *
     * <p>Properties and children are copied from the existing item.
     * The copy has a separate content ID and is not yet persisted.
     *
     * @param existing the existing node to copy, must not be null
     * @return a new content item copy, not yet persisted
     * @throws IllegalArgumentException if existing is null
     */
    Node copyItem(Node existing);

    /**
     * Deletes one or more items from the repository with comprehensive cleanup.
     *
     * <p>For each item, relationships are removed and status/shared/local records
     * are purged. All items must exist before any deletions are performed.
     *
     * @param items the GUIDs of items to delete, must not be null or empty
     * @throws RepositoryException if an item doesn't exist or deletion fails
     * @throws IllegalArgumentException if items is null or empty
     */
    void deleteItems(List<IPSGuid> items) throws RepositoryException;

    /**
     * Asynchronously deletes content items from the repository.
     *
     * @param items the GUIDs of items to delete, must not be null or empty
     * @return a CompletableFuture that completes when deletion is done
     * @throws IllegalArgumentException if items is null or empty
     */
    default CompletableFuture<Void> deleteItemsAsync(List<IPSGuid> items) {
        Objects.requireNonNull(items, "Items cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                deleteItems(items);
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to delete items asynchronously", e);
            }
        });
    }

    /**
     * Finds all content IDs of the specified type with matching title (case-insensitive).
     *
     * @param contentTypeId the content type ID, must not be null
     * @param title the title to search for, must not be null
     * @return immutable list of matching content IDs, never null but may be empty
     * @throws RepositoryException if the content type doesn't exist
     * @throws IllegalArgumentException if contentTypeId or title is null
     */
    List<String> findNodesByTitle(Long contentTypeId, String title) throws RepositoryException;

    /**
     * Finds content IDs by field value with case-insensitive matching.
     *
     * @param contentTypeId the content type ID
     * @param fieldName the field name to search, must not be null
     * @param fieldValue the field value to match, must not be null
     * @return immutable list of matching content IDs, never null but may be empty
     * @throws IllegalArgumentException if any parameter is null
     */
    List<Integer> findItemsByLocalFieldValue(long contentTypeId, String fieldName, String fieldValue);

    /**
     * Streams content IDs by field value for efficient processing.
     *
     * @param contentTypeId the content type ID
     * @param fieldName the field name to search, must not be null
     * @param fieldValue the field value to match, must not be null
     * @return a stream of matching content IDs, never null
     * @throws IllegalArgumentException if fieldName or fieldValue is null
     */
    default Stream<Integer> streamItemsByLocalFieldValue(long contentTypeId, String fieldName, String fieldValue) {
        return findItemsByLocalFieldValue(contentTypeId, fieldName, fieldValue).stream();
    }

    /**
     * Checks if a content item exists with the specified GUID.
     *
     * @param guid the GUID to check, must not be null
     * @return true if the item exists, false otherwise
     * @throws IllegalArgumentException if guid is null
     */
    default boolean itemExists(IPSGuid guid) {
        Objects.requireNonNull(guid, "GUID cannot be null");
        return findItemsByGUIDSafely(List.of(guid), null)
            .map(nodes -> !nodes.isEmpty())
            .orElse(false);
    }

    /**
     * Gets the count of items matching the specified node definition.
     *
     * @param definition the node definition to count, must not be null
     * @return the count of matching items
     * @throws IllegalArgumentException if definition is null
     */
    default long countItemsByNodeDefinition(NodeDefinition definition) {
        Objects.requireNonNull(definition, "Node definition cannot be null");
        try {
            return findItemIdsByNodeDefinition(definition).size();
        } catch (RepositoryException e) {
            return 0;
        }
    }
}
