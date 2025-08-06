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
package com.percussion.services.sitemgr;

import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.memory.IPSCacheAccess;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Modern site manager interface for comprehensive site and location scheme management
 * with Java 11 features. This service provides site CRUD operations, location scheme
 * management, publishing context handling, and content type publishing validation
 * with enhanced safety and performance patterns.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable operations</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Stream API for efficient site and scheme processing</li>
 * <li>CompletableFuture support for asynchronous operations</li>
 * <li>Functional interfaces for site filtering and processing</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Site lifecycle management with validation</li>
 * <li>Location scheme management for publishing paths</li>
 * <li>Publishing context management and validation</li>
 * <li>Content type to site publishing compatibility checking</li>
 * <li>Cache integration for improved performance</li>
 * </ul>
 */
public interface IPSSiteManager {

    // Site Management Operations

    /**
     * Create a new site instance with enhanced initialization.
     *
     * @return a new site instance, never {@code null}
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    IPSSite createSite();

    /**
     * Load all sites in modifiable state for editing operations.
     *
     * @return list of modifiable sites, never {@code null}, may be empty
     */
    List<IPSSite> loadSitesModifiable();

    /**
     * Get a stream of all modifiable sites for efficient processing.
     *
     * @return Stream of modifiable sites, never {@code null}
     */
    default Stream<IPSSite> streamSitesModifiable() {
        return loadSitesModifiable().stream();
    }

    /**
     * Load a modifiable site by ID with enhanced validation.
     *
     * @param siteid the site ID, not {@code null}
     * @return the modifiable site instance
     * @throws PSNotFoundException if the site is not found
     * @throws IllegalArgumentException if siteid is null
     */
    default IPSSite loadSiteModifiable(IPSGuid siteid) throws PSNotFoundException {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return loadSiteModifiableImpl(siteid);
    }

    /**
     * Internal implementation for loading modifiable site by ID.
     */
    IPSSite loadSiteModifiableImpl(IPSGuid siteid) throws PSNotFoundException;

    /**
     * Load a modifiable site by name with enhanced validation.
     *
     * @param siteName the site name, not {@code null} or empty
     * @return the modifiable site instance
     * @throws PSNotFoundException if the site is not found
     * @throws IllegalArgumentException if siteName is null or empty
     */
    default IPSSite loadSiteModifiable(String siteName) throws PSNotFoundException {
        Objects.requireNonNull(siteName, "siteName cannot be null");
        if (siteName.trim().isEmpty()) {
            throw new IllegalArgumentException("siteName cannot be empty");
        }
        return loadSiteModifiableImpl(siteName.trim());
    }

    /**
     * Internal implementation for loading modifiable site by name.
     */
    IPSSite loadSiteModifiableImpl(String siteName) throws PSNotFoundException;

    /**
     * Find a site from database safely, bypassing cache.
     *
     * @param siteid the site ID, not {@code null}
     * @return Optional containing the site if found, empty otherwise
     * @throws IllegalArgumentException if siteid is null
     */
    default Optional<IPSSite> findSiteFromDatabaseSafely(IPSGuid siteid) {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return Optional.ofNullable(findSiteFromDatabase(siteid));
    }

    /**
     * Find a site from database, bypassing cache.
     *
     * @param siteid the site ID, not {@code null}
     * @return the site instance, may be {@code null} if not found
     * @throws IllegalArgumentException if siteid is null
     */
    default IPSSite findSiteFromDatabase(IPSGuid siteid) {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return findSiteFromDatabaseImpl(siteid);
    }

    /**
     * Internal implementation for finding site from database.
     */
    IPSSite findSiteFromDatabaseImpl(IPSGuid siteid);

