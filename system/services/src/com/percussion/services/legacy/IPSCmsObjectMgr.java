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
package com.percussion.services.legacy;

import java.util.Comparator;
import java.util.HashMap;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSCmsObject;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSConfig;
import com.percussion.design.objectstore.PSRole;
import com.percussion.i18n.PSLocale;
import com.percussion.i18n.PSLocaleFormat;
import com.percussion.server.PSPersistentPropertyMeta;
import com.percussion.server.PSPersistentProperty;
import com.percussion.server.PSUserSession;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.data.IPSIdentifiableItem;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.relationship.data.PSRelationshipConfigName;
import com.percussion.utils.exceptions.PSORMException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.workflow.IPSStatesContext;
import com.percussion.workflow.IPSWorkflowAppsContext;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Legacy CMS object manager with Java 11 enhancements for accessing legacy schema
 * via modern object mappings. Provides Optional-based safe access, Stream API support,
 * and enhanced type safety for system object management.
 *
 * This manager replaces the legacy sys_psxCms and XML DOM access patterns with
 * modern Java 11 approaches for better maintainability and performance.
 *
 * @author dougrand
 */
public interface IPSCmsObjectMgr extends IPSCmsContentSummaries {

    /**
     * Creates a new locale object with enhanced validation and Optional support.
     * The new object will have an assigned GUID but will not yet be persisted.
     *
     * @param languageString the language string that identifies this locale, never null or empty
     * @param displayName the display name of the locale, never null or empty
     * @return the locale with status initially set to inactive, never null
     * @throws IllegalArgumentException if parameters are null or empty
     */
    PSLocale createLocale(String languageString, String displayName);

    /**
     * Finds a locale object by the locale ID with Optional return type.
     *
     * @param id the locale ID
     * @return Optional containing the locale if found, empty otherwise
     */
    Optional<PSLocale> loadLocale(int id);

    /**
     * Finds a locale object by the language string with Optional return type.
     *
     * @param lang the language name to match, never null or empty
     * @return Optional containing the locale if found, empty otherwise
     * @throws IllegalArgumentException if lang is null or empty
     */
    Optional<PSLocale> findLocaleByLanguageString(String lang);

    /**
     * Updates content items' last modified date to current time with enhanced validation.
     * Sets the {@link PSComponentSummary#getContentLastModifiedDate()} property.
     *
     * @param ids the content IDs, never null, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    void touchItems(Collection<Integer> ids);

    /**
     * Get item entries for the given content ids sorted by the provided comparator.
     *
     * @param contentIds non-null list of content ids
     * @param comparator optional comparator used to order the results
     * @return non-null list of {@link com.percussion.services.legacy.IPSItemEntry}
     */
    List<IPSItemEntry> findItemEntries(List<Integer> contentIds, Comparator<IPSItemEntry> comparator);

    /**
     * Sets the post date for content items if not already set.
     * Updates {@link PSComponentSummary#getContentPostDate()} property to current time.
     *
     * @param ids the content IDs, never null, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    void setPostDate(Collection<Integer> ids);

    /**
     * Sets the publish date for the specified content items.
     * Updates {@link PSComponentSummary#getContentPublishDate()} property.
     *
     * @param ids the content IDs, never null, may be empty
     * @param date the publish date to set, may be null to clear
     * @throws IllegalArgumentException if ids is null
     */
    void setPublishDate(Collection<Integer> ids, java.util.Date date);

    /**
     * Update a named summary date field for the provided batch of content ids.
     * This method is intended for internal use in batched update operations.
     *
     * @param fieldName the database field name to update, not {@code null}
     * @param dateToSet the date to set, may be {@code null} to clear
     * @param ids the list of content ids to update, never {@code null} or empty
     * @param updateExisting true to update existing values, false to only set when empty
     */
    void updateSummaryDateFieldBatch(String fieldName, java.util.Date dateToSet, java.util.List<Integer> ids, boolean updateExisting);

    /**
     * Finds the first publish date for the given content item.
     *
     * @param contentId the content ID to search for, never null
     * @return Optional containing the first publish date if found, empty otherwise
     * @throws IllegalArgumentException if contentId is null
     */
    Optional<LocalDateTime> getFirstPublishDate(Integer contentId);

