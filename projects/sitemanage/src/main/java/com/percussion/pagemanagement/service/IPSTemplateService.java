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

import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplate.PSTemplateTypeEnum;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.guid.IPSGuid;
import java.util.Collection;
import java.util.List;

/**
 * Provides various CRUD operations for template objects.
 *
 * @author YuBingChen
 */
public interface IPSTemplateService extends IPSDataService<PSTemplate, PSTemplateSummary, String> {

  /** The content type name of the page template. */
  String TPL_CONTENT_TYPE = "percPageTemplate";

  /**
   * Creates a new template from a base template.
   *
   * @param plainBaseTemplateName The base template name, not blank.
   * @param templateName The new template name, not blank.
   * @param id The site or template ID, not blank.
   * @return The created template summary, never {@code null}.
   * @throws PSAssemblyException If assembly fails.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplateSummary createNewTemplate(String plainBaseTemplateName, String templateName, String id)
      throws PSAssemblyException, PSDataServiceException;

  /**
   * Finds all templates, including base and user-created templates.
   *
   * @return The templates, never {@code null}, but may be empty.
   * @throws IPSGenericDao.LoadException If loading fails.
   * @throws PSTemplateException If a template error occurs.
   */
  List<PSTemplateSummary> findAll() throws IPSGenericDao.LoadException, PSTemplateException;

  /**
   * Gets the template edit URL.
   *
   * @param id The template ID, not blank.
   * @return The edit URL, never blank.
   */
  String getTemplateEditUrl(String id);

  /**
   * Finds all templates for the selected site, including base and user-created templates.
   *
   * @param siteName The site name, not blank.
   * @return The templates, never {@code null}, but may be empty.
   * @throws IPSGenericDao.LoadException If loading fails.
   * @throws PSTemplateException If a template error occurs.
   */
  List<PSTemplateSummary> findAll(String siteName)
      throws IPSGenericDao.LoadException, PSTemplateException;

  /**
   * Finds all user-created templates.
   *
   * @return The template summaries, never {@code null}, may be empty.
   * @throws PSTemplateException If a template error occurs.
   */
  List<PSTemplateSummary> findAllUserTemplates() throws PSTemplateException;

  /**
   * Loads a list of user template summaries.
   *
   * @param ids A list of user template IDs, not {@code null}, may be empty.
   * @param siteName The site name, not blank.
   * @return The loaded template summaries, not {@code null}, may be empty.
   * @throws PSTemplateException If a template error occurs.
   */
  List<PSTemplateSummary> loadUserTemplateSummaries(List<String> ids, String siteName)
      throws PSTemplateException;

  /**
   * Finds all base templates based on the supplied type.
   *
   * @param type The type of the base templates, may be null or empty.
   * @return A list of requested templates, never {@code null}, may be empty.
   */
  List<PSTemplateSummary> findBaseTemplates(String type);

  /**
   * Creates a template from a name and a specified source template.
   *
   * @param name The name of the created template, not blank.
   * @param srcId The ID of the source template, not blank.
   * @return The created template summary, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   * @deprecated Use {@link #createTemplate(String, String, String)} instead.
   */
  @Deprecated
  PSTemplateSummary createTemplate(String name, String srcId) throws PSDataServiceException;

  /**
   * Saves the specified template to the specified site.
   *
   * @param template The template to save, not {@code null}.
   * @param siteId The site ID, may be {@code null}.
   * @return The saved template, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplate save(PSTemplate template, String siteId) throws PSDataServiceException;

  /**
   * Saves the specified template to the specified site and page.
   *
   * @param template The template to save, not {@code null}.
   * @param siteId The site ID, may be {@code null}.
   * @param pageId The page ID, may be {@code null}.
   * @return The saved template, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplate save(PSTemplate template, String siteId, String pageId) throws PSDataServiceException;

  /**
   * Creates a template from a name and a specified source template with no specific type.
   *
   * @param name The name of the created template, not blank.
   * @param srcId The ID of the source template, not blank.
   * @param siteId The site ID, not blank.
   * @return The created template summary, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplateSummary createTemplate(String name, String srcId, String siteId)
      throws PSDataServiceException;

  /**
   * Creates a template from a name and a specified source template using a specific type.
   *
   * @param name The name of the created template, not blank.
   * @param srcId The ID of the source template, not blank.
   * @param siteId The site ID, not blank.
   * @param type The type of template to create.
   * @return The created template summary, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplateSummary createTemplate(
      String name, String srcId, String siteId, PSTemplateTypeEnum type)
      throws PSDataServiceException;

  /**
   * Finds the specified template.
   *
   * @param id The template ID, not blank.
   * @return The template summary, may be {@code null} if not found.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplateSummary find(String id) throws PSDataServiceException;

  /**
   * Loads the specified template.
   *
   * @param id The template ID, not blank.
   * @return The template, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSTemplate load(String id) throws PSDataServiceException;

  /**
   * Deletes the specified template if it is not used by any pages.
   *
   * @param id The template ID, not blank.
   * @throws PSDataServiceException If a data service error occurs.
   * @throws PSNotFoundException If the template is not found.
   */
  void delete(String id) throws PSDataServiceException, PSNotFoundException;

