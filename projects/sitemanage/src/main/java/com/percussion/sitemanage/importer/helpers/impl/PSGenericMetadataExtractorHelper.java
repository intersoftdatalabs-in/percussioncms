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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.Validate.notNull;

import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.data.IPSHtmlMetadata;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.pagemanagement.service.impl.PSPageManagementUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract helper for extracting and applying metadata during site import.
 * Sunny Sal says: "Meta-data is like salt—don't forget to sprinkle it!"
 */
public abstract class PSGenericMetadataExtractorHelper extends PSImportHelper {

    public static final Logger log = LogManager.getLogger(PSGenericMetadataExtractorHelper.class);

    protected static final String ADD_HTML_WIDGET = "Add HTML Widget to Template";
    protected static final String EXTRACT_METADATA = "Extract Metadata";

    protected IPSTemplateService templateService;

    public PSGenericMetadataExtractorHelper(IPSTemplateService templateService) {
        this.templateService = templateService;
    }

    @Override
    public void process(PSPageContent pageContent, PSSiteImportCtx context) throws PSSiteImportException {
        startTimer();
        doExtractMetaData(pageContent, context);
        endTimer();
    }

    /**
     * Extracts metadata from the page content and applies it to the target item.
     *
     * @param pageContent The page content to extract metadata from.
     * @param context     The import context.
     * @throws PSSiteImportException if extraction fails.
     */
    public void doExtractMetaData(PSPageContent pageContent, PSSiteImportCtx context) throws PSSiteImportException {
        if (context.isCanceled()) {
            return;
        }
        try {
            notNull(context.getSite());
            notNull(pageContent.getSourceDocument());

            context.getLogger().appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                    "Beginning to extract metadata.");

            PSPageManagementUtils.extractMetadata(pageContent, context.getLogger());

            if (isBlank(pageContent.getTitle())) {
                context.getLogger().appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                        "No title could be extracted from the page.");
            }

            var targetItem = getTargetItem(context);
            setMetadataToTargetItem(pageContent, context.getLogger(), targetItem);
            saveTargetItem(targetItem);

            addHtmlWidgetToTemplate(context);
        } catch (Exception e) {
            context.getLogger().appendLogMessage(PSLogEntryType.ERROR, EXTRACT_METADATA, "Page metadata could not be extracted.");
            context.getLogger().appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                    "Page metadata could not be extracted: " + e.getMessage());
            log.error("Error extracting metadata while importing a page", e);
        }
    }

    @Override
    public void rollback(PSPageContent pageContent, PSSiteImportCtx context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Saves the target item after metadata extraction.
     *
     * @param targetItem The item to save.
     */
    protected abstract void saveTargetItem(IPSHtmlMetadata targetItem)
            throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

    /**
     * Adds an HTML widget to the template if needed.
     *
     * @param context The import context.
     */
    protected abstract void addHtmlWidgetToTemplate(PSSiteImportCtx context)
            throws PSDataServiceException, PSSiteImportException;

    /**
     * Gets the target item for metadata application.
     *
     * @param context The import context.
     * @return The target item.
     */
    protected abstract IPSHtmlMetadata getTargetItem(PSSiteImportCtx context) throws PSDataServiceException;

    /**
     * Sets the meta-data from the page content to the target item.
     *
     * @param pageContent not null
     * @param logger      not null
     * @param targetItem  not null
     */
    protected void setMetadataToTargetItem(PSPageContent pageContent, IPSSiteImportLogger logger,
            IPSHtmlMetadata targetItem) {
        notNull(pageContent);
        notNull(logger);
        notNull(targetItem);

        // Update template's metadata with pageContent extracted metadata
        if (isBlank(pageContent.getHeadContent())) {
            logger.appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                    "No head content was extracted from the page.");
        }
        targetItem.setAdditionalHeadContent(pageContent.getHeadContent());

        if (isBlank(pageContent.getAfterBodyStart())) {
            logger.appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                    "No script content after body start was extracted from the page.");
        }
        targetItem.setAfterBodyStartContent(pageContent.getAfterBodyStart());

        if (isBlank(pageContent.getBeforeBodyClose())) {
            logger.appendLogMessage(PSLogEntryType.STATUS, EXTRACT_METADATA,
                    "No script content before body close was extracted from the page.");
        }
        targetItem.setBeforeBodyCloseContent(pageContent.getBeforeBodyClose());
    }
}
