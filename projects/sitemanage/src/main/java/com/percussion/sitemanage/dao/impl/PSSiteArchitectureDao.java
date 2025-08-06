// REFACTORED: CP-JAVA11
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
package com.percussion.sitemanage.dao.impl;

import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.data.PSDataItemSummary;
import com.percussion.share.service.IPSDataItemSummaryService;
import com.percussion.share.service.IPSDataService;
import com.percussion.sitemanage.dao.IPSSiteArchitectureDao;
import com.percussion.sitemanage.data.PSSiteArchitecture;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.publishing.IPSPublishingWs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DAO for retrieving site architecture information.
 * Sunny Sal says: "Architecture is not just for buildings, yaar!"
 */
@Component("siteArchitectureDao")
@Lazy
public class PSSiteArchitectureDao implements IPSSiteArchitectureDao {

    private final IPSDataItemSummaryService dataItemSummaryService;
    private final IPSPublishingWs pubWs;

    @Autowired
    public PSSiteArchitectureDao(IPSDataItemSummaryService dataItemSummaryService, IPSPublishingWs pubWs) {
        this.dataItemSummaryService = dataItemSummaryService;
        this.pubWs = pubWs;
    }

    @Override
    public PSSiteArchitecture find(String id) throws LoadException {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return createSiteArchitecture(id);
    }

    /**
     * Creates the {@link PSSiteArchitecture} for the given site, expanded up to the first level.
     *
     * @param name The name of the site.
     * @return The site architecture for the given site.
     * @throws LoadException on error.
     */
    private PSSiteArchitecture createSiteArchitecture(String name) throws LoadException {
        try {
            var site = pubWs.findSite(name);
            var sa = new PSSiteArchitecture();
            sa.setName(name);
            var folderRoot = site.getFolderRoot();
            var section = createSiteSection(folderRoot);
            sa.setSections(List.of(section));
            return sa;
        } catch (PSErrorException | IPSDataService.DataServiceNotFoundException | IPSDataService.DataServiceLoadException e) {
            throw new LoadException(e);
        }
    }

    /**
     * Creates a site section for the given folder root.
     *
     * @param folderRoot not null.
     * @return The site section object for the given folder root.
     */
    private PSSiteSection createSiteSection(String folderRoot)
            throws IPSDataService.DataServiceNotFoundException, IPSDataService.DataServiceLoadException {
        var siteSection = new PSSiteSection();
        var id = dataItemSummaryService.pathToId(folderRoot);
        var sums = dataItemSummaryService.findFolderChildren(id);
        // Find the first nav tree content type and set section details
        sums.stream()
                .filter(itemSummary -> NAV_TREE_CONTENTTYPE_NAME.equals(itemSummary.getType()))
                .findFirst()
                .ifPresent(itemSummary -> {
                    siteSection.setId(itemSummary.getId());
                    siteSection.setTitle(itemSummary.getName());
                    siteSection.setFolderPath(folderRoot);
                });
        return siteSection;
    }

    @Override
    public List<PSSiteArchitecture> findAll() throws com.percussion.share.dao.IPSGenericDao.LoadException {
        // Not implemented yet
        return List.of();
    }

    @Override
    public void delete(String id) throws com.percussion.share.dao.IPSGenericDao.DeleteException {
        // Not implemented yet
    }

    @Override
    public PSSiteArchitecture save(PSSiteArchitecture object)
            throws com.percussion.share.dao.IPSGenericDao.SaveException {
        // Not implemented yet
        return null;
    }

    @Override
    public List<PSSiteSection> getSections(String id) throws com.percussion.share.dao.IPSGenericDao.LoadException {
        // Not implemented yet
        return List.of();
    }

    /**
     * The constant for the name of the nav tree content type.
     */
    private static final String NAV_TREE_CONTENTTYPE_NAME = "rffNavTree";
}
