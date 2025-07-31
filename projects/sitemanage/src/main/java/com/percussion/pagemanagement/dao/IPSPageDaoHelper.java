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
package com.percussion.pagemanagement.dao;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.share.service.exception.PSValidationException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper interface for page DAO operations.
 */
public interface IPSPageDaoHelper {

    /**
     * Sets the workflow ID on the given page according to the parent folder's workflow association.
     * @param page the page to update.
     */
    void setWorkflowAccordingToParentFolder(PSPage page) throws PSValidationException;

    /**
     * Gets the workflow ID to use when creating pages in the specified folder path.
     * @param folderPath the path, cannot be {@code null} or empty.
     * @return the workflow ID.
     */
    int getWorkflowIdForPath(String folderPath) throws PSValidationException;

    /**
     * Finds all page IDs using the specified template.
     * @param templateId never blank.
     * @return list of page IDs, never {@code null}, may be empty.
     */
    Collection<Integer> findPageIdsByTemplate(String templateId);

    /**
     * Updates older revisions of pages to use the current template after a template is removed.
     * @param deletedTemplate the template ID that was deleted, must not be blank.
     */
    void replaceTemplateForPageInOlderRevisions(String deletedTemplate);

    /**
     * Gets IDs of pages using the given template in an older revision (not current or tip).
     * @param deletedTemplate the template ID that was deleted, must not be blank.
     * @return IDs of pages, never {@code null}, may be empty.
     */
    Collection<Integer> findPageIdsByTemplateInRecentRevision(String deletedTemplate);

    /**
     * Finds the template used by the current revision of the given pages.
     * @param pages list of page IDs to update, not {@code null}.
     * @return map of page ID to template, never {@code null}, may be empty.
     */
    Map<String, String> findTemplateUsedByCurrentRevisionOfPages(List<Integer> pages);

    /**
     * Finds all imported page IDs using the specified template.
     * @param templateId template ID to find, must not be blank.
     * @param pages list of page IDs to find, not {@code null}.
     * @return list of page IDs, never {@code null}, may be empty.
     */
    Collection<Integer> findImportedPageIdsByTemplate(String templateId, List<Integer> pages);

    /**
     * Gets content IDs for fetching by status.
     * @param criteria search criteria.
     * @param contentIDs list of content IDs.
     * @return collection of content IDs.
     */
    Collection<Integer> getContentIdsForFetchingByStatus(PSSearchCriteria criteria, List<Integer> contentIDs);

    /**
     * Finds the link text used by the current revision of the given pages.
     * @param pages list of page IDs to update, not {@code null}.
     * @return map of content ID to link text, never {@code null}, may be empty.
     */
    Map<String, String> findLinkTextForCurrentRevisionOfPages(List<Integer> pages);
}
