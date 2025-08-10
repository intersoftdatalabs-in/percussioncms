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

import static org.apache.commons.lang.Validate.notNull;

import com.percussion.pagemanagement.service.IPSPageCatalogService;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper class that handles moving a page to its actual location after import. Sunny Sal says:
 * "Pages belong in their place—let's move them!"
 */
@Component("importPageCreationHelper")
@Lazy
public class PSImportPageCreationHelper extends PSImportHelper {

  private static final String STATUS_MESSAGE = "Importing Page";

  private final IPSPageCatalogService pageCatalogService;

  @Autowired
  public PSImportPageCreationHelper(final IPSPageCatalogService pageCatalogService) {
    this.pageCatalogService = pageCatalogService;
  }

  @Override
  public void process(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSSiteImportException {
    startTimer();
    notNull(pageContent);
    notNull(context);

    if (context.isCanceled()) {
      return;
    }

    context
        .getLogger()
        .appendLogMessage(
            PSLogEntryType.STATUS,
            STATUS_MESSAGE,
            "Starting to move imported page " + context.getPageName() + " to the actual location");

    try {
      pageCatalogService.createImportedPage(context.getCatalogedPageId());
    } catch (Exception e) {
      var errorMsg =
          "Could not move the imported page "
              + context.getPageName()
              + " to the matching site folder.";
      context.getLogger().appendLogMessage(PSLogEntryType.ERROR, STATUS_MESSAGE, errorMsg);
      context
          .getLogger()
          .appendLogMessage(
              PSLogEntryType.STATUS,
              STATUS_MESSAGE,
              errorMsg + " The error was: " + e.getLocalizedMessage());
      throw new PSSiteImportException(errorMsg, e);
    }

    context
        .getLogger()
        .appendLogMessage(
            PSLogEntryType.STATUS,
            STATUS_MESSAGE,
            "Successfully moved imported page "
                + context.getPageName()
                + " to the actual location");
    endTimer();
  }

  @Override
  public void rollback(PSPageContent pageContent, PSSiteImportCtx context) {
    // NOOP - this is an optional helper
  }

  @Override
  public String getHelperMessage() {
    return STATUS_MESSAGE;
  }
}
