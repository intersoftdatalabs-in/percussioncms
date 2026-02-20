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

import com.percussion.rx.publisher.IPSPublisherItemStatus;
import com.percussion.rx.publisher.IPSPublisherJobStatus;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.IPSCataloger;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.publisher.data.PSContentListItem;
import com.percussion.services.publisher.data.PSContentListResults;
import com.percussion.services.publisher.data.PSItemPublishingHistory;
import com.percussion.services.publisher.data.PSSiteItem;
import com.percussion.services.publisher.data.PSSortCriterion;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.query.QueryResult;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Service to perform CRUD operations on content lists and execute content list publishing
 * with comprehensive Java 11 modernization. This service provides enhanced publishing
 * functionality including content list management, site item tracking, and publishing
 * workflow coordination with modern safety and performance patterns.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for all lookup operations</li>
 * <li>Stream API for efficient content list and publishing data processing</li>
 * <li>CompletableFuture support for asynchronous publishing operations</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Functional interfaces for content filtering and processing</li>
 * </ul>
 *
 * @author dougrand
 */
public interface IPSPublisherService extends IPSCataloger {

    /**
     * Create a content list object with the given name with enhanced validation.
     * A GUID will be assigned, but the returned instance will be transient until
     * {@link #saveContentLists(List)} has been called.
     *
     * @param name the name, must be unique when the instance is saved,
     *             not {@code null} or empty
     * @return a new instance, never {@code null}
     * @throws IllegalArgumentException if name is null or empty
     */
    default IPSContentList createContentList(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        return createContentListImpl(name.trim());
    }

    /**
     * Internal implementation for content list creation.
     */
    IPSContentList createContentListImpl(String name);

    /**
     * Load the content lists specified by the IDs with enhanced validation.
     *
     * @param ids a list of GUIDs, not {@code null} or empty
     * @return an immutable list of instances, never {@code null} or empty.
     *         Instances that are not in the database will be returned as
     *         {@code null} elements in the list
     * @throws PSPublisherException if there is a database error
     * @throws IllegalArgumentException if ids is null or empty
     */
    List<IPSContentList> loadContentLists(List<IPSGuid> ids) throws PSPublisherException;

