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
package com.percussion.pagemanagement.dao;

import java.util.List;
import javax.jcr.RepositoryException;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSPageSummary;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;

/**
 * Data access object for PSPage entities.
 * Provides methods for finding, saving, and deleting pages and their summaries.
 */
public interface IPSPageDao extends IPSGenericDao<PSPage, String> {

    PSPageSummary findSummary(String id) throws IPSPageService.PSPageException;

    List<PSPageSummary> findAllSummaries() throws IPSPageService.PSPageException;

    PSPage findPage(String name, String folderPath) throws PSDataServiceException;

    PSPage findPageByPath(String fullFolderPath) throws IPSPageService.PSPageException;

    /**
     * Finds all pages located under the specified path and using the specified template.
     *
     * @param path the internal folder path. If blank, result is same as {@link #findPageByPath(String)}.
     * @param templateId never blank.
     * @return list of pages, never {@code null}, may be empty.
     */
    List<PSPage> findPagesBySiteAndTemplate(String path, String templateId) throws PSDataServiceException;

    /**
     * Finds all pages under the specified path in the specified workflow and state.
     *
     * @param path the internal folder path, never blank.
     * @param workflowId workflow ID.
     * @param stateId set to -1 to include pages in all workflow states.
     * @return list of pages, never {@code null}, may be empty.
     */
    List<PSPageSummary> findPagesBySiteAndWf(String path, int workflowId, int stateId) throws PSDataServiceException;

    /**
     * Deletes a page by ID.
     * @param id the page ID, never {@code null} or empty.
     * @param force {@code true} to delete even if being edited by another user, {@code false} otherwise.
     */
    void delete(String id, boolean force) throws PSDataServiceException;

    /**
     * Gets page IDs by field name and value.
     * @param fieldName the field name, never {@code null}.
     * @param fieldValue the field value, never {@code null}.
     * @return list of content IDs, never {@code null}, may be empty.
     */
    List<Integer> getPageIdsByFieldNameAndValue(String fieldName, String fieldValue) throws IPSPageService.PSPageException;

    /**
     * Gets the content type ID of the page content type.
     * @return the ID.
     */
    long getPageContentTypeId() throws IPSPageService.PSPageException;

    /**
     * Finds all pages for the specified site path.
     * @param sitePath e.g. /Sites/SiteName
     * @return never {@code null}. List of pages for the specified site.
     */
    List<PSPage> findAllPagesBySite(String sitePath) throws PSDataServiceException;
}
