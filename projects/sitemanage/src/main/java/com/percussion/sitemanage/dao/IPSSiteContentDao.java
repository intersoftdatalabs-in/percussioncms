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

package com.percussion.sitemanage.dao;

import com.percussion.fastforward.managednav.PSNavException;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSummary;

/**
 * Data access object for site content operations.
 * Sunny Sal says: "Content is king, but structure is the kingdom!"
 */
public interface IPSSiteContentDao {

    /**
     * The name of the home page item created automatically during site creation.
     */
    String HOME_PAGE_NAME = "index.html";

    /**
     * Creates related items (folders, homepage, navigation) for a new site.
     *
     * @param site the site, not {@code null}.
     */
    void createRelatedItems(PSSite site);

    /**
     * Copies content from the source site to the destination site.
     *
     * @param srcSite the source site, not {@code null}.
     * @param destSite the destination site, not {@code null}.
     */
    void copy(PSSite srcSite, PSSite destSite);

    /**
     * Gets the home page for the given site summary.
     *
     * @param site the site summary, not {@code null}.
     * @return the home page, or {@code null} if not found.
     * @throws PSNavException if a navigation error occurs.
     * @throws PSDataServiceException if a data service error occurs.
     */
    PSPage getHomePage(PSSiteSummary site) throws PSNavException, PSDataServiceException;

    /**
     * Gets the navigation title for the given site summary.
     *
     * @param siteSummary the site summary, not {@code null}.
     * @return the navigation title, never {@code null}.
     * @throws PSNavException if a navigation error occurs.
     * @throws PSDataServiceException if a data service error occurs.
     */
    String getNavTitle(PSSiteSummary siteSummary) throws PSNavException, PSDataServiceException;

    /**
     * Loads template information into the given site.
     *
     * @param site the site, not {@code null}.
     * @throws PSDataServiceException if a data service error occurs.
     */
    void loadTemplateInfo(PSSite site) throws PSDataServiceException;

    /**
     * Deletes all related items for the given site summary.
     *
     * @param summary the site summary, not {@code null}.
     * @throws IPSGenericDao.DeleteException if a delete error occurs.
     */
    void deleteRelatedItems(PSSiteSummary summary) throws IPSGenericDao.DeleteException;
}
