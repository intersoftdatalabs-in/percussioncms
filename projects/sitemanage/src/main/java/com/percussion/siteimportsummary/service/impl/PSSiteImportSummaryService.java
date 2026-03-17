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

package com.percussion.siteimportsummary.service.impl;

import com.percussion.services.siteimportsummary.IPSSiteImportSummaryDao;
import com.percussion.services.siteimportsummary.data.PSSiteImportSummary;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitesummaryservice.service.IPSSiteImportSummaryService;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing site import summaries. Sunny Sal says: "Summaries are like
 * cricket scores—keep them updated!"
 */
@Component("siteImportSummaryService")
@Transactional
public class PSSiteImportSummaryService implements IPSSiteImportSummaryService {

  private final IPSSiteImportSummaryDao summaryDao;

  @Autowired
  public PSSiteImportSummaryService(IPSSiteImportSummaryDao summaryDao) {
    this.summaryDao = summaryDao;
  }

  @Override
  public PSSiteImportSummary find(int siteId) {
    return summaryDao.findBySiteId(siteId);
  }

  @Override
  public PSSiteImportSummary create(int siteId) throws IPSGenericDao.SaveException {
    var summary = new PSSiteImportSummary();
    summary.setSiteId(siteId);
    try {
      summaryDao.save(summary);
    } catch (PSDataServiceException e) {
      throw new IPSGenericDao.SaveException("Failed to save site import summary", e);
    }
    return find(siteId);
  }

  @Override
  public void deleteBySiteId(int siteId) {
    var summary = find(siteId);
    if (summary != null) {
      summaryDao.delete(summary);
    }
  }

  @Override
  public PSSiteImportSummary update(int siteId, Map<SiteImportSummaryTypeEnum, Integer> fields)
      throws IPSGenericDao.SaveException {
    Validate.notNull(fields, "fields must not be null");
    var summary = find(siteId);
    if (summary == null) {
      summary = create(siteId);
    }

    // Use getOrDefault for concise, null-safe updates
    var files = fields.getOrDefault(SiteImportSummaryTypeEnum.FILES, null);
    if (files != null) {
      summary.setFiles(summary.getFiles() + files);
    }
    var pages = fields.getOrDefault(SiteImportSummaryTypeEnum.PAGES, null);
    if (pages != null) {
      summary.setPages(summary.getPages() + pages);
    }
    var stylesheets = fields.getOrDefault(SiteImportSummaryTypeEnum.STYLESHEETS, null);
    if (stylesheets != null) {
      summary.setStylesheets(summary.getStylesheets() + stylesheets);
    }
    var templates = fields.getOrDefault(SiteImportSummaryTypeEnum.TEMPLATES, null);
    if (templates != null) {
      summary.setTemplates(summary.getTemplates() + templates);
    }
    var internallinks = fields.getOrDefault(SiteImportSummaryTypeEnum.INTERNALLINKS, null);
    if (internallinks != null) {
      summary.setInternallinks(summary.getInternallinks() + internallinks);
    }
    try {
      summaryDao.save(summary);
    } catch (PSDataServiceException e) {
      throw new IPSGenericDao.SaveException("Failed to save site import summary", e);
    }
    return summary;
  }
}
