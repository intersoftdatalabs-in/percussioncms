// REFACTORED: CP-JAVA11
/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
 * ...existing code...
 */
package com.percussion.sitemanage.dao;

import com.percussion.pubserver.IPSPubServerService;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSitePublishProperties;
import com.percussion.sitemanage.data.PSSiteSummary;

import java.util.List;

/**
 * Data access object for Percussion CMS sites.
 * Sunny Sal says: "A good DAO is like a good chai—keeps things running smoothly!"
 */
public interface IPSiteDao extends IPSGenericDao<PSSite, String> {

    /**
     * Finds a site summary by the site name.
     *
     * @param name the site name, not blank.
     * @return the site summary, or {@code null} if not found.
     */
    PSSiteSummary findByName(String name);

    /**
     * Finds a site summary by its ID.
     *
     * @param id the site ID, not blank.
     * @return the site summary, or {@code null} if not found.
     * @throws LoadException if an error occurs loading the site.
     */
    PSSiteSummary findSummary(String id) throws LoadException;

    /**
     * Finds all site summaries.
     *
     * @return a list of all site summaries, never {@code null}.
     */
    List<PSSiteSummary> findAllSummaries();

    /**
     * Finds the site summary by the legacy ID.
     *
     * @param id the legacy ID of the site, not {@code null}.
     * @param isValidate {@code true} to validate the site contains a "category" folder;
     *                   {@code false} to skip validation (e.g., for preview or publishing).
     * @return the site summary, or {@code null} if not found.
     */
    PSSiteSummary findByLegacySiteId(String id, boolean isValidate);

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
     * Gets the delivery type for the specified site.
     *
     * @param site the existing site, not {@code null}.
     * @return the delivery type, never {@code null} or empty.
     * @throws PSNotFoundException if the site is not found.
     */
    String getSiteDeliveryType(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the publish-now infrastructure (edition, content list) for the specified site.
     * Assumes publish-now support does not exist for the site.
     *
     * @param site the existing site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addPublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Adds the unpublish-now infrastructure (edition, content list) for the specified site.
     * Assumes unpublish-now support does not exist for the site.
     *
     * @param site the existing site, not {@code null}.
     * @throws PSNotFoundException if the site is not found.
     */
    void addUnpublishNow(IPSSite site) throws PSNotFoundException;

    /**
     * Creates a site with content copied from an existing site.
     * The result is a new site visible in the Finder, including a copy of all content
     * from the original site (templates are not included). The site will have a default
     * publishing configuration.
     *
     * @param origId the ID of the original site, not blank.
     * @param newName the name of the new site, not blank.
     * @return the newly created site, never {@code null}.
     * @throws PSDataServiceException if a data service error occurs.
     * @throws IPSPubServerService.PSPubServerServiceException if a publishing server error occurs.
     * @throws PSNotFoundException if the original site is not found.
     */
    PSSite createSiteWithContent(String origId, String newName)
            throws PSDataServiceException, IPSPubServerService.PSPubServerServiceException, PSNotFoundException;

    /**
     * Finds the parent site of the specified path.
     *
     * @param path never blank.
     * @return summary of the parent site, or {@code null} if not found.
     */
    PSSiteSummary findByPath(String path);
}
