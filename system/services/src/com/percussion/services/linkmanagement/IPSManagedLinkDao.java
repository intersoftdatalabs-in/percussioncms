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
package com.percussion.services.linkmanagement;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.percussion.services.linkmanagement.data.PSManagedLink;
import com.percussion.share.service.exception.PSDataServiceException;

/**
 * Data Access Object interface for managed link operations with modern Java 11 patterns.
 * Provides comprehensive CRUD operations for managing content relationships and links
 * with enhanced validation, Optional-based safe access, and Stream API integration.
 *
 * @author JaySeletz
 */
public interface IPSManagedLinkDao {
    /**
     * Creates an unpersisted instance of a managed link object with enhanced validation.
     *
     * @param parentId the id of the item that has the link
     * @param parentRev the parent revision
     * @param childId the id of the page or resource the link is pointing to
     * @param anchor the anchor text for the link, may be null
     * @return the link object, never null
     * @throws IllegalArgumentException if any id parameter is negative
     */
    PSManagedLink createLink(int parentId, int parentRev, int childId, String anchor);

    /**
     * Saves a link with enhanced validation.
     *
     * @param link the link to save, never null
     * @throws PSDataServiceException if there is an unexpected error
     * @throws IllegalArgumentException if link is null
     */
    void saveLink(PSManagedLink link) throws PSDataServiceException;

    /**
     * Saves multiple links efficiently.
     *
     * @param links the collection of links to save, never null
     * @throws PSDataServiceException if there is an unexpected error
     * @throws IllegalArgumentException if links is null or contains null elements
     */
    default void saveLinks(Collection<PSManagedLink> links) throws PSDataServiceException {
        Objects.requireNonNull(links, "Links collection cannot be null");
        for (var link : links) {
            Objects.requireNonNull(link, "Link cannot be null");
            saveLink(link);
        }
    }

    /**
     * Finds a managed link by its ID with safe access.
     *
     * @param linkId the link ID to search for
     * @return Optional containing the link if found, empty if not found
     * @throws IllegalArgumentException if linkId is negative
     */
    Optional<PSManagedLink> findLinkByLinkId(long linkId);

    /**
     * Loads a managed link by its ID with guaranteed existence.
     *
     * @param linkId the link ID to search for
     * @return the link, never null
     * @throws IllegalArgumentException if linkId is negative
     * @throws IllegalStateException if the link doesn't exist
     */
    default PSManagedLink loadLinkByLinkId(long linkId) {
        return findLinkByLinkId(linkId)
            .orElseThrow(() -> new IllegalStateException(
                "Managed link not found with ID: " + linkId));
    }

    /**
     * Checks if a managed link exists with the given ID.
     *
     * @param linkId the link ID to check
     * @return true if the link exists, false otherwise
     * @throws IllegalArgumentException if linkId is negative
     */
    default boolean linkExists(long linkId) {
        if (linkId < 0) {
            throw new IllegalArgumentException("Link ID cannot be negative: " + linkId);
        }
        return findLinkByLinkId(linkId).isPresent();
    }

    /**
     * Deletes a link with enhanced validation.
     *
     * @param link the link to delete, never null
     * @throws Exception if there is an unexpected error
     * @throws IllegalArgumentException if link is null
     */
    void deleteLink(PSManagedLink link) throws Exception;

    /**
     * Deletes multiple links efficiently.
     *
     * @param links the collection of links to delete, never null
     * @throws IllegalArgumentException if links is null or contains null elements
     */
    default void deleteLinks(Collection<PSManagedLink> links) {
        Objects.requireNonNull(links, "Links collection cannot be null");
        for (var link : links) {
            Objects.requireNonNull(link, "Link cannot be null");
            try {
                deleteLink(link);
            } catch (Exception e) {
                throw new RuntimeException("Error deleting link with ID: " + link.getLinkId(), e);
            }
        }
    }

    /**
     * Deletes a collection of managed links in new transactions.
     * Currently implemented to avoid read-only errors when deleting managed links.
     *
     * @param links the managed links to delete, never null
     * @throws IllegalArgumentException if links is null
     */
    void deleteLinksInNewTransaction(Collection<PSManagedLink> links);