  /**
   * Gets the template thumbnail path.
   *
   * @param summary The template summary, not {@code null}.
   * @param siteName The site name, not blank.
   * @return The thumbnail path, never blank.
   */
  String getTemplateThumbPath(PSTemplateSummary summary, String siteName);

  /**
   * Loads HTML metadata for a template.
   *
   * @param id The template ID, not blank.
   * @return The HTML metadata, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   */
  PSHtmlMetadata loadHtmlMetadata(String id) throws PSDataServiceException;

  /**
   * Saves or replaces the HTML metadata fields of a template.
   *
   * @param metadata The metadata object, must have ID set.
   * @throws PSDataServiceException If a data service error occurs.
   */
  void saveHtmlMetadata(PSHtmlMetadata metadata) throws PSDataServiceException;

  /**
   * Deletes the specified template.
   *
   * @param id The template ID, not blank.
   * @param force {@code true} to delete the template even if it is in use, {@code false} otherwise.
   * @throws PSDataServiceException If a data service error occurs.
   * @throws PSNotFoundException If the template is not found.
   */
  void delete(String id, boolean force) throws PSDataServiceException, PSNotFoundException;

  /**
   * Finds the user template with the specified name.
   *
   * @param name The template name, not blank.
   * @return The template summary, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   * @deprecated Used by unit tests only. Not for production use.
   */
  @Deprecated
  PSTemplateSummary findUserTemplateByName_UsedByUnitTestOnly(String name)
      throws PSDataServiceException;

  /**
   * Finds the user template for the specified name and site.
   *
   * @param templateName The template name, not blank.
   * @param siteName The site name, not blank.
   * @return The template GUID, may be {@code null} if not found.
   * @throws PSValidationException If validation fails.
   * @throws DataServiceLoadException If loading fails.
   */
  IPSGuid findUserTemplateIdByName(String templateName, String siteName)
      throws PSValidationException, DataServiceLoadException;

  /**
   * Determines if the specified template is currently associated to any pages.
   *
   * @param templateId The template ID, not blank.
   * @return {@code true} if the template is used by one or more pages, {@code false} otherwise.
   * @throws PSValidationException If validation fails.
   */
  boolean isAssociatedToPages(String templateId) throws PSValidationException;

  /**
   * Returns a list of IDs of pages associated with the template.
   *
   * @param templateId The template ID, not blank.
   * @return List of page IDs if the template is used by one or more pages.
   */
  Collection<Integer> getPageIdsForTemplate(String templateId);

  /**
   * Exports the template with the specified ID and name, without content.
   *
   * @param id The template ID, not blank.
   * @param name The template name, not blank.
   * @return The exported template, never {@code null}.
   * @throws PSValidationException If validation fails.
   * @throws PSTemplateException If a template error occurs.
   */
  PSTemplate exportTemplate(String id, String name)
      throws PSValidationException, PSTemplateException;

  /**
   * Imports the specified template to the specified site.
   *
   * @param template The template to import, not {@code null}.
   * @param siteId The site ID, may be {@code null}.
   * @return The imported template, never {@code null}.
   * @throws PSDataServiceException If a data service error occurs.
   * @throws IPSPathService.PSPathNotFoundServiceException If the path is not found.
   */
  PSTemplate importTemplate(PSTemplate template, String siteId)
      throws PSDataServiceException, IPSPathService.PSPathNotFoundServiceException;

  /** Exception thrown when an unexpected error occurs in this service. */
  class PSTemplateException extends PSDataServiceException {
    private static final long serialVersionUID = 1L;

    public PSTemplateException() {
      super();
    }

    public PSTemplateException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSTemplateException(String message) {
      super(message);
    }

    public PSTemplateException(Throwable cause) {
      super(cause);
    }
  }
}
