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

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.data.IPSHtmlMetadata;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSPageManagementUtils;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper used to extract the metadata and save it into the page. Sunny Sal says: "Metadata is like
 * vitamins—don't skip it!"
 */
@Component("pageMetadataExtractorHelper")
@Lazy
public class PSPageMetadataExtractorHelper extends PSGenericMetadataExtractorHelper {

  private final IPSPageService pageService;
  private final IPSPageCatalogService catalogService;
  private final IPSItemWorkflowService itemWorkflowService;

  private static final HashMap<String, PSTemplate> unassignedTemplateCache = new HashMap<>();

  @Autowired
  public PSPageMetadataExtractorHelper(
      IPSTemplateService templateService,
      IPSPageService pageService,
      IPSPageCatalogService catalogService,
      IPSItemWorkflowService itemWorkflowService) {
    super(templateService);
    this.pageService = pageService;
    this.catalogService = catalogService;
    this.itemWorkflowService = itemWorkflowService;
  }

  @Override
  protected void saveTargetItem(IPSHtmlMetadata targetItem)
      throws IPSItemWorkflowService.PSItemWorkflowServiceException, PSDataServiceException {
    var page = (PSPage) targetItem;
    // The target Item in this case is a PSPage
    var workflowTimer = System.nanoTime();
    itemWorkflowService.checkOut(page.getId());
    pageService.save(page);
    itemWorkflowService.checkIn(page.getId());
    PSHelperPerformanceMonitor.updateStats(
        "PSPageMetaDataExtractor:PageWorkflow", ((System.nanoTime() - workflowTimer) / 1_000_000));
  }

  @Override
  protected void addHtmlWidgetToTemplate(PSSiteImportCtx context)
      throws PSDataServiceException, PSSiteImportException {
    var template = unassignedTemplateCache.get(context.getSite().getName());

    if (template == null) {
      if (unassignedTemplateCache.size() > 5) {
        unassignedTemplateCache.clear();
      }

      // Load site's home page template
      template =
          templateService.load(
              catalogService.getCatalogTemplateIdBySite(context.getSite().getName()));
      // Set Theme (Only first time)
      if (isBlank(template.getTheme())) {
        template.setTheme(context.getThemeSummary().getName());
      }

      // Add the widget only if it is not already created
      if (isEmpty(template.getWidgets())) {
        // Create Raw HTML widget and add the widget to the template
        var rawHtmlWidget = PSPageManagementUtils.createRawHtmlWidgetItem("1");
        template.getRegionTree().setRegionWidgets(REGION_CONTENT, asList(rawHtmlWidget));

        context
            .getLogger()
            .appendLogMessage(
                PSLogEntryType.STATUS,
                ADD_HTML_WIDGET,
                "The HTML widget was successfully added to the Unassigned template.");

        // Save template and finish
        templateService.save(template);

        context
            .getLogger()
            .appendLogMessage(
                PSLogEntryType.STATUS,
                EXTRACT_METADATA,
                "Metadata was successfully saved to the Unassigned template.");
      }
      unassignedTemplateCache.put(context.getSite().getName(), template);
    }
  }

  @Override
  protected IPSHtmlMetadata getTargetItem(PSSiteImportCtx context)
      throws IPSDataService.DataServiceLoadException,
          PSValidationException,
          IPSDataService.DataServiceNotFoundException {
    return pageService.find(context.getCatalogedPageId());
  }

  @Override
  public String getHelperMessage() {
    return "changing cataloged page information";
  }
}
