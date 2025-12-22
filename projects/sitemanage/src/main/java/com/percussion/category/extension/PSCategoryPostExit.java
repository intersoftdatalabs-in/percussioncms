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

package com.percussion.category.extension;

import com.percussion.error.PSException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.io.StringReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PSCategoryPostExit implements IPSResultDocumentProcessor {

  public static final Logger log = LogManager.getLogger(PSCategoryPostExit.class);

  @Override
  public void init(IPSExtensionDef def, java.io.File codeRoot) {
    // No initialization required
  }

  @Override
  public boolean canModifyStyleSheet() {
    return false;
  }

  @Override
  public Document processResultDocument(
      Object[] params, IPSRequestContext request, Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException {

    Document doc = null;
    // Get data from XML, filter based on selectable and deleted attributes,
    // filter by toplevelcategory set as the control property, and return as Document.

    var siteName = request.getParameter("sitename");
    var parentCategory = request.getParameter("parentCategory");

    if (StringUtils.isBlank(parentCategory) || "root".equalsIgnoreCase(parentCategory))
      parentCategory = null;

    if ("null".equals(siteName)) siteName = null;

    try {
      var categoriesToReturn =
          PSCategoryControlUtils.getCategories(siteName, parentCategory, false, true);

      if (categoriesToReturn == null)
        throw new PSExtensionProcessingException(
            "Either none of the categories is selectable or the category XML is empty!"
                + " PSCategoryPostExit.processResultDocument()",
            new PSException());

      var returnString = PSCategoryControlUtils.getCategoryXmlInString(categoriesToReturn);
      doc = PSXmlDocumentBuilder.createXmlDocument(new StringReader(returnString.trim()), false);
    } catch (PSDataServiceException | IOException | SAXException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSExtensionProcessingException("Error converting categories to XML", e);
    }

    return doc;
  }
}
