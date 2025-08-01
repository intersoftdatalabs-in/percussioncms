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

package com.percussion.contentmigration.service;

import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSWidgetAssetRelationshipService;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;

import java.util.List;

/**
 * Service for migrating content from an unassigned page to a template, from one template to another,
 * or for pages within the template.
 *
 * <p>All methods are backward compatible. Use dependency injection for implementation.
 */
public interface IPSContentMigrationService {
    /**
     * Finds all applicable widgets based on the template and reference page (if exists), runs content matching rules,
     * converts the content into fields, creates local content, and associates it to the target page.
     * Skips pages checked out by someone else. If the page is checked out to the current user, it remains checked out.
     * Otherwise, the page is checked out, updated, and checked back in.
     * If the page is in a non-editable state, it is moved to an editable state and left there.
     *
     * @param siteName The site name. If not blank, removes the pages from the unassigned queue after migration.
     * @param templateId The template id used as the basis for migration. Must not be {@code null}.
     * @param referencePageId The reference page id. If not blank, the rendered page is used for matching.
     * @param newPageIds Must not be empty. Migrates content for all valid, modifiable pages.
     * @throws PSContentMigrationException on error.
     */
    void migrateContent(String siteName, String templateId, String referencePageId, List<String> newPageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

    /**
     * Migrates content on template change. Content matched by widget name is automatically migrated;
     * for the rest, runs content matching rules on unassigned assets.
     *
     * @param templateId must not be blank.
     * @param referencePageId may be blank.
     * @param newPageIds must not be empty.
     * @throws PSContentMigrationException on error.
     */
    void migrateContentOnTemplateChange(String templateId, String referencePageId, List<String> newPageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

    /**
     * Migrates same template changes to other pages of the template.
     * Skips migration if page and template content migration versions match.
     *
     * @param templateId must not be blank.
     * @param pageIds if {@code null}, migrates all pages using the template; otherwise, only the listed pages.
     */
    void migrateSameTemplateChanges(String templateId, List<String> pageIds)
            throws PSContentMigrationException, PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

    /**
     * Gets all pages using the supplied template.
     *
     * @param templateId must not be {@code null}
     * @return List of page guid strings, never {@code null}, may be empty.
     */
    List<String> getTemplatePages(String templateId) throws IPSPageService.PSPageException;
}