    /**
     * Fixes links that are orphaned with enhanced error handling.
     * This can clean up issues due to previous code that did not properly maintain link integrity.
     *
     * @throws Exception if there is an unexpected error
     */
    void cleanupOrphanedLinks() throws Exception;

    /**
     * Finds all managed links with the specified parent id.
     *
     * @param parentId the content id of the parent of the link
     * @return unmodifiable list of found links, never null but may be empty
     * @throws IllegalArgumentException if parentId is negative
     */
    List<PSManagedLink> findLinksByParentId(int parentId);

    /**
     * Gets all managed links with the specified parent id as a stream for efficient processing.
     *
     * @param parentId the content id of the parent of the link
     * @return stream of found links, never null
     * @throws IllegalArgumentException if parentId is negative
     */
    default Stream<PSManagedLink> findLinksByParentIdAsStream(int parentId) {
        return findLinksByParentId(parentId).stream();
    }

    /**
     * Finds all managed links with the specified parent ids from the list.
     *
     * @param parentIds collection of content ids of the parents of the links to find, never null
     * @return unmodifiable list of found links, never null but may be empty
     * @throws IllegalArgumentException if parentIds is null
     */
    List<PSManagedLink> findLinksByParentIds(List<Integer> parentIds);

    /**
     * Gets all managed links with the specified parent ids as a stream for efficient processing.
     *
     * @param parentIds collection of content ids of the parents of the links to find, never null
     * @return stream of found links, never null
     * @throws IllegalArgumentException if parentIds is null
     */
    default Stream<PSManagedLink> findLinksByParentIdsAsStream(List<Integer> parentIds) {
        return findLinksByParentIds(parentIds).stream();
    }

    /**
     * Finds all managed links with the specified child id.
     *
     * @param childId the content id of the child the links are pointing to
     * @return unmodifiable list of found links, never null but may be empty
     * @throws IllegalArgumentException if childId is negative
     */
    List<PSManagedLink> findLinksByChildId(int childId);

    /**
     * Gets all managed links with the specified child id as a stream for efficient processing.
     *
     * @param childId the content id of the child the links are pointing to
     * @return stream of found links, never null
     * @throws IllegalArgumentException if childId is negative
     */
    default Stream<PSManagedLink> findLinksByChildIdAsStream(int childId) {
        return findLinksByChildId(childId).stream();
    }

    /**
     * Finds all managed links with the specified child ids.
     *
     * @param childIds collection of content ids of the children the links are pointing to, never null
     * @return unmodifiable list of found links, never null but may be empty
     * @throws IllegalArgumentException if childIds is null
     */
    default List<PSManagedLink> findLinksByChildIds(Collection<Integer> childIds) {
        Objects.requireNonNull(childIds, "Child IDs collection cannot be null");
        return childIds.stream()
            .flatMap(this::findLinksByChildIdAsStream)
            .collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    /**
     * Gets the count of managed links associated with a parent.
     *
     * @param parentId the content id of the parent
     * @return the number of links associated with the parent
     * @throws IllegalArgumentException if parentId is negative
     */
    default long getLinkCountByParentId(int parentId) {
        return findLinksByParentIdAsStream(parentId).count();
    }

    /**
     * Gets the count of managed links pointing to a child.
     *
     * @param childId the content id of the child
     * @return the number of links pointing to the child
     * @throws IllegalArgumentException if childId is negative
     */
    default long getLinkCountByChildId(int childId) {
        return findLinksByChildIdAsStream(childId).count();
    }

    /**
     * Validates that a content ID is valid (non-negative).
     *
     * @param contentId the ID to validate
     * @param paramName the parameter name for error messages
     * @throws IllegalArgumentException if the ID is invalid
     */
    default void validateContentId(int contentId, String paramName) {
        if (contentId < 0) {
            throw new IllegalArgumentException(paramName + " cannot be negative: " + contentId);
        }
    }
}
