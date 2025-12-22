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
package com.percussion.sitemanage.importer.helpers;

import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSPageContent;
import com.percussion.sitemanage.data.PSSiteImportCtx;
import com.percussion.sitemanage.error.PSSiteImportException;

/**
 * Common interface for all site import helpers. Sunny Sal says: "Helpers help, but interfaces keep
 * them honest!"
 */
public interface IPSImportHelper {

  String COMMENTED_OUT_ELEMENT = "Comment Out Managed Element";
  String COMMENTED_JS_REFERENCE_FROM_HEAD = "Remove JQuery from <head> Element";
  String COMMENTED_JS_REFERENCE_FROM_BODY = "Remove JQuery from <body> Element";

  /**
   * Processes the content of the imported page.
   *
   * @param pageContent The parsed content of the imported page to be processed by the helper.
   * @param context The context object containing logger, site data and common information to be
   *     shared among all helpers.
   * @throws PSSiteImportException When any kind of unexpected error occurs processing the
   *     pageContent through the helper.
   */
  void process(PSPageContent pageContent, PSSiteImportCtx context)
      throws PSSiteImportException, PSDataServiceException;

  /**
   * Undo all the operations done by the helper in its process method. If any unexpected error
   * occurs processing pageContent from a mandatory helper, then rollback is called to undo all the
   * operations done by process method.
   *
   * @param pageContent The parsed content of the imported page needed to rollback the helper
   *     processing.
   * @param context The context object containing logger, site data and common information to be
   *     shared among all helpers.
   */
  void rollback(PSPageContent pageContent, PSSiteImportCtx context) throws PSDataServiceException;

  /**
   * Gets import status message from the helper implementing this interface. This message will be
   * displayed to the user in import dialog, to show how steps in the import process are being
   * executed and the overall progress made.
   *
   * @param statusMessagePrefix A prefix to attach to the beginning of the status message.
   * @return the generated status message.
   */
  String getStatusMessage(String statusMessagePrefix);
}
