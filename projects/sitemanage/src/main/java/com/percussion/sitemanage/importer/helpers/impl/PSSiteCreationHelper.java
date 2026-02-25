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


import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.dao.IPSiteDao;
import com.percussion.sitemanage.dao.impl.PSSiteContentDao;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper responsible for creating a site and related elements during import. Sunny Sal says: "A
 * site a day keeps the 404s away!"
 */
@Component("siteCreationHelper")
@Lazy
public class PSSiteCreationHelper extends PSImportHelper {

  public static final Logger log = LogManager.getLogger(PSSiteCreationHelper.class);

  private final IPSiteDao siteDao;
  private final IPSPageService pageService;

  private static final String DEFAULT_TEMPLATE_NAME = "Home";
  private static final String DEFAULT_LANDING_PAGE_NAME = "Home";
  private static final String STATUS_MESSAGE = "creating site";

  @Autowired
  public PSSiteCreationHelper(IPSiteDao siteDao, IPSPageService pageService) {
    this.siteDao = siteDao;
    this.pageService = pageService;
  }

  @Override
  public void process(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSSiteImportException {
    startTimer();
    context
        .getLogger()
        .appendLogMessage(PSLogEntryType.STATUS, "Create Site", "The site creation has started.");

    var newSite = context.getSite().orElseThrow(() -> new IllegalStateException("Site must be provided in context"));

    // Set plain template as base template
    newSite.setBaseTemplateName(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME);
    newSite.setTemplateName(DEFAULT_TEMPLATE_NAME);

    // If page title could be extracted, get it from pageContent. Otherwise default to page name and
    // log a message.
    var importedPageTitle = pageContent.getTitle();
    if (StringUtils.isBlank(importedPageTitle)) {
      importedPageTitle = DEFAULT_LANDING_PAGE_NAME;
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS,
              "Extract page title",
              "No title could be extracted from the page. Defaulting to page name.");
    }
    newSite.setHomePageTitle(importedPageTitle);
    newSite.setNavigationTitle(importedPageTitle);

    try {
      // Save and create related elements
      var savedSite = siteDao.save(newSite);
      context.setSite(savedSite);
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS, "Create site", "The site was created successfully.");

      // Set the template id on the context
      var homePage =
          pageService.findPage(PSSiteContentDao.HOME_PAGE_NAME, savedSite.getFolderPath());
      if (homePage != null) {
        context.setTemplateId(homePage.getTemplateId());
        context.setPageName(PSSiteContentDao.HOME_PAGE_NAME);
      }

      // Create site import summary entry
      long siteId = savedSite.getSiteId().orElseThrow();
      context.getSummaryService().ifPresent(svc -> {
          try {
              svc.create((int) siteId);
          } catch (com.percussion.share.dao.IPSGenericDao.SaveException e) {
              throw new RuntimeException(e);
          }
      });

      // Update the template and page count
      var summaryStats =
          new HashMap<IPSSiteImportSummaryService.SiteImportSummaryTypeEnum, Integer>();
      if (context.getSummaryStats().isPresent()) {
        summaryStats.putAll(context.getSummaryStats().get());
        context.setSummaryStats(null);
      }
      summaryStats.put(IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.TEMPLATES, 1);
      summaryStats.put(IPSSiteImportSummaryService.SiteImportSummaryTypeEnum.PAGES, 1);
      context.getSummaryService().ifPresent(svc -> {
          try {
              svc.update((int) siteId, summaryStats);
          } catch (com.percussion.share.dao.IPSGenericDao.SaveException e) {
              throw new RuntimeException(e);
          }
      });

    } catch (RuntimeException | PSDataServiceException e) {
      // Errors in mandatory helpers are not logged in siteImportLogger,
      // because that log is discarded. Log the error in the server log.
      var message = "There was an unexpected error creating the new site.";
      log.error(message + ". Caused by: " + e.getMessage() + ExceptionUtils.getStackTrace(e));
      throw new PSSiteImportException(message, e);
    }
    endTimer();
  }

  @Override
  @SuppressWarnings("unused")
  public void rollback(PSPageContent pageContent, PSSiteImportCtx context) {
    var optSite = context.getSite();
    if (optSite.isEmpty()) {
      return; // nothing to roll back
    }
    try {
      // Delete site and related content
      siteDao.remove(optSite.get().getId());
    } catch (PSDataServiceException e) {
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.ERROR,
              "Delete Site",
              "Failed to roll back site creation: " + e.getLocalizedMessage());
    }
  }

  @Override
  public String getHelperMessage() {
    return STATUS_MESSAGE;
  }
}
