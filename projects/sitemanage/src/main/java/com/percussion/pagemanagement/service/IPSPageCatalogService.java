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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.data.PSCatalogPageSummary;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.error.PSSiteImportException;

import java.util.List;

/**
 * Service for catalog page operations.
 *
 * @author JaySeletz
 */
public interface IPSPageCatalogService {

    /**
     * Finds all catalog pages for the specified site.
     *
     * @param siteName The name of the site, not <code>null</code> or empty.
     * @return A list of page IDs, never <code>null</code>, may be empty.
     * @throws Exception If there are any unexpected errors.
     */
    List<String> findCatalogPages(String siteName) throws Exception;

    /**
     * Catalogs a page, creating a "page stub" as a placeholder.
     * The specified page cannot be cataloged and returns <code>null</code>
     * in the following scenarios:
     * <ul>
     *   <li>The maximum number of cataloged pages has been reached.</li>
     *   <li>The page already exists for that name and folder path.</li>
     *   <li>The page already exists under the (imported) normal location of the site.</li>
     *   <li>There is already a page with the specified folder path.</li>
     * </ul>
     * @param siteName The name of the site for which the page is cataloged, not <code>null</code> or empty.
     * @param href The href of the source of the page, used to import the page content later.
     * @param pageName The name to use for the page.
     * @param folderPath The folder path relative from the root of the site, including the leading "/". Not <code>null</code> or empty.
     * @param linkText The link text of the page.
     * @return The saved page stub, or <code>null</code> if one of the scenarios described above is met.
     * @throws Exception if there are any unexpected errors.
     */
    PSPage addCatalogPage(String siteName, String pageName, String linkText, String folderPath, String href) throws Exception;

    /**
     * Converts the specified catalog page path to the imported folder path.
     * @param path The folder path of the cataloged page, not <code>null</code>.
     * @return The imported folder path. Never <code>null</code>.
     */
    String convertToImportedFolderPath(String path);

    /**
     * Gets a summary for the specified catalog page.
     *
     * @param id The ID of the page, not <code>null</code>.
     * @return The summary for the supplied ID, or <code>null</code> if not found.
     * @throws Exception If there are any unexpected errors.
     */
    PSCatalogPageSummary getCatalogPageSummary(String id) throws Exception;

    /**
     * Gets the unassigned template for the specified site.
     *
     * @param siteName The name of the site, not <code>null</code> or empty.
     * @return The template ID, or <code>null</code> if not found.
     * @throws PSDataServiceException If a data service error occurs.
     * @throws PSSiteImportException If a site import error occurs.
     */
    String getCatalogTemplateIdBySite(String siteName) throws PSDataServiceException, PSSiteImportException;

    /**
     * Moves the cataloged page from the "page stub" location to the local location.
     * If the local location doesn't exist, it is created.
     *
     * @param pageId The ID of the page, not <code>null</code> or empty.
     * @throws Exception If there are any unexpected errors.
     */
    void createImportedPage(String pageId) throws Exception;

    /**
     * Finds all imported catalog pages for the specified site.
     *
     * @param siteName The name of the site, not <code>null</code> or empty.
     * @return A list of page IDs, never <code>null</code>, may be empty.
     * @throws Exception If there are any unexpected errors.
     */
    List<String> findImportedPageIds(String siteName) throws Exception;

    /**
     * Verifies if the page is an already imported page for the given site.
     *
     * @param page The site to check the page in. Assumed not <code>null</code>.
     * @return <code>true</code> if the page already exists under the given site, <code>false</code> otherwise.
     */
    boolean doesImportedPageExist(PSPage page);

    /**
     * Gets the full folder path for the given folder path and site.
     *
     * @param folderPath The folder path, not <code>null</code>.
     * @param site The site summary, not <code>null</code>.
     * @return The full folder path.
     */
    String getFullFolderPath(String folderPath, PSSiteSummary site);

    /**
     * Checks if a page with the given full folder path exists.
     *
     * @param fullFolderPath The full folder path, not <code>null</code>.
     * @return <code>true</code> if a page exists at the given path, <code>false</code> otherwise.
     */
    boolean pageWithFolderPathExists(String fullFolderPath);
}