    /**
     * Clears the start date for the specified content items.
     *
     * @param ids the content IDs, never null, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    void clearStartDate(Collection<Integer> ids);

    /**
     * Clears the expiry date for the specified content items.
     *
     * @param ids the content IDs, never null, may be empty
     * @throws IllegalArgumentException if ids is null
     */
    void clearExpiryDate(Collection<Integer> ids);

    /**
     * Finds locale objects by status with Stream support.
     *
     * @param status the status value to filter by
     * @return Stream of locales matching the status, never null
     */
    Stream<PSLocale> findLocalesByStatus(int status);

    /**
     * Finds locales by name and/or label with nullable filtering and Stream support.
     *
     * @param lang the language string, may be null or empty to not filter by name
     * @param label the label, may be null or empty to not filter by label, SQL wildcards supported
     * @return Stream of matching locales, never null
     */
    Stream<PSLocale> findLocales(String lang, String label);

    /**
     * Finds all locale objects with Stream support.
     *
     * @return Stream of all locales, never null
     */
    Stream<PSLocale> findAllLocales();

    /**
     * Finds a locale format profile by BCP-47 language string.
     *
     * @param lang language string, never null or empty
     * @return Optional containing the format row if present
     */
    Optional<PSLocaleFormat> findLocaleFormatByLanguageString(String lang);

    /**
     * Finds all locale format profiles.
     *
     * @return Stream of all format rows, never null
     */
    Stream<PSLocaleFormat> findAllLocaleFormats();

    /**
     * Saves or updates a locale format profile.
     *
     * @param format never null
     * @throws PSORMException if persistence fails
     */
    void saveLocaleFormat(PSLocaleFormat format) throws PSORMException;

    /**
     * Saves or updates a locale object with enhanced error handling.
     *
     * @param locale the locale object, never null
     * @throws PSORMException if the data persistence layer encounters a problem
     * @throws PSMissingBeanConfigurationException if configuration is missing
     * @throws IllegalArgumentException if locale is null
     */
    void saveLocale(PSLocale locale) throws PSORMException, PSMissingBeanConfigurationException;

    /**
     * Removes the specified locale with enhanced validation.
     *
     * @param locale the locale object with non-zero ID, never null
     * @throws PSORMException if the data persistence layer encounters a problem
     * @throws IllegalArgumentException if locale is null or has zero ID
     */
    void deleteLocale(PSLocale locale) throws PSORMException;

    /**
     * Finds component summaries by content type with Stream support.
     *
     * @param contentType the content type being searched for
     * @return Stream of matching component summaries, never null
     * @throws PSORMException if there is a problem in the data persistence layer
     */
    Stream<PSComponentSummary> findComponentSummariesByType(long contentType) throws PSORMException;

    /**
     * Retrieve all available action menus.  This method was added to support
     * the legacy helpers used by sitemanage module.  The menus are represented by
     * {@link com.percussion.services.menus.PSActionMenu}, which lives in the
     * <code>services</code> package rather than the old cms.objectstore namespace.
     *
     * @return never {@code null}
     */
    List<PSActionMenu> findActionMenus();

    /**
     * Finds content IDs by content type with Stream support.
     *
     * @param contentType the content type being searched for
     * @return Stream of matching content IDs, never null
     * @throws PSORMException if there is a problem in the persistence layer
     */
    Stream<Integer> findContentIdsByType(long contentType) throws PSORMException;

    /**
     * Finds content IDs by workflow with Stream support.
     *
     * @param workflowId the workflow ID being searched for
     * @return Stream of matching content IDs, never null
     * @throws PSORMException if there is a problem in the persistence layer
     */
    Stream<Integer> findContentIdsByWorkflow(int workflowId) throws PSORMException;

    /**
     * Finds content IDs by workflow and state with Stream support.
     *
     * @param workflowId the workflow ID being searched for
     * @param stateId the state ID being searched for
     * @return Stream of matching content IDs, never null
     * @throws PSORMException if there is a problem in the persistence layer
     */
    Stream<Integer> findContentIdsByWorkflowStatus(int workflowId, int stateId) throws PSORMException;

    /**
     * Find a single item entry for the given content id.
     *
     * @param contentId the content id to look up
     * @return the item entry or {@code null} if not found
     */
    IPSItemEntry findItemEntry(int contentId);

    /**
     * Find content types for the specified content ids.
     *
     * @param contentIds the content ids, never null
     * @return set of content type ids, may be empty
     */
    Set<Long> findContentTypesForIds(Collection<? extends Object> contentIds);

