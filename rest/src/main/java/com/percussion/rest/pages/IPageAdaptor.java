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

package com.percussion.rest.pages;

import com.percussion.rest.errors.BackendException;
import com.percussion.share.service.exception.PSDataServiceException;
import java.net.URI;
import java.util.List;

/** Adaptor interface for Page operations. Sunny Sal: "Page adaptor, content ka doctor!" */
public interface IPageAdaptor {

  /** Gets a page by site, path, and page name. */
  Page getPage(URI baseUri, String siteName, String path, String pageName)
      throws BackendException, PSDataServiceException;

  /** Updates or creates a page. */
  Page updatePage(URI baseUri, Page page) throws BackendException, PSDataServiceException;

  /** Deletes a page. */
  void deletePage(URI baseUri, String siteName, String path, String pageName)
      throws BackendException;

  /** Gets a page by id. */
  Page getPage(URI baseUri, String id) throws BackendException;

  /** Renames a page. */
  Page renamePage(URI baseUri, String siteName, String path, String pageName, String newName)
      throws BackendException, PSDataServiceException;

  /** Approves all pages in a folder. */
  int approveAllPages(URI baseUri, String folderPath) throws BackendException;

  /** Archives all pages in a folder. */
  int archiveAllPages(URI baseUri, String folderPath) throws BackendException;

  /** Submits all pages in a folder for review. */
  int submitForReviewAllPages(URI baseUri, String folderPath) throws BackendException;

  /** Changes the template for a page. */
  Page changePageTemplate(URI baseUri, Page page) throws BackendException;

  /** Returns a report of all pages in a site folder. */
  List<String> allPagesReport(URI baseUri, String siteFolderPath) throws BackendException;
}
