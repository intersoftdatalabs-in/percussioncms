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
package com.percussion.sitemanage.importer.helpers.impl;

import com.percussion.pagemanagement.dao.IPSPageDao;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSPageManagementUtils;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSTemplateImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper responsible for creating a template and a new page using that template. Sunny Sal says:
 * "Templates are like blueprints—make them solid!"
 */
@Component("templateCreationHelper")
@Lazy
public class PSTemplateCreationHelper extends PSImportHelper {

  private final IPSTemplateService templateService;
  private final IPSPageDao pageDao;
  private final IPSAssemblyService assemblyService;
  private final IPSIdMapper idMapper;
  private final IPSSiteTemplateService siteTemplateService;
  private final IPSPageService pageService;

  public static final String LOG_ENTRY_PREFIX = "Import Template From Url";
  private static final String STATUS_MESSAGE = "creating template";

  public static final Logger log = LogManager.getLogger(PSTemplateCreationHelper.class);

  @Autowired
  public PSTemplateCreationHelper(
      IPSTemplateService templateService,
      IPSPageDao pageDao,
      IPSAssemblyService assemblyService,
      IPSIdMapper idMapper,
      IPSSiteTemplateService siteTemplateService,
      IPSPageService pageService) {
    this.templateService = templateService;
    this.pageDao = pageDao;
    this.assemblyService = assemblyService;
    this.idMapper = idMapper;
    this.siteTemplateService = siteTemplateService;
    this.pageService = pageService;
  }

  @SuppressWarnings("unused")
  @Override
  public void process(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSTemplateImportException, IPSPageService.PSPageException {
    startTimer();
    // caller should always provide a site; unwrap once
    var site =
        context
            .getSite()
            .orElseThrow(() -> new IllegalStateException("Site must be provided in context"));

    // Initial names, using site-wide naming conventions.
    var pageName = PSPageManagementUtils.PAGE_NAME;
    var templateName = PSPageManagementUtils.TEMPLATE_NAME;

    // If possible, extract name from URL to use for page and template, instead.
    var extractedName = extractPageNameFromUrl(context.getSiteUrl().orElse(""));
    if (!StringUtils.isEmpty(extractedName)) {
      pageName = extractedName;
      templateName = extractedName;
    } else {
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS,
              LOG_ENTRY_PREFIX,
              "Template and page name couldn't be extracted from URL. Defaulting to "
                  + PSPageManagementUtils.TEMPLATE_NAME);
    }

    // Generate names to avoid collision with existing pages and templates.
    pageName = pageService.generateNewPageName(pageName, site.getFolderPath());
    templateName =
        siteTemplateService.generateNewTemplateName(
            PSPageManagementUtils.TEMPLATE_NAME, site.getId());

    try {
      // TODO Replace plain template with new perc.base.empty that will be later added to base
      // package.
      // Create template
      context.setTemplateName(templateName);
      var newTemplate =
          templateService.createNewTemplate(
              IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME, templateName, site.getId());
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS,
              LOG_ENTRY_PREFIX,
              "Template was successfully created with name: " + templateName);

      // Create page
      context.setPageName(pageName);
      var newPage = createNewPage(pageName, newTemplate.getId(), site.getFolderPath());
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS,
              LOG_ENTRY_PREFIX,
              "Page was successfully created with name: " + templateName);

      // Assign the new template id to the context object
      context.setTemplateId(newTemplate.getId());
    } catch (PSAssemblyException | PSDataServiceException e) {
      var message = "There was an unexpected error importing the template from the provided URL.";
      log.error(message + ". Caused by: " + e.getMessage());
      throw new PSTemplateImportException(message, e);
    }
    endTimer();
  }

  @SuppressWarnings("unused")
  @Override
  public void rollback(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSDataServiceException {
    // Only act if a site was stored in the context
    context
        .getSite()
        .ifPresent(
            site -> {
              // Delete page if it was created
              context
                  .getPageName()
                  .filter(StringUtils::isNotEmpty)
                  .ifPresent(
                      pageName -> {
                        try {
                          var page = pageService.findPage(pageName, site.getFolderPath());
                          if (page != null) {
                            pageService.delete(page.getId());
                          }
                        } catch (PSDataServiceException e) {
                          // LOG and ignore rollback failures
                          log.warn(
                              "Failed to delete imported page during rollback: {}", e.getMessage());
                        }
                      });

              // Delete template if it was created
              context
                  .getTemplateName()
                  .filter(StringUtils::isNotEmpty)
                  .ifPresent(
                      templateName -> {
                        var siteTemplates = siteTemplateService.findTemplatesBySite(site.getId());
                        for (var template : siteTemplates) {
                          if (template.getName().equals(templateName)) {
                            try {
                              templateService.delete(template.getId());
                            } catch (PSNotFoundException | PSDataServiceException e) {
                              log.warn(PSExceptionUtils.getMessageForLog(e));
                              log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                            }
                            break;
                          }
                        }
                      });
            });
  }

  /**
   * Extracts the text of the URL behind the last slash "/" and before the URL parameters or query
   * section in the URL.
   *
   * @param url The URL to process (can include http:// prefix or not). No syntax restrictions.
   * @return extracted name if possible, or an empty string if it couldn't be extracted. Never null.
   */
  public String extractPageNameFromUrl(String url) {
    if (StringUtils.isEmpty(url)) {
      return "";
    }

    // Clean URL before processing
    var cleanUrl = url.replace('\\', '/');

    // Remove protocol prefix - http://, https://, etc ;
    var protocolSeparator = "://";
    var protocolPosition = cleanUrl.indexOf(protocolSeparator);
    if (protocolPosition != -1) {
      cleanUrl = cleanUrl.substring(protocolPosition + protocolSeparator.length());
    }

    // Remove / at the start
    if (cleanUrl.length() > 0 && cleanUrl.charAt(0) == '/') {
      cleanUrl = cleanUrl.substring(1);
    }

    // Remove trailing slash
    if (cleanUrl.endsWith("/")) {
      cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
    }

    // Start from the last slash found, without including it.
    var startIndex = cleanUrl.lastIndexOf("/");

    if (startIndex == -1 || startIndex == cleanUrl.length() - 1) {
      return "";
    } else {
      startIndex++;
    }

    // End at the first ?, ; or . found.
    var endIndex = cleanUrl.replace('?', '.').replace(';', '.').indexOf('.', startIndex);

    if (endIndex == -1) {
      return cleanUrl.substring(startIndex);
    } else {
      return cleanUrl.substring(startIndex, endIndex);
    }
  }

  /**
   * Creates a new page with specified name in folderPath, using template with templateId.
   *
   * @param name The name that this page will have. Will also be used to set link title and page
   *     title.
   * @param templateId The template id of the template that this page will use.
   * @param folderPath The folder path inside the site where the page will be created.
   * @return PSPage Class that holds information of the created page.
   */
  public PSPage createNewPage(String name, String templateId, String folderPath)
      throws PSDataServiceException {
    var page = new PSPage();
    page.setName(name);
    page.setFolderPath(folderPath);
    page.setTitle(name);
    page.setTemplateId(templateId);
    page.setLinkTitle(name);
    return pageService.save(page);
  }

  @Override
  public String getHelperMessage() {
    return STATUS_MESSAGE;
  }
}