    /**
     * Saves component summaries with enhanced validation.
     *
     * @param summaries component summaries to persist, never null
     * @throws PSORMException if there is a problem in the data persistence layer
     * @throws IllegalArgumentException if summaries is null
     */
    void saveComponentSummaries(List<PSComponentSummary> summaries) throws PSORMException;

    /**
     * Deletes component summaries with enhanced validation.
     *
     * @param summaries component summaries to delete, never null
     * @throws PSORMException if there is a problem in the data persistence layer
     * @throws IllegalArgumentException if summaries is null
     */
    void deleteComponentSummaries(List<PSComponentSummary> summaries) throws PSORMException;

    /**
     * Evicts component summaries from second-level cache with enhanced validation.
     *
     * @param ids component summary IDs to evict, never null
     * @throws IllegalArgumentException if ids is null
     */
    void evictComponentSummaries(List<Integer> ids);

    /**
     * Loads a workflow app context with Optional return type.
     *
     * @param workflowAppId the workflow app ID to load
     * @return Optional containing the workflow app context if found, empty otherwise
     */
    Optional<IPSWorkflowAppsContext> loadWorkflowAppContext(int workflowAppId);

    /**
     * Loads a workflow state with Optional return type.
     *
     * @param workflowAppId the workflow app ID
     * @param stateId the state ID
     * @return Optional containing the workflow state if found, empty otherwise
     */
    Optional<IPSStatesContext> loadWorkflowState(int workflowAppId, int stateId);

    /**
     * Filters items by publishable flag with enhanced generics and Stream support.
     *
     * @param <T> the type of identifiable items
     * @param items items to filter, never null
     * @param flags publishable flags to match, never null
     * @return Stream of items that pass the filter, never null
     * @throws PSORMException if there is a problem in the data persistence layer
     * @throws IllegalArgumentException if items or flags is null
     */
    <T extends IPSIdentifiableItem> Stream<T> filterItemsByPublishableFlag(
            List<T> items, List<String> flags) throws PSORMException;

    /**
     * Handles data eviction from second-level cache with enhanced type safety.
     *
     * @param clazz the class involved in persistence, never null
     * @param id the serializable primary key, may be null to evict all instances
     * @throws PSORMException if there is a problem in the persistence layer
     * @throws IllegalArgumentException if clazz is null
     */
    void handleDataEviction(Class<?> clazz, Serializable id) throws PSORMException;

    /**
     * Finds all configurations with Stream support.
     *
     * @return Stream of configuration objects, never null
     * @throws PSCmsException if error occurred during the lookup process
     */
    Stream<PSConfig> findAllConfigs() throws PSCmsException;

    /**
     * Finds configuration by name with Optional return type.
     *
     * @param name the configuration name, never null or empty
     * @return Optional containing the configuration if found, empty otherwise
     * @throws PSCmsException if error occurred during the lookup process
     * @throws IllegalArgumentException if name is null or empty
     */
    Optional<PSConfig> findConfig(String name) throws PSCmsException;

    /**
     * Saves the specified configuration with enhanced validation.
     *
     * @param config the configuration to save, never null
     * @throws PSCmsException if failed to save the config
     * @throws IllegalArgumentException if config is null
     */
    void saveConfig(PSConfig config) throws PSCmsException;

    /**
     * Flushes the second level Hibernate cache.
     * Use with caution - only appropriate when external database modifications occur.
     */
    void flushSecondLevelCache();

    /**
     * Finds all relationship configuration names with Stream support.
     *
     * @return Stream of relationship config names, never null
     */
    Stream<PSRelationshipConfigName> findAllRelationshipConfigNames();

    /**
     * Finds relationship configuration names by name pattern with Stream support.
     *
     * @param name the name pattern with optional wildcards, never null or empty
     * @return Stream of matching relationship config names, never null
     * @throws IllegalArgumentException if name is null or empty
     */
    Stream<PSRelationshipConfigName> findRelationshipConfigNames(String name);

    /**
     * Finds backend roles by name with nullable filtering and Stream support.
     *
     * @param name the role name pattern, may be null or empty to find all roles, asterisk wildcards supported
     * @return Stream of matching roles, never null
     */
    Stream<PSRole> findRolesByName(String name);

