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
package com.percussion.pagemanagement.extension;

import com.percussion.category.extension.PSCategoryControlUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.pagemanagement.service.IPSPageCategoryService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.assembly.jexl.PSDocumentUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import java.io.File;
import java.io.IOException;
import jakarta.servlet.ServletException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Document;

/**
 * Exit to generate category tree XML for a site.
 *
 * <p>Sunny Sal says: "Categories are like Bollywood families—big, nested, and always dramatic!"
 */
public class PSCategoryTreeXmlExit implements IPSResultDocumentProcessor {

  private IPSPageCategoryService pageCategoryService;

  /** Logger for this service. */
  public static final Logger log = LogManager.getLogger(PSCategoryTreeXmlExit.class);

  @Override
  public void init(IPSExtensionDef extDef, File file) {
    PSSpringWebApplicationContextUtils.injectDependencies(this);
  }

  @Override
  public boolean canModifyStyleSheet() {
    return false;
  }

  @Override
  public Document processResultDocument(
      Object[] args, IPSRequestContext requestContext, Document document)
      throws PSParameterMismatchException, PSExtensionProcessingException {
    var domWriter = new DOMWriter();
    org.dom4j.Document doc;
    try {
      var sitename = requestContext.getParameter("sitename");
      var rootpath = requestContext.getParameter("rootpath");
      var returnString =
          PSCategoryControlUtils.getCategoryXmlInString(
              PSCategoryControlUtils.getCategories(sitename, rootpath, false, true));
      returnString = returnString.replace("<topLevelNodes>", "").replace("</topLevelNodes>", "");
      doc = PSCategoryControlUtils.convertToOldFormatXml(DocumentHelper.parseText(returnString));
    } catch (PSDataServiceException | DocumentException e) {
      log.error("Failed to retrieve category xml: {}", PSExceptionUtils.getMessageForLog(e));
      throw new PSExtensionProcessingException("Failed to retrieve category xml: ", e);
    }

    try {
      return domWriter.write(doc);
    } catch (DocumentException e) {
      log.error("Failed to write category xml: {}", PSExceptionUtils.getMessageForLog(e));
      throw new PSExtensionProcessingException("Failed to write category xml: ", e);
    }
  }

  protected String getResourceUrl() throws PSDataServiceException {
    return getPageCategoryService().loadConfiguration().getTree().getUrl();
  }

  protected String loadResource(String resourceUrl) throws IOException, ServletException {
    return new PSDocumentUtils().getDocument(resourceUrl);
  }

  public IPSPageCategoryService getPageCategoryService() {
    return pageCategoryService;
  }

  public void setPageCategoryService(IPSPageCategoryService pageCategoryService) {
    this.pageCategoryService = pageCategoryService;
  }
}
