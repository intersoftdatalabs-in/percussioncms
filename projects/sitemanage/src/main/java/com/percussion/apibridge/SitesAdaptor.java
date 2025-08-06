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

package com.percussion.apibridge;

import com.percussion.cms.IPSConstants;
import com.percussion.rest.sites.ISiteAdaptor;
import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.SiteList;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * Adaptor for managing sites in Percussion CMS.
 */
@PSSiteManageBean
@Lazy
public class SitesAdaptor implements ISiteAdaptor {
    private static final Logger log = LogManager.getLogger(IPSConstants.API_LOG);

    @Autowired
    private IPSPublishingWs publishingWs;

    @Autowired
    private IPSSiteDataService siteDataService;

    @Autowired
    private IPSSiteSectionService siteSectionService;

    /***
     * Default constructor.
     */
    public SitesAdaptor() {
        // No-op constructor for dependency injection.
    }

    /***
     * Find all sites.
     * @return SiteList
     */
    @Override
    public SiteList findAllSites() {
        var sites = siteDataService.findAll();
        return ApiUtils.convertSiteSummaryList(sites);
    }

    /***
     * Save a site.
     * @param site The site to save.
     */
    @Override
    public void saveSite(Site site) {
        // Not yet implemented
    }

    /***
     * Find site by name.
     * @param name The site name.
     * @return Site or null if not found.
     */
    @Override
    public Site findByName(String name) {
        // Not yet implemented
        return null;
    }

    /***
     * Find site by GUID.
     * @param guid The site GUID.
     * @return Site or null if not found.
     */
    @Override
    public Site findByGuid(String guid) {
        // Not yet implemented
        return null;
    }

    /***
     * Delete the site.
     * @param site The site to delete.
     */
    @Override
    public void deleteSite(Site site) {
        // Not yet implemented
    }

    /***
     * Create a new Site.
     * @return The created site.
     */
    @Override
    public Site createSite() {
        // Not yet implemented
        return null;
    }
}