    /**
     * Finds public or current GUIDs for the given content IDs with Stream support.
     *
     * @param ids the input content IDs, never null or empty
     * @return Stream of GUIDs in the same order as input IDs, never null
     * @throws IllegalArgumentException if ids is null or empty
     */
    Stream<IPSGuid> findPublicOrCurrentGuids(List<Integer> ids);

    /**
     * Loads a CMS object by object type with Optional return type.
     *
     * @param objectType the object type to search for
     * @return Optional containing the CMS object if found, empty otherwise
     */
    Optional<PSCmsObject> loadCmsObject(int objectType);

    /**
     * Finds all CMS objects with Stream support.
     *
     * @return Stream of all CMS objects, never null
     */
    Stream<PSCmsObject> findAllCmsObjects();

    /**
     * Finds all persistent property metadata with Stream support.
     *
     * @return Stream of persistent property metadata, never null
     */
    Stream<PSPersistentPropertyMeta> findAllPersistentMeta();

    /**
     * Saves persistent property metadata with enhanced validation and Stream support.
     *
     * @param metaList the metadata list to save, never null
     * @return Stream of saved metadata, never null
     * @throws IllegalArgumentException if metaList is null
     */
    Stream<PSPersistentPropertyMeta> saveAllPersistentMeta(List<PSPersistentPropertyMeta> metaList);

    /**
     * Deletes persistent property metadata with enhanced validation.
     *
     * @param metaList the metadata list to delete, never null
     * @throws IllegalArgumentException if metaList is null
     */
    void deleteAllPersistentMeta(List<PSPersistentPropertyMeta> metaList);

    /**
     * Finds persistent properties by user name.
     *
     * @param userName the username to search for, not null
     * @return List of persistent properties for the user, never null (may be empty)
     */
    List<PSPersistentProperty> findPersistentPropertiesByName(String userName);

    /**
     * Saves a single persistent property metadata object.
     *
     * @param meta the metadata to save, not null
     * @return the saved metadata object
     * @throws IllegalArgumentException if meta is null
     */
    PSPersistentPropertyMeta savePersistentPropertyMeta(PSPersistentPropertyMeta meta);

    /**
     * Saves a single persistent property object.
     *
     * @param property the property to save, not null
     * @throws IllegalArgumentException if property is null
     */
    void savePersistentProperty(PSPersistentProperty property);

    /**
     * Deletes a single persistent property.
     *
     * @param property the property to delete, not null
     * @throws IllegalArgumentException if property is null
     */
    void deletePersistentProperty(PSPersistentProperty property);

    /**
     * Updates a single persistent property (merge existing row).
     *
     * <p>Must live on this interface (not only the concrete manager) so callers can use the
     * Spring-proxied bean without casting to {@code PSCmsObjectMgr}, which fails under JDK dynamic
     * proxies used for {@code @Transactional}.
     *
     * @param property the property to update, not null
     * @throws IllegalArgumentException if property is null
     */
    void updatePersistentProperty(PSPersistentProperty property);

    /**
     * Changes the workflow for a single item.
     *
     * @param itemId the item's content ID, not null
     * @param workflowId the workflow ID to assign, not null
     * @param stateNames list of workflow state names, may be null or empty
     * @throws PSORMException if an ORM error occurs
     */
    void changeWorkflowForItem(int itemId, int workflowId, List<String> stateNames) throws PSORMException;

    /**
     * Convenience method to update workflow for a batch of items. Delegates to
     * {@link #changeWorkflowForItem(int, int, List)} for each id.
     *
     * @param itemIds collection of item IDs, never {@code null}
     * @param workflowId the workflow id
     * @param stateNames names of valid states, may be {@code null}
     * @throws PSORMException if any underlying operation fails
     */
    default void changeWorkflowForItems(Collection<Integer> itemIds, int workflowId, List<String> stateNames) throws PSORMException {
        if (itemIds != null) {
            for (Integer id : itemIds) {
                changeWorkflowForItem(id, workflowId, stateNames);
            }
        }
    }

    /**
     * Force checkin content for all users in the provided session map.
     * This method is used during user session cleanup to release checked-out content.
     *
     * @param usersMap map of username to PSUserSession, never null
     * @throws IllegalArgumentException if usersMap is null
     */
    void forceCheckinUsers(HashMap<String, PSUserSession> usersMap);
}