    /**
     * Load an unmodifiable site by ID safely.
     *
     * @param siteid the site ID, not {@code null}
     * @return Optional containing the unmodifiable site if found, empty otherwise
     * @throws IllegalArgumentException if siteid is null
     */
    default Optional<IPSSite> loadUnmodifiableSiteSafely(IPSGuid siteid) {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        try {
            return Optional.of(loadUnmodifiableSite(siteid));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Load an unmodifiable site by ID.
     *
     * @param siteid the site ID, not {@code null}
     * @return the unmodifiable site instance
     * @throws PSNotFoundException if the site is not found
     * @throws IllegalArgumentException if siteid is null
     */
    default IPSSite loadUnmodifiableSite(IPSGuid siteid) throws PSNotFoundException {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return loadUnmodifiableSiteImpl(siteid);
    }

    /**
     * Internal implementation for loading unmodifiable site.
     */
    IPSSite loadUnmodifiableSiteImpl(IPSGuid siteid) throws PSNotFoundException;

    /**
     * Find a site by ID safely (cached lookup).
     *
     * @param siteid the site ID, not {@code null}
     * @return Optional containing the site if found, empty otherwise
     * @throws IllegalArgumentException if siteid is null
     */
    default Optional<IPSSite> findSiteSafely(IPSGuid siteid) {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return Optional.ofNullable(findSite(siteid));
    }

    /**
     * Find a site by ID (cached lookup).
     *
     * @param siteid the site ID, not {@code null}
     * @return the site instance, may be {@code null} if not found
     * @throws IllegalArgumentException if siteid is null
     */
    default IPSSite findSite(IPSGuid siteid) {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return findSiteImpl(siteid);
    }

    /**
     * Internal implementation for finding site by ID.
     */
    IPSSite findSiteImpl(IPSGuid siteid);

    /**
     * Load a site by ID with exception handling.
     *
     * @param siteid the site ID, not {@code null}
     * @return the site instance
     * @throws PSNotFoundException if the site is not found
     * @throws IllegalArgumentException if siteid is null
     */
    default IPSSite loadSite(IPSGuid siteid) throws PSNotFoundException {
        Objects.requireNonNull(siteid, "siteid cannot be null");
        return loadSiteImpl(siteid);
    }

    /**
     * Internal implementation for loading site by ID.
     */
    IPSSite loadSiteImpl(IPSGuid siteid) throws PSNotFoundException;

    /**
     * Find all sites in the system.
     *
     * @return list of all sites, never {@code null}, may be empty
     */
    List<IPSSite> findAllSites();

    /**
     * Get a stream of all sites for efficient processing.
     *
     * @return Stream of all sites, never {@code null}
     */
    default Stream<IPSSite> streamAllSites() {
        return findAllSites().stream();
    }

    /**
     * Get all site ID to name mappings.
     *
     * @return map of site ID to site name, never {@code null}, may be empty
     */
    Map<IPSGuid, String> getAllSiteIdNames();

    /**
     * Find a site by name safely.
     *
     * @param sitename the site name, not {@code null} or empty
     * @return Optional containing the site if found, empty otherwise
     * @throws IllegalArgumentException if sitename is null or empty
     */
    default Optional<IPSSite> findSiteSafely(String sitename) {
        Objects.requireNonNull(sitename, "sitename cannot be null");
        if (sitename.trim().isEmpty()) {
            throw new IllegalArgumentException("sitename cannot be empty");
        }
        return Optional.ofNullable(findSite(sitename.trim()));
    }

    /**
     * Find a site by name.
     *
     * @param sitename the site name, not {@code null} or empty
     * @return the site instance, may be {@code null} if not found
     * @throws IllegalArgumentException if sitename is null or empty
     */
    default IPSSite findSite(String sitename) {
        Objects.requireNonNull(sitename, "sitename cannot be null");
        if (sitename.trim().isEmpty()) {
            throw new IllegalArgumentException("sitename cannot be empty");
        }
        return findSiteImpl(sitename.trim());
    }

    /**
     * Internal implementation for finding site by name.
     */
    IPSSite findSiteImpl(String sitename);

    /**
     * Load a site by name with exception handling and enhanced validation.
     *
     * @param sitename the site name, not {@code null} or empty
     * @return the site instance
     * @throws PSNotFoundException if the site is not found
     * @throws IllegalArgumentException if sitename is null or empty
     */
    default IPSSite loadSite(String sitename) throws PSNotFoundException {
        Objects.requireNonNull(sitename, "sitename cannot be null");
        if (sitename.trim().isEmpty()) {
            throw new IllegalArgumentException("sitename cannot be empty");
        }
        return loadSiteImpl(sitename.trim());
    }

    /**
     * Internal implementation for loading site by name.
     */
    IPSSite loadSiteImpl(String sitename) throws PSNotFoundException;

    /**
     * Find a site by name with legacy exception handling.
     *
     * @param sitename the site name, not {@code null} or empty
     * @return the site instance
     * @throws PSSiteManagerException if the site is not found or other error occurs
     * @deprecated Use {@link #findSiteSafely(String)} or {@link #loadSite(String)} instead
     */
    @Deprecated
    IPSSite findSiteByName(String sitename) throws PSSiteManagerException;

    /**
     * Save a site to the repository with enhanced validation.
     *
     * @param site the site to save, not {@code null}
     * @throws IllegalArgumentException if site is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void saveSite(IPSSite site) {
        Objects.requireNonNull(site, "site cannot be null");
        saveSiteImpl(site);
    }

    /**
     * Internal implementation for saving site.
     */
    void saveSiteImpl(IPSSite site);

    /**
     * Asynchronously save a site without blocking the calling thread.
     *
     * @param site the site to save, not {@code null}
     * @return CompletableFuture that completes when the save operation finishes
     * @throws IllegalArgumentException if site is null
     */
    default CompletableFuture<Void> saveSiteAsync(IPSSite site) {
        Objects.requireNonNull(site, "site cannot be null");
        return CompletableFuture.runAsync(() -> saveSite(site));
    }

    /**
     * Delete a site from the repository with enhanced validation.
     *
     * @param site the site to delete, not {@code null}
     * @throws IllegalArgumentException if site is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void deleteSite(IPSSite site) {
        Objects.requireNonNull(site, "site cannot be null");
        deleteSiteImpl(site);
    }

    /**
     * Internal implementation for deleting site.
     */
    void deleteSiteImpl(IPSSite site);

    /**
     * Filter sites using a predicate for advanced filtering.
     *
     * @param predicate the condition to test sites against, not {@code null}
     * @return a list of matching sites, never {@code null}, may be empty
     * @throws IllegalArgumentException if predicate is null
     */
    default List<IPSSite> findSitesWhere(Predicate<IPSSite> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return streamAllSites()
            .filter(predicate)
            .toList();
    }

    /**
     * Check if a site exists by ID.
     *
     * @param siteid the site ID to check, not {@code null}
     * @return {@code true} if the site exists, {@code false} otherwise
     * @throws IllegalArgumentException if siteid is null
     */
    default boolean siteExists(IPSGuid siteid) {
        return findSiteSafely(siteid).isPresent();
    }

    /**
     * Check if a site exists by name.
     *
     * @param sitename the site name to check, not {@code null} or empty
     * @return {@code true} if the site exists, {@code false} otherwise
     * @throws IllegalArgumentException if sitename is null or empty
     */
    default boolean siteExists(String sitename) {
        return findSiteSafely(sitename).isPresent();
    }

    // Location Scheme Management Operations

    /**
     * Create a new location scheme instance.
     *
     * @return a new location scheme instance, never {@code null}
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    IPSLocationScheme createScheme();

    /**
     * Load a location scheme by ID safely.
     *
     * @param schemeId the scheme ID, not {@code null}
     * @return Optional containing the scheme if found, empty otherwise
     * @throws IllegalArgumentException if schemeId is null
     */
    default Optional<IPSLocationScheme> loadSchemeSafely(IPSGuid schemeId) {
        Objects.requireNonNull(schemeId, "schemeId cannot be null");
        try {
            return Optional.of(loadScheme(schemeId));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Load a location scheme by ID.
     *
     * @param schemeId the scheme ID, not {@code null}
     * @return the location scheme instance
     * @throws PSNotFoundException if the scheme is not found
     * @throws IllegalArgumentException if schemeId is null
     */
    default IPSLocationScheme loadScheme(IPSGuid schemeId) throws PSNotFoundException {
        Objects.requireNonNull(schemeId, "schemeId cannot be null");
        return loadSchemeImpl(schemeId);
    }

    /**
     * Internal implementation for loading scheme by GUID.
     */
    IPSLocationScheme loadSchemeImpl(IPSGuid schemeId) throws PSNotFoundException;

    /**
     * Load a modifiable location scheme by ID with enhanced validation.
     *
     * @param schemeId the scheme ID, not {@code null}
     * @return the modifiable location scheme instance
     * @throws PSNotFoundException if the scheme is not found
     * @throws IllegalArgumentException if schemeId is null
     */
    default IPSLocationScheme loadSchemeModifiable(IPSGuid schemeId) throws PSNotFoundException {
        Objects.requireNonNull(schemeId, "schemeId cannot be null");
        return loadSchemeModifiableImpl(schemeId);
    }

    /**
     * Internal implementation for loading modifiable scheme.
     */
    IPSLocationScheme loadSchemeModifiableImpl(IPSGuid schemeId) throws PSNotFoundException;

    /**
     * Load a location scheme by legacy ID.
     *
     * @param schemeId the legacy scheme ID
     * @return the location scheme instance
     * @throws PSNotFoundException if the scheme is not found
     * @deprecated Use {@link #loadScheme(IPSGuid)} instead
     */
    @Deprecated
    IPSLocationScheme loadScheme(int schemeId) throws PSNotFoundException;

    /**
     * Find location schemes by assembly information with enhanced validation.
     *
     * @param template the assembly template, not {@code null}
     * @param context the publishing context, not {@code null}
     * @param contenttypeid the content type ID, not {@code null}
     * @return list of matching location schemes, never {@code null}, may be empty
     * @throws IllegalArgumentException if any parameter is null
     */
    default List<IPSLocationScheme> findSchemeByAssemblyInfo(
            IPSAssemblyTemplate template, IPSPublishingContext context, IPSGuid contenttypeid) {
        Objects.requireNonNull(template, "template cannot be null");
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(contenttypeid, "contenttypeid cannot be null");
        return findSchemeByAssemblyInfoImpl(template, context, contenttypeid);
    }

    /**
     * Internal implementation for finding schemes by assembly info.
     */
    List<IPSLocationScheme> findSchemeByAssemblyInfoImpl(
            IPSAssemblyTemplate template, IPSPublishingContext context, IPSGuid contenttypeid);

    /**
     * Find location schemes by assembly information using IDs with enhanced validation.
     *
     * @param templateid the template ID, not {@code null}
     * @param context the publishing context, not {@code null}
     * @param contenttypeid the content type ID, not {@code null}
     * @return list of matching location schemes, never {@code null}, may be empty
     * @throws IllegalArgumentException if any parameter is null
     */
    default List<IPSLocationScheme> findSchemeByAssemblyInfo(IPSGuid templateid,
                                                           IPSPublishingContext context, IPSGuid contenttypeid) {
        Objects.requireNonNull(templateid, "templateid cannot be null");
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(contenttypeid, "contenttypeid cannot be null");
        return findSchemeByAssemblyInfoImpl(templateid, context, contenttypeid);
    }

    /**
     * Internal implementation for finding schemes by assembly info with template ID.
     */
    List<IPSLocationScheme> findSchemeByAssemblyInfoImpl(IPSGuid templateid,
                                                        IPSPublishingContext context, IPSGuid contenttypeid);

    /**
     * Find location schemes by assembly information using all IDs with enhanced validation.
     *
     * @param templateid the template ID, not {@code null}
     * @param contextid the context ID, not {@code null}
     * @param contenttypeid the content type ID, not {@code null}
     * @return list of matching location schemes, never {@code null}, may be empty
     * @throws IllegalArgumentException if any parameter is null
     */
    default List<IPSLocationScheme> findSchemeByAssemblyInfo(IPSGuid templateid,
                                                           IPSGuid contextid, IPSGuid contenttypeid) {
        Objects.requireNonNull(templateid, "templateid cannot be null");
        Objects.requireNonNull(contextid, "contextid cannot be null");
        Objects.requireNonNull(contenttypeid, "contenttypeid cannot be null");
        return findSchemeByAssemblyInfoImpl(templateid, contextid, contenttypeid);
    }

    /**
     * Internal implementation for finding schemes by assembly info with all IDs.
     */
    List<IPSLocationScheme> findSchemeByAssemblyInfoImpl(IPSGuid templateid,
                                                        IPSGuid contextid, IPSGuid contenttypeid);

    /**
     * Save a location scheme to the repository with enhanced validation.
     *
     * @param scheme the scheme to save, not {@code null}
     * @throws IllegalArgumentException if scheme is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void saveScheme(IPSLocationScheme scheme) {
        Objects.requireNonNull(scheme, "scheme cannot be null");
        saveSchemeImpl(scheme);
    }

    /**
     * Internal implementation for saving scheme.
     */
    void saveSchemeImpl(IPSLocationScheme scheme);

    /**
     * Delete a location scheme from the repository with enhanced validation.
     *
     * @param scheme the scheme to delete, not {@code null}
     * @throws IllegalArgumentException if scheme is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void deleteScheme(IPSLocationScheme scheme) {
        Objects.requireNonNull(scheme, "scheme cannot be null");
        deleteSchemeImpl(scheme);
    }

    /**
     * Internal implementation for deleting scheme.
     */
    void deleteSchemeImpl(IPSLocationScheme scheme);

    // Publishing Context Management Operations

    /**
     * Load a publishing context by legacy ID.
     *
     * @param contextid the legacy context ID
     * @return the publishing context instance
     * @throws PSNotFoundException if the context is not found
     * @deprecated Use {@link #loadContext(IPSGuid)} instead
     */
    @Deprecated
    IPSPublishingContext loadContext(int contextid) throws PSNotFoundException;

    /**
     * Load a publishing context by ID safely.
     *
     * @param contextid the context ID, not {@code null}
     * @return Optional containing the context if found, empty otherwise
     * @throws IllegalArgumentException if contextid is null
     */
    default Optional<IPSPublishingContext> loadContextSafely(IPSGuid contextid) {
        Objects.requireNonNull(contextid, "contextid cannot be null");
        try {
            return Optional.of(loadContext(contextid));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Load a publishing context by ID with enhanced validation.
     *
     * @param contextid the context ID, not {@code null}
     * @return the publishing context instance
     * @throws PSNotFoundException if the context is not found
     * @throws IllegalArgumentException if contextid is null
     */
    default IPSPublishingContext loadContext(IPSGuid contextid) throws PSNotFoundException {
        Objects.requireNonNull(contextid, "contextid cannot be null");
        return loadContextImpl(contextid);
    }

    /**
     * Internal implementation for loading context by GUID.
     */
    IPSPublishingContext loadContextImpl(IPSGuid contextid) throws PSNotFoundException;

    /**
     * Load a modifiable publishing context by ID with enhanced validation.
     *
     * @param contextid the context ID, not {@code null}
     * @return the modifiable publishing context instance
     * @throws PSNotFoundException if the context is not found
     * @throws IllegalArgumentException if contextid is null
     */
    default IPSPublishingContext loadContextModifiable(IPSGuid contextid) throws PSNotFoundException {
        Objects.requireNonNull(contextid, "contextid cannot be null");
        return loadContextModifiableImpl(contextid);
    }

    /**
     * Internal implementation for loading modifiable context.
     */
    IPSPublishingContext loadContextModifiableImpl(IPSGuid contextid) throws PSNotFoundException;

    /**
     * Load a publishing context by name safely.
     *
     * @param contextname the context name, not {@code null} or empty
     * @return Optional containing the context if found, empty otherwise
     * @throws IllegalArgumentException if contextname is null or empty
     */
    default Optional<IPSPublishingContext> loadContextSafely(String contextname) {
        Objects.requireNonNull(contextname, "contextname cannot be null");
        if (contextname.trim().isEmpty()) {
            throw new IllegalArgumentException("contextname cannot be empty");
        }
        try {
            return Optional.of(loadContext(contextname.trim()));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Load a publishing context by name with enhanced validation.
     *
     * @param contextname the context name, not {@code null} or empty
     * @return the publishing context instance
     * @throws PSNotFoundException if the context is not found
     * @throws IllegalArgumentException if contextname is null or empty
     */
    default IPSPublishingContext loadContext(String contextname) throws PSNotFoundException {
        Objects.requireNonNull(contextname, "contextname cannot be null");
        if (contextname.trim().isEmpty()) {
            throw new IllegalArgumentException("contextname cannot be empty");
        }
        return loadContextImpl(contextname.trim());
    }

    /**
     * Internal implementation for loading context by name.
     */
    IPSPublishingContext loadContextImpl(String contextname) throws PSNotFoundException;

    /**
     * Find a publishing context by name with legacy exception handling.
     *
     * @param contextname the context name, not {@code null} or empty
     * @return the publishing context instance
     * @throws PSSiteManagerException if the context is not found or other error occurs
     * @deprecated Use {@link #loadContextSafely(String)} or {@link #loadContext(String)} instead
     */
    @Deprecated
    IPSPublishingContext findContextByName(String contextname) throws PSSiteManagerException;

    // Catalog Operations

    /**
     * Get supported catalog types.
     *
     * @return array of supported types, never {@code null}
     */
    PSTypeEnum[] getTypes();

    /**
     * Get catalog summaries by type with enhanced validation.
     *
     * @param type the catalog type, not {@code null}
     * @return list of catalog summaries, never {@code null}, may be empty
     * @throws PSNotFoundException if type is not found
     * @throws PSCatalogException if catalog error occurs
     * @throws IllegalArgumentException if type is null
     */
    default List<IPSCatalogSummary> getSummaries(PSTypeEnum type)
            throws PSNotFoundException, PSCatalogException {
        Objects.requireNonNull(type, "type cannot be null");
        return getSummariesImpl(type);
    }

    /**
     * Internal implementation for getting summaries.
     */
    List<IPSCatalogSummary> getSummariesImpl(PSTypeEnum type)
            throws PSNotFoundException, PSCatalogException;

    /**
     * Load catalog item by type and name with enhanced validation.
     *
     * @param type the catalog type, not {@code null}
     * @param item the item name, not {@code null} or empty
     * @throws PSCatalogException if catalog error occurs
     * @throws IllegalArgumentException if type is null or item is null/empty
     */
    default void loadByType(PSTypeEnum type, String item) throws PSCatalogException {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(item, "item cannot be null");
        if (item.trim().isEmpty()) {
            throw new IllegalArgumentException("item cannot be empty");
        }
        loadByTypeImpl(type, item.trim());
    }

    /**
     * Internal implementation for loading by type.
     */
    void loadByTypeImpl(PSTypeEnum type, String item) throws PSCatalogException;

    /**
     * Save catalog item by ID with enhanced validation.
     *
     * @param id the item ID, not {@code null}
     * @return the saved item identifier
     * @throws PSCatalogException if catalog error occurs
     * @throws IllegalArgumentException if id is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default String saveByType(IPSGuid id) throws PSCatalogException {
        Objects.requireNonNull(id, "id cannot be null");
        return saveByTypeImpl(id);
    }

    /**
     * Internal implementation for saving by type.
     */
    String saveByTypeImpl(IPSGuid id) throws PSCatalogException;

    // Publishing Path and Site Operations

    /**
     * Get the publish path for a site and folder with enhanced validation.
     *
     * @param siteId the site ID, not {@code null}
     * @param folderId the folder ID, not {@code null}
     * @return the publish path
     * @throws PSSiteManagerException if operation fails
     * @throws PSNotFoundException if site or folder not found
     * @throws IllegalArgumentException if siteId or folderId is null
     */
    default String getPublishPath(IPSGuid siteId, IPSGuid folderId)
            throws PSSiteManagerException, PSNotFoundException {
        Objects.requireNonNull(siteId, "siteId cannot be null");
        Objects.requireNonNull(folderId, "folderId cannot be null");
        return getPublishPathImpl(siteId, folderId);
    }

    /**
     * Internal implementation for getting publish path.
     */
    String getPublishPathImpl(IPSGuid siteId, IPSGuid folderId)
            throws PSSiteManagerException, PSNotFoundException;

    /**
     * Get the site folder ID for content with enhanced validation.
     *
     * @param siteId the site ID, not {@code null}
     * @param contentId the content ID, not {@code null}
     * @return the site folder ID
     * @throws PSSiteManagerException if operation fails
     * @throws PSNotFoundException if site or content not found
     * @throws IllegalArgumentException if siteId or contentId is null
     */
    default IPSGuid getSiteFolderId(IPSGuid siteId, IPSGuid contentId)
            throws PSSiteManagerException, PSNotFoundException {
        Objects.requireNonNull(siteId, "siteId cannot be null");
        Objects.requireNonNull(contentId, "contentId cannot be null");
        return getSiteFolderIdImpl(siteId, contentId);
    }

    /**
     * Internal implementation for getting site folder ID.
     */
    IPSGuid getSiteFolderIdImpl(IPSGuid siteId, IPSGuid contentId)
            throws PSSiteManagerException, PSNotFoundException;

    /**
     * Get all sites where an item is published with enhanced validation.
     *
     * @param contentId the content ID, not {@code null}
     * @return list of sites containing the item, never {@code null}, may be empty
     * @throws IllegalArgumentException if contentId is null
     */
    default List<IPSSite> getItemSites(IPSGuid contentId) {
        Objects.requireNonNull(contentId, "contentId cannot be null");
        return getItemSitesImpl(contentId);
    }

    /**
     * Internal implementation for getting item sites.
     */
    List<IPSSite> getItemSitesImpl(IPSGuid contentId);

    /**
     * Get a stream of sites containing a specific item for efficient processing.
     *
     * @param contentId the content ID, not {@code null}
     * @return Stream of sites, never {@code null}
     * @throws IllegalArgumentException if contentId is null
     */
    default Stream<IPSSite> streamItemSites(IPSGuid contentId) {
        return getItemSites(contentId).stream();
    }

    /**
     * Check if a content type is publishable to a site with enhanced validation.
     *
     * @param contentTypeId the content type ID, not {@code null}
     * @param siteId the site ID, not {@code null}
     * @return {@code true} if publishable, {@code false} otherwise
     * @throws PSSiteManagerException if operation fails
     * @throws PSNotFoundException if content type or site not found
     * @throws IllegalArgumentException if contentTypeId or siteId is null
     */
    default boolean isContentTypePublishableToSite(IPSGuid contentTypeId, IPSGuid siteId)
            throws PSSiteManagerException, PSNotFoundException {
        Objects.requireNonNull(contentTypeId, "contentTypeId cannot be null");
        Objects.requireNonNull(siteId, "siteId cannot be null");
        return isContentTypePublishableToSiteImpl(contentTypeId, siteId);
    }

    /**
     * Internal implementation for content type publishability check.
     */
    boolean isContentTypePublishableToSiteImpl(IPSGuid contentTypeId, IPSGuid siteId)
            throws PSSiteManagerException, PSNotFoundException;

    // Cache and Notification Management

    /**
     * Get the cache access service.
     *
     * @return the cache access service, may be {@code null}
     */
    IPSCacheAccess getCache();

    /**
     * Set the cache access service.
     *
     * @param cache the cache access service to set
     */
    void setCache(IPSCacheAccess cache);

    /**
     * Get the notification service.
     *
     * @return the notification service, may be {@code null}
     */
    IPSNotificationService getNotifications();

    /**
     * Set the notification service.
     *
     * @param notifications the notification service to set
     */
    void setNotifications(IPSNotificationService notifications);

    // Additional Query Operations

    /**
     * Find all publishing contexts in the system.
     *
     * @return list of all contexts, never {@code null}, may be empty
     * @throws PSNotFoundException if operation fails
     */
    List<IPSPublishingContext> findAllContexts() throws PSNotFoundException;

    /**
     * Get a stream of all publishing contexts for efficient processing.
     *
     * @return Stream of all contexts, never {@code null}
     * @throws PSNotFoundException if operation fails
     */
    default Stream<IPSPublishingContext> streamAllContexts() throws PSNotFoundException {
        return findAllContexts().stream();
    }

    /**
     * Find all location schemes in the system.
     *
     * @return list of all schemes, never {@code null}, may be empty
     */
    List<IPSLocationScheme> findAllSchemes();

    /**
     * Get a stream of all location schemes for efficient processing.
     *
     * @return Stream of all schemes, never {@code null}
     */
    default Stream<IPSLocationScheme> streamAllSchemes() {
        return findAllSchemes().stream();
    }

    /**
     * Find distinct site variable names across all sites.
     *
     * @return list of distinct variable names, never {@code null}, may be empty
     */
    List<String> findDistinctSiteVariableNames();

    /**
     * Delete a publishing context from the repository with enhanced validation.
     *
     * @param context the context to delete, not {@code null}
     * @throws IllegalArgumentException if context is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void deleteContext(IPSPublishingContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        deleteContextImpl(context);
    }

    /**
     * Internal implementation for deleting context.
     */
    void deleteContextImpl(IPSPublishingContext context);

    /**
     * Find location schemes by context ID with enhanced validation.
     *
     * @param contextid the context ID, not {@code null}
     * @return list of matching schemes, never {@code null}, may be empty
     * @throws IllegalArgumentException if contextid is null
     */
    default List<IPSLocationScheme> findSchemesByContextId(IPSGuid contextid) {
        Objects.requireNonNull(contextid, "contextid cannot be null");
        return findSchemesByContextIdImpl(contextid);
    }

    /**
     * Internal implementation for finding schemes by context ID.
     */
    List<IPSLocationScheme> findSchemesByContextIdImpl(IPSGuid contextid);

    /**
     * Save a publishing context to the repository with enhanced validation.
     *
     * @param context the context to save, not {@code null}
     * @throws IllegalArgumentException if context is null
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    default void saveContext(IPSPublishingContext context) {
        Objects.requireNonNull(context, "context cannot be null");
        saveContextImpl(context);
    }

    /**
     * Internal implementation for saving context.
     */
    void saveContextImpl(IPSPublishingContext context);

    /**
     * Create a new publishing context instance.
     *
     * @return a new publishing context instance, never {@code null}
     */
    @Transactional(noRollbackFor = PSNotFoundException.class)
    IPSPublishingContext createContext();

    /**
     * Get mapping of context IDs to names.
     *
     * @return map of context ID to context name, never {@code null}, may be empty
     */
    Map<Integer, String> getContextNameMap();

    /**
     * Find site template associations mapping.
     *
     * @return map of site template pairs to associated GUIDs, never {@code null}, may be empty
     */
    Map<PSPair<IPSGuid, String>, Collection<IPSGuid>> findSiteTemplatesAssociations();

    // Utility Methods

    /**
     * Count the total number of sites in the system.
     *
     * @return the total number of sites
     */
    default long getSiteCount() {
        return streamAllSites().count();
    }

    /**
     * Count the total number of location schemes in the system.
     *
     * @return the total number of schemes
     */
    default long getSchemeCount() {
        return streamAllSchemes().count();
    }

    /**
     * Get site management statistics and information.
     *
     * @return a string representation of site management statistics
     */
    default String getSiteManagerStatistics() {
        var siteCount = getSiteCount();
        var schemeCount = getSchemeCount();
        return String.format("SiteManager Statistics: %d sites, %d location schemes",
            siteCount, schemeCount);
    }

    /**
     * Modern key class for location scheme mapping with enhanced equals/hashCode.
     */
    final class LocationSchemeKey implements Serializable {
        private static final long serialVersionUID = 1L;

        private final IPSGuid templateId;
        private final IPSGuid contextId;
        private final IPSGuid contentTypeId;

        /**
         * Constructs a location scheme key with enhanced validation.
         *
         * @param templateId template ID, not {@code null}
         * @param contextId context ID, not {@code null}
         * @param contentTypeId content type ID, not {@code null}
         * @throws IllegalArgumentException if any parameter is null
         */
        public LocationSchemeKey(IPSGuid templateId, IPSGuid contextId, IPSGuid contentTypeId) {
            this.templateId = Objects.requireNonNull(templateId, "templateId cannot be null");
            this.contextId = Objects.requireNonNull(contextId, "contextId cannot be null");
            this.contentTypeId = Objects.requireNonNull(contentTypeId, "contentTypeId cannot be null");
        }

        /**
         * Get the template ID.
         * @return the template ID, never {@code null}
         */
        public IPSGuid getTemplateId() {
            return templateId;
        }

        /**
         * Get the context ID.
         * @return the context ID, never {@code null}
         */
        public IPSGuid getContextId() {
            return contextId;
        }

        /**
         * Get the content type ID.
         * @return the content type ID, never {@code null}
         */
        public IPSGuid getContentTypeId() {
            return contentTypeId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            var that = (LocationSchemeKey) obj;
            return Objects.equals(templateId, that.templateId) &&
                   Objects.equals(contextId, that.contextId) &&
                   Objects.equals(contentTypeId, that.contentTypeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(templateId, contextId, contentTypeId);
        }

        @Override
        public String toString() {
            return String.format("LocationSchemeKey{templateId=%s, contextId=%s, contentTypeId=%s}",
                templateId, contextId, contentTypeId);
        }
    }
}