    /**
     * Load content lists safely, returning an Optional for error handling.
     *
     * @param ids a list of GUIDs, not {@code null} or empty
     * @return an Optional containing the list of content lists, empty if loading fails
     * @throws IllegalArgumentException if ids is null or empty
     */
    default Optional<List<IPSContentList>> loadContentListsSafely(List<IPSGuid> ids) {
        Objects.requireNonNull(ids, "ids cannot be null");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("ids cannot be empty");
        }
        try {
            return Optional.of(loadContentLists(ids));
        } catch (PSPublisherException e) {
            return Optional.empty();
        }
    }

    /**
     * Load content lists as a stream for efficient processing.
     *
     * @param ids a list of GUIDs, not {@code null} or empty
     * @return Stream of content lists (excluding null entries), never {@code null}
     * @throws PSPublisherException if there is a database error
     * @throws IllegalArgumentException if ids is null or empty
     */
    default Stream<IPSContentList> streamContentLists(List<IPSGuid> ids) throws PSPublisherException {
        return loadContentLists(ids).stream()
            .filter(Objects::nonNull);
    }

    /**
     * Load the content list specified by the ID. The returned object should be
     * treated as an immutable object and may not be saved by calling
     * {@link #saveContentList(IPSContentList)}.
     *
     * @param id a GUID of the content list, not {@code null}
     * @return the Content List instance, never {@code null}
     * @throws PSNotFoundException if cannot find the specified Content List
     * @throws IllegalArgumentException if id is null
     */
    IPSContentList loadContentList(IPSGuid id) throws PSNotFoundException;

    /**
     * Load the content list specified by the ID, returning an Optional for safe access.
     * This is the preferred method for content list access as it provides null safety.
     *
     * @param id a GUID of the content list, not {@code null}
     * @return an Optional containing the content list if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<IPSContentList> findContentList(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        try {
            return Optional.of(loadContentList(id));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Asynchronously load a content list for non-blocking operations.
     *
     * @param id a GUID of the content list, not {@code null}
     * @return CompletableFuture containing the content list
     * @throws IllegalArgumentException if id is null
     */
    default CompletableFuture<Optional<IPSContentList>> loadContentListAsync(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        return CompletableFuture.supplyAsync(() -> findContentList(id));
    }

    /**
     * Load the content list specified by the ID. The returned object can be
     * modified and saved by calling {@link #saveContentList(IPSContentList)}.
     *
     * @param id a GUID of the content list, not {@code null}
     * @return the Content List instance, never {@code null}
     * @throws PSNotFoundException if cannot find the specified Content List
     * @throws IllegalArgumentException if id is null
     */
    IPSContentList loadContentListModifiable(IPSGuid id) throws PSNotFoundException;

    /**
     * Load a modifiable content list safely, returning an Optional for safe access.
     *
     * @param id a GUID of the content list, not {@code null}
     * @return an Optional containing the modifiable content list if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<IPSContentList> findContentListModifiable(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        try {
            return Optional.of(loadContentListModifiable(id));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Load the Content List by the specified name.
     *
     * @param name the name of the Content List, not {@code null} or empty
     * @return the Content List instance, never {@code null}
     * @throws PSNotFoundException if cannot find the specified Content List
     * @throws IllegalArgumentException if name is null or empty
     */
    IPSContentList loadContentList(String name) throws PSNotFoundException;

    /**
     * Load the Content List by name, returning an Optional for safe access.
     * This is the preferred method for content list lookup by name.
     *
     * @param name the name of the Content List, not {@code null} or empty
     * @return an Optional containing the content list if found, empty otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<IPSContentList> findContentListByName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        try {
            return Optional.of(loadContentList(name.trim()));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }


    /**
     * Load a modifiable edition by GUID. Implementations should provide the concrete behavior.
     *
     * @param id the edition GUID, not {@code null}
     * @return the modifiable edition instance
     */
    IPSEdition loadEditionModifiable(IPSGuid id) throws PSNotFoundException;

    /**
     * Create a new edition instance and edition content list. Kept for UI compatibility.
     */
    IPSEdition createEdition();

    IPSEditionContentList createEditionContentList();

    /**
     * Find an edition task definition by id.
     *
     * @param id the task id
     * @return the task definition or <code>null</code> if not found
     */
    IPSEditionTaskDef findEditionTaskById(IPSGuid id);

    /**
     * Find edition task log entries for a given publishing job id.
     *
     * @param jobId the publishing job id
     * @return a list of task log entries, never <code>null</code>
     */
    java.util.List<IPSEditionTaskLog> findEditionTaskLogEntriesByJobId(Long jobId);

    /**
     * Get the edition id associated with a job.
     *
     * @param jobid the job id
     * @return the edition id or <code>null</code>
     */
    IPSGuid findEditionIdForJob(long jobid);

    /**
     * Load an edition by GUID.
     *
     * @param id the edition guid
     * @return the edition instance
     * @throws PSNotFoundException if the edition does not exist
     */
    IPSEdition loadEdition(IPSGuid id) throws PSNotFoundException;

    /**
     * Save an edition instance.
     */
    void saveEdition(IPSEdition edition);

    /**
     * Find all editions matching a filter.
     */
    List<IPSEdition> findAllEditions(String filter);

    /**
     * Find all editions associated with a publishing server.
     *
     * @param pubServerId the publishing server GUID, not {@code null}
     * @return a list of editions, never {@code null}
     */
    List<IPSEdition> findAllEditionsByPubServer(IPSGuid pubServerId);

    /**
     * Find publish status records for the specified site.
     *
     * @param siteId the site GUID, not {@code null}
     * @return a list of publish status entries, never {@code null}
     */
    List<IPSPubStatus> findPubStatusBySite(IPSGuid siteId);

    /**
     * Find publish status records for the specified edition.
     *
     * @param editionId the edition GUID, not {@code null}
     * @return a list of publish status entries, never {@code null}
     */
    List<IPSPubStatus> findPubStatusByEdition(IPSGuid editionId);

    /**
     * Load a delivery type for modification.
     */
    IPSDeliveryType loadDeliveryTypeModifiable(IPSGuid id);

    /**
     * Persist or update a delivery type.
     */
    void saveDeliveryType(IPSDeliveryType deliveryType);

    /**
     * Delete a delivery type.
     */
    void deleteDeliveryType(IPSDeliveryType deliveryType);

    /**
     * Notify the publisher that a set of folders moved under the supplied site. Implementations may
     * (and typically do) update internal publish queues to account for the move. The caller must
     * provide a collection of integer folder IDs.
     */
    default void markFolderIdsForMovedFolders(IPSGuid siteid, Collection<Integer> folderIds) {
        throw new UnsupportedOperationException("markFolderIdsForMovedFolders not implemented");
    }

    /**
     * Execute the given content list and return the results. Kept in the service
     * interface for backward compatibility with callers that used the concrete
     * implementation directly.
     *
     * @param list the content list to execute, not {@code null}
     * @param overrides parameter overrides, may be {@code null}
     * @param publish whether to publish
     * @param deliveryContextId the delivery context id, not {@code null}
     * @param siteId the site id, not {@code null}
     * @return the content list results
     * @throws PSPublisherException if execution fails
     */
    PSContentListResults runContentList(IPSContentList list, Map<String, String> overrides, boolean publish, IPSGuid deliveryContextId, IPSGuid siteId) throws PSPublisherException;

    /**
     * Convenience execution method that wraps {@link #runContentList(...)} and
     * returns a simple list of {@link PSContentListItem} for callers that do not
     * wish to deal with an iterator-based result.
     */
    List<PSContentListItem> executeContentList(IPSContentList list, Map<String, String> overrides, boolean publish, IPSGuid deliveryContextId, IPSGuid siteId) throws PSPublisherException;

    /**
     * Execute the generator and expander to produce content list items from a
     * previously obtained {@link QueryResult}. This method is provided to allow
     * callers that already have a JCR query result to proceed directly to
     * expansion and filtering without re-running the generator.
     *
     * @param list the content list definition
     * @param expander the template expander to use
     * @param result the query result from the content list generator
     * @param publish whether this is for publish (affects unpublishing behavior)
     * @param siteId the site GUID, not {@code null}
     * @param deliveryContextId the delivery context GUID, may be {@code null}
     * @param expparams the expander parameters, not {@code null}
     * @param overrideParams override parameters for filtering and expansion
     * @return a list of {@link PSContentListItem}, never {@code null}
     * @throws PSPublisherException if execution fails
     */
    List<PSContentListItem> getContentListItems(IPSContentList list,
                                                IPSTemplateExpander expander,
                                                QueryResult result,
                                                boolean publish,
                                                IPSGuid siteId,
                                                IPSGuid deliveryContextId,
                                                Map<String, String> expparams,
                                                Map<String, String> overrideParams) throws PSPublisherException;

    /**
     * Save the passed content lists to the database with enhanced validation.
     * The content lists with {@code null} versions will be inserted and the others will be updated.
     *
     * @param lists a list of content lists, not {@code null} or empty
     * @throws IllegalArgumentException if lists is null or empty
     */
    default void saveContentLists(List<IPSContentList> lists) {
        Objects.requireNonNull(lists, "lists cannot be null");
        if (lists.isEmpty()) {
            throw new IllegalArgumentException("lists cannot be empty");
        }
        saveContentListsImpl(lists);
    }

    /**
     * Internal implementation for content lists saving.
     */
    void saveContentListsImpl(List<IPSContentList> lists);

    /**
     * Save the passed content list to the database with enhanced validation.
     * The content list with {@code null} version will be inserted and the others will be updated.
     *
     * @param clist the content list to be saved, not {@code null}
     * @throws IllegalArgumentException if clist is null
     */
    default void saveContentList(IPSContentList clist) {
        Objects.requireNonNull(clist, "clist cannot be null");
        saveContentListImpl(clist);
    }

    /**
     * Internal implementation for content list saving.
     */
    void saveContentListImpl(IPSContentList clist);

    /**
     * Asynchronously save a content list without blocking the calling thread.
     *
     * @param clist the content list to save, not {@code null}
     * @return CompletableFuture that completes when the save operation finishes
     * @throws IllegalArgumentException if clist is null
     */
    default CompletableFuture<Void> saveContentListAsync(IPSContentList clist) {
        Objects.requireNonNull(clist, "clist cannot be null");
        return CompletableFuture.runAsync(() -> saveContentList(clist));
    }

    /**
     * Delete the passed content lists from the database with enhanced validation.
     *
     * @param lists a list of content list objects, not {@code null} or empty
     * @throws IllegalArgumentException if lists is null or empty
     */
    default void deleteContentLists(List<IPSContentList> lists) {
        Objects.requireNonNull(lists, "lists cannot be null");
        if (lists.isEmpty()) {
            throw new IllegalArgumentException("lists cannot be empty");
        }
        deleteContentListsImpl(lists);
    }

    /**
     * Internal implementation for content lists deletion.
     */
    void deleteContentListsImpl(List<IPSContentList> lists);

    /**
     * Delete the passed status list from the database with enhanced validation.
     *
     * @param statusList a list of status objects, not {@code null} or empty
     * @throws IllegalArgumentException if statusList is null or empty
     */
    default void deleteStatusList(List<IPSPubStatus> statusList) {
        Objects.requireNonNull(statusList, "statusList cannot be null");
        if (statusList.isEmpty()) {
            throw new IllegalArgumentException("statusList cannot be empty");
        }
        deleteStatusListImpl(statusList);
    }

    /**
     * Internal implementation for status list deletion.
     */
    void deleteStatusListImpl(List<IPSPubStatus> statusList);

    /**
     * Find the content list whose ID matches the given ID.
     *
     * @param contentListId the ID of the content list, not {@code null}
     * @return the single matching content list, may be {@code null}
     *         if cannot find a Content List with the specified ID
     * @throws PSNotFoundException if the content list is not found
     * @throws IllegalArgumentException if contentListId is null
     */
    IPSContentList findContentListById(IPSGuid contentListId) throws PSNotFoundException;

    /**
     * Find content list by ID safely, returning an Optional for safe access.
     *
     * @param contentListId the ID of the content list, not {@code null}
     * @return an Optional containing the content list if found, empty otherwise
     * @throws IllegalArgumentException if contentListId is null
     */
    default Optional<IPSContentList> findContentListByIdSafely(IPSGuid contentListId) {
        Objects.requireNonNull(contentListId, "contentListId cannot be null");
        try {
            return Optional.ofNullable(findContentListById(contentListId));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Get all the content lists, filtered by name with enhanced validation.
     *
     * @param filter a name filter, only content lists with names that include
     *               the given string will be returned. Equivalent to %filter% in
     *               SQL. Not {@code null} but can be empty
     * @return an immutable list of content lists, might be empty, but never {@code null}
     * @throws IllegalArgumentException if filter is null
     */
    default List<IPSContentList> findAllContentLists(String filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        return findAllContentListsImpl(filter);
    }

    /**
     * Internal implementation for finding all content lists.
     */
    List<IPSContentList> findAllContentListsImpl(String filter);

    /**
     * Get a stream of content lists for efficient processing.
     *
     * @param filter the name filter to apply, not {@code null}
     * @return Stream of content lists, never {@code null}
     * @throws IllegalArgumentException if filter is null
     */
    default Stream<IPSContentList> streamContentLists(String filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        return findAllContentLists(filter).stream();
    }

    /**
     * Filter content lists using a predicate for advanced filtering.
     *
     * @param filter the name filter to apply initially, not {@code null}
     * @param predicate additional predicate for filtering, not {@code null}
     * @return Stream of matching content lists, never {@code null}
     * @throws IllegalArgumentException if filter or predicate is null
     */
    default Stream<IPSContentList> filterContentLists(String filter, Predicate<IPSContentList> predicate) {
        Objects.requireNonNull(filter, "filter cannot be null");
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return streamContentLists(filter).filter(predicate);
    }

    /**
     * Count content lists matching the filter.
     *
     * @param filter the name filter to apply, not {@code null}
     * @return the count of matching content lists
     * @throws IllegalArgumentException if filter is null
     */
    default long countContentLists(String filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        return streamContentLists(filter).count();
    }

    /**
     * Check if any content lists exist for the given filter.
     *
     * @param filter the name filter to apply, not {@code null}
     * @return {@code true} if any content lists match, {@code false} otherwise
     * @throws IllegalArgumentException if filter is null
     */
    default boolean hasContentLists(String filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        return streamContentLists(filter).findAny().isPresent();
    }

    /**
     * Check if a content list exists by name.
     *
     * @param name the content list name to check, not {@code null} or empty
     * @return {@code true} if the content list exists, {@code false} otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default boolean contentListExists(String name) {
        return findContentListByName(name).isPresent();
    }

    /**
     * Check if a content list exists by ID.
     *
     * @param id the content list ID to check, not {@code null}
     * @return {@code true} if the content list exists, {@code false} otherwise
     * @throws IllegalArgumentException if id is null
     */
    default boolean contentListExists(IPSGuid id) {
        return findContentList(id).isPresent();
    }

    // Backwards-compatibility methods (delegated by implementation)

    IPSDeliveryType loadDeliveryType(String dtypeName);

    IPSDeliveryType loadDeliveryType(IPSGuid id);

    List<IPSEditionContentList> loadEditionContentLists(IPSGuid editionId);

    void deleteEditionContentList(IPSEditionContentList list);

    void saveEditionContentList(IPSEditionContentList list);

    /**
     * Return all configured delivery types. Added for compatibility with legacy callers.
     *
     * @return a list of delivery types, never {@code null}
     */
    List<IPSDeliveryType> findAllDeliveryTypes();

    /**
     * Create a new delivery type instance. Kept for compatibility with UI code.
     *
     * @return a new delivery type instance, never {@code null}
     */
    IPSDeliveryType createDeliveryType();

    /**
     * Find all content lists associated with the given site.
     *
     * @param siteId the site GUID, not {@code null}
     * @return a list of content lists, never {@code null}
     * @throws PSNotFoundException if the site cannot be found
     */
    List<IPSContentList> findAllContentListsBySite(IPSGuid siteId) throws PSNotFoundException;

    /**
     * Find all content lists that are unused. Kept as a convenience for callers.
     *
     * @return a list of unused content lists, never {@code null}
     */
    List<IPSContentList> findAllUnusedContentLists();

    /**
     * Update publishing info (compatibility shim).
     *
     * @param stati the list of publisher item statuses, not {@code null}
     */
    void updatePublishingInfo(List<IPSPublisherItemStatus> stati);

    /**
     * Find publication status for a job id.
     *
     * @param jobid the job id
     * @return the publication status or {@code null} if not found
     */
    IPSPubStatus findPubStatusForJob(long jobid);

    /**
     * Find all editions for the given site.
     *
     * @param siteId the site GUID, not {@code null}
     * @return a list of editions, never {@code null}
     */
    List<IPSEdition> findAllEditionsBySite(IPSGuid siteId);

    /**
     * Find site items by content ids.
     *
     * @param siteid the site GUID, not {@code null}
     * @param deliveryContext the delivery context id
     * @param contentIds collection of content ids, not {@code null} or empty
     * @return a collection of site items, never {@code null}
     */
    Collection<IPSSiteItem> findSiteItemsByIds(IPSGuid siteid, int deliveryContext, Collection<Integer> contentIds);

    /**
     * Find all published site items for a site and delivery context.
     *
     * @param siteid the site GUID, not {@code null}
     * @param deliveryContext the delivery context identifier
     * @return collection of site items, never {@code null}
     */
    Collection<IPSSiteItem> findSiteItems(IPSGuid siteid, int deliveryContext);

    /**
     * Find site items by content ids under READ_UNCOMMITTED transaction isolation.
     */
    Collection<IPSSiteItem> findSiteItemsByIds_ReadUncommit(IPSGuid siteid, int deliveryContext, Collection<Integer> contentIds);

    /**
     * Find server items by content ids.
     *
     * @param serverId the pub server GUID, not {@code null}
     * @param deliveryContext the delivery context id
     * @param contentIds collection of content ids, not {@code null} or empty
     * @return a collection of site items for the server, never {@code null}
     */
    Collection<IPSSiteItem> findServerItemsByIds(IPSGuid serverId, int deliveryContext, Collection<Integer> contentIds);

    void initPublishingStatus(long statusid, Date start, IPSGuid edition) throws PSNotFoundException;

    void finishedPublishingStatus(long statusid, Date end, IPSPubStatus.EndingState endingStatus);

    IPSPubStatus updateCounts(long statusid);

    List<IPSEditionTaskDef> loadEditionTasks(IPSGuid editionid);

    IPSEditionTaskLog createEditionTaskLog();

    /**
     * Create an edition task definition - compatibility helper for callers.
     * Implementations should provide the concrete behavior.
     *
     * @return a new {@link IPSEditionTaskDef}
     */
    default IPSEditionTaskDef createEditionTask() {
        return createEditionTaskImpl();
    }

    /**
     * Internal implementation for creating an edition task definition.
     */
    IPSEditionTaskDef createEditionTaskImpl();

    /**
     * Purge the job log for the specified job id. Default no-op for
     * compatibility; implementations may provide concrete behavior.
     *
     * @param jobId the job id to purge
     */
    default void purgeJobLog(Long jobId) {
        // no-op default for compatibility
    }

    /**
     * Delete all site items for the specified site. Default no-op for
     * compatibility; implementations may provide concrete behavior.
     *
     * @param siteGuid the site GUID
     */
    default void deleteSiteItems(IPSGuid siteGuid) {
        // no-op default for compatibility
    }

    /**
     * Returns the server id string for the current server. Default returns
     * <code>null</code> for compatibility; implementations should provide
     * a concrete value where available.
     *
     * @return server identifier string or <code>null</code>
     */
    default String getServerId() {
        return null;
    }

    void saveEditionTaskLog(IPSEditionTaskLog log);

    void cancelUnfinishedJobItems(long jobId);

    List<IPSPubItemStatus> findPubItemStatusForJob(long jobid);

    /**
     * Compatibility method returning an Iterable for legacy callers.
     */
    default Iterable<IPSPubItemStatus> findPubItemStatusForJobIterable(long jobid) {
        return findPubItemStatusForJob(jobid);
    }

    List<Long> findReferenceIdsToUnpublishByServer(IPSGuid serverId, String flags);

    List<IPSPubItemStatus> findPubItemStatusForReferenceIds(List<Long> refs);

    /**
     * Update published item dates for items produced by a given job. Added for legacy compatibility.
     */
    void updateItemPubDateByJob(long jobId, Date date);

    /**
     * Construct an assembly URL. Kept as a service method for backward compatibility with many callers.
     */
    String constructAssemblyUrl(String host, int port, String protocol,
            IPSGuid siteguid, IPSGuid contentid, IPSGuid folderguid,
            IPSAssemblyTemplate template, IPSItemFilter filter, int context,
            boolean publish);

    /**
     * Find all items for the given content types, including their parents.
     * @param ctypeids content type GUIDs
     * @return collection of item ids
     */
    Collection<Integer> getContentTypeItems(Collection<IPSGuid> ctypeids);

    /**
     * Find items that have changed since last publish for a site and delivery context.
     */
    Collection<Integer> findItemsSinceLastPublish(IPSGuid siteId, int deliveryContext, Collection<Integer> cids);

    /**
     * Touch active assembly parents for the supplied content ids, returning those changed.
     */
    Collection<Integer> touchActiveAssemblyParents(Collection<Integer> cids);

    /**
     * Touch active assembly parents by content GUIDs.
     */
    Collection<Integer> touchActiveAssemblyParentsByGuids(Collection<IPSGuid> cids);

    /**
     * Touch specified items and their active assembly parents.
     */
    Collection<Integer> touchItemsAndActiveAssemblyParents(Collection<Integer> cids);

    /**
     * Find an edition by name. Backward compatibility convenience method.
     */
    IPSEdition findEditionByName(String name);

    /**
     * Save a edition task definition.
     */
    void saveEditionTask(IPSEditionTaskDef task);

    /**
     * Delete a edition task definition.
     */
    void deleteEditionTask(IPSEditionTaskDef task);

    /**
     * Delete an edition and its associated content lists and tasks.
     */
    void deleteEdition(IPSEdition edition);

    List<PSSiteItem> findSiteItemsForReferenceIds(List<Long> refs);

    Object[] findUnpublishInfoForAssemblyItem(IPSGuid contentId, IPSGuid contextId, IPSGuid templateId, IPSGuid siteId, Long serverId, String targetPath);

    /**
     * Find publishing job IDs that have expired before the provided date.
     * @param beforeDate the cutoff date for expiration, not {@code null}
     * @return list of expired job ids, never {@code null}
     */
    List<Long> findExpiredJobs(Date beforeDate);

    /**
     * Find publishing job IDs that are expired or marked hidden before the provided date.
     * @param beforeDate the cutoff date for expiration, not {@code null}
     * @return list of expired or hidden job ids, never {@code null}
     */
    List<Long> findExpiredAndHiddenJobs(Date beforeDate);

    // Additional methods may be added as needed for compatibility
}

