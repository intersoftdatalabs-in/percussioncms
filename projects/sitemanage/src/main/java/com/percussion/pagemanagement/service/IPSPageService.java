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

import com.percussion.assetmanagement.data.PSReportFailedToRunException;
import com.percussion.pagemanagement.data.PSNonSEOPagesRequest;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSPageChangeEvent;
import com.percussion.pagemanagement.data.PSPageReportLine;
import com.percussion.pagemanagement.data.PSSEOStatistics;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.data.PSUnassignedResults;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;

/** Provides various CRUD operations for page objects. */
public interface IPSPageService extends IPSDataService<PSPage, PSPage, String> {

  /**
   * Creates/saves a page.
   *
   * @param page The page, never <code>null</code>.
   * @return The saved page, never <code>null</code>.
   */
  PSPage save(PSPage page) throws PSDataServiceException;

  /**
   * Finds the specified page.
   *
   * @param id The ID of the page, never <code>null</code> or empty.
   * @return The page item, or <code>null</code> if not found.
   */
  PSPage find(String id)
      throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException;

  /**
   * Loads the specified page.
   *
   * @param id The ID of the page, never <code>null</code> or empty.
   * @return The page, never <code>null</code>.
   */
  PSPage load(String id)
      throws PSValidationException,
          DataServiceLoadException,
          IPSDataService.DataServiceNotFoundException;

  /**
   * Finds the specified page by name and folder path.
   *
   * @param name The name of the page, never <code>null</code> or empty.
   * @param folderPath The folder path of the page, never <code>null</code> or empty.
   * @return The page item, or <code>null</code> if not found.
   * @throws PSPageException If an error occurs finding the page.
   */
  PSPage findPage(String name, String folderPath) throws PSDataServiceException;

  /**
   * Finds the specified page by full path.
   *
   * @param fullPath The full folder path, never <code>null</code>, empty, or blank.
   * @return The page object, or <code>null</code> if not found.
   * @throws PSPageException If an error occurs finding the page.
   */
  PSPage findPageByPath(String fullPath) throws PSPageException, PSValidationException;

  /**
   * Finds all the pages that use a certain template and returns the results paged.
   *
   * @param templateId The ID of the template, never <code>null</code>.
   * @param startIndex The starting index for pagination.
   * @param maxResults The page size for pagination.
   * @param sortColumn The attribute to order by.
   * @param sortOrder Ascending or descending order.
   * @param pageId The page item ID to find in the list.
   * @return A paged item list of path items.
   * @throws PSPageException If there was a problem retrieving the list of pages.
   */
  PSPagedItemList findPagesByTemplate(
      String templateId,
      Integer startIndex,
      Integer maxResults,
      String sortColumn,
      String sortOrder,
      String pageId)
      throws PSDataServiceException;

  /**
   * Finds all pages for a given request which are not optimized for searching.
   *
   * @param request The request to find the pages by workflow state, never <code>null</code>.
   * @return The SEO statistics for pages which are considered sub-optimal for searching.
   * @throws PSPageException If the workflow could not be found, or other system failure.
   */
  List<PSSEOStatistics> findNonSEOPages(PSNonSEOPagesRequest request) throws PSDataServiceException;

  /**
   * Deletes the specified page. All local content of the page will also be deleted. The page will
   * not be deleted if it is being edited by another user.
   *
   * @param id The ID of the page, never <code>null</code> or empty.
   */
  void delete(String id) throws PSValidationException;

  /**
   * Deletes the specified page, with option to force deletion.
   *
   * @param id The ID of the page, never <code>null</code> or empty.
   * @param force <code>true</code> to delete even if being edited by another user.
   */
  void delete(String id, boolean force) throws PSValidationException;

  /**
   * Deletes the specified page, with options to force and purge.
   *
   * @param id The ID of the page, never <code>null</code> or empty.
   * @param force <code>true</code> to delete even if being edited by another user.
   * @param purgeItem <code>true</code> to purge the item, <code>false</code> to recycle.
   */
  void delete(String id, boolean force, boolean purgeItem) throws PSValidationException;

  /**
   * Generates a new page name.
   *
   * @param pageName The base page name.
   * @param folderPath The folder path.
   * @return The generated page name.
   * @throws PSPageException If an error occurs.
   */
  String generateNewPageName(String pageName, String folderPath) throws PSPageException;

  /**
   * Creates a copy of the page in its current folder.
   *
   * @param id The page ID.
   * @param addToRecent Whether to add to recent items.
   * @return The new page ID.
   * @throws PSDataServiceException If a data service error occurs.
   * @throws IPSPathService.PSPathNotFoundServiceException If the path is not found.
   */
  String copy(String id, boolean addToRecent)
      throws PSDataServiceException, IPSPathService.PSPathNotFoundServiceException;

