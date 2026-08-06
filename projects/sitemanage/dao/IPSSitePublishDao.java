// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 * ...existing code...
 */

package com.percussion.sitemanage.dao;

import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSitePublishProperties;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSitePublishService;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;

/**
 * Data access object for site publishing operations.
 * Sunny Sal says: "Publishing is like a Bollywood blockbuster—lots of moving parts!"
 */
public interface IPSSitePublishDao {

    String FULL = "FULL";
    String FULL_SITE = "FULL_SITE";
    String FULL_ASSET = "FULL_ASSET";
    String STAGING_SITE = "STAGING_SITE";
    String STAGING_ASSET = "STAGING_ASSET";

    /**
     * Finds all site summaries.
     *
     * @return a list of all site summaries, never {@code null}.
     */
    List<PSSiteSummary> findAllSummaries();

    /**
     * Finds a site summary by legacy site ID.
     *
     * @param id the legacy site ID, not {@code null}.
     * @param isValidate {@code true} to validate the site contains a "category" folder.
     * @return the site summary, or {@code null} if not found.
     */
    PSSiteSummary findByLegacySiteId(String id, boolean isValidate);

    /**
     * Finds a site summary by name.
     *
     * @param name the site name, not blank.
     * @return the site summary, or {@code null} if not found.
     * @throws IPSGenericDao.LoadException if an error occurs loading the site.
     */
    PSSiteSummary findSummary(String name) throws IPSGenericDao.LoadException;

    /**
     * Updates the specified site and its related edition/content-list/pubservers with the
     * new name and description.
     *
     * @param site the existing site, not {@code null}.
     * @param newName the new name of the site, not blank.
     * @param newDescription the new description of the site, may be blank.
     * @return {@code true} if a pubserver was modified as a result of the change, {@code false} otherwise.
     * @throws PSNotFoundException if the site is not found.
     */
    boolean updateSite(IPSSite site, String newName, String newDescription) throws PSNotFoundException;

    /**
     * Updates the specified site with the given publishing properties and updates
     * the content-list with the user-specified delivery type.
     *
     * @param site the existing site, not {@code null}.
     * @param publishProps publishing properties to update, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void updateSitePublishProperties(IPSSite site, PSSitePublishProperties publishProps) throws PSNotFoundException;

    /**
     * Creates a new site with the given name.
     *
     * @param siteName the site name, not blank.
     * @return the created site, never {@code null}.
     * @throws PSNotFoundException if the site cannot be created.
     */
    IPSSite createSite(String siteName) throws PSNotFoundException;

    /**
     * Finds an edition by publishing server ID and publish type.
     *
     * @param pubServerId the publishing server GUID, not {@code null}.
     * @param pubType the publish type, not {@code null}.
     * @return the edition, or {@code null} if not found.
     */
    IPSEdition findEdition(IPSGuid pubServerId, IPSSitePublishService.PubType pubType);

    /**
     * Gets the publishing root path for the given base path and site name.
     *
     * @param basePath the base path, not {@code null}.
     * @param siteName the site name, not blank.
     * @return the publishing root path, never {@code null}.
     */
    String getPublishingRoot(String basePath, String siteName);

    /**
     * Gets the publishing base path for the given site root and site name.
     *
     * @param siteRoot the site root, not blank.
     * @param siteName the site name, not blank.
     * @return the publishing base path, never {@code null}.
     */
    String getPublishingBase(String siteRoot, String siteName);

    /**
     * Gets the publishing delivery root for the given site name, server type, and delivery root path.
     *
     * @param siteName the site name, not blank.
     * @param publishServerType the publishing server type, not blank.
     * @param deliveryRootPath the delivery root path, not blank.
     * @return the publishing delivery root, never {@code null}.
     */
    String getPublishingDeliveryRoot(String siteName, String publishServerType, String deliveryRootPath);

    /**
     * Makes the publishing directory name for the given site name.
     *
     * @param siteName the site name, not blank.
     * @return the publishing directory name, never {@code null}.
     */
    String makePublishingDir(String siteName);

    /**
     * Converts a site to a summary.
     *
     * @param site the site, not {@code null}.
     * @param summary the summary to populate, not {@code null}.
     */
    void convertToSummary(IPSSite site, PSSiteSummary summary);

    /**
     * Creates publishing items for the given publishing server.
     *
     * @param site the site, not {@code null}.
     * @param pubServer the publishing server, not {@code null}.
     * @param isDefaultServer {@code true} if this is the default server.
     * @throws PSNotFoundException if the site or server is not found.
     */
    void createPublishingItemsForPubServer(IPSSite site, PSPubServer pubServer, boolean isDefaultServer) throws PSNotFoundException;

    /**
     * Gets the web server file system root.
     *
     * @return the web server file system root, never {@code null}.
     */
    String getWebServerFileSystemRoot();

    /**
     * Sets the web server file system root.
     *
     * @param fileSystemRoot the file system root, not blank.
     */
    void setWebServerFileSystemRoot(String fileSystemRoot);

    /**
     * Gets the web server port.
     *
     * @return the web server port, never {@code null}.
     */
    String getWebServerPort();

    /**
     * Sets the web server port.
     *
     * @param webServerPort the web server port, not blank.
     */
    void setWebServerPort(String webServerPort);

    /**
     * Updates server editions for the given site and servers.
     *
     * @param site the site, not {@code null}.
     * @param oldServer the old server, not {@code null}.
     * @param server the new server, not {@code null}.
     * @param isDefaultServer {@code true} if this is the default server.
     * @throws PSNotFoundException if the site or server is not found.
     */
    void updateServerEditions(IPSSite site, PSPubServer oldServer, PSPubServer server, boolean isDefaultServer) throws PSNotFoundException;

    /**
     * Deletes the site with the given name.
     *
     * @param name the site name, not blank.
     */
    void deleteSite(String name);

    /**
     * Gets the delivery type for the given site.
     *
     * @param site the site, not {@code null}.
     * @return the delivery type, never {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    String getSiteDeliveryType(IPSSite site) throws PSNotFoundException;

    /**
     * Gets the staging delivery type for the given site.
     *
     * @param site the site, not {@code null}.
     * @return the staging delivery type, never {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    String getStagingDeliveryType(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the publish-now infrastructure for the given site.
     *
     * @param site the site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addPublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the unpublish-now infrastructure for the given site.
     *
     * @param site the site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addUnpublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the staging publish-now infrastructure for the given site.
     *
     * @param site the site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addStagingPublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the staging unpublish-now infrastructure for the given site.
     *
     * @param site the site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addStagingUnpublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Sets the given publishing server as the default for the site.
     *
     * @param site the site, not {@code null}.
     * @param pubServer the publishing server, not {@code null}.
     * @throws PSNotFoundException if the site or server is not found.
     */
    void setPublishServerAsDefault(IPSSite site, PSPubServer pubServer) throws PSNotFoundException;

    /**
     * Deletes publishing items by publishing server.
     *
     * @param pubServer the publishing server, not {@code null}.
     * @throws PSNotFoundException if the server is not found.
     */
    void deletePublishingItemsByPubServer(PSPubServer pubServer) throws PSNotFoundException;

    /**
     * Saves the given site.
     *
     * @param site the site, not {@code null}.
     * @return {@code true} if the site is new, {@code false} otherwise.
     * @throws IPSPubServerService.PSPubServerServiceException if a publishing server error occurs.
     * @throws PSNotFoundException if the site is not found.
     */
    boolean saveSite(PSSite site) throws IPSPubServerService.PSPubServerServiceException, PSNotFoundException;
}
