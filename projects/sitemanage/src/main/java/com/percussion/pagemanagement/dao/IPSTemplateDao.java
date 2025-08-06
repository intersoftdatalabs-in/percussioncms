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
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.utils.guid.IPSGuid;

/**
 * DAO for CRUD operations on templates.
 */
public interface IPSTemplateDao extends IPSGenericDao<PSTemplate, String> {

    /**
     * Saves the specified template and adds it to the specified folder.
     * @param template the template to save, not {@code null}.
     * @param siteId the site ID, may be {@code null} if not attaching to a site.
     * @return the saved template, not {@code null}.
     */
    PSTemplate save(PSTemplate template, String siteId) throws PSDataServiceException;

    /**
     * Loads the specified base template by ID.
     * @param id the template ID, not {@code null}.
     * @return the template, never {@code null}.
     */
    IPSAssemblyTemplate loadBaseTemplateById(IPSGuid id) throws IPSTemplateService.PSTemplateException;

    /**
     * Loads the specified base template by name.
     * @param name the template name, not {@code null}.
     * @return the template, never {@code null}.
     */
    PSAssemblyTemplate loadBaseTemplateByName(String name) throws IPSTemplateService.PSTemplateException;

    PSTemplate createTemplate(String name, String sourceTemplateId) throws PSDataServiceException;

    /**
     * Finds all readonly system templates for the supplied type.
     * @param type the type of readonly/base templates.
     * @return never {@code null} or empty, sorted alphabetically by name.
     */
    List<PSTemplateSummary> findBaseTemplates(String type);

    /**
     * Finds all user-created templates.
     * @return template summaries, never {@code null}, may be empty, sorted alphabetically by name.
     */
    List<PSTemplateSummary> findAllUserTemplates() throws IPSTemplateService.PSTemplateException;

    /**
     * Finds all templates (excluding PSTemplateTypeEnum.UNASSIGNED).
     * @return all template summaries, never {@code null}, may be empty, sorted alphabetically by name.
     */
    List<PSTemplateSummary> findAllSummaries() throws com.percussion.share.dao.IPSGenericDao.LoadException, IPSTemplateService.PSTemplateException;

    /**
     * Finds all templates for the given site (excluding PSTemplateTypeEnum.UNASSIGNED).
     * @param siteName the site name.
     * @return template summaries, never {@code null}, may be empty, sorted alphabetically by name.
     */
    List<PSTemplateSummary> findAllSummaries(String siteName) throws com.percussion.share.dao.IPSGenericDao.LoadException, IPSTemplateService.PSTemplateException;

    /**
     * Loads a list of user template summaries.
     * @param ids list of user template IDs, not {@code null}, may be empty.
     * @param siteName the site name.
     * @return loaded template summaries, not {@code null}, may be empty.
     */
    List<PSTemplateSummary> loadUserTemplateSummaries(List<String> ids, String siteName) throws IPSTemplateService.PSTemplateException;

    /**
     * Finds the user template with the specified name. Used by unit tests only.
     * @param name never blank.
     * @return the template, never {@code null}.
     * @deprecated Use only in unit tests; not for production.
     */
    @Deprecated
    PSTemplate findUserTemplateByName_UsedByUnitTestOnly(String name) throws PSDataServiceException;

    /**
     * Finds the user template ID for the specified name and site.
     * @param templateName the template name, not blank.
     * @param siteName the site name, not blank.
     * @return the template ID, may be {@code null} if not found.
     */
    IPSGuid findUserTemplateIdByName(String templateName, String siteName);

    /**
     * Generates a template to export.
     * @param id the template ID, never blank.
     * @param name the template name, may be empty.
     * @return the loaded template.
     */
    PSTemplate generateTemplateToExport(String id, String name) throws IPSTemplateService.PSTemplateException;

    /**
     * Imports a template from a source.
     * @param template the template, never blank.
     * @param siteId the site for the template, may be empty.
     * @return the loaded template.
     */
    PSTemplate generateTemplateFromSource(PSTemplate template, String siteId) throws IPSTemplateService.PSTemplateException, IPSPathService.PSPathNotFoundServiceException;

    /**
     * Gets the thumbnail path for a template given the site name.
     * @param summary the template summary.
     * @param siteName the site name, may be {@code null}.
     * @return the thumbnail path, may be {@code null} if siteName is empty or {@code null}.
     */
    String getTemplateThumbPath(PSTemplateSummary summary, String siteName);

    /**
     * Retrieves user templates by type.
     * @param type the template type: NORMAL or UNASSIGNED, may be {@code null}.
     * @return list of templates, never {@code null}, may be empty.
     */
    List<PSTemplate> findUserTemplatesByType(PSTemplateTypeEnum type) throws IPSTemplateService.PSTemplateException;

    /**
     * Retrieves summaries for user templates by type.
     * @param type the template type: NORMAL or UNASSIGNED, may be {@code null}.
     * @return list of template summaries, never {@code null}, may be empty.
     */
    List<PSTemplateSummary> findAllUserTemplateSummariesByType(PSTemplateTypeEnum type) throws IPSTemplateService.PSTemplateException;

    enum BaseTemplateTypeEnum {
        all, base, resp
    }
}