  /**
   * Creates a copy of the page in the specified folder.
   *
   * @param id The page ID.
   * @param targetFolder The target folder.
   * @param addToRecent Whether to add to recent items.
   * @return The new page ID.
   * @throws PSDataServiceException If a data service error occurs.
   * @throws IPSPathService.PSPathNotFoundServiceException If the path is not found.
   */
  String copy(String id, String targetFolder, boolean addToRecent)
      throws PSDataServiceException, IPSPathService.PSPathNotFoundServiceException;

  /**
   * Gets a URL which can be used for editing an existing page.
   *
   * @param id The ID of the page, never blank.
   * @return The edit URL, never blank.
   */
  String getPageEditUrl(String id);

  /**
   * Gets a URL which can be used for viewing a read-only page.
   *
   * @param id The ID of the page, never blank.
   * @return The view URL, never blank.
   */
  String getPageViewUrl(String id);

  /**
   * Determines if the supplied item is a page or its content type is {@link #PAGE_CONTENT_TYPE}.
   *
   * @param id The item ID in question.
   * @return <code>true</code> if the item is a page; otherwise <code>false</code>.
   * @throws PSPageException If an error occurs.
   */
  boolean isPageItem(String id) throws PSPageException;

  /**
   * Adds a page change listener to get notified when the page changes.
   *
   * @param pageChangeListener The listener, must not be <code>null</code>.
   */
  void addPageChangeListener(IPSPageChangeListener pageChangeListener);

  /**
   * Notifies listeners that the page has changed.
   *
   * @param pageChangeEvent The page change event, must not be <code>null</code>.
   */
  void notifyPageChange(PSPageChangeEvent pageChangeEvent);

  /**
   * Dummy service to get notified on page meta-data save.
   *
   * @param pageId The string representation of the page GUID, must not be <code>null</code>.
   * @return PSNoContent, never blank.
   */
  PSNoContent savePageMetadata(String pageId);

  /**
   * Changes the template of the supplied page.
   *
   * @param pageId The string representation of the page GUID, must not be <code>null</code>.
   * @param templateId The string representation of the template GUID, must not be <code>null</code>
   *     .
   * @return PSNoContent, never blank.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSNoContent changeTemplate(String pageId, String templateId) throws PSDataServiceException;

  /**
   * Updates the template migration version of the page to match the version in its template.
   *
   * @param pageId The ID of the page to update, must specify an existing page, and the page must be
   *     checked out to the current user.
   * @throws PSDataServiceException If a data service error occurs.
   */
  void updateTemplateMigrationVersion(String pageId) throws PSDataServiceException;

  /**
   * Updates the migration empty widget flag for the page.
   *
   * @param pageId The ID of the page to update, must specify an existing page, and the page must be
   *     checked out to the current user.
   * @param flag The flag value.
   * @throws PSDataServiceException If a data service error occurs.
   */
  void updateMigrationEmptyWidgetFlag(String pageId, boolean flag) throws PSDataServiceException;

  /**
   * Gets the status of the empty widget flag for the page.
   *
   * @param pageId The ID of the page to get the flag, must specify an existing page, and the page
   *     must be checked out to the current user.
   * @return The flag value.
   * @throws DataServiceLoadException If a data service load error occurs.
   * @throws DataServiceNotFoundException If the data is not found.
   * @throws PSValidationException If a validation error occurs.
   */
  boolean getMigrationEmptyWidgetFlag(String pageId)
      throws DataServiceLoadException, DataServiceNotFoundException, PSValidationException;

  /**
   * Lists all pages in the content repository for the specified site.
   *
   * @param siteName The site name.
   * @return A list of CSV-formattable report lines.
   * @throws PSReportFailedToRunException If the report failed to run.
   * @throws PSDataServiceException If a data service error occurs.
   */
  List<PSPageReportLine> findAllPages(String siteName)
      throws PSReportFailedToRunException, PSDataServiceException;

  /**
   * Gets the import status for cataloged pages.
   *
   * @param siteName The name of the site, must not be <code>null</code> nor empty.
   * @param startIndex The start index (first item is 1).
   * @param maxResults The maximum number of results to return.
   * @return The unassigned results, never <code>null</code>.
   * @throws PSPageException If an error occurs.
   */
  PSUnassignedResults getUnassignedPagesBySite(
      String siteName, Integer startIndex, Integer maxResults) throws PSPageException;

  /**
   * Clears the migration empty flag for the page.
   *
   * @param pageId The page ID.
   * @return PSNoContent.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSNoContent clearMigrationEmptyFlag(String pageId) throws PSDataServiceException;

  /** Exception thrown when an unexpected error occurs in this service. */
  class PSPageException extends PSDataServiceException {
    private static final long serialVersionUID = 1L;

    public PSPageException() {
      super();
    }

    public PSPageException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSPageException(String message) {
      super(message);
    }

    public PSPageException(Throwable cause) {
      super(cause);
    }
  }

  /** The content type name of the page item. */
  String PAGE_CONTENT_TYPE = "percPage";

  /**
   * Validates if the page can be deleted.
   *
   * @param id The page ID.
   * @return PSNoContent.
   * @throws PSValidationException If a validation error occurs.
   */
  PSNoContent validateDelete(String id) throws PSValidationException;
}
